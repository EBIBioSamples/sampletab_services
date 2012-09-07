package uk.ac.ebi.fgpt.webapp;

import java.util.List;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.arrayexpress2.sampletab.parser.SampleTabSaferParser;


/**
 * A simple wrapper object that encapsulates a SampleTab document as a list of lists of strings.
 *
 * @author Adam Faulconbridge
 * @date 07/08/12
 */
public class SampleTabRequest {
    private List<List<String>> sampletab;

    /**
     * Default constructor to allow deserialization of JSON into a request bean: present to allow Jackson/spring to
     * construct a request bean from POST requests properly.
     */
    @SuppressWarnings("unused")
    private SampleTabRequest() {
    }

    public SampleTabRequest(List<List<String>> sampletab) {
        this.sampletab = sampletab;
    }

    public List<List<String>> getSampleTab() {
        return sampletab;
    }

    public void setSampletab(List<List<String>> sampletab) {
        this.sampletab = sampletab;
    }

    //internal function for combining the list of lists into a tab/newline separated string
    public String asSingleString() {
        StringBuilder sb = new StringBuilder();
        boolean firstLine = true;
        for (List<String> line : sampletab){
            if (!firstLine){
                sb.append("\n");
            }
            
            boolean firstCell = true;
            for(String cell : line){
                if (!firstCell){
                    sb.append("\t");
                }
                sb.append(cell);
                firstCell = false;
            }
            
            firstLine = false;
        }
        return sb.toString();
    }
    
    public SampleData asSampleData() throws ParseException {
        String singleString = asSingleString();
        SampleTabSaferParser parser = new SampleTabSaferParser();
        return parser.parse(singleString);
    }
}
