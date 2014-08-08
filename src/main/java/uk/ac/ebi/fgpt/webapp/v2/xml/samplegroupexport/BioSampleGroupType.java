//
// This file was generated by the JavaTM Architecture for XML Binding(JAXB) Reference Implementation, vJAXB 2.1.10 
// See <a href="http://java.sun.com/xml/jaxb">http://java.sun.com/xml/jaxb</a> 
// Any modifications to this file will be lost upon recompilation of the source schema. 
// Generated on: 2014.07.22 at 04:51:31 PM BST 
//


package uk.ac.ebi.fgpt.webapp.v2.xml.samplegroupexport;

import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlType;


/**
 * <p>Java class for bioSampleGroupType complex type.
 * 
 * <p>The following schema fragment specifies the expected content contained within this class.
 * 
 * <pre>
 * &lt;complexType name="bioSampleGroupType">
 *   &lt;complexContent>
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType">
 *       &lt;sequence>
 *         &lt;element name="Annotation" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}annotationType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="TermSource" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}termSourceREFType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="Property" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}propertyType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="Organization" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}organizationType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="Person" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}personType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="Database" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}databaseType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="Publication" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}publicationType" maxOccurs="unbounded" minOccurs="0"/>
 *         &lt;element name="SampleIds" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}SampleIdsType" minOccurs="0"/>
 *         &lt;element name="BioSample" type="{http://www.ebi.ac.uk/biosamples/SampleGroupExport/1.0}bioSampleType" maxOccurs="unbounded" minOccurs="0"/>
 *       &lt;/sequence>
 *       &lt;attribute name="id" use="required" type="{http://www.w3.org/2001/XMLSchema}string" />
 *     &lt;/restriction>
 *   &lt;/complexContent>
 * &lt;/complexType>
 * </pre>
 * 
 * 
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "bioSampleGroupType", propOrder = {
    "annotation",
    "termSource",
    "property",
    "organization",
    "person",
    "database",
    "publication",
    "sampleIds",
    "bioSample"
})
public class BioSampleGroupType {

    @XmlElement(name = "Annotation")
    protected List<AnnotationType> annotation;
    @XmlElement(name = "TermSource")
    protected List<TermSourceREFType> termSource;
    @XmlElement(name = "Property")
    protected List<PropertyType> property;
    @XmlElement(name = "Organization")
    protected List<OrganizationType> organization;
    @XmlElement(name = "Person")
    protected List<PersonType> person;
    @XmlElement(name = "Database")
    protected List<DatabaseType> database;
    @XmlElement(name = "Publication")
    protected List<PublicationType> publication;
    @XmlElement(name = "SampleIds")
    protected SampleIdsType sampleIds;
    @XmlElement(name = "BioSample")
    protected List<BioSampleType> bioSample;
    @XmlAttribute(required = true)
    protected String id;

    /**
     * Gets the value of the annotation property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the annotation property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getAnnotation().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link AnnotationType }
     * 
     * 
     */
    public List<AnnotationType> getAnnotation() {
        if (annotation == null) {
            annotation = new ArrayList<AnnotationType>();
        }
        return this.annotation;
    }

    /**
     * Gets the value of the termSource property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the termSource property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTermSource().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link TermSourceREFType }
     * 
     * 
     */
    public List<TermSourceREFType> getTermSource() {
        if (termSource == null) {
            termSource = new ArrayList<TermSourceREFType>();
        }
        return this.termSource;
    }

    /**
     * Gets the value of the property property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the property property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProperty().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PropertyType }
     * 
     * 
     */
    public List<PropertyType> getProperty() {
        if (property == null) {
            property = new ArrayList<PropertyType>();
        }
        return this.property;
    }

    /**
     * Gets the value of the organization property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the organization property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getOrganization().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link OrganizationType }
     * 
     * 
     */
    public List<OrganizationType> getOrganization() {
        if (organization == null) {
            organization = new ArrayList<OrganizationType>();
        }
        return this.organization;
    }

    /**
     * Gets the value of the person property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the person property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPerson().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PersonType }
     * 
     * 
     */
    public List<PersonType> getPerson() {
        if (person == null) {
            person = new ArrayList<PersonType>();
        }
        return this.person;
    }

    /**
     * Gets the value of the database property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the database property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getDatabase().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link DatabaseType }
     * 
     * 
     */
    public List<DatabaseType> getDatabase() {
        if (database == null) {
            database = new ArrayList<DatabaseType>();
        }
        return this.database;
    }

    /**
     * Gets the value of the publication property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the publication property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getPublication().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link PublicationType }
     * 
     * 
     */
    public List<PublicationType> getPublication() {
        if (publication == null) {
            publication = new ArrayList<PublicationType>();
        }
        return this.publication;
    }

    /**
     * Gets the value of the sampleIds property.
     * 
     * @return
     *     possible object is
     *     {@link SampleIdsType }
     *     
     */
    public SampleIdsType getSampleIds() {
        return sampleIds;
    }

    /**
     * Sets the value of the sampleIds property.
     * 
     * @param value
     *     allowed object is
     *     {@link SampleIdsType }
     *     
     */
    public void setSampleIds(SampleIdsType value) {
        this.sampleIds = value;
    }

    /**
     * Gets the value of the bioSample property.
     * 
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the bioSample property.
     * 
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getBioSample().add(newItem);
     * </pre>
     * 
     * 
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link BioSampleType }
     * 
     * 
     */
    public List<BioSampleType> getBioSample() {
        if (bioSample == null) {
            bioSample = new ArrayList<BioSampleType>();
        }
        return this.bioSample;
    }

    /**
     * Gets the value of the id property.
     * 
     * @return
     *     possible object is
     *     {@link String }
     *     
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the value of the id property.
     * 
     * @param value
     *     allowed object is
     *     {@link String }
     *     
     */
    public void setId(String value) {
        this.id = value;
    }

}
