package uk.ac.ebi.fgpt.webapp.v2;

import java.net.URI;
import java.net.URISyntaxException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.mged.magetab.error.ErrorItemFactory;
import org.springframework.stereotype.Service;

import uk.ac.ebi.arrayexpress2.magetab.exception.ParseException;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.SampleData;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.msi.TermSource;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.GroupNode;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.SampleNode;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.AbstractNodeAttributeOntology;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.CharacteristicAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.CommentAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.DatabaseAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.DerivedFromAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.MaterialAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.OrganismAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.SexAttribute;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.attribute.UnitAttribute;
import uk.ac.ebi.fgpt.sampletab.utils.samplegroupexport.BioSampleGroupType;
import uk.ac.ebi.fgpt.sampletab.utils.samplegroupexport.BioSampleType;
import uk.ac.ebi.fgpt.sampletab.utils.samplegroupexport.DatabaseType;
import uk.ac.ebi.fgpt.sampletab.utils.samplegroupexport.PropertyType;
import uk.ac.ebi.fgpt.sampletab.utils.samplegroupexport.QualifiedValueType;
import uk.ac.ebi.fgpt.sampletab.utils.samplegroupexport.TermSourceREFType;

@Service
public class BioSampleConverter {

	// 2014-05-20T23:00:00+00:00
	private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'+00:00'", Locale.ENGLISH);
	
