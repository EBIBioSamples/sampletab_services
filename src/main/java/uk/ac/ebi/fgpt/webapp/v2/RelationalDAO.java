package uk.ac.ebi.fgpt.webapp.v2;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import javax.persistence.EntityManager;

import org.springframework.stereotype.Service;

import uk.ac.ebi.fg.biosd.model.expgraph.BioSample;
import uk.ac.ebi.fg.biosd.model.organizational.BioSampleGroup;
import uk.ac.ebi.fg.biosd.model.organizational.MSI;
import uk.ac.ebi.fg.core_model.persistence.dao.hibernate.toplevel.AccessibleDAO;
import uk.ac.ebi.fg.core_model.resources.Resources;

@Service
public class RelationalDAO {

	public Optional getObjectFromMSIOfSampleAccession(String sampleAcc, ObjectRetrieval objectRetrieval) {
		EntityManager em = null;
		try {
			// connect to database
			em = Resources.getInstance().getEntityManagerFactory().createEntityManager();
			AccessibleDAO<BioSample> daoBioSample = new AccessibleDAO<>(BioSample.class, em);

			// get biosample
			BioSample bioSample = null;
			try {
				bioSample = daoBioSample.findAndFail(sampleAcc);
			} catch (IllegalArgumentException e) {
				// no such sample accession in database
				// therefore no accession
				return Optional.empty();
			}

			// get submission of biosample
			if (bioSample != null) {
				Set<MSI> msis = bioSample.getMSIs();
				if (msis.size() == 1) {
					MSI msi = msis.iterator().next();
					// now we have a MSI object, we can handle it
					return Optional.ofNullable(objectRetrieval.retriveFrom(msi));
				} else {
					// something is wrong with the database, throw an exception
					throw new IllegalStateException("Sample " + sampleAcc + " has " + msis.size() + " MSIs");
				}
			} else {
				// should never get here...
				throw new IllegalStateException("bioSample cannot be null");
			}
		} finally {
			if (em != null && em.isOpen()) {
				em.close();
			}
		}
	}

	public Optional getObjectFromMSIOfGroupAccession(String groupAcc, ObjectRetrieval objectRetrieval) {
		EntityManager em = null;
		try {
			// connect to database
			em = Resources.getInstance().getEntityManagerFactory().createEntityManager();
			AccessibleDAO<BioSampleGroup> daoBioSampleGroup = new AccessibleDAO<>(BioSampleGroup.class, em);

			// get biosample
			BioSampleGroup bioSampleGroup = null;
			try {
				bioSampleGroup = daoBioSampleGroup.findAndFail(groupAcc);
			} catch (IllegalArgumentException e) {
				// no such sample accession in database
				// therefore no accession
				return Optional.empty();
			}

			// get submission of biosample
			if (bioSampleGroup != null) {
				Set<MSI> msis = bioSampleGroup.getMSIs();
				if (msis.size() == 1) {
					MSI msi = msis.iterator().next();
					// now we have a MSI object, we can handle it
					return Optional.ofNullable(objectRetrieval.retriveFrom(msi));
				} else {
					// something is wrong with the database, throw an exception
					throw new IllegalStateException("Sample " + groupAcc + " has " + msis.size() + " MSIs");
				}
			} else {
				// should never get here...
				throw new IllegalStateException("bioSample cannot be null");
			}
		} finally {
			if (em != null && em.isOpen()) {
				em.close();
			}
		}
	}

	private interface ObjectRetrieval {
		/**
		 * This is the function to handle the msi object. The wrapper function
		 * will handle creating and closing the database connection if needed.
		 * 
		 * @param msi
		 * @return
		 */
		public abstract Optional retriveFrom(MSI msi);
	}

	public Optional<String> getSubmissionIDForSampleAccession(String sampleAcc) {
		return getObjectFromMSIOfSampleAccession(sampleAcc, new ObjectRetrieval() {
			@Override
			public Optional<String> retriveFrom(MSI msi) {
				return Optional.of(msi.getAcc());
			}
		});
	}

	public Optional<Set<String>> getSubmissionSampleAccessions(String sampleAcc) {
		return getObjectFromMSIOfSampleAccession(sampleAcc, new ObjectRetrieval() {
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
		return getObjectFromMSIOfSampleAccession(sampleAcc, new ObjectRetrieval() {
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
		return getObjectFromMSIOfGroupAccession(accession, new ObjectRetrieval() {
			@Override
			public Optional<String> retriveFrom(MSI msi) {
				return Optional.of(msi.getAcc());
			}
		});
	}

}
