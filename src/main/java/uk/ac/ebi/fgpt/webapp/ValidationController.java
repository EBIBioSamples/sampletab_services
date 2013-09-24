package uk.ac.ebi.fgpt.webapp;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.mged.magetab.error.ErrorItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.listener.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.arrayexpress2.sampletab.parser.SampleTabParser;
import uk.ac.ebi.arrayexpress2.sampletab.validator.SampleTabValidator;

/**
 * A spring controller that returns an accessioned version of a POSTed SampleTab
 *
 * @author Adam Faulconbridge
 * @date 02/05/12
 */
@Controller
@RequestMapping
public class ValidationController {
        
    private Logger log = LoggerFactory.getLogger(getClass());
                
    @RequestMapping(value = "/v1/json/va", method = RequestMethod.POST)
    public @ResponseBody Outcome doValidation(@RequestBody SampleTabRequest sampletab) {
        //setup parser to listen for errors
        SampleTabParser<SampleData> parser = new SampleTabParser<SampleData>(new SampleTabValidator());
        
        final List<ErrorItem> errorItems;
        errorItems = new ArrayList<ErrorItem>();
        parser.addErrorItemListener(new ErrorItemListener() {
            public void errorOccurred(ErrorItem item) {
                errorItems.add(item);
            }
        });
         
        try {
            //convert json object to string
            String singleString = sampletab.asSingleString();
            
            //setup the string as an input stream
            InputStream is = new ByteArrayInputStream(singleString.getBytes("UTF-8"));
            
            //parse the input into sampletab
            //will also validate
            SampleData sampledata = parser.parse(is);
            
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
