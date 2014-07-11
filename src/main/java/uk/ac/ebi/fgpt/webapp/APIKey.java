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
        } else {
            //invalid API key, throw exception
            throw new IllegalArgumentException("Invalid API key ("+apikey+")");
        }
    }
    
    public static boolean canKeyOwnerEditSource(String keyOwner, String source) {
        if (keyOwner == null || keyOwner.trim().length() == 0) {
            throw new IllegalArgumentException();
        }
        if (source == null || source.trim().length() == 0) {
            throw new IllegalArgumentException();
        }
        
        if ("BioSamples".equals(keyOwner)) {
            //BioSamples key can edit anything
            return true;
        } else if (source.equals(keyOwner)) {
            //source key can edit their own samples
            return true;
        } else {
            //deny everyone else
            return false;
        }
    }
}