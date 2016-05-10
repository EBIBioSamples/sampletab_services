package uk.ac.ebi.fgpt.webapp.v2;

import java.sql.SQLException;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.core_model.persistence.dao.hibernate.toplevel.AccessibleDAO;
import uk.ac.ebi.fg.core_model.resources.Resources;

@Service
public class RelationalDAO {

	private Logger log = LoggerFactory.getLogger(getClass());

	public Optional getObjectFromMSIOfSampleAccession(String sampleAcc, ObjectRetrieval<?> objectRetrieval) {
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
				return Optional.of(sampleAccs);
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

}
