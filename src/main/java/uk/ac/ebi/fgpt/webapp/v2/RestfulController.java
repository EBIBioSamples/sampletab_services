package uk.ac.ebi.fgpt.webapp.v2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.TypedQuery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.msi.TermSource;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.SampleNode;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.AbstractNodeAttributeOntology;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.CharacteristicAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.CommentAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.DatabaseAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.DerivedFromAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.MaterialAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.OrganismAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.SexAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.renderer.SampleTabWriter;
import uk.ac.ebi.fg.core_model.resources.Resources;
import uk.ac.ebi.fgpt.sampletab.Accessioner;
import uk.ac.ebi.fgpt.sampletab.utils.SampleTabUtils;
import uk.ac.ebi.fgpt.sampletab.utils.samplegroupexport.BioSampleType;
import uk.ac.ebi.fgpt.sampletab.utils.samplegroupexport.DatabaseType;
import uk.ac.ebi.fgpt.sampletab.utils.samplegroupexport.PropertyType;
import uk.ac.ebi.fgpt.sampletab.utils.samplegroupexport.QualifiedValueType;
import uk.ac.ebi.fgpt.sampletab.utils.samplegroupexport.TermSourceREFType;
import uk.ac.ebi.fgpt.webapp.APIKey;
import uk.ac.ebi.fgpt.webapp.SampletabProperties;

@Controller
@RequestMapping("/v2")
public class RestfulController {

    
    private Logger log = LoggerFactory.getLogger(getClass());
    
    @Autowired
    private Accessioner accessioner;	
	
    //2014-05-20T23:00:00+00:00
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+00:00'", Locale.ENGLISH);
    
    public RestfulController()  {
        
    }
    
    protected Accessioner getAccessioner() {
        return accessioner;
    }

    protected File getSubmissionPath() {
    	File path = new File(SampletabProperties.getProperty("submissionpath"));
    	path = path.getAbsoluteFile();
    	return path;
    }
    
    
    @RequestMapping(value="/source/{source}/sample", method=RequestMethod.POST, produces="text/plain", consumes="application/xml")
    public ResponseEntity<String> saveSourceSampleNew(@PathVariable String source, @RequestParam String apikey, @RequestBody BioSampleType sample) throws ParseException, IOException  {
        //ensure source is case insensitive
        source = source.toLowerCase();
        
    	ResponseEntity<String> response = accessionSourceSampleNew(source, apikey);
        
    	if (response.getStatusCode() == HttpStatus.ACCEPTED) {
	        //a request body was provided, so save it somewhere
	    	//after adding the accession
	    	SampleData sd = handleBioSampleType(sample);
	    	String accession = response.getBody();
	    	List<SampleNode> samples = new ArrayList<SampleNode>();
			samples.addAll(sd.scd.getNodes(SampleNode.class));
    		//TODO validate number of samples
    		//TODO validate sample accession
	    	samples.get(0).setSampleAccession(accession);
	        saveSampleData(sd);
    	}
        
        return response;
    }
    
    @RequestMapping(value="/source/{source}/sample", method=RequestMethod.POST, produces="text/plain")
    public ResponseEntity<String> accessionSourceSampleNew(@PathVariable String source, @RequestParam String apikey)  {
        //ensure source is case insensitive
        source = source.toLowerCase();
        
    	String keyOwner = null;
        try {
        	keyOwner = APIKey.getAPIKeyOwner(apikey);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.FORBIDDEN);
        }
        
        if (!APIKey.canKeyOwnerEditSource(keyOwner, source)) {
            return new ResponseEntity<String>("apikey is not permitted for source", HttpStatus.FORBIDDEN);
        }
        
        String newAccession = getAccessioner().singleAssaySample(source);
        ResponseEntity<String> response = new ResponseEntity<String>(newAccession, HttpStatus.ACCEPTED);        
        
