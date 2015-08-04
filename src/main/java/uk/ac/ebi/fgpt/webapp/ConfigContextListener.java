package uk.ac.ebi.fgpt.webapp;

import java.sql.Connection;
import java.sql.SQLException;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;

import uk.ac.ebi.fgpt.sampletab.Accessioner;

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
public class ConfigContextListener implements ServletContextListener {

	
	@Autowired
	private DataSource accessionDataSource;
	
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
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
	}
	

}
