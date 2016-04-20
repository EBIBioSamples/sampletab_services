package uk.ac.ebi.fgpt.webapp.v1;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mged.magetab.error.ErrorItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import uk.ac.ebi.fgpt.sampletab.Accessioner;
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
                    
    
    @Autowired
    private APIKey apiKey;
    
    @Autowired
    private Accessioner accessioner;
    
    @Value("${submissionpath}") //this is read from the context xml Parameter element
    private String submissionPath;
    
    private Corrector corrector;
    
    public SubmissionController()  {        
        corrector = new Corrector();
    }
    
    protected File getSubmissionPath() {
    	File path = new File(submissionPath);
    	path = path.getAbsoluteFile();
    	return path;
    }
    
    
    private synchronized int getNewSubID() throws IOException{
        int maxSubID = 0;
        Pattern pattern = Pattern.compile("^GSB-([0-9]+)$");
        File pathSubdir = new File(getSubmissionPath(), "GSB");
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
        File subDir = new File(getSubmissionPath(), SampleTabUtils.getSubmissionDirFile("GSB-"+maxSubID).toString());
        if (!subDir.mkdirs()) {
            throw new IOException("Unable to create submission directory");
        }
        //can't do linux-specific group writing, so make all writable.
        //won't be too bad, since the parent directory should not be world writable
        subDir.setWritable(true, false);
        
        return maxSubID;
        
    }
    
    protected static Outcome getErrorOutcome(String message, String comment) {
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
            keyOwner = apiKey.getAPIKeyOwner(apikey);
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
                	//if there is no sub ID and no samples/groups, then its an error
                	if (sampledata.scd.getAllNodes().size() == 0) {
                		return getErrorOutcome("Submission identifier invalid", "Must update existing submission Identifier, or specify samples/groups");
                	}
                    sampledata.msi.submissionIdentifier = "GSB-"+getNewSubID();
                } else if (!sampledata.msi.submissionIdentifier.matches("^GSB-[1-9][0-9]*$")) {
                    return getErrorOutcome("Submission identifier invalid", "Submission identifier must match regular expression ^GSB-[1-9][0-9]*$");
                }
            }
            
            
            
            File subdir = SampleTabUtils.getSubmissionDirFile(sampledata.msi.submissionIdentifier);
            subdir = new File(getSubmissionPath(), subdir.toString());
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
            
            //if there are any groups, use those only
            //if there are no groups, then
            //create a new group and add all non-grouped samples to it
            if (sampledata.scd.getNodes(GroupNode.class).size() == 0) {
	            GroupNode othergroup = new GroupNode("Submission "+sampledata.msi.submissionIdentifier);
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
	                log.info("Added group node \""+othergroup.getNodeName()+"\"");
	                // also need to accession the new node
	            }
            }
            //correct errors
            synchronized(corrector) {
                corrector.correct(sampledata);
            }
            
            //assign accessions to sampletab object
    		// now assign and retrieve accessions for samples that do not have them
    		Collection<SampleNode> samples = sampledata.scd.getNodes(SampleNode.class);
    		for (SampleNode sample : samples) {
    			if (sample.getSampleAccession() == null) {
    				String accession;
    				if (sampledata.msi.submissionReferenceLayer) {
    					accession = accessioner.singleReferenceSample(sample.getNodeName(), keyOwner);
    				} else {
    					accession = accessioner.singleAssaySample(sample.getNodeName(),keyOwner);
    				}
    				sample.setSampleAccession(accession);
    			}
    		}

    		// now assign and retrieve accessions for groups that do not have them
    		//group acessions MUST be assigned per submission - too similar otherwise
    		Collection<GroupNode> groups = sampledata.scd.getNodes(GroupNode.class);
    		for (GroupNode group : groups) {
    			if (group.getGroupAccession() == null) {
    				String accession = accessioner.singleGroup(group.getNodeName(), keyOwner);
    				group.setGroupAccession(accession);
    			}
    		}
        
            //before doing a writeout, check all objects are either owned by the submitter, or are references
            for (SampleNode sample : sampledata.scd.getNodes(SampleNode.class)) {
            	String sampleAcc = sample.getSampleAccession();
            	//check if its a reference sample
            	if (sample.getAttributes().size() == 0) {
            		//reference sample, anyone is allowed to reference
            	} else {
            		//real sample, check with the owner
            		Optional<String> owner = accessioner.getUserNameForAccession(sampleAcc);
            		log.info("Checking owner of "+sampleAcc+" - "+keyOwner+" vs "+owner.get());
            		if (owner.isPresent() && !apiKey.canKeyOwnerEditSource(keyOwner, owner.get())) {
                        return getErrorOutcome("Unable to update "+sampleAcc, "Insufficient priviliges");
            		}
            	}
            }
            for (GroupNode group : sampledata.scd.getNodes(GroupNode.class)) {
            	String groupAcc = group.getGroupAccession();
        		//real sample, check with the owner
        		Optional<String> owner = accessioner.getUserNameForAccession(groupAcc);
        		log.info("Checking owner of "+groupAcc+" - "+keyOwner+" vs "+owner.get());
        		if (owner.isPresent() && !apiKey.canKeyOwnerEditSource(keyOwner, owner.get())) {
                    return getErrorOutcome("Unable to update "+groupAcc, "Insufficient priviliges");
        		}
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
