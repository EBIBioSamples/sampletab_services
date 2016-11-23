package uk.ac.ebi.fgpt.webapp.v2;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.apache.commons.lang.math.RandomUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.biosd.sampletab.loader.Loader;
import uk.ac.ebi.fg.biosd.sampletab.persistence.Persister;
import uk.ac.ebi.fg.biosd.sampletab.persistence.Unloader;
import uk.ac.ebi.fg.core_model.persistence.dao.hibernate.toplevel.AccessibleDAO;
import uk.ac.ebi.fg.core_model.resources.Resources;
import uk.ac.ebi.utils.exceptions.ExceptionUtils;

@Service
public class RelationalDAO {

	private Logger log = LoggerFactory.getLogger(getClass());
	
	public Loader loader = new Loader();

	public Optional getObjectFromMSIOfSampleAccession(String sampleAcc, ObjectRetrieval<?> objectRetrieval) {
		//make sure we get a clean entity manager factory each time
		//this is so that we can reflect the latest data
		Resources.getInstance().reset();
		EntityManagerFactory emf = Resources.getInstance().getEntityManagerFactory();
		EntityManager em = null;
		Optional<?> toReturn = Optional.empty();
		try {
			// connect to database
			em = emf.createEntityManager();
			AccessibleDAO<BioSample> daoBioSample = new AccessibleDAO<>(BioSample.class, em);

			// get biosample
			BioSample bioSample = null;
			try {
				bioSample = daoBioSample.findAndFail(sampleAcc);
			} catch (IllegalArgumentException e) {
				// no such sample accession in database
				// therefore no accession
				toReturn = Optional.empty();
				bioSample = null;
			}

			// get submission of biosample
			if (bioSample != null) {
				Set<MSI> msis = bioSample.getMSIs();
				if (msis.size() == 1) {
					MSI msi = msis.iterator().next();
					// now we have a MSI object, we can handle it
					toReturn = objectRetrieval.retriveFrom(msi);
				} else {
					// something is wrong with the database, throw an exception
					throw new IllegalStateException("Sample " + sampleAcc + " has " + msis.size() + " MSIs");
				}
			} 
		} finally {
			if (em != null && em.isOpen()) {
				try {
					em.close();
				} catch (IllegalStateException e){
					//log it only
					log.warn("Exception closing entity manager", e);
				}
			}
		}
		return toReturn;
	}

	public Optional getObjectFromMSIOfGroupAccession(String groupAcc, ObjectRetrieval<?> objectRetrieval) {
		//make sure we get a clean entity manager factory each time
		//this is so that we can reflect the latest data
		Resources.getInstance().reset();
		EntityManagerFactory emf = Resources.getInstance().getEntityManagerFactory();
		EntityManager em = null;
		Optional<?> toReturn = Optional.empty();
		try {
			// connect to database
			em = emf.createEntityManager();
			AccessibleDAO<BioSampleGroup> daoBioSampleGroup = new AccessibleDAO<>(BioSampleGroup.class, em);

			// get biosample
			BioSampleGroup bioSampleGroup = null;
			try {
				bioSampleGroup = daoBioSampleGroup.findAndFail(groupAcc);
			} catch (IllegalArgumentException e) {
				// no such sample accession in database
				// therefore no accession
				toReturn = Optional.empty();
				bioSampleGroup = null;
			}

			// get submission of biosample
			if (bioSampleGroup != null) {
				Set<MSI> msis = bioSampleGroup.getMSIs();
				if (msis.size() == 1) {
					MSI msi = msis.iterator().next();
					// now we have a MSI object, we can handle it
					toReturn = objectRetrieval.retriveFrom(msi);
				} else {
					// something is wrong with the database, throw an exception
					throw new IllegalStateException("Group " + groupAcc + " has " + msis.size() + " MSIs");
				}
			} 
		} finally {
			if (em != null && em.isOpen()) {
				try {
					em.close();
				} catch (IllegalStateException e){
					//log it only
					log.warn("Exception closing entity manager", e);
				}
			}
		}
		return toReturn;
	}

