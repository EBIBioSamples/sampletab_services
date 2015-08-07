package uk.ac.ebi.fgpt.webapp;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jndi.JndiTemplate;

import uk.ac.ebi.fgpt.sampletab.Accessioner;

@Configuration
public class Config {

	public Config() {
		// TODO Auto-generated constructor stub
	}


	/**
	 * This is a bean backing the accessioning service connection to the database.
	 * It should only be used for startup connection validation - most of the time
	 * you should use the Accessioner from the getAccessioner bean via Spring autowiring.
	 * 
	 * @return
	 * @throws NamingException
	 */
    @Bean
    public DataSource getAccessionDataSource() throws NamingException {
        JndiTemplate jndiTemplate = new JndiTemplate();
        return (DataSource) jndiTemplate.lookup("java:comp/env/jdbc/accessionDB");
    }

    /**
     * This is the main bean for interactions with the accessioning service.
     * 
     * @return
     * @throws NamingException
     */
    @Bean
    public Accessioner getAccessioner() throws NamingException {
        //setup the accesioner data source via JNDI
        //for Tomcat, need an Resource defined in the context xml file  
        // which is the file named like the path with <Context in it
		/*
   <Resource 
      name="jdbc/accessionDB"
      type="javax.sql.DataSource"
      factory="org.apache.tomcat.dbcp.dbcp2.BasicDataSourceFactory"
      driverClassName="oracle.jdbc.driver.OracleDriver"
      url="jdbc:oracle:thin:@xxxxx.ebi.ac.uk:xxxx:XXXXX"
      username="xxxxx"
      password="xxxxx"
      />
		 */
		
    	//Use spring to get the JNDI reference
        JndiTemplate jndiTemplate = new JndiTemplate();
        DataSource dataSource = (DataSource) jndiTemplate.lookup("java:comp/env/jdbc/accessionDB");
		
		//create the datasource
    	return new Accessioner(dataSource);
    }
    
    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        return propertySourcesPlaceholderConfigurer;
    }
}
