package uk.ac.ebi.fgpt.webapp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * A singleton class that acts as a wrapper around a {@link java.util.Properties} object that contains all system-wide
 * sampletab properties.  All method calls are delegated to an internal Properties object.
 *
 */
public class SampletabProperties {

    private static SampletabProperties sampletabProperties = new SampletabProperties();
    private static boolean initialized = false;

    public static SampletabProperties getSampletabProperties() {
        return sampletabProperties;
    }

    public static int size() {
        init();
        return sampletabProperties.getProperties().size();
    }

    public static boolean isEmpty() {
        init();
        return sampletabProperties.getProperties().isEmpty();
    }

    public static Enumeration<Object> keys() {
        init();
        return sampletabProperties.getProperties().keys();
    }

    public static Enumeration<Object> elements() {
        init();
        return sampletabProperties.getProperties().elements();
    }

    public static boolean contains(Object value) {
        init();
        return sampletabProperties.getProperties().contains(value);
    }

    public static boolean containsValue(Object value) {
        init();
        return sampletabProperties.getProperties().containsValue(value);
    }

    public static boolean containsKey(Object key) {
        init();
        return sampletabProperties.getProperties().containsKey(key);
    }

    public static Object get(Object key) {
        init();
        return sampletabProperties.getProperties().get(key);
    }

    public static Object put(Object key, Object value) {
        init();
        return sampletabProperties.getProperties().put(key, value);
    }

    public static Object remove(Object key) {
        init();
        return sampletabProperties.getProperties().remove(key);
    }

    public static void putAll(Map<?, ?> t) {
        init();
        sampletabProperties.getProperties().putAll(t);
    }

    public static void clear() {
        init();
        sampletabProperties.getProperties().clear();
    }

    public static Set<Object> keySet() {
        init();
        return sampletabProperties.getProperties().keySet();
    }

    public static Set<Map.Entry<Object, Object>> entrySet() {
        init();
        return sampletabProperties.getProperties().entrySet();
    }

    public static Collection<Object> values() {
        init();
        return sampletabProperties.getProperties().values();
    }

    public static Enumeration<?> propertyNames() {
        init();
        return sampletabProperties.getProperties().propertyNames();
    }

    public static String getProperty(String key, String defaultValue) {
        init();
        return sampletabProperties.getProperties().getProperty(key, defaultValue);
    }

    public static String getProperty(String key) {
        init();
        return sampletabProperties.getProperties().getProperty(key);
    }

    public static Set<String> stringPropertyNames() {
        init();
        return sampletabProperties.getProperties().stringPropertyNames();
    }

    private static void init() {
        if (!initialized) {
        	sampletabProperties.loadProperties();
            initialized = true;
        }
    }

    private File sampletabPropertiesFile;
    private Properties properties;
    private Logger log = LoggerFactory.getLogger(getClass());

    protected Logger getLog() {
        return log;
    }

    public void loadProperties() {
        this.properties = new Properties();
        try {
            properties.load(new BufferedInputStream(new FileInputStream(sampletabPropertiesFile)));
            getLog().info("Loaded properties from " + sampletabPropertiesFile.getAbsolutePath());
        }
        catch (IOException e) {
            getLog().error("Could not read from file " + sampletabPropertiesFile.getAbsolutePath() + ": " +
                    "properties will not be loaded");
            throw new RuntimeException("Could not read from file " + sampletabPropertiesFile.getAbsolutePath() + ": " +
                    "properties will not be loaded", e);
        }
    }

    public void setPropertiesFile(File sampletabPropertiesFile) {
        this.sampletabPropertiesFile = sampletabPropertiesFile;
    }

    private Properties getProperties() {
        return properties;
    }
}