	private interface ObjectRetrieval<T> {
		/**
		 * This is the function to handle the msi object. The wrapper function
		 * will handle creating and closing the database connection if needed.
		 * 
		 * @param msi
		 * @return
		 */
		public abstract Optional<T> retriveFrom(MSI msi);
	}

	public Optional<String> getSubmissionIDForSampleAccession(String sampleAcc) {
		return getObjectFromMSIOfSampleAccession(sampleAcc, new ObjectRetrieval<String>() {
			@Override
			public Optional<String> retriveFrom(MSI msi) {
				return Optional.of(msi.getAcc());
			}
		});
	}

	public Optional<Set<String>> getSubmissionSampleAccessions(String sampleAcc) {
		return getObjectFromMSIOfSampleAccession(sampleAcc, new ObjectRetrieval<Set<String>>() {
			@Override
			public Optional<Set<String>> retriveFrom(MSI msi) {
				Set<String> sampleAccs = new HashSet<>();
				for (BioSample sample : msi.getSamples()) {
					sampleAccs.add(sample.getAcc());
				}
				if (sampleAccs.size() == 0) return Optional.empty();
				else return Optional.of(sampleAccs);
			}
		});
	}

	public Optional<Set<String>> getSubmissionGroupAccessions(String sampleAcc) {
		return getObjectFromMSIOfSampleAccession(sampleAcc, new ObjectRetrieval<Set<String>>() {
			@Override
			public Optional<Set<String>> retriveFrom(MSI msi) {
				Set<String> groupAccs = new HashSet<>();
				for (BioSampleGroup group : msi.getSampleGroups()) {
					groupAccs.add(group.getAcc());
				}
				if (groupAccs.size() == 0) return Optional.empty();
				return Optional.of(groupAccs);
			}
		});
	}

	public Optional<String> getSubmissionIDForGroupAccession(String accession) {
		return getObjectFromMSIOfGroupAccession(accession, new ObjectRetrieval<String>() {
			@Override
			public Optional<String> retriveFrom(MSI msi) {
				return Optional.of(msi.getAcc());
			}
		});
	}
	
	public synchronized void persist(SampleData st) {
		MSI msi = loader.fromSampleData(st);
		String msiAcc = st.msi.submissionIdentifier;
		//unload previous version, if any
		for ( int attempts = 5; ; )
		{
			try 
			{			
				log.info ( "Unloading previous version of " + msiAcc + " (if any)" );
				new Unloader()
					.unload ( msi );
				log.info ( "done." );
				break;
			}
			catch ( RuntimeException aex ) {
				handleFailedPersistenceAttempt ( --attempts, aex, msiAcc );
			}
		}
		//now presist it
		for (int attempts = 5;;) {
			try {
				// only persist if there is something worth it persisting
				if (msi.getSamples().size() + msi.getSampleGroups().size() > 0) {
					log.info("Now persisting submission "+msiAcc);
					new Persister().persist(msi);
					log.info("Submission "+msiAcc+" persisted.");
				}
				break;
			} catch (RuntimeException aex) {
				handleFailedPersistenceAttempt(--attempts, aex, msiAcc);
				msi = null;
			}

		}
	}
	
	/**
	 * Deal with a failed persistence attempt, to report error messages and start another attempt.
	 * 
	 */
	private void handleFailedPersistenceAttempt( int attemptNo, RuntimeException attemptEx, String submissionAccession )
	{
		if ( attemptNo == 0 )
			throw new RuntimeException ( "Error while saving '" + submissionAccession + "': " + attemptEx.getMessage (), attemptEx );

		Throwable aex1 = ExceptionUtils.getRootCause ( attemptEx );
		if ( !(aex1 instanceof SQLException) ) throw new RuntimeException ( 
			"Error while saving '" + submissionAccession + "': " + attemptEx.getMessage (), attemptEx 
		);
		
		log.warn ( "SQL exception: {}, this is likely due to concurrency, will retry {} more times", 
			attemptEx.getMessage (), attemptNo 
		);
	}

}
