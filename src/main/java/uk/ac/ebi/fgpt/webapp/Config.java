package uk.ac.ebi.fgpt.webapp;

import javax.sql.DataSource;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.lookup.JndiDataSourceLookup;
import uk.ac.ebi.fgpt.sampletab.Accessioner;

@SpringBootApplication
public class Config {
    
	private Logger log = LoggerFactory.getLogger(getClass());

	public Config() {}
	
		
	@Bean(name="accessionJdbcTemplate")
	public JdbcTemplate getJdbcTemplate() {
		JndiDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
		DataSource accessionDataSource = dataSourceLookup.getDataSource("java:comp/env/jdbc/accessionDB");
		return new JdbcTemplate(accessionDataSource);		
	}
	
	@Bean(name="hibernateJdbcTemplate")
	public JdbcTemplate getHibernateJdbcTemplate() {
		JndiDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
		DataSource accessionDataSource = dataSourceLookup.getDataSource("java:comp/env/jdbc/hibernateDB");
		return new JdbcTemplate(accessionDataSource);		
	}
		
	
	@Bean
    public Accessioner getAccessioner() {
		JndiDataSourceLookup dataSourceLookup = new JndiDataSourceLookup();
		DataSource accessionDataSource = dataSourceLookup.getDataSource("java:comp/env/jdbc/accessionDB");
    	return new Accessioner(accessionDataSource);
    }
    
    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer = new PropertySourcesPlaceholderConfigurer();
        return propertySourcesPlaceholderConfigurer;
    }
}
