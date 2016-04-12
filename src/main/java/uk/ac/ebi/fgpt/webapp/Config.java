package uk.ac.ebi.fgpt.webapp;

import javax.naming.NamingException;
import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import uk.ac.ebi.fgpt.sampletab.Accessioner;

//@Configuration
@SpringBootApplication
public class Config {
    
	private Logger log = LoggerFactory.getLogger(getClass());

	public Config() {
	}    
	
	@Bean(name = "accessionDataSource")
    @Primary
    public DataSource getAccesionDataSource() {

		JndiDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
		DataSource dataSource = dataSourceLookup.getDataSource("java:comp/env/jdbc/accessionDB");
		return dataSource;
    }
	
    @Bean
    @Autowired
    public Accessioner getAccessioner(@Qualifier("accessionDataSource") DataSource accessionDataSource) throws NamingException {
    	return new Accessioner(accessionDataSource);
    }
    
    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        return propertySourcesPlaceholderConfigurer;
    }
}
