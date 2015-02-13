package uk.ac.ebi.fgpt.webapp.v1;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mged.magetab.error.ErrorItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;

import uk.ac.ebi.arrayexpress2.magetab.datamodel.graph.Node;
import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.magetab.listener.ErrorItemListener;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.GroupNode;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.SampleNode;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.DerivedFromAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.parser.SampleTabParser;
import uk.ac.ebi.arrayexpress2.sampletab.renderer.SampleTabWriter;
import uk.ac.ebi.fgpt.sampletab.AccessionerENA;
import uk.ac.ebi.fgpt.sampletab.Corrector;
import uk.ac.ebi.fgpt.sampletab.utils.SampleTabUtils;
import uk.ac.ebi.fgpt.webapp.APIKey;

/**
 * A spring controller that returns an accessioned version of a POSTed SampleTab
 *
 * @author Adam Faulconbridge
 * @date 02/05/12
 */
@Controller
@RequestMapping
public class SubmissionController {
        
    private Logger log = LoggerFactory.getLogger(getClass());
                
    private final File path;
    private AccessionerENA accessioner = null;

    private String host;
    private Integer port;
    private String database;
    private String username;
    private String password;
    
    
    private Corrector corrector;
    
    public SubmissionController() {
        Properties properties = new Properties();
        try {
            InputStream is = SubmissionController.class.getResourceAsStream("/sampletab.properties");
            properties.load(is);
        } catch (IOException e) {
            log.error("Unable to read resource properties", e);
            path = null;
            return;
        }
        path = new File(properties.getProperty("submissionpath"));
        if (!path.exists()){
            //TODO throw error
            log.error("Submission path "+path+" does not exist");
        }
        
        properties = new Properties();
        try {
            InputStream is = AccessionerController.class.getResourceAsStream("/oracle.properties");
            properties.load(is);
        } catch (IOException e) {
            log.error("Unable to read resource properties", e);
            return;
        }
        
        host = properties.getProperty("hostname");
        port = new Integer(properties.getProperty("port"));
        database = properties.getProperty("database");
        username = properties.getProperty("username");
        password = properties.getProperty("password");
                
        corrector = new Corrector();
    }
    
    
    private synchronized int getNewSubID() throws IOException{
        int maxSubID = 0;
        Pattern pattern = Pattern.compile("^GSB-([0-9]+)$");
        File pathSubdir = new File(path, "GSB");
        for (File subdir : pathSubdir.listFiles()) {
            if (!subdir.isDirectory()) {
                continue;
            } else {
                log.info("Looking at subid "+subdir.getName()+" with pattern "+pattern.pattern());
                Matcher match = pattern.matcher(subdir.getName());
                if (match.matches()) {
                    log.info("Found match with "+match.groupCount()+" groups "+match.group(1));
                    Integer subid = new Integer(match.group(1));
                    if (subid > maxSubID) {
                        maxSubID = subid;
                    }
                }
            }
        }
        maxSubID++;
        File subDir = new File(path.getAbsolutePath(), SampleTabUtils.getSubmissionDirFile("GSB-"+maxSubID).toString());
        if (!subDir.mkdirs()) {
            throw new IOException("Unable to create submission directory");
        }
        //can't do linux-specific group writing, so make all writable.
        //won't be too bad, since the parent directory should not be world writable
        subDir.setWritable(true, false);
        
        return maxSubID;
        
    }
    
