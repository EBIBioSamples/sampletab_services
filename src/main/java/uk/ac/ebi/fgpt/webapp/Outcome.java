package uk.ac.ebi.fgpt.webapp;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.mged.magetab.error.ErrorItem;

import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.arrayexpress2.sampletab.renderer.SampleTabWriter;

public class Outcome {

    private List<Map<String,String>> errors;
    private List<List<String>> sampletab;
    
    /**
     * Default constructor to allow deserialization of JSON into a request bean: present to allow Jackson/spring to
     * construct a request bean from POST requests properly.
     */
    public Outcome() {
        
    }
    
    public Outcome(List<List<String>> sampletab, List<Map<String,String>> errors) {
        setSampletab(sampletab);
        setErrors(errors);
    }
    
    public Outcome(SampleData sampledata, Collection<ErrorItem> errorItems) {
        
        List<Map<String,String>> errorList = new ArrayList<Map<String,String>>();
        for (ErrorItem errorItem : errorItems){
            Map<String, String> errorMap = new HashMap<String, String>();
            errorMap.put("type", errorItem.getErrorType());
            errorMap.put("code", new Integer(errorItem.getErrorCode()).toString());
            errorMap.put("line", new Integer(errorItem.getLine()).toString());
            errorMap.put("col", new Integer(errorItem.getCol()).toString());
            errorMap.put("message", errorItem.getMesg());
            errorMap.put("comment", errorItem.getComment());
        }
        setErrors(errorList);
        
        if (sampledata == null) {
            //no nothing
        } else {
            //write the sampledata out to a string
            //then split that string into cells and store
            StringWriter sw = new StringWriter();
            SampleTabWriter stw = new SampleTabWriter(sw);
            try {
                stw.write(sampledata);
            } catch (IOException e) {
                e.printStackTrace();
            }
            String sampleTabString = sw.toString();
            List<List<String>> sampleTabListList = new ArrayList<List<String>>();
            for (String line : sampleTabString.split("\n")){
                List<String> lineList = new ArrayList<String>();
                for (String cell : line.split("\t")){
                    lineList.add(cell);
                }
                sampleTabListList.add(lineList);
            }
            setSampletab(sampleTabListList);
        }
    }

    public List<List<String>> getSampletab() {
        return sampletab;
    }
    
    public void setSampletab(List<List<String>> sampletab) {
        this.sampletab = sampletab;
    }
    
    public List<Map<String, String>> getErrors() {
        return errors;
    }
    
    public void setErrors(List<Map<String, String>> errors) {
        this.errors = errors;
    }
    
}
