package uk.ac.ebi.fgpt.webapp.v1;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.sql.SQLException;
import java.sql.SQLRecoverableException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletResponse;
import javax.sql.DataSource;

import org.mged.magetab.error.ErrorItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.jndi.JndiTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.listener.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.arrayexpress2.sampletab.parser.SampleTabParser;
import uk.ac.ebi.fgpt.sampletab.Accessioner;
import uk.ac.ebi.fgpt.sampletab.CorrectorAddAttr;

/**
 * A spring controller that returns an accessioned version of a POSTed SampleTab
 * 
 * @author Adam Faulconbridge
 * @date 02/05/12
 */
@Controller
@RequestMapping
public class AccessionerController {
    
	@Autowired
	private Accessioner accessioner;
	
    private Logger log = LoggerFactory.getLogger(getClass());
    
    public AccessionerController() {
    	
    }
    
    /*
     * Echoing function. Used for triggering download of javascript
     * processed sampletab files. No way to download a javascript string
     * directly from memory, so it is bounced back off the server through
     * this method.
     */
    @RequestMapping(value = "/echo", method = RequestMethod.POST)
    public void echo(String input, HttpServletResponse response) throws IOException {
        //set it to be marked as a download file
        //response.setContentType("application/octet-stream");
        response.setContentType("application/force-download; charset=UTF-8");
        //set the filename to download it as
        response.addHeader("Content-Disposition","attachment; filename=\"sampletab.txt\"");
        response.setHeader("Content-Transfer-Encoding", "binary");

        //writer to the output stream
        //let springs default error handling take over and redirect on error.
        Writer out = null; 
        try {
            out = new OutputStreamWriter(response.getOutputStream(), "UTF-8");
            out.write(input);
        } finally {
            if (out != null){
                try {
                    out.close();
                    response.flushBuffer();
                } catch (IOException e) {
                    //do nothing
                }
            }
        }
        
    }
        
    //old URL mapping for backwards compatability
    @RequestMapping(value = "/jsac", method = RequestMethod.POST) 
    public @ResponseBody Outcome doAccessionOld(@RequestBody SampleTabRequest sampletab) {
        return doAccession(sampletab);
    }
        
    @RequestMapping(value = "/v1/json/ac", method = RequestMethod.POST)
    public @ResponseBody Outcome doAccession(@RequestBody SampleTabRequest sampletab) {
        //setup parser to listen for errors
        SampleTabParser<SampleData> parser = new SampleTabParser<SampleData>();
        
        final List<ErrorItem> errorItems;
        errorItems = new ArrayList<ErrorItem>();
        parser.addErrorItemListener(new ErrorItemListener() {
            public void errorOccurred(ErrorItem item) {
                errorItems.add(item);
            }
        });
         
        
        Outcome outcome = null;
        try {
            //convert json object to string
            String singleString = sampletab.asSingleString();
            
            //setup the string as an input stream
            InputStream is = new ByteArrayInputStream(singleString.getBytes("UTF-8"));
            
            //parse the input into sampletab
            SampleData sampledata = parser.parse(is);
            //some corrections for hipsci
            if (sampledata.msi.submissionIdentifier.equals("GCG-HipSci")) {
                sampledata.msi.submissionIdentifier = "GSB-3";
            }

            //assign accessions to sampletab object
            sampledata = accessioner.convert(sampledata);
            
            //return the accessioned file, and any generated errors            
            outcome = new Outcome(sampledata, errorItems);
            
        } catch (ParseException e) {
            //catch parsing errors for malformed submissions
            log.error(e.getMessage(), e);
            outcome = new Outcome(null, e.getErrorItems());
        } catch (UnsupportedEncodingException e) {
            //catch parsing errors for malformed submissions
        	//these errors should never happen
            log.error(e.getMessage(), e);
            Map<String, String> error;
            List<Map<String,String>> errors = new ArrayList<Map<String,String>>();
            error = new HashMap<String, String>();
            error.put("type", e.getClass().getName());
            error.put("message", e.getLocalizedMessage());
            errors.add(error);
            outcome = new Outcome(null, errors);
        } catch (Exception e) {
            //general catch all for other errors, e.g SQL
            log.error(e.getMessage(), e);
            List<Map<String,String>> errors = new ArrayList<Map<String,String>>();
            Map<String, String> error = new HashMap<String, String>();
            error.put("type", e.getClass().getName());
            error.put("message", e.getLocalizedMessage());
            errors.add(error);
            outcome = new Outcome(null, errors);
        } 
        return outcome;
    }
}
