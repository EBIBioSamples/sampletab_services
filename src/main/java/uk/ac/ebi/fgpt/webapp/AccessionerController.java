package uk.ac.ebi.fgpt.webapp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.arrayexpress2.sampletab.parser.SampleTabParser;
import uk.ac.ebi.arrayexpress2.sampletab.renderer.SampleTabWriter;
import uk.ac.ebi.fgpt.sampletab.Accessioner;
import uk.ac.ebi.fgpt.sampletab.SampleTabcronBulk;

import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 * A spring controller that returns an accessioned version of a POSTed SampleTab
 *
 * @author Adam Faulconbridge
 * @date 02/05/12
 */
@Controller
@RequestMapping
public class AccessionerController {
    
    private SampleTabParser<SampleData> parser = new SampleTabParser<SampleData>();
    
    private String host;
    private int port;
    private String database;
    private String username;
    private String password;
    private Accessioner accessioner;
    
    private Logger log = LoggerFactory.getLogger(getClass());
    
    public AccessionerController(){

        Properties mysqlProperties = new Properties();
        try {
            InputStream is = SampleTabcronBulk.class.getResourceAsStream("/mysql.properties");
            mysqlProperties.load(is);
        } catch (IOException e) {
            log.error("Unable to read resource mysql.properties");
            e.printStackTrace();
            return;
        }
        this.host = mysqlProperties.getProperty("hostname");
        this.port = new Integer(mysqlProperties.getProperty("port"));
        this.database = mysqlProperties.getProperty("database");
        this.username = mysqlProperties.getProperty("username");
        this.password = mysqlProperties.getProperty("password");
    }
    
    @RequestMapping(value = "/accession", method = RequestMethod.POST)
    public void doAccession(@RequestParam("file")MultipartFile file, HttpServletResponse response) {
        
        //convert input into a sample data object
        InputStream is;
        try {
            is = file.getInputStream();
        } catch (IOException e) {
            e.printStackTrace();
            log.error("Unable to recieve that SampleTab file. Contact administrator for more information.");
            return;
            //return "Unable to recieve that SampleTab file. Contact administrator for more information.";
            //note: maximum upload filesize specified in sampletab-accessioner-config.xml
        }
        SampleData st = null;
        try{
            st = parser.parse(is);
        } catch (ParseException e) {
            e.printStackTrace();
            log.error("Unable to parse that SampleTab file. Contact administrator for more information.");
            return;
            //return "Unable to parse that SampleTab file. Contact administrator for more information.";
        } 
        
        //assign accessions
        Accessioner a;
        try {
            a = getAccessioner();
            st = a.convert(st);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            log.error("Unable to connect to accession database. Contact administrator for more information.");
            return;
            //return "Unable to connect to accession database. Contact administrator for more information.";
        } catch (SQLException e) {
            e.printStackTrace();
            log.error("Unable to connect to accession database. Contact administrator for more information.");
            return;
            //return "Unable to connect to accession database. Contact administrator for more information.";
        } catch (ParseException e) {
            e.printStackTrace();
            log.error("Unable to assign accessions. Contact administrator for more information.");
            return;
            //return "Unable to assign accessions. Contact administrator for more information.";
        }

        //convert it back to a string to be returned
        //graph2tab library is not multi-threaded
        /*
        StringWriter out = new StringWriter();
        synchronized(AccessionerController.class){
            SampleTabWriter sampletabwriter = new SampleTabWriter(out);
            try {
                sampletabwriter.write(st);
            } catch (IOException e) {
                e.printStackTrace();
                return;
                //return "Unable to output SampleTab. Contact administrator for more information.";
            }
        }
        
        return out.toString();
        */
        //set it to be marked as a download file
        response.setContentType("application/octet-stream");
        //set the filename to download it as
        response.addHeader("Content-Disposition","attachment; filename=sampletab.txt");
        //writer to the output stream
        try {
            Writer out = new OutputStreamWriter(response.getOutputStream());
            SampleTabWriter sampletabwriter = new SampleTabWriter(out);
            sampletabwriter.write(st);
            sampletabwriter.close();
            response.flushBuffer();
        } catch (IOException e) {
            e.printStackTrace();
            return;
            //return "Unable to output SampleTab. Contact administrator for more information.";
        }
        
    }
    
    private Accessioner getAccessioner() throws ClassNotFoundException, SQLException{
        if (accessioner == null){
            accessioner = new Accessioner(host, port, database, username, password);
        }
        return accessioner;
    }
    
    
}
