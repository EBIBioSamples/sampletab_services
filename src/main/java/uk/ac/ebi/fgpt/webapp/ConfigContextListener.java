package uk.ac.ebi.fgpt.webapp;

import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Listens to startup of the web app and tests database and other
 * connections.
 * 
 * If any of the connections fail tests (e.g. missing or wrong JNDI config) then
 * the webapp should fail to start
 * 
 * @author faulcon
 *
 */
@WebListener
public class ConfigContextListener implements ServletContextListener {

    private Logger log = LoggerFactory.getLogger(getClass());
    	
	@Autowired
	private DataSource accessionDataSource;
	
    @Value("${submissionpath}") //this is read from the context xml Parameter element
    private String submissionPath;
	
	public ConfigContextListener() {
		
	}

	@Override
	public void contextInitialized(ServletContextEvent event) {
		
		//test the connection to the accessioning database
		//this will usually have been defined in the Context.xml via JNDI
		//and be autowired by spring
		Connection c = null;
		try {
			c = accessionDataSource.getConnection();
			if (!c.isValid(10)) {
				log.error("Unable to validate database connection");
				System.out.println("Unable to validate database connection");
				throw new RuntimeException("Unable to validate database connection");
			}
		} catch (SQLException e) {
			throw new RuntimeException(e);
		} finally {
			if (c != null) {
				try {
					c.close();
				} catch (SQLException e) {
					//do nothing
				}
			}
		}
		
		//test the submission path is valid and correct
    	File path = new File(submissionPath);
    	path = path.getAbsoluteFile();
    	
    	if (path == null || !path.exists() || !path.isDirectory()) {
    		log.error("SampletabProperties path is not valid");
			System.out.println("SampletabProperties path is not valid");
    		throw new RuntimeException("SampletabProperties path is not valid");
    	}
    	
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
	}
	

}
