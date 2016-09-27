package uk.ac.ebi.fgpt.webapp.v2;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.GroupNode;
import uk.ac.ebi.arrayexpress2.sampletab.datamodel.scd.node.SampleNode;
import uk.ac.ebi.arrayexpress2.sampletab.renderer.SampleTabWriter;
import uk.ac.ebi.fgpt.sampletab.Accessioner;
import uk.ac.ebi.fgpt.sampletab.utils.SampleTabUtils;
import uk.ac.ebi.fgpt.sampletab.utils.samplegroupexport.BioSampleGroupType;
import uk.ac.ebi.fgpt.sampletab.utils.samplegroupexport.BioSampleType;
import uk.ac.ebi.fgpt.webapp.APIKey;

@Controller
@RequestMapping("/v2")
public class RestfulController {

	private Logger log = LoggerFactory.getLogger(getClass());

	@Autowired
	private APIKey apiKey;

	@Autowired
	private Accessioner accessioner;

	@Autowired
	private RelationalDAO relationalDAO;
	
	@Autowired
	private BioSampleConverter bioSampleConverter;

	@Value("${submissionpath}") // this is read from the context xml Parameter
								// element
	private String submissionPath;

	public RestfulController() {

	}

	protected File getSubmissionPath() {
		if (submissionPath == null) {
			throw new RuntimeException("Expected submissionpath to be non-null");
		}
		log.info("submissionpath is " + submissionPath);
		File path = new File(submissionPath);
		path = path.getAbsoluteFile();
		return path;
	}

	@RequestMapping(value = "/source/{source}/sample", method = RequestMethod.POST, produces = "text/plain")
	public ResponseEntity<String> accessionSourceSampleNew(@PathVariable String source, @RequestParam String apikey) {
		return accessionSourceSample(source, UUID.randomUUID().toString(), apikey);
	}

	@RequestMapping(value = "/source/{source}/sample", method = RequestMethod.POST, produces = "text/plain", consumes = "application/xml")
	public ResponseEntity<String> saveSourceSampleNew(@PathVariable String source, @RequestParam String apikey,
			@RequestBody BioSampleType sample) throws ParseException, IOException {
		return saveSourceSample(source, UUID.randomUUID().toString(), apikey, sample);
	}

	@RequestMapping(value = "/source/{source}/sample/{sourceid}", method = RequestMethod.POST, produces = "text/plain")
	public @ResponseBody ResponseEntity<String> accessionSourceSample(@PathVariable String source,
			@PathVariable String sourceid, @RequestParam String apikey) {
		// ensure source is case insensitive
		source = source.toLowerCase();
		
		String keyOwner = null;
		try {
			keyOwner = apiKey.getAPIKeyOwner(apikey);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.FORBIDDEN);
		}
		if (!apiKey.canKeyOwnerEditSource(keyOwner, source)) {
			return new ResponseEntity<String>("That API key is not permitted for that source", HttpStatus.FORBIDDEN);
		}

		// reject if its a biosamples ID
		if (sourceid.matches("SAM[NED]A?[0-9]+")) {
			return new ResponseEntity<String>(
					"POST must be a new submission, use PUT for updates",
					HttpStatus.BAD_REQUEST);
		} 
		
		// reject if already acessioned (POST is a one-time operation)
		if (accessioner.testAssaySample(sourceid, source)) {
			return new ResponseEntity<String>("POST must be a new submission, use PUT for updates",
					HttpStatus.BAD_REQUEST);
		}
		
		String accession = accessioner.singleAssaySample(sourceid, source);

		// because this is in POST, it must be a new submission, 
		// therefore it can't have an existing submission
		//this should never be true, but better safe than sorry....
		Optional<String> submission = relationalDAO.getSubmissionIDForSampleAccession(accession);
		if (submission.isPresent()) {
			return new ResponseEntity<String>("POST must be a new submission, use PUT for updates",
					HttpStatus.BAD_REQUEST);
		}
		