    private static Outcome getErrorOutcome(String message, String comment) {
        Outcome o = new Outcome();
        List<Map<String,String>> errorList = new ArrayList<Map<String,String>>();
        Map<String, String> errorMap = new HashMap<String, String>();
        //errorMap.put("type", errorItem.getErrorType());
        //errorMap.put("code", new Integer(errorItem.getErrorCode()).toString());
        //errorMap.put("line", new Integer(errorItem.getLine()).toString());
        //errorMap.put("col", new Integer(errorItem.getCol()).toString());
        errorMap.put("message", message);
        errorMap.put("comment", comment);
        errorList.add(errorMap);
        o.setErrors(errorList);
        return o;
        
    }
    
    
    @RequestMapping(value = "/v1/json/sb", method = RequestMethod.POST)
    public @ResponseBody Outcome doSubmission(@RequestBody SampleTabRequest sampletab, String apikey) {
        boolean isSRA = false;
        boolean isCGAP = false;
        
        String keyOwner;
        try { 
            keyOwner = APIKey.getAPIKeyOwner(apikey);
        } catch (IllegalArgumentException e) {
            //invalid API key, return errors
            return getErrorOutcome("Invalid API key ("+apikey+")", "Contact biosamples@ebi.ac.uk for assistance");
        }
        
        isSRA = "ENA".equals(keyOwner);
        isCGAP = "CGAP".equals(keyOwner);
        
        // setup an overall try/catch to catch and report all errors
        try {
            //setup parser to listen for errors
            SampleTabParser<SampleData> parser = new SampleTabParser<SampleData>();
            
            final List<ErrorItem> errorItems;
            errorItems = new ArrayList<ErrorItem>();
            parser.addErrorItemListener(new ErrorItemListener() {
                public void errorOccurred(ErrorItem item) {
                    errorItems.add(item);
                }
            });
            SampleData sampledata = null;
            //convert json object to string
            String singleString = sampletab.asSingleString();
            
            //setup the string as an input stream
            InputStream is = new ByteArrayInputStream(singleString.getBytes("UTF-8"));
             
            try {
                //parse the input into sampletab
                //will also validate
                sampledata = parser.parse(is);
            } catch (ParseException e) {
                //catch parsing errors for malformed submissions
                log.error("parsing error", e);
                return new Outcome(null, e.getErrorItems());
            } 
            
            //look at submission id
            if (isSRA) {
                //extra validation for SRA
                if (sampledata.msi.submissionIdentifier == null || !sampledata.msi.submissionIdentifier.matches("^GEN-[ERD]R[AP][0-9]+$")) {
                    return getErrorOutcome("Submission identifier invalid", "SRA submission identifier must match regular expression ^GEN-[SED]R[AP][0-9]+$");
                }         
            } else if (isCGAP) {
                //extra validation for HipScip
                if (sampledata.msi.submissionIdentifier == null || !sampledata.msi.submissionIdentifier.matches("^GCG-HipSci$")) {
                    return getErrorOutcome("Submission identifier invalid", "Submission identifier must match GCG-HipSci");
                }         
                //do some re-routing
                sampledata.msi.submissionIdentifier = "GSB-3";
            } else {
                //must be a GSB submission ID
                if (sampledata.msi.submissionIdentifier == null || sampledata.msi.submissionIdentifier.length() == 0) {
                    sampledata.msi.submissionIdentifier = "GSB-"+getNewSubID();
                } else if (!sampledata.msi.submissionIdentifier.matches("^GSB-[1-9][0-9]*$")) {
                    return getErrorOutcome("Submission identifier invalid", "Submission identifier must match regular expression ^GSB-[1-9][0-9]*$");
                }
            }
            File subdir = SampleTabUtils.getSubmissionDirFile(sampledata.msi.submissionIdentifier);
            subdir = new File(path.toString(), subdir.toString());
            File outFile = new File(subdir, "sampletab.pre.txt");

            //replace implicit derived from with explicit derived from relationships
            for (SampleNode sample : sampledata.scd.getNodes(SampleNode.class)) {
                if (sample.getParentNodes().size() > 0) {
                    for (Node parent : new HashSet<Node>(sample.getParentNodes())) {
                        if (SampleNode.class.isInstance(parent)) {
                            SampleNode parentsample = (SampleNode) parent;
                            DerivedFromAttribute attr = new DerivedFromAttribute(parentsample.getSampleAccession());
                            sample.addAttribute(attr);
                            sample.removeParentNode(parentsample);
                            parentsample.removeChildNode(sample);
                        }
                    }
                }
            }
            
            //create a new group and add all non-grouped samples to it
            GroupNode othergroup = new GroupNode("Other Group");
            for (SampleNode sample : sampledata.scd.getNodes(SampleNode.class)) {
                // check there is not an existing group first...
                boolean sampleInGroup = false;
                //even if it has child nodes, both parent and child must be in a group
                //this will lead to some weird looking row duplications, but since this is an internal 
                //intermediate file it is not important
                //Follow up: since implicit derived from relationships are made explicit above, 
                //this is not an issue any more
                for (Node n : sample.getChildNodes()) {
                   if (GroupNode.class.isInstance(n)) {
                        sampleInGroup = true;
                    }
                }
                
                if (!sampleInGroup){
                    log.debug("Adding sample " + sample.getNodeName() + " to group " + othergroup.getNodeName());
                    othergroup.addSample(sample);
                }
            }
            //only add the new group if it has any samples
            if (othergroup.getParentNodes().size() > 0){
                sampledata.scd.addNode(othergroup);
                log.info("Added Other group node");
                // also need to accession the new node
            }
            
            //correct errors
            synchronized(corrector) {
                corrector.correct(sampledata);
            }
            
            //assign accessions to sampletab object
            synchronized(this) {
                accessioner = new AccessionerENA(host, port, database, username, password);
                sampledata = accessioner.convert(sampledata);
                accessioner.close();
                accessioner = null;
            }
            
            SampleTabWriter writer = null;
            try {
                if (!subdir.exists() && !subdir.mkdirs()) {
                    throw new IOException("Unable to create parent directories");
                }
                writer = new SampleTabWriter(new BufferedWriter(new FileWriter(outFile)));
                writer.write(sampledata);
            } catch (IOException e) {
                log.error("Problem writing to "+outFile, e);
                return getErrorOutcome("Unable to store submission", "Retry later or contact biosamples@ebi.ac.uk if this error persists");
            } finally {
                if (writer != null) {
                    try {
                        writer.close();
                    } catch (IOException e) {
                        //do nothing
                    }
                }
            }
            
            //return the submitted file, and any generated errors            
            return new Outcome(sampledata, errorItems);
            
        } catch (Exception e) {
            //general catch all for other errors, e.g SQL, IO
            log.error("Unrecognized error", e);
            return getErrorOutcome("Unknown error", "Contact biosamples@ebi.ac.uk for assistance");
        } 
    }
    
    
}