	public SampleData handleBioSampleType(BioSampleType xmlSample) throws ParseException {
		// take the JaxB created object and produce a more typical SampleData
		// storage
		SampleData sd = new SampleData();
		SampleNode sample = new SampleNode();
		

		for (PropertyType property : xmlSample.getProperty()) {
			for (QualifiedValueType value : property.getQualifiedValue()) {
				if (property.getClazz().equals("Sample Name")) {
					sample.setNodeName(value.getValue());
				} else if (property.getClazz().equals("Sample Description")) {
					sample.setSampleDescription(value.getValue());
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
					// TODO unit
					if (value.getTermSourceREF() != null) {
						handleTermSource(value.getTermSourceREF(), attr, sd);
					}
				} else if (property.isComment()) {
					CommentAttribute attr = new CommentAttribute(property.getClazz(), value.getValue());
					sample.addAttribute(attr);
					// TODO unit
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
			DatabaseAttribute dba = new DatabaseAttribute();
			if (db.getName() != null && db.getName().trim().length() > 0){
				dba.setAttributeValue(db.getName().trim());
			}
			if (db.getID() != null && db.getID().trim().length() > 0){
				dba.databaseID = db.getID().trim();
			}
			if (db.getURI() != null && db.getURI().trim().length() > 0){
				URI uri;
				try {
					uri = new URI(db.getURI().trim());
				} catch (URISyntaxException e) {
					throw new ParseException(false, e, ErrorItemFactory.getErrorItemFactory()
							.generateErrorItem("Unvalid URI "+db.getURI().trim(), 1540, DatabaseType.class));
				}
				dba.databaseURI = uri.toString();
			}
			sample.addAttribute(dba);
		}

		if (xmlSample.getId() != null && xmlSample.getId().matches("SAM(N|E|D)A?[0-9]*")) {
			sample.setSampleAccession(xmlSample.getId());
		}

		// add the sample to the scd section after it has been fully constructed
		sd.scd.addNode(sample);

		if (xmlSample.getSubmissionReleaseDate() != null) {
			Date releaseDate = null;
			try {
				releaseDate = dateFormat.parse(xmlSample.getSubmissionReleaseDate());
			} catch (java.text.ParseException e) {
				// do nothing?
			}
			if (releaseDate != null) {
				sd.msi.submissionReleaseDate = releaseDate;
			}
		}
		// update date is taken to be when it is received by restful service
		// even if it might have a different update date from upstream,
		// receiving it is a newer update

		//submission identifier is taken from the database or created
		//if submitter provides it, its ignored
		
		return sd;
	}

	public SampleData handleBioSampleGroupType(BioSampleGroupType xmlGroup) throws ParseException {
		// take the JaxB created object and produce a more typical SampleData
		// storage
		SampleData sd = new SampleData();
		GroupNode group = new GroupNode();

		for (PropertyType property : xmlGroup.getProperty()) {
			for (QualifiedValueType value : property.getQualifiedValue()) {
				if (property.getClazz().equals("Group Name")) {
					group.setNodeName(value.getValue());
				} else if (property.getClazz().equals("Group Description")) {
					group.setGroupDescription(value.getValue());
				} else if (property.getClazz().equals("Group Accession")) {
					group.setGroupAccession(value.getValue());
				} else if (property.getClazz().equals("Submission Release Date")) {
					Date releaseDate = null;
					try {
						releaseDate = dateFormat.parse(value.getValue());
					} catch (java.text.ParseException e) {
						// do nothing?
					}
					if (releaseDate != null) {
						sd.msi.submissionReleaseDate = releaseDate;
					}

					// update date is taken to be when it is received by restful service
					// even if it might have a different update date from upstream,
					// receiving it is a newer update
				} else if (property.getClazz().equals("Material")) {
					AbstractNodeAttributeOntology attr = new MaterialAttribute(value.getValue());
					group.addAttribute(attr);
					if (value.getTermSourceREF() != null) {
						handleTermSource(value.getTermSourceREF(), attr, sd);
					}
				} else if (property.getClazz().equals("Sex")) {
					AbstractNodeAttributeOntology attr = new SexAttribute(value.getValue());
					group.addAttribute(attr);
					if (value.getTermSourceREF() != null) {
						handleTermSource(value.getTermSourceREF(), attr, sd);
					}
				} else if (property.getClazz().equals("Organism")) {
					AbstractNodeAttributeOntology attr = new OrganismAttribute(value.getValue());
					group.addAttribute(attr);
					if (value.getTermSourceREF() != null) {
						handleTermSource(value.getTermSourceREF(), attr, sd);
					}
				} else if (property.isCharacteristic()) {
					CharacteristicAttribute attr = new CharacteristicAttribute(property.getClazz(), value.getValue());
					if (value.getUnit() != null) {
						attr.unit = new UnitAttribute(value.getUnit());
					}
					group.addAttribute(attr);
					if (value.getTermSourceREF() != null) {
						handleTermSource(value.getTermSourceREF(), attr, sd);
					}
				} else if (property.isComment()) {
					CommentAttribute attr = new CommentAttribute(property.getClazz(), value.getValue());
					if (value.getUnit() != null) {
						attr.unit = new UnitAttribute(value.getUnit());
					}
					group.addAttribute(attr);
					if (value.getTermSourceREF() != null) {
						handleTermSource(value.getTermSourceREF(), attr, sd);
					}
				}
			}
		}
		for (DatabaseType db : xmlGroup.getDatabase()) {
			group.addAttribute(new DatabaseAttribute(db.getName(), db.getID(), db.getURI()));
		}

		if (xmlGroup.getId() != null && xmlGroup.getId().matches("SAMEG[0-9]*")) {
			group.setGroupAccession(xmlGroup.getId());
		}
		
		for (String sampleAccession : xmlGroup.getSampleIds().getId()) {
			//these must all be references
			SampleNode sample = new SampleNode();
			sample.setNodeName(sampleAccession);
			sample.setSampleAccession(sampleAccession);
			sd.scd.addNode(sample);
			group.addParentNode(sample);
			sample.addChildNode(group);
		}
		
		//TODO organization, publication, person

		// add the group to the scd section after it has been fully constructed
		sd.scd.addNode(group);

		//submission identifier is taken from the database or created
		//if submitter provides it, its ignored
		
		return sd;
	}

	private void handleTermSource(TermSourceREFType termSource, AbstractNodeAttributeOntology attr, SampleData sd) {
		if (termSource == null)
			throw new IllegalArgumentException("termSource cannot be null");
		if (attr == null)
			throw new IllegalArgumentException("termSource cannot be null");
		if (sd == null)
			throw new IllegalArgumentException("termSource cannot be null");

		if (termSource.getName() != null) {
			TermSource ts = new TermSource(termSource.getName(), termSource.getURI(), termSource.getVersion());
			attr.setTermSourceREF(sd.msi.getOrAddTermSource(ts));
		}
		attr.setTermSourceID(termSource.getTermSourceID());
	}
}
