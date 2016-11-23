package uk.ac.ebi.fgpt.webapp.v2;

import java.util.Optional;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.dao.IncorrectResultSizeDataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.support.JdbcDaoSupport;
import org.springframework.stereotype.Service;

@Service
public class SubmissionTrackDAO{
	
	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private JdbcTemplate jdbcTemplate;
	
	public SubmissionTrackDAO(){}
	
	
	// CREATE TABLE SUBMISSIONS (accession varchar(255), submission varchar(255), CONSTRAINT submissions_accessions PRIMARY KEY (accession));
	
	public Optional<String> getSubmissionForAccession(String accession) {
		try {
			return Optional.ofNullable(jdbcTemplate.queryForObject("SELECT submission FROM SUBMISSIONS WHERE accession = ?", String.class, accession));
		} catch (EmptyResultDataAccessException e) {
			return Optional.empty();
		} 
	}

	public void setSubmissionForAccession(String accession, String submission) {
		jdbcTemplate.update("INSERT INTO SUBMISSIONS VALUES ( ? , ? )", accession, submission);
	}
}
