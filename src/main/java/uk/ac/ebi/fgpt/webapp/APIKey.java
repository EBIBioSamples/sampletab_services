package uk.ac.ebi.fgpt.webapp;

public class APIKey {

    
    public static String getAPIKeyOwner(String apikey) {
        //generate keys with following python:
        //  "".join([random.choice("ABCDEFGHKLMNPRTUWXY0123456789") for x in xrange(16)])
        //NB: avoid similar looking letters/numbers
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
        	return ""; //unused at the moment...
        } else {
            //invalid API key, throw exception
            throw new IllegalArgumentException("Invalid API key ("+apikey+")");
        }
    }
    
    public static boolean canKeyOwnerEditSource(String keyOwner, String source) {
        if (keyOwner == null || keyOwner.trim().length() == 0) {
            throw new IllegalArgumentException("keyOnwer must a sensible string");
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
            return false;
        }
    }
}