        return response;
    }
    
    

    @RequestMapping(value="/source/{source}/sample/{sourceid}", method=RequestMethod.PUT, produces="text/plain", consumes="application/xml")
    public @ResponseBody ResponseEntity<String> saveUpdate(@PathVariable String source, @PathVariable String sourceid, @RequestParam String apikey, 
    		@RequestBody BioSampleType sample) throws ParseException, IOException {
        //ensure source is case insensitive
        source = source.toLowerCase();
        
    	String keyOwner = null;
        try {
        	keyOwner = APIKey.getAPIKeyOwner(apikey);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.FORBIDDEN);
        }
        
        if (!APIKey.canKeyOwnerEditSource(keyOwner, source)) {
            return new ResponseEntity<String>("That API key is not permitted for that source", HttpStatus.FORBIDDEN);
        }
                
        //a request body was provided, so handle it
    	//after adding the accession
    	SampleData sd = handleBioSampleType(sample);
		SampleNode sampleNode = sd.scd.getNodes(SampleNode.class).iterator().next();
    	ResponseEntity<String> response;
    	String accession = null;
    	
    	//validate number of samples is not needed, since type specifys one BioSample xml element
    	
    	
    	if (sourceid.matches("SAM[NED]A?[0-9]+")) {
    		//its a biosamples ID
    		//check content accession matches address accession
    		if (sampleNode.getSampleAccession() == null || !sampleNode.getSampleAccession().equals(sourceid)) {
    			return new ResponseEntity<String>("Sample Accession in XML must match sourceid in URL", HttpStatus.CONFLICT);
    		}
    		//accept this submission
        	response = new ResponseEntity<String>(accession, HttpStatus.ACCEPTED);
    	} else {
    		//its not a biosamples id, but a source id
    		accession = getAccessioner().retrieveAssaySample(sourceid, source);
            //reject if not already accessioned (PUT is an update)
    		if (accession == null) {
    			return new ResponseEntity<String>("PUT must be an update, use POST for new submissions", HttpStatus.BAD_REQUEST);
    		}
    		//check content accession matches address accession
    		if (sampleNode.getSampleAccession() != null && !sampleNode.getSampleAccession().equals(sourceid)) {
    			return new ResponseEntity<String>("Sample accession in XML must match previous accession", HttpStatus.CONFLICT);
    		}
    		//accept this submission
        	response = new ResponseEntity<String>(accession, HttpStatus.ACCEPTED);        
    	}
    	
    	if (accession != null) {
	    	String submission = getSubmissionForSampleAccession(accession);
			if (submission != null) {
				sd.msi.submissionIdentifier = submission;
			}
    	}
    	
    	//save the output somewhere 
        saveSampleData(sd);
        
        return response;
        
    }

    @RequestMapping(value="/source/{source}/sample/{sourceid}/submission", method=RequestMethod.GET, produces="text/plain")
    public @ResponseBody ResponseEntity<String> getSubmissionOfSample(@PathVariable String source, @PathVariable String sourceid, @RequestParam String apikey) {
    	//ensure source is case insensitive
        source = source.toLowerCase();
    	String keyOwner = null;
        try {
        	keyOwner = APIKey.getAPIKeyOwner(apikey);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.FORBIDDEN);
        }
        
        if (!APIKey.canKeyOwnerEditSource(keyOwner, source)) {
            return new ResponseEntity<String>("That API key is not permitted for that source", HttpStatus.FORBIDDEN);
        }

    	if (sourceid.matches("SAM[NED]A?[0-9]+")) {
    		//its a biosamples ID
	    	String submission = getSubmissionForSampleAccession(sourceid);
			return new ResponseEntity<String>(submission, HttpStatus.ACCEPTED);
    	} else {
            return new ResponseEntity<String>("Only implmemented for BioSample accessions", HttpStatus.FORBIDDEN);
    	}
    }

    @RequestMapping(value="/source/{source}/sample/{sourceid}", method=RequestMethod.POST, produces="text/plain")
    public @ResponseBody ResponseEntity<String> accessionSourceSample(@PathVariable String source, @PathVariable String sourceid, @RequestParam String apikey) {
        //ensure source is case insensitive
        source = source.toLowerCase();
    	String keyOwner = null;
        try {
        	keyOwner = APIKey.getAPIKeyOwner(apikey);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.FORBIDDEN);
        }
        
        if (!APIKey.canKeyOwnerEditSource(keyOwner, source)) {
            return new ResponseEntity<String>("That API key is not permitted for that source", HttpStatus.FORBIDDEN);
        }
    	
    	if (sourceid.matches("SAM[NED]A?[0-9]+")) {
    		//its a biosamples ID
    		return new ResponseEntity<String>("Do not request a new BioSamples accession for an existing BioSamples accession", HttpStatus.BAD_REQUEST);
    	} else {
    		String accession = getAccessioner().singleAssaySample(sourceid, source);
    		return new ResponseEntity<String>(accession, HttpStatus.ACCEPTED);
    	}
    }


    @RequestMapping(value="/source/{source}/sample/{sourceid}", method=RequestMethod.POST, produces="text/plain", consumes="application/xml")
    public @ResponseBody ResponseEntity<String> saveSourceSample(@PathVariable String source, @PathVariable String sourceid, 
    		@RequestParam String apikey, @RequestBody BioSampleType sample) throws ParseException, IOException {
        //ensure source is case insensitive
        source = source.toLowerCase();
        
        
    	String keyOwner = null;
        try {
        	keyOwner = APIKey.getAPIKeyOwner(apikey);
        } catch (IllegalArgumentException e) {
            return new ResponseEntity<String>(e.getMessage(), HttpStatus.FORBIDDEN);
        }
        
        if (!APIKey.canKeyOwnerEditSource(keyOwner, source)) {
            return new ResponseEntity<String>("That API key is not permitted for that source", HttpStatus.FORBIDDEN);
        }

        //a request body was provided, so handle it
    	//after adding the accession
    	SampleData sd = handleBioSampleType(sample);
    	ResponseEntity<String> response;
    	
    	if (sourceid.matches("SAM[NED]A?[0-9]+")) {
    		//its a biosamples ID
    		return new ResponseEntity<String>("Do not request a new BioSamples accession for an existing BioSamples accession", HttpStatus.BAD_REQUEST);
    	}
        //reject if already acessioned (POST is a one-time operation)
    	if (getAccessioner().testAssaySample(sourceid, source)) {
			return new ResponseEntity<String>("POST must be a new submission, use PUT for updates", HttpStatus.BAD_REQUEST);
    	}
		String accession = getAccessioner().singleAssaySample(sourceid, source);

		//because this is in POST, it must be a new submission, therefore it won't have an existing submission
    	String submission = getSubmissionForSampleAccession(accession);
		if (submission != null) {
			return new ResponseEntity<String>("POST must be a new submission, use PUT for updates", HttpStatus.BAD_REQUEST);
		}
		
		
    	List<SampleNode> samples = new ArrayList<SampleNode>();
		samples.addAll(sd.scd.getNodes(SampleNode.class));
		//TODO validate number of samples
		//TODO validate sample accession
    	samples.get(0).setSampleAccession(accession);
    	response = new ResponseEntity<String>(accession, HttpStatus.ACCEPTED);
    	
    	
    	//save the output somewhere 
        saveSampleData(sd);
        
        return response;
    }
    
    
    private SampleData handleBioSampleType(BioSampleType xmlSample) throws ParseException {
        // take the JaxB created object and produce a more typical SampleData storage
        SampleData sd = new SampleData();
        SampleNode sample = new SampleNode();
        
        for (PropertyType property : xmlSample.getProperty()) {
            for (QualifiedValueType value : property.getQualifiedValue()) {
                if (property.getClazz().equals("Sample Name")) {
                    sample.setNodeName(value.getValue());
                } else if (property.getClazz().equals("Sample Description")) {
                    sample.setSampleDescription(value.getValue());
                } else if (property.getClazz().equals("Sample Accession")) {
                    sample.setSampleAccession(value.getValue());
                } else if (property.getClazz().equals("Material")) {
                    AbstractNodeAttributeOntology attr = new MaterialAttribute(value.getValue());
                    sample.addAttribute(attr); 
                    if (value.getTermSourceREF() != null) {
                        handleTermSource(value.getTermSourceREF(), attr, sd);
                    }
                } else if (property.getClazz().equals("Sex")) {
                    AbstractNodeAttributeOntology attr = new SexAttribute(value.getValue());
                    sample.addAttribute(attr); 
                    if (value.getTermSourceREF() != null) {
                        handleTermSource(value.getTermSourceREF(), attr, sd);
                    }
                } else if (property.getClazz().equals("Organism")) {
                    AbstractNodeAttributeOntology attr = new OrganismAttribute(value.getValue());
                    sample.addAttribute(attr); 
                    if (value.getTermSourceREF() != null) {
                        handleTermSource(value.getTermSourceREF(), attr, sd);
                    }
                } else if (property.isCharacteristic()) {
                    CharacteristicAttribute attr = new CharacteristicAttribute(property.getClazz(), value.getValue());
                    sample.addAttribute(attr);
                    //TODO unit
                    if (value.getTermSourceREF() != null) {
                        handleTermSource(value.getTermSourceREF(), attr, sd);
                    }
                } else if (property.isComment()) {
                    CommentAttribute attr = new CommentAttribute(property.getClazz(), value.getValue());
                    sample.addAttribute(attr);
                    //TODO unit
                    if (value.getTermSourceREF() != null) {
                        handleTermSource(value.getTermSourceREF(), attr, sd);
                    }
                }
            }
        }
        for (String derived : xmlSample.getDerivedFrom()) {
            sample.addAttribute(new DerivedFromAttribute(derived));
        }
        for (DatabaseType db : xmlSample.getDatabase()) {
            sample.addAttribute(new DatabaseAttribute(db.getName(), db.getID(), db.getURI()));
        } 
        
        if (xmlSample.getId() != null && xmlSample.getId().matches("SAM(N|E|D)A?[0-9]*")) {
        	sample.setSampleAccession(xmlSample.getId());
        }
        
        //add the sample to the scd section after it has been fully constructed
        sd.scd.addNode(sample);
        
        if (xmlSample.getSubmissionReleaseDate() != null ){
        	Date releaseDate = null;
        	try {
        		releaseDate = dateFormat.parse(xmlSample.getSubmissionReleaseDate());
        	} catch (java.text.ParseException e) {
        		//do nothing
			}
        	if (releaseDate != null) {
        		sd.msi.submissionReleaseDate = releaseDate;
        	}
        }
        //update date is taken to be when it is received by restful service
        //even if it might have a different update date from upstream, receiving it is a newer update
        
        return sd;
    }
    
    private void handleTermSource(TermSourceREFType termSource, AbstractNodeAttributeOntology attr, SampleData sd) {
        if (termSource == null) throw new IllegalArgumentException("termSource cannot be null");
        if (attr == null) throw new IllegalArgumentException("termSource cannot be null");
        if (sd == null) throw new IllegalArgumentException("termSource cannot be null");
        
        if (termSource.getName() != null) {
            TermSource ts = new TermSource(termSource.getName(), termSource.getURI(), termSource.getVersion());
            attr.setTermSourceREF(sd.msi.getOrAddTermSource(ts));
        }
        attr.setTermSourceID(termSource.getTermSourceID());
    }
    
    private void saveSampleData(SampleData sd) throws IOException {
        //need to assign a submission id
        if (sd.msi.submissionIdentifier == null) {
            int maxSubID = 0;
            Pattern pattern = Pattern.compile("^GSB-([0-9]+)$");
            File pathSubdir = new File(getSubmissionPath(), "GSB");
            for (File subdir : pathSubdir.listFiles()) {
                if (!subdir.isDirectory()) {
                    continue;
                } else {
                    log.trace("Looking at subid "+subdir.getName()+" with pattern "+pattern.pattern());
                    Matcher match = pattern.matcher(subdir.getName());
                    if (match.matches()) {
                        log.trace("Found match with "+match.groupCount()+" groups "+match.group(1));
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
            
            sd.msi.submissionIdentifier = "GSB-"+maxSubID;
        }
        

        File subdir = new File(getSubmissionPath(), SampleTabUtils.getSubmissionDirFile(sd.msi.submissionIdentifier).toString());
        File outFile = new File(subdir, "sampletab.pre.txt");

        //TODO check this is a sensible submission if overwriting
        
        SampleTabWriter writer = null;
        try {
            if (!subdir.exists() && !subdir.mkdirs()) {
                throw new IOException("Unable to create parent directories");
            }
            writer = new SampleTabWriter(new BufferedWriter(new FileWriter(outFile)));
            writer.write(sd);
            log.info("wrote to "+outFile);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    //do nothing
                }
            }
        }
        
        /*
        
        At this point, its been written to a temporary staging area by the owner of the executing tomcat.
        
        A cron script (running as the internal user) will pick it up and copy it to the real file area. That script
        will trigger Conan for downstream processing.
        
         */
    }
    
    /**
     * This is the method that uses Hibernate to connect to the database. 
     * @param acc
     * @return
     */
    public String getSubmissionForSampleAccession(String acc) {        
        EntityManager em = Resources.getInstance().getEntityManagerFactory().createEntityManager();
        TypedQuery<String> q = em.createQuery("SELECT msi.acc FROM BioSample bs JOIN bs.MSIs AS msi WHERE bs.acc = '"+acc+"'", String.class);
        try {
        	return q.getSingleResult();
        } catch (NoResultException e) {
        	//there was no match, return null
        	return null;
        }
    }
}
