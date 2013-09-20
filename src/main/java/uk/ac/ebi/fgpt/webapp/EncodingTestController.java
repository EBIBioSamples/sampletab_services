package uk.ac.ebi.fgpt.webapp;

import org.mged.magetab.error.ErrorItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.listener.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.magetab.validator.Validator;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.arrayexpress2.sampletab.parser.SampleTabParser;
import uk.ac.ebi.arrayexpress2.sampletab.validator.SampleTabValidator;
import uk.ac.ebi.fgpt.sampletab.Accessioner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.servlet.http.HttpServletResponse;

/**
 * A spring controller for the purposes of testing encoding problems
 *
 * @author Adam Faulconbridge
 * @date 20/09/13
 */
@Controller
@RequestMapping
public class EncodingTestController {
        
    private Logger log = LoggerFactory.getLogger(getClass());
                
    @RequestMapping(value = "/v1/json/en", method = RequestMethod.POST)
    public @ResponseBody Outcome doValidation(@RequestBody SampleTabRequest sampletab) {
        
         
        try {
            
            //for GSB-2 (mosquitoes) the description has lots of UTF-8 characters
            log.info("Test 1");
            log.info(sampletab.getSampleTab().get(3).get(1));
            
            //setup parser to listen for errors
            SampleTabParser<SampleData> parser = new SampleTabParser<SampleData>(new SampleTabValidator());
            
            final List<ErrorItem> errorItems;
            errorItems = new ArrayList<ErrorItem>();
            parser.addErrorItemListener(new ErrorItemListener() {
                public void errorOccurred(ErrorItem item) {
                    errorItems.add(item);
                }
            });
            
            //convert json object to string
            String singleString = sampletab.asSingleString();
            
            //setup the string as an input stream
            InputStream is = new ByteArrayInputStream(singleString.getBytes("UTF-8"));
            
            //parse the input into sampletab
            //will also validate
            SampleData sampledata = parser.parse(is);

            log.info("Test 1");
            log.info(sampledata.msi.submissionDescription);
            
            
            //return the accessioned file, and any generated errors            
            return new Outcome(sampledata, errorItems);
            
        } catch (ParseException e) {
            //catch parsing errors for malformed submissions
            log.error(e.getMessage());
            return new Outcome(null, e.getErrorItems());
        } catch (Exception e) {
            //general catch all for other errors, e.g SQL
            log.error(e.getMessage());
            return new Outcome();
        } 
    }
    
    
}
