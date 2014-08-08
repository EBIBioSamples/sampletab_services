package uk.ac.ebi.fgpt.webapp.v2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

import uk.ac.ebi.fgpt.sampletab.Accessioner;
import uk.ac.ebi.fgpt.sampletab.utils.SampleTabUtils;
import uk.ac.ebi.fgpt.webapp.APIKey;
import uk.ac.ebi.fgpt.webapp.v2.xml.samplegroupexport.BioSampleType;
import uk.ac.ebi.fgpt.webapp.v2.xml.samplegroupexport.DatabaseType;
import uk.ac.ebi.fgpt.webapp.v2.xml.samplegroupexport.PropertyType;
import uk.ac.ebi.fgpt.webapp.v2.xml.samplegroupexport.QualifiedValueType;
import uk.ac.ebi.fgpt.webapp.v2.xml.samplegroupexport.TermSourceREFType;

@Controller
@RequestMapping("/v2")
public class RestfulController {
    
    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;
    private Accessioner accessioner = null;
    
    private File path;
    
    private Logger log = LoggerFactory.getLogger(getClass());
    
    public RestfulController() {
        Properties properties = new Properties();
        try {
            InputStream is = getClass().getResourceAsStream("/oracle.properties");
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
        

        path = new File(properties.getProperty("submissionpath"));
        if (!path.exists()){
            //TODO throw error
            log.error("Submission path "+path+" does not exist");
        }
    }
    
    protected Accessioner getAccessioner() {
        if (accessioner == null) {
            accessioner = new Accessioner(host, port, database, username, password);
        }
        return accessioner;
    }
    
    @RequestMapping(value="/source/{source}/sample", method=RequestMethod.POST, produces="text/plain")
    public @ResponseBody String createAccession(@PathVariable String source, @RequestParam String apikey, @RequestBody BioSampleType sample) 
        throws SQLException, ClassNotFoundException, ParseException, IOException {
        //ensure source is case insensitive
        source = source.toLowerCase();

        String keyOwner = APIKey.getAPIKeyOwner(apikey);
        //TODO handle wrong api keys better
        
        if (!APIKey.canKeyOwnerEditSource(keyOwner, source)) {
            //TODO handle invalid key better
            throw new IllegalArgumentException("apikey is not permitted for source");
        }
        
        String newAccession = getAccessioner().singleAssaySample(source);
             
        if (sample != null) {
            //a request body was provided, so save it somewhere
            saveSampleData(handleBioSampleType(sample));
        }
        
        return newAccession;
    }

    @RequestMapping(value="/source/{source}/sample/{sourceid}", method=RequestMethod.PUT, produces="text/plain")
    public @ResponseBody String createAccession(@PathVariable String source, @PathVariable String sourceid, @RequestParam String apikey, @RequestBody BioSampleType sample) 
        throws SQLException, ClassNotFoundException, ParseException, IOException {
        
        //ensure source is case insensitive
        source = source.toLowerCase();

        SampleData sd = handleBioSampleType(sample);
        if (sd != null) {
            //a request body was provided, so save it somewhere
            saveSampleData(sd);
        }
        
        if (sourceid.matches("SAMEA[0-9]*")) {
            //sourceid is a biosample accession already
            //TODO check that this biosample accession belongs to this source
            return sourceid;
        } else {
            //source id is not a biosample accession
            String newAccession = getAccessioner().singleAssaySample(sourceid, source);

            String keyOwner = APIKey.getAPIKeyOwner(apikey);
            //TODO handle wrong api keys better
            
            if (!APIKey.canKeyOwnerEditSource(keyOwner, source)) {
                //TODO handle invalid key better
                throw new IllegalArgumentException("apikey is not permitted for source");
            }
            
            return newAccession;
        }
    }
    
    private SampleData handleBioSampleType(BioSampleType xmlSample) throws ParseException {
        // take the JaxB created object and produce a more typical SampleData storage
        SampleData sd = new SampleData();
        SampleNode sample = new SampleNode();
        sd.scd.addNode(sample);
        
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
                    handleTermSource(value.getTermSourceREF(), attr, sd);
                } else if (property.getClazz().equals("Sex")) {
                    AbstractNodeAttributeOntology attr = new SexAttribute(value.getValue());
                    sample.addAttribute(attr); 
                    handleTermSource(value.getTermSourceREF(), attr, sd);
                } else if (property.getClazz().equals("Organism")) {
                    AbstractNodeAttributeOntology attr = new OrganismAttribute(value.getValue());
                    sample.addAttribute(attr); 
                    handleTermSource(value.getTermSourceREF(), attr, sd);
                } else if (property.isCharacteristic()) {
                    CharacteristicAttribute attr = new CharacteristicAttribute(property.getClazz(), value.getValue());
                    sample.addAttribute(attr);
                    //TODO unit
                    handleTermSource(value.getTermSourceREF(), attr, sd);
                } else if (property.isComment()) {
                    CommentAttribute attr = new CommentAttribute(property.getClazz(), value.getValue());
                    sample.addAttribute(attr);
                    //TODO unit
                    handleTermSource(value.getTermSourceREF(), attr, sd);
                }
            }
        }
        for (String derived : xmlSample.getDerivedFrom()) {
            sample.addAttribute(new DerivedFromAttribute(derived));
        }
        for (DatabaseType db : xmlSample.getDatabase()) {
            sample.addAttribute(new DatabaseAttribute(db.getName(), db.getID(), db.getURI()));
        } 
        
        return sd;
    }
    
    private void handleTermSource(TermSourceREFType termSource, AbstractNodeAttributeOntology attr, SampleData sd) {
        if (termSource.getName() != null) {
            TermSource ts = new TermSource(termSource.getName(), termSource.getURI(), termSource.getVersion());
            attr.setTermSourceREF(sd.msi.getOrAddTermSource(ts));
        }
        attr.setTermSourceID(termSource.getTermSourceID());
    }
    
    private void saveSampleData(SampleData sd) throws IOException {
        //need to assign a submissino id
        if (sd.msi.submissionIdentifier == null) {
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
            
            sd.msi.submissionIdentifier = "GSB-"+maxSubID;
        }

        File subdir = SampleTabUtils.getSubmissionDirFile(sd.msi.submissionIdentifier);
        File outFile = new File(subdir, "sampletab.pre.txt");

        SampleTabWriter writer = null;
        try {
            if (!subdir.exists() && !subdir.mkdirs()) {
                throw new IOException("Unable to create parent directories");
            }
            writer = new SampleTabWriter(new BufferedWriter(new FileWriter(outFile)));
            writer.write(sd);
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    //do nothing
                }
            }
        }
    }
}
