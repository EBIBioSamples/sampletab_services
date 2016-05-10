package uk.ac.ebi.fgpt.webapp;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import uk.ac.ebi.fgpt.sampletab.Accessioner;
import uk.ac.ebi.fgpt.sampletab.Accessioner.AccessionUser;

@Service
public class APIKey {
    
	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private Accessioner accessioner;
    
    public String getAPIKeyOwner(String apikey) throws IllegalArgumentException {
        //generate keys with following python:
        //  "".join([random.choice("ABCDEFGHKLMNPRTUWXY0123456789") for x in xrange(16)])
        //NB: avoid similar looking letters/numbers
    	
    	Optional<AccessionUser> user = accessioner.getUserForAPIkey(apikey);
    	if (!user.isPresent()) {
            //invalid API key, throw exception
            throw new IllegalArgumentException("Invalid API key ("+apikey+")");
    	} else {
    		return user.get().username;
    	}
    	/*
        if (apikey != null && apikey.equals("NZ80KZ7G13NHYDM3")) {
            return "ENA";
        } else if (apikey != null && apikey.equals("XWURYU77KWT663IQ")) {
            return "CGAP";
        } else if (apikey != null && apikey.equals("FZJ5VRBEZEJ5ZDP8")) {
            return "BBMRI.eu";
        } else if (apikey != null && apikey.equals("P6RR7LPGH7PBYR9E")) {
            return "EVA";
        } else if (apikey != null && apikey.equals("EL1NRAAGEDCWKB5D")) {
            return "PRIDE";
        } else if (apikey != null && apikey.equals("XCG5UNPFKELPEL50")) {
            return "ArrayExpress";
        } else if (apikey != null && apikey.equals("Y1Y1PKRGPP7PWD82")) {
            return "BioSamples";
        } else if (apikey != null && apikey.equals("12E90E8NL4PH9BG7")) {
        	return "hESCreg";
        } else if (apikey != null && apikey.equals("WNTGPBNW0NGC3876")) {
        	return "EBiSCIMS";
        } else if (apikey != null && apikey.equals("R1HKT5T756W92EM5")) {
        	return "FAANG";
        } else if (apikey != null && apikey.equals("M5C5G091K5P4FRU4")) {
        	return "EVA";
        } else if (apikey != null && apikey.equals("TW7NGC4T3ETAUED8")) {
        	return "DGVa";
        } else if (apikey != null && apikey.equals("EFXD8H4KFWRT7HEM")) {
            return ""; // available...
        } else {
            //invalid API key, throw exception
            throw new IllegalArgumentException("Invalid API key ("+apikey+")");
        }
        */
    }
    
    public boolean canKeyOwnerEditSource(String keyOwner, String source) {
        if (keyOwner == null || keyOwner.trim().length() == 0) {
            throw new IllegalArgumentException("keyOwner must a sensible string");
        }
        if (source == null || source.trim().length() == 0) {
            throw new IllegalArgumentException("source must be a sensible string");
        }
        
        if ("BioSamples".toLowerCase().equals(keyOwner.toLowerCase())) {
            //BioSamples key can edit anything
            return true;
        } else if (source.toLowerCase().equals(keyOwner.toLowerCase())) {
            //source key can edit their own samples
            return true;
        } else {
            //deny everyone else
        	log.info("Keyowner "+keyOwner+" attempted to access "+source);
            return false;
        }
    }
}
