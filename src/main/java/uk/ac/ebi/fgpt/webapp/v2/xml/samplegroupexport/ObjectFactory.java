//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.07.22 at 04:51:31 PM BST 
//


package uk.ac.ebi.fgpt.webapp.v2.xml.samplegroupexport;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.annotation.XmlElementDecl;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;


/**
 * This object contains factory methods for each 
 * Java content interface and Java element interface 
 * generated in the uk.ac.ebi.biosamples.samplegroupexport._1 package. 
 * <p>An ObjectFactory allows you to programatically 
 * construct new instances of the Java representation 
 * for XML content. The Java representation of XML 
 * content can consist of schema derived interfaces 
 * and classes representing the binding of schema 
 * type definitions, element declarations and model 
 * groups.  Factory methods for each of these are 
 * provided in this class.
 * 
 */
@XmlRegistry
public class ObjectFactory {

    private final static QName _BioSample_QNAME = new QName("http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0", "BioSample");
    private final static QName _BioSampleGroup_QNAME = new QName("http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0", "BioSampleGroup");

    /**
     * Create a new ObjectFactory that can be used to create new instances of schema derived classes for package: uk.ac.ebi.biosamples.samplegroupexport._1
     * 
     */
    public ObjectFactory() {
    }

    /**
     * Create an instance of {@link GroupIdsType }
     * 
     */
    public GroupIdsType createGroupIdsType() {
        return new GroupIdsType();
    }

    /**
     * Create an instance of {@link BioSampleType }
     * 
     */
    public BioSampleType createBioSampleType() {
        return new BioSampleType();
    }

    /**
     * Create an instance of {@link SampleIdsType }
     * 
     */
    public SampleIdsType createSampleIdsType() {
        return new SampleIdsType();
    }

    /**
     * Create an instance of {@link PersonType }
     * 
     */
    public PersonType createPersonType() {
        return new PersonType();
    }

    /**
     * Create an instance of {@link BioSampleGroupType }
     * 
     */
    public BioSampleGroupType createBioSampleGroupType() {
        return new BioSampleGroupType();
    }

    /**
     * Create an instance of {@link BioSamples }
     * 
     */
    public BioSamples createBioSamples() {
        return new BioSamples();
    }

    /**
     * Create an instance of {@link TermSourceREFType }
     * 
     */
    public TermSourceREFType createTermSourceREFType() {
        return new TermSourceREFType();
    }

    /**
     * Create an instance of {@link PublicationType }
     * 
     */
    public PublicationType createPublicationType() {
        return new PublicationType();
    }

    /**
     * Create an instance of {@link PropertyType }
     * 
     */
    public PropertyType createPropertyType() {
        return new PropertyType();
    }

    /**
     * Create an instance of {@link QualifiedValueType }
     * 
     */
    public QualifiedValueType createQualifiedValueType() {
        return new QualifiedValueType();
    }

    /**
     * Create an instance of {@link OrganizationType }
     * 
     */
    public OrganizationType createOrganizationType() {
        return new OrganizationType();
    }

    /**
     * Create an instance of {@link AnnotationType }
     * 
     */
    public AnnotationType createAnnotationType() {
        return new AnnotationType();
    }

    /**
     * Create an instance of {@link DatabaseType }
     * 
     */
    public DatabaseType createDatabaseType() {
        return new DatabaseType();
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BioSampleType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0", name = "BioSample")
    public JAXBElement<BioSampleType> createBioSample(BioSampleType value) {
        return new JAXBElement<BioSampleType>(_BioSample_QNAME, BioSampleType.class, null, value);
    }

    /**
     * Create an instance of {@link JAXBElement }{@code <}{@link BioSampleGroupType }{@code >}}
     * 
     */
    @XmlElementDecl(namespace = "http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0", name = "BioSampleGroup")
    public JAXBElement<BioSampleGroupType> createBioSampleGroup(BioSampleGroupType value) {
        return new JAXBElement<BioSampleGroupType>(_BioSampleGroup_QNAME, BioSampleGroupType.class, null, value);
    }

}