		return new ResponseEntity<String>(accession, HttpStatus.ACCEPTED);
	}

	@RequestMapping(value = "/source/{source}/sample/{sourceid}", method = RequestMethod.POST, produces = "text/plain", consumes = "application/xml")
	public @ResponseBody ResponseEntity<String> saveSourceSample(@PathVariable String source,
			@PathVariable String sourceid, @RequestParam String apikey, @RequestBody BioSampleType sample)
					throws ParseException, IOException {
		// ensure source is case insensitive
		source = source.toLowerCase();

		ResponseEntity<String> response = accessionSourceSample(source, sourceid, apikey);

		if (response.getStatusCode() == HttpStatus.ACCEPTED) {
			// a request body was provided, so save it somewhere
			// after adding the accession
			SampleData sd = bioSampleConverter.handleBioSampleType(sample);
			String accession = response.getBody();
			sd.scd.getNodes(SampleNode.class).iterator().next().setSampleAccession(accession);
			
			saveSampleData(sd, accession);			
		}

		return response;
	}

	@RequestMapping(value = "/source/{source}/sample/{sourceid}", method = RequestMethod.PUT, produces = "text/plain", consumes = "application/xml")
	public @ResponseBody ResponseEntity<String> saveUpdate(@PathVariable String source, @PathVariable String sourceid,
			@RequestParam String apikey, @RequestBody BioSampleType sample) throws ParseException, IOException {
		// ensure source is case insensitive
		source = source.toLowerCase();

		String keyOwner = null;
		try {
			keyOwner = apiKey.getAPIKeyOwner(apikey);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.FORBIDDEN);
		}

		if (!apiKey.canKeyOwnerEditSource(keyOwner, source)) {
			return new ResponseEntity<String>("That API key is not permitted for that source", HttpStatus.FORBIDDEN);
		}

		// a request body was provided, so handle it
		// after adding the accession
		SampleData sd = bioSampleConverter.handleBioSampleType(sample);
		SampleNode sampleNode = sd.scd.getNodes(SampleNode.class).iterator().next();
		ResponseEntity<String> response;
		String accession;

		// validate number of samples is not needed, since type specifys one
		// BioSample xml element

		if (sourceid.matches("SAM[NED]A?[0-9]+")) {
			// its a biosamples ID
			// check content accession matches address accession
			if (sampleNode.getSampleAccession() == null || !sampleNode.getSampleAccession().equals(sourceid)) {
				return new ResponseEntity<String>("Sample Accession in XML must match sourceid in URL ( "+sampleNode.getSampleAccession()+" vs "+sourceid+" )",
						HttpStatus.CONFLICT);
			}
			// accept this submission
			accession = sourceid;
			response = new ResponseEntity<String>(accession, HttpStatus.ACCEPTED);
		} else {
			// its not a biosamples id, but a source id
			accession = accessioner.retrieveAssaySample(sourceid, source);

			// reject if not already accessioned (PUT is an update)
			if (accession == null) {
				return new ResponseEntity<String>("PUT must be an update, use POST for new submissions or wait for submission to be processed",
						HttpStatus.BAD_REQUEST);
			}
			// check content accession matches address accession
			if (sampleNode.getSampleAccession() != null && !sampleNode.getSampleAccession().equals(accession)) {
				return new ResponseEntity<String>("Sample accession in XML must match previous accession ( "+sampleNode.getSampleAccession()+" vs "+accession+" )",
						HttpStatus.CONFLICT);
			}
			// accept this submission
			response = new ResponseEntity<String>(accession, HttpStatus.ACCEPTED);
		}

		//get the existing submission ID
		Optional<String> submission = relationalDAO.getSubmissionIDForSampleAccession(accession);
		if (submission.isPresent()) {
			sd.msi.submissionIdentifier = submission.get();
		} else {
			//no existing submission ID, refer them to post
			return new ResponseEntity<String>("PUT must be an update, use POST for new submissions",
					HttpStatus.BAD_REQUEST);
		}

		// save the output somewhere
		saveSampleData(sd, accession);

		return response;

	}
	
	
	@RequestMapping(value = "/source/{source}/sample/{sourceid}", method = RequestMethod.GET, produces = "text/plain")
	public @ResponseBody ResponseEntity<String> getAccessionOfSample(@PathVariable String source,
			@PathVariable String sourceid, @RequestParam String apikey) {

		// ensure source is case insensitive
		source = source.toLowerCase();
		String keyOwner = null;
		try {
			keyOwner = apiKey.getAPIKeyOwner(apikey);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.FORBIDDEN);
		}

		if (!apiKey.canKeyOwnerEditSource(keyOwner, source)) {
			return new ResponseEntity<String>("That API key is not permitted for that source", HttpStatus.FORBIDDEN);
		}

		if (sourceid.matches("SAMEA[0-9]+")) {
			return new ResponseEntity<String>(sourceid, HttpStatus.ACCEPTED);
		} else if (sourceid.matches("SAME[0-9]+")) {
			return new ResponseEntity<String>(sourceid, HttpStatus.ACCEPTED);
		} else {
			String acc = accessioner.retrieveAssaySample(sourceid, source);
			if (acc == null) {
				return new ResponseEntity<String>(sourceid+" not recognized", HttpStatus.NOT_FOUND);			
			} else {
				return new ResponseEntity<String>(acc, HttpStatus.ACCEPTED);
			}
		}
	}

	@RequestMapping(value = "/source/{source}/sample/{sourceid}/submission", method = RequestMethod.GET, produces = "text/plain")
	public @ResponseBody ResponseEntity<String> getSubmissionOfSample(@PathVariable String source,
			@PathVariable String sourceid, @RequestParam String apikey) {
		// ensure source is case insensitive
		source = source.toLowerCase();
		String keyOwner = null;
		try {
			keyOwner = apiKey.getAPIKeyOwner(apikey);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.FORBIDDEN);
		}

		if (!apiKey.canKeyOwnerEditSource(keyOwner, source)) {
			return new ResponseEntity<String>("That API key is not permitted for that source", HttpStatus.FORBIDDEN);
		}

		if (sourceid.matches("SAM[NED]A?[0-9]+")) {
			// its a biosamples ID
			Optional<String> submissionId = relationalDAO.getSubmissionIDForSampleAccession(sourceid);
			if (submissionId.isPresent()) {
				return new ResponseEntity<String>(submissionId.get(), HttpStatus.ACCEPTED);
			} else {
				return new ResponseEntity<String>("sample " + sourceid + " is not recognized", HttpStatus.NOT_FOUND);
			}
		} else {
			return new ResponseEntity<String>("Only implmemented for BioSample accessions", HttpStatus.FORBIDDEN);
		}
	}	
	
	/* sample end points above */
	/* group end points below */
	
	@RequestMapping(value = "/source/{source}/group", method = RequestMethod.POST, produces = "text/plain")
	public ResponseEntity<String> accessionSourceGroupNew(@PathVariable String source, @RequestParam String apikey) {
		return accessionSourceGroup(source, UUID.randomUUID().toString(), apikey);
	}

	@RequestMapping(value = "/source/{source}/group", method = RequestMethod.POST, produces = "text/plain", consumes = "application/xml")
	public ResponseEntity<String> saveSourceGroupNew(@PathVariable String source, @RequestParam String apikey,
			@RequestBody BioSampleGroupType group) throws ParseException, IOException {
		return saveSourceGroup(source, UUID.randomUUID().toString(), apikey, group);
	}

	@RequestMapping(value = "/source/{source}/group/{sourceid}", method = RequestMethod.POST, produces = "text/plain")
	public @ResponseBody ResponseEntity<String> accessionSourceGroup(@PathVariable String source,
			@PathVariable String sourceid, @RequestParam String apikey) {
		// ensure source is case insensitive
		source = source.toLowerCase();
		
		String keyOwner = null;
		try {
			keyOwner = apiKey.getAPIKeyOwner(apikey);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.FORBIDDEN);
		}
		if (!apiKey.canKeyOwnerEditSource(keyOwner, source)) {
			return new ResponseEntity<String>("That API key is not permitted for that source", HttpStatus.FORBIDDEN);
		}

		// reject if its a biosamples ID
		if (sourceid.matches("SAMG[0-9]+")) {
			return new ResponseEntity<String>(
					"POST must be a new submission, use PUT for updates",
					HttpStatus.BAD_REQUEST);
		} 
		
		// reject if already acessioned (POST is a one-time operation)
		if (accessioner.testGroup(sourceid, source)) {
			return new ResponseEntity<String>("POST must be a new submission, use PUT for updates",
					HttpStatus.BAD_REQUEST);
		}
		
		String accession = accessioner.singleGroup(sourceid, source);

		// because this is in POST, it must be a new submission, 
		// therefore it can't have an existing submission
		//this should never be true, but better safe than sorry....
		Optional<String> submission = relationalDAO.getSubmissionIDForGroupAccession(accession);
		if (submission.isPresent()) {
			return new ResponseEntity<String>("POST must be a new submission, use PUT for updates",
					HttpStatus.BAD_REQUEST);
		}
		
		return new ResponseEntity<String>(accession, HttpStatus.ACCEPTED);
	}

	@RequestMapping(value = "/source/{source}/group/{sourceid}", method = RequestMethod.POST, produces = "text/plain", consumes = "application/xml")
	public @ResponseBody ResponseEntity<String> saveSourceGroup(@PathVariable String source,
			@PathVariable String sourceid, @RequestParam String apikey, @RequestBody BioSampleGroupType group)
					throws ParseException, IOException {
		// ensure source is case insensitive
		source = source.toLowerCase();

		ResponseEntity<String> response = accessionSourceGroup(source, sourceid, apikey);

		if (response.getStatusCode() == HttpStatus.ACCEPTED) {
			// a request body was provided, so save it somewhere
			// after adding the accession
			SampleData sd = bioSampleConverter.handleBioSampleGroupType(group);
			String accession = response.getBody();
			sd.scd.getNodes(GroupNode.class).iterator().next().setGroupAccession(accession);
			
			saveGroupData(sd, accession);			
		}

		return response;
	}

	@RequestMapping(value = "/source/{source}/group/{sourceid}", method = RequestMethod.PUT, produces = "text/plain", consumes = "application/xml")
	public @ResponseBody ResponseEntity<String> saveGroupUpdate(@PathVariable String source, @PathVariable String sourceid,
			@RequestParam String apikey, @RequestBody BioSampleGroupType group) throws ParseException, IOException {
		// ensure source is case insensitive
		source = source.toLowerCase();

		String keyOwner = null;
		try {
			keyOwner = apiKey.getAPIKeyOwner(apikey);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.FORBIDDEN);
		}

		if (!apiKey.canKeyOwnerEditSource(keyOwner, source)) {
			return new ResponseEntity<String>("That API key is not permitted for that source", HttpStatus.FORBIDDEN);
		}

		// a request body was provided, so handle it
		// after adding the accession
		SampleData sd = bioSampleConverter.handleBioSampleGroupType(group);
		GroupNode groupNode = sd.scd.getNodes(GroupNode.class).iterator().next();
		ResponseEntity<String> response;
		String accession;

		// validate number of samples is not needed, since type specifies one
		// BioSample xml element

		if (sourceid.matches("SAM[NED]A?[0-9]+")) {
			// its a biosamples ID
			// check content accession matches address accession
			if (groupNode.getGroupAccession() == null || !groupNode.getGroupAccession().equals(sourceid)) {
				return new ResponseEntity<String>("Group Accession in XML must match sourceid in URL",
						HttpStatus.CONFLICT);
			}
			// accept this submission
			accession = sourceid;
			response = new ResponseEntity<String>(accession, HttpStatus.ACCEPTED);
		} else {
			// its not a biosamples id, but a source id
			accession = accessioner.retrieveGroup(sourceid, source);

			// reject if not already accessioned (PUT is an update)
			if (accession == null) {
				return new ResponseEntity<String>("PUT must be an update, use POST for new submissions",
						HttpStatus.BAD_REQUEST);
			}
			// check content accession matches address accession
			if (groupNode.getGroupAccession() != null && !groupNode.getGroupAccession().equals(sourceid)) {
				return new ResponseEntity<String>("Group accession in XML must match previous accession",
						HttpStatus.CONFLICT);
			}
			// accept this submission
			response = new ResponseEntity<String>(accession, HttpStatus.ACCEPTED);
		}

		//get the existing submission ID
		Optional<String> submission = relationalDAO.getSubmissionIDForGroupAccession(accession);
		if (submission.isPresent()) {
			sd.msi.submissionIdentifier = submission.get();
		} else {
			//no existing submission ID, refer them to post
			return new ResponseEntity<String>("PUT must be an update, use POST for new submissions",
					HttpStatus.BAD_REQUEST);
		}

		// save the output somewhere
		saveGroupData(sd, accession);

		return response;

	}
	
	
	@RequestMapping(value = "/source/{source}/group/{sourceid}", method = RequestMethod.GET, produces = "text/plain")
	public @ResponseBody ResponseEntity<String> getAccessionOfGroup(@PathVariable String source,
			@PathVariable String sourceid, @RequestParam String apikey) {

		// ensure source is case insensitive
		source = source.toLowerCase();
		String keyOwner = null;
		try {
			keyOwner = apiKey.getAPIKeyOwner(apikey);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.FORBIDDEN);
		}

		if (!apiKey.canKeyOwnerEditSource(keyOwner, source)) {
			return new ResponseEntity<String>("That API key is not permitted for that source", HttpStatus.FORBIDDEN);
		}

		if (sourceid.matches("SAMEG[0-9]+")) {
			return new ResponseEntity<String>(sourceid, HttpStatus.ACCEPTED);
		} else {
			String acc = accessioner.retrieveGroup(sourceid, source);
			if (acc == null) {
				return new ResponseEntity<String>(sourceid+" not recognized", HttpStatus.NOT_FOUND);			
			} else {
				return new ResponseEntity<String>(acc, HttpStatus.ACCEPTED);
			}
		}
	}

	@RequestMapping(value = "/source/{source}/group/{sourceid}/submission", method = RequestMethod.GET, produces = "text/plain")
	public @ResponseBody ResponseEntity<String> getSubmissionOfGroup(@PathVariable String source,
			@PathVariable String sourceid, @RequestParam String apikey) {
		// ensure source is case insensitive
		source = source.toLowerCase();
		String keyOwner = null;
		try {
			keyOwner = apiKey.getAPIKeyOwner(apikey);
		} catch (IllegalArgumentException e) {
			return new ResponseEntity<String>(e.getMessage(), HttpStatus.FORBIDDEN);
		}

		if (!apiKey.canKeyOwnerEditSource(keyOwner, source)) {
			return new ResponseEntity<String>("That API key is not permitted for that source", HttpStatus.FORBIDDEN);
		}

		if (sourceid.matches("SAMEG[0-9]+")) {
			// its a biosamples ID
			Optional<String> submissionId = relationalDAO.getSubmissionIDForGroupAccession(sourceid);
			if (submissionId.isPresent()) {
				return new ResponseEntity<String>(submissionId.get(), HttpStatus.ACCEPTED);
			} else {
				return new ResponseEntity<String>("group " + sourceid + " is not recognized", HttpStatus.NOT_FOUND);
			}
		} else {
			return new ResponseEntity<String>("Only implmemented for BioSample accessions", HttpStatus.FORBIDDEN);
		}
	}	

	/* group endpoints above*/
	
	private void saveSampleData(SampleData sd, String sampleAcc) throws IOException {
		// TODO check this is a sensible submission to be overwriting
		Optional<Set<String>> sampleAccs = relationalDAO.getSubmissionSampleAccessions(sampleAcc);
		Optional<Set<String>> groupAccs = relationalDAO.getSubmissionGroupAccessions(sampleAcc);
		if (sampleAccs.isPresent()) {
			// if there is a previous submission, must be only this sample in it and no groups
			if (sampleAccs.get().size() != 1 
					|| (groupAccs.isPresent() && groupAccs.get().size() > 0)) {
				throw new IllegalStateException("Cannot update a SampleTab submission via XML");
			}

			String oldSampleAccession = sampleAccs.get().iterator().next();
			if (!oldSampleAccession.equals(sampleAcc)) {
				// should never reach here, but just in case...
				throw new IllegalStateException(
						"Submission owns a different sample (" + oldSampleAccession + " instead of " + sampleAcc + ")");
			}
		}
		saveData(sd);
	}
	
	private void saveGroupData(SampleData sd, String groupAcc) throws IOException {
		// TODO check this is a sensible submission to be overwriting
		Optional<Set<String>> sampleAccs = relationalDAO.getSubmissionSampleAccessions(groupAcc);
		Optional<Set<String>> groupAccs = relationalDAO.getSubmissionGroupAccessions(groupAcc);
		if (groupAccs.isPresent()) {
			// if there is a previous submission, must be only this group in it and no samples
			if (groupAccs.get().size() != 1
					|| (sampleAccs.isPresent() && sampleAccs.get().size() > 0)) {
				throw new IllegalStateException("Cannot update a SampleTab submission via XML");
			}

			String oldGroupAccession = groupAccs.get().iterator().next();
			if (!oldGroupAccession.equals(groupAcc)) {
				// should never reach here, but just in case...
				throw new IllegalStateException(
						"Submission owns a different group (" + oldGroupAccession + " instead of " + groupAcc + ")");
			}
		}
		saveData(sd);
	}

	private void saveData(SampleData sd) throws IOException {
		// may need to assign a submission id
		if (sd.msi.submissionIdentifier == null) {
			//note that this isn't strictly an atomic operation, so may have issues with collisions between 
			//multiple servers writing to the same place at the same time
			int maxSubID = 0;
			Pattern pattern = Pattern.compile("^GSB-([0-9]+)$");
			File pathSubdir = new File(getSubmissionPath(), "GSB");
			for (File subdir : pathSubdir.listFiles()) {
				if (!subdir.isDirectory()) {
					continue;
				} else {
					log.trace("Looking at subid " + subdir.getName() + " with pattern " + pattern.pattern());
					Matcher match = pattern.matcher(subdir.getName());
					if (match.matches()) {
						log.trace("Found match with " + match.groupCount() + " groups " + match.group(1));
						Integer subid = new Integer(match.group(1));
						if (subid > maxSubID) {
							maxSubID = subid;
						}
					}
				}
			}
			maxSubID++;
			File subDir = new File(getSubmissionPath(),
					SampleTabUtils.getSubmissionDirFile("GSB-" + maxSubID).toString());
			if (!subDir.mkdirs()) {
				throw new IOException("Unable to create submission directory");
			}
			// can't do linux-specific group writing, so make all writable.
			// won't be too bad, since the parent directory should not be world
			// writable
			subDir.setWritable(true, false);

			sd.msi.submissionIdentifier = "GSB-" + maxSubID;
		}

		File subdir = new File(getSubmissionPath(),
				SampleTabUtils.getSubmissionDirFile(sd.msi.submissionIdentifier).toString());
		File outFile = new File(subdir, "sampletab.pre.txt");

		SampleTabWriter writer = null;
		try {
			if (!subdir.exists() && !subdir.mkdirs()) {
				throw new IOException("Unable to create parent directories");
			}
			writer = new SampleTabWriter(new BufferedWriter(new FileWriter(outFile)));
			writer.write(sd);
			log.info("wrote to " + outFile);
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (IOException e) {
					// do nothing
				}
			}
		}

		/*
		 * 
		 * At this point, its been written to a temporary staging area by the
		 * owner of the executing tomcat.
		 * 
		 * A cron script (running as the internal user) will pick it up and copy
		 * it to the real file area. That script will trigger Conan for
		 * downstream processing.
		 * 
		 */
	}

}
