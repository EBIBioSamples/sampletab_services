package uk.ac.ebi.fgpt.webapp.v2;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;


@Service
public class RelationalDAO {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	@Qualifier("hibernateJdbcTemplate")
	private JdbcTemplate jdbcTemplate;


	public Optional<String> getSubmissionIDForSampleAccession(String accession) {
		List<String> submissions = null;
		try {
			submissions = jdbcTemplate.queryForList(
							"SELECT MSI.ACC FROM MSI JOIN MSI_SAMPLE ON MSI.ID = MSI_SAMPLE.MSI_ID "
							+ "JOIN BIO_PRODUCT ON BIO_PRODUCT.ID = MSI_SAMPLE.SAMPLE_ID WHERE BIO_PRODUCT.ACC = ?", 
							String.class, accession);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		} 
		
		if (submissions.size() == 0) {
			return Optional.empty();
		} else if (submissions.size() > 1) {
			throw new IllegalStateException("Sample " + accession + " has " + submissions.size() + " MSIs");
		} else {
			return Optional.of(submissions.get(0));
		}
	}

	public Optional<String> getSubmissionIDForGroupAccession(String accession) {
		List<String> submissions = null;
		try {
			submissions = jdbcTemplate.queryForList(
							"SELECT MSI.ACC FROM MSI JOIN MSI_SAMPLE_GROUP ON MSI.ID = MSI_SAMPLE_GROUP.MSI_ID "
							+ "JOIN BIO_SMP_GRP ON BIO_SMP_GRP.ID = MSI_SAMPLE_GROUP.SAMPLE_ID WHERE BIO_SMP_GRP.ACC = ?", 
							String.class, accession);
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		} 

		if (submissions.size() == 0) {
			return Optional.empty();
		} else if (submissions.size() > 1) {
			throw new IllegalStateException("Group " + accession + " has " + submissions.size() + " MSIs");
		} else {
			return Optional.of(submissions.get(0));
		}
	}


	public Optional<Set<String>> getSubmissionSampleAccessions(String sampleAcc) {
		Optional<String> subId = getSubmissionIDForSampleAccession(sampleAcc);
		if (!subId.isPresent()) {
			return Optional.empty();
		}
		List<String> samples = null;
		try {
			samples = jdbcTemplate.queryForList(
					"SELECT BIO_PRODUCT.ACC FROM BIO_PRODUCT JOIN MSI_SAMPLE ON MSI_SAMPLE.SAMPLE_ID = BIO_PRODUCT.ID "
					+ " JOIN MSI ON MSI_SAMPLE.MSI_ID = MSI.ID"
					+ " WHERE MSI.ACC = ?", 
							String.class, subId.get());
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		} 		
		
		if (samples.size() == 0) {
			return Optional.empty();
		}
		
		Set<String> samplesSet = new HashSet<>(samples);
		return Optional.of(samplesSet);
	}

	public Optional<Set<String>> getSubmissionGroupAccessions(String sampleAcc) {
		Optional<String> subId = getSubmissionIDForSampleAccession(sampleAcc);
		if (!subId.isPresent()) {
			return Optional.empty();
		}
		List<String> samples = null;
		try {
			samples = jdbcTemplate.queryForList(
					"SELECT BIO_SMP_GRP.ACC FROM BIO_SMP_GRP JOIN MSI_SAMPLE_GROUP ON MSI_SAMPLE_GROUP.GROUP_ID = BIO_SMP_GRP.ID "
					+ " JOIN MSI ON  MSI_SAMPLE_GROUP.MSI_ID = MSI.ID "
					+ " WHERE MSI.ACC = ? ", 
							String.class, subId.get());
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		} 		

		if (samples.size() == 0) {
			return Optional.empty();
		}
		
		Set<String> samplesSet = new HashSet<>(samples);
		return Optional.of(samplesSet);
	}

}
