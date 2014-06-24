package uk.ac.ebi.fgpt.webapp;

import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import uk.ac.ebi.fgpt.sampletab.Accessioner;
import uk.ac.ebi.fgpt.sampletab.AccessionerENA;

@Controller
@RequestMapping("/v2")
public class RestfulController {
    
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private Accessioner accessioner = null;
    
    private Logger log = LoggerFactory.getLogger(getClass());
    
    public RestfulController() {
        Properties properties = new Properties();
        try {
            InputStream is = AccessionerController.class.getResourceAsStream("/oracle.properties");
            properties.load(is);
        } catch (IOException e) {
            log.error("Unable to read resource properties", e);
            this.host = null;
            this.port = -1;
            this.database = null;
            this.username = null;
            this.password = null;
            return;
        }
        this.host = properties.getProperty("hostname");
        this.port = new Integer(properties.getProperty("port"));
        this.database = properties.getProperty("database");
        this.username = properties.getProperty("username");
        this.password = properties.getProperty("password");
    }
    
    protected Accessioner getAccessioner() {
        if (accessioner == null) {
            accessioner = new Accessioner(host, port, database, username, password);
        }
        return accessioner;
    }
    
    @RequestMapping(value="/{source}/sample/{sourceid}", method=RequestMethod.PUT)
    public @ResponseBody String createAccession(@PathVariable String source, @PathVariable String sourceid, @RequestParam String apikey) 
        throws SQLException, ClassNotFoundException {
        String newAccession = getAccessioner().singleAssaySample(sourceid, source);

        String keyOwner = APIKey.getAPIKeyOwner(apikey);
        //TODO handle wrong api keys better
        
        if (!APIKey.canKeyOwnerEditSource(keyOwner, source)) {
            //TODO handle invalid key better
            throw new IllegalArgumentException("apikey is not permitted for source");
        }
        
        return newAccession;
    }
    
    @RequestMapping(value="/{source}/sample", method=RequestMethod.POST, produces="text/plain")
    public @ResponseBody String createAccession(@PathVariable String source, @RequestParam String apikey) 
        throws SQLException, ClassNotFoundException {
        String newAccession = getAccessioner().singleAssaySample(source);

        String keyOwner = APIKey.getAPIKeyOwner(apikey);
        //TODO handle wrong api keys better
        
        if (!APIKey.canKeyOwnerEditSource(keyOwner, source)) {
            //TODO handle invalid key better
            throw new IllegalArgumentException("apikey is not permitted for source");
        }
        
        return newAccession;
    }
}
