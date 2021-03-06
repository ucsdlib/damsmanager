package edu.ucsd.library.xdre.imports;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import com.hp.hpl.jena.graph.Triple;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResourceFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.util.iterator.ExtendedIterator;

import edu.ucsd.library.xdre.collection.MetadataImportHandler;
import edu.ucsd.library.xdre.tab.ExcelSource;
import edu.ucsd.library.xdre.tab.SubjectTabularRecord;
import edu.ucsd.library.xdre.utils.AudioMetadata;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DAMSRepository;
import edu.ucsd.library.xdre.utils.DFile;
import edu.ucsd.library.xdre.utils.DamsURI;
import edu.ucsd.library.xdre.utils.EmbeddedMetadata;
import edu.ucsd.library.xdre.utils.ImageWatermarking;
import edu.ucsd.library.xdre.utils.RDFStore;
import edu.ucsd.library.xdre.utils.VideoMetadata;
import edu.ucsd.library.xdre.utils.Watermarking;
import edu.ucsd.library.xdre.utils.ZoomifyTilesConverter;

/**
 * 
 * RDFDAMS4ImportTsHandler: Import objects (metadata and files) in DAMS4 RDF/XML format with support to lookup records from the triplestore.
 * @author lsitu@ucsd.edu
 */
public class RDFDAMS4ImportTsHandler extends MetadataImportHandler{
	public static final String LICENSE = "License";
	public static final String PERMISSION = "Permission";
	public static final String RESTRICTION = "Restriction";
	public static final String RELATEDRESOURCE = "RelatedResource";
	public static final String SOURCECAPTURE = "SourceCapture";
	public static final String COPYRIGHT = "Copyright";
	public static final String OTHERRIGHTS = "OtherRights";
	public static final String NOTE = "Note";
	public static final String MADSSCHEME = "MADSScheme";
	public static final String LANGUAGE = "Language";

	private static Logger log = Logger.getLogger(RDFDAMS4ImportTsHandler.class);

	private Map<String, String> idsMap = new HashMap<String, String>();
	private Map<String, File> filesMap = new HashMap<String, File>();
	
	private DAMSClient damsClient = null;
	private DAMSRepository damsRepository = null;
	private String importOption = null;
	private File[] rdfFiles = null;
	private String[] filesPaths = null;

	//private int objectsCount = 0;
	private int recordsCount = 0;
	private int filesCount = 0;
	private int ingestFailedCount = 0;
	private int derivFailedCount = 0;
	private int solrFailedCount = 0;
	private Map<String, String> objRecords = new HashMap<String, String>();
	private Map<String, String> collRecords = new HashMap<String, String>();
	private List<String> recordsIngested = new ArrayList<String>();
	
	private List<String> objWithFiles = new ArrayList<String>();
	private StringBuilder ingestFailed = new StringBuilder();
	private StringBuilder metadataFailed = new StringBuilder();
	private StringBuilder derivativesFailed = new StringBuilder();
	private StringBuilder filesIngested = new StringBuilder();
	private StringBuilder arkReport = new StringBuilder();
	private StringBuilder authorityReport = new StringBuilder();
	private StringBuilder objectArkReport = null;

	private boolean replace = false;
	private boolean watermarking = false;

	private int processIndex = 0;
	private boolean[] status = null;
	private StringBuilder[] messages = null;
	private String[] processNames = {"Object RDF/XML validation", "Object metadata build", "File ingest", "SOLR index request"};
	private StringBuilder ingestMessages = new StringBuilder();
	private String preprocessedTimestamp = "";

	/**
	 * Constructor
	 * @param damsClient
	 * @param rdf
	 * @param mode
	 * @throws Exception
	 */
	public RDFDAMS4ImportTsHandler(DAMSClient damsClient, File[] rdfFiles, String importOption) throws Exception {
		super(damsClient, null);
		this.damsClient = damsClient;
		this.rdfFiles = rdfFiles;
		this.importOption = importOption;
		this.damsRepository = DAMSRepository.getRepository();
	}

	public String[] getFilesPaths() {
		return filesPaths;
	}

	public void setFilesPaths(String[] filesPaths) {
		this.filesPaths = filesPaths;
	}
	
	public boolean isReplace() {
		return replace;
	}

	public void setReplace(boolean replace) {
		this.replace = replace;
	}

	/**
	 * Get the watermarking flag
	 */
	public boolean isWatermarking() {
		return watermarking;
	}

	/**
	 * Set the watermarking flag
	 * @param watermarking
	 */
	public void setWatermarking(boolean watermarking) {
			this.watermarking = watermarking;
	}
	
	public String getPreprocessedTimestamp() {
		return preprocessedTimestamp;
	}

	public void setPreprocessedTimestamp(String preprocessedTimestamp) {
		this.preprocessedTimestamp = preprocessedTimestamp;
	}

	/**
	 * Procedure to populate the RDF metadata and ingest the files
	 */
	@Override
	public boolean execute() throws Exception {
		if(filesPaths != null){
			File file = null;
			// List the source files
			for(int i=0; i<filesPaths.length; i++){
				file = new File(filesPaths[i]);
				if(file.exists()){
					listFile(filesMap, file);
				}
			}
		}
		
		String message = "";
		Document doc = null;
		DamsURI damsURI = null;

		List<String> recordsToReplace = null;		
		String oid = null;
		int fLen = rdfFiles.length;
		String currFile = null;
		SAXReader saxReader = new SAXReader();
		boolean reportTitleAdded = false;
		for(int i=0; i<fLen&&!interrupted; i++){
			recordsToReplace = new ArrayList<>();
			currFile = rdfFiles[i].getName();

			preprocessedTimestamp = damsDateFormat.format(rdfFiles[i].lastModified());

			setStatus("Processing external import for file " + currFile + " (" + (i+1) + " of " + fLen + ") ... " );
			try{		
				doc = saxReader.read(rdfFiles[i]);
				List<Node> nodes = doc.selectNodes("//@rdf:about");
				for(int j=0; j<nodes.size(); j++){
					Node nUri = nodes.get(j);
					String iUri = nUri.getStringValue();
					Node parentNode = nUri.getParent();
					String nName = parentNode.getName();

					// initiate ARK report header
					if (!reportTitleAdded && (nName.endsWith("Object") || nName.endsWith("Collection") 
							&& (((Element)parentNode).isRootElement() || parentNode.getParent().getName().equals("RDF")))) {
						reportTitleAdded = true;
						logMessage ( (nName.indexOf("Collection") >= 0 ? "Collection" : "Object") + " Import status:\n[Title]   -   [URI]   -   [Status]   -   [Timestamp]");
						toArkReport (arkReport, "ARK", "Title", "File name", "Outcome", "Event date");
					}

					if (iUri.endsWith("/COL") || !(iUri.startsWith("http") && iUri.indexOf("/ark:/") > 0)){
						// Assign ARK
						Element grNode = parentNode.getParent();
						if(nName.endsWith("Object") || nName.endsWith("Component") || nName.endsWith("File")
								|| (nName.indexOf("Collection") >= 0 && (((Element)parentNode).isRootElement() || grNode.getName().equals("RDF")))){

							String objId = iUri;
							
							if(nName.endsWith("Component") || nName.endsWith("File")){
								damsURI = DamsURI.toParts(iUri, null);
								objId = damsURI.getObject();
							}
							String srcObjKey = objId + "::" + rdfFiles[i].getAbsolutePath();
							oid = idsMap.get(srcObjKey);
							
							// Assign new ARK
							if(oid == null){
								oid = getNewId();
								idsMap.put(srcObjKey, oid);
							}

							if(nName.endsWith("Object")){
								objId = oid;
								objRecords.put(objId, currFile);
							}else if(nName.endsWith("Component") || nName.endsWith("File")){
								damsURI.setObject(oid);
								// XXX
								// Assign cid and fid for Component and FIle if required
								objId = damsURI.toString();
							}else
								objId = oid;
							nUri.setText(objId);
							updateReference(doc, iUri, objId);
						} else {
							String field = null;
							Node tNode = null;
							String xPath = null;
							Map<String, String> props= new TreeMap<String, String>();
							String elemXPath = parentNode.getPath();
							if (nName.endsWith("Collection") || nName.endsWith("CollectionPart")){
								collRecords.put(iUri, currFile);
								// Retrieve the Collection record
								field = "dams:title/mads:authoritativeLabel";
								xPath = "dams:title/mads:Title/mads:authoritativeLabel";
								tNode = parentNode.selectSingleNode(xPath);
								if(tNode == null){
									// Loop through to locate the rdfs:label if not selected by xPath.
									Node n = parentNode.selectSingleNode("dams:title");
									for(Iterator<Element> it=((Element)n).elementIterator(); it.hasNext();){
										Element elem = it.next();
										if(elem.getNamespacePrefix().equals("mads") && elem.getName().equals("Title"))
											tNode = elem.selectSingleNode("mads:authoritativeLabel");
									}
								}
							}/* else if (nName.endsWith("Language") || nName.endsWith("Authority") || nName.endsWith("Subject") || nName.endsWith("Name") || nName.endsWith("Topic") || nName.endsWith("GenreForm") || nName.endsWith("Temporal") || nName.endsWith("Geographic")){
								// Subject, Authority records use mads:authoritativeLabel
								field = "name_tesim";
								xPath = "mads:authoritativeLabel";
								tNode = parentNode.selectSingleNode(xPath);
							} */else if (nName.endsWith(COPYRIGHT)){
								// Copyright records use dams:copyrightStatus, plus other properties in the next step.
								field = "dams:copyrightStatus";
								xPath = "dams:copyrightStatus";
								tNode = parentNode.selectSingleNode(xPath);
								props = copyrightProperties(parentNode);
							} else if (nName.endsWith(LICENSE)){
								// License records use dams:LicenseNote, plus other properties in the next step.
								field = "dams:licenseNote";
								xPath = "dams:licenseNote";
								tNode = parentNode.selectSingleNode(xPath);
								props = licenseProperties(parentNode);
							} else if (nName.endsWith(OTHERRIGHTS)){
								// Copyright records use dams:copyrightStatus, plus other properties in the next step.
								field = "dams:otherRightsBasis";
								xPath = "dams:otherRightsBasis";
								tNode = parentNode.selectSingleNode(xPath);
								props = otherRightsProperties(parentNode);
							} else if (nName.endsWith(RELATEDRESOURCE)){
								// RelatedResource records use dams:description, plus other properties in the next step.
								field = "dams:description";
								xPath = "dams:description";
								tNode = parentNode.selectSingleNode(xPath);
								props = relatedResourceProperties(parentNode);
							} else if (nName.endsWith(SOURCECAPTURE)){
								// SourceCapture records use dams:sourceType, plus other properties in the next step.
								field = "dams:sourceType";
								xPath = "dams:sourceType";
								tNode = parentNode.selectSingleNode(xPath);
								props = sourceCaptureProperties(parentNode);
							} else if (nName.endsWith(NOTE)){
								// Note records use rdf:value, dams:type, dams:displayLabel.
								field = "rdf:value";
								xPath = "rdf:value";
								tNode = parentNode.selectSingleNode(xPath);
								props = noteProperties(parentNode);
							} else if(nName.endsWith(PERMISSION) || nName.equals(RESTRICTION)){
								field = "dams:type";
								xPath = "dams:type";
								tNode = parentNode.selectSingleNode(xPath);
								props = dateProperties(parentNode);
							} else if(elemXPath.indexOf("mads", elemXPath.lastIndexOf('/') + 1) >= 0){
								// MADSScheme and Language
								if(nName.endsWith(MADSSCHEME)){
									field = "mads:code";
									xPath = "mads:code";
									tNode = parentNode.selectSingleNode(xPath);
									if(tNode == null){
										field = "rdfs:label";
										xPath = "rdfs:label";
										tNode = parentNode.selectSingleNode("*[name()='" + xPath + "']");
									}
								} else {
									// Subject, Authority records use mads:authoritativeLabel
									field = "mads:authoritativeLabel";
									xPath = "mads:authoritativeLabel";
									tNode = parentNode.selectSingleNode(xPath);
									if(tNode == null){
										// Try to use the mads:code for mapping when mads:authoritativeLabel is not available
										field = "mads:code";
										xPath = "mads:code";
										tNode = parentNode.selectSingleNode(xPath);
									}else{
										Node diplayLabelNode = parentNode.selectSingleNode("*[name()='dams:displayLabel']");
										props.put("dams:displayLabel", encodeLiteralValue(diplayLabelNode));
									}

									// ID matching for mads:hasExactExternalAuthority
									String madsHasExactExternalAuthority = null;
									Node madsHasExactExternalAuthorityNode = parentNode.selectSingleNode("mads:hasExactExternalAuthority");
									if(madsHasExactExternalAuthorityNode != null){
										Node mhValueNode = madsHasExactExternalAuthorityNode.selectSingleNode("@rdf:resource");
										if (mhValueNode != null){
											madsHasExactExternalAuthority = madsHasExactExternalAuthorityNode.getStringValue();
											props.put("mads:hasExactExternalAuthority", "<" + madsHasExactExternalAuthority + ">");
										}
									}
								}
								
							} else {
								// other dams:Subject records that use mads:authoritativeLabel
								field = "mads:authoritativeLabel";
								xPath = "mads:authoritativeLabel";
								tNode = parentNode.selectSingleNode(xPath);
								if(tNode == null) {
									// XXX Other Rights records like Statute, License, Other Rights etc. 
									field = "rdf:value";
									xPath = "rdf:value";
									tNode = parentNode.selectSingleNode(xPath);
									field = "dams:code";
									if (tNode == null) {
										xPath = "dams:code";
										tNode = parentNode.selectSingleNode(xPath);
									}
								}
							}
							if(tNode == null && !field.equals("dams:licenseNote")){
								throw new Exception("Element " + xPath + " is missing from the " + nName + " record " + iUri + " in file " + currFile + ".");
							}
							
							updateDocument(doc, parentNode, field, encodeLiteralValue(tNode), props);
						}
					}else{
						if (nName.endsWith("Object"))
							objRecords.put(iUri, currFile);
						if (nName.endsWith("Collection") || nName.endsWith("CollectionPart"))
							collRecords.put(iUri, currFile);
						if (replace && !(nName.endsWith("Component") || nName.endsWith("File")))
							recordsToReplace.add(iUri);
					}
				}			

				String dams4Rdf = doc.asXML();
				logData("dams4_" + rdfFiles[i].getName(),  dams4Rdf);
				
				// Ingest the records
				String subjectId = null;
				DamsURI objURI = null;
				List<DamsURI> objURIs = null;
				RDFStore graph = null;
				
				rdfStore = new RDFStore();
				rdfStore.loadRDFXML(dams4Rdf);
				initHandler();
				
				Model iRdf = null;

				items = sortRecords(items);
				int jLen = items.size();
				//System.out.println(currFile + " records found: " + jLen);
				for (int j=0; j<jLen&&!interrupted; j++) {

					processIndex = 0;
					status = new boolean[processNames.length];
					messages = new StringBuilder[processNames.length];
					for (int k = 0; k <messages.length; k++) {
						messages[k] = new StringBuilder();
					}
					objectArkReport = new StringBuilder();
					
					String eventDate = damsDateFormat.format(new Date());

					// Logging for Object RDF/XML validation, which is successful.
					status[0] = true;
					messages[processIndex].append(eventDate);

					Model objModel = null;
					graph = new RDFStore();
					recordsCount++;
					// Add subject
					subjectId = items.get(j);
					try{
						setStatus("Processing metadata for record " + subjectId  + " (" + (j+1) + " of " + jLen + ") in file " + currFile + " ... " ); 
						boolean succeeded = false;
						objURIs = objects.get(subjectId);
	
						for(int k=0; k<objURIs.size(); k++){
							objURI = objURIs.get(k);
							iRdf = rdfStore.querySubject(objURI.toString());
							objModel = graph.merge(iRdf);
						}
						
						String rdfXml = graph.export(RDFStore.RDFXML_ABBREV_FORMAT);
						if (DAMSRepository.isAuthorityRecord(subjectId, graph.getModel())) {
							// create authority record
							try (InputStream in = new ByteArrayInputStream(rdfXml.getBytes("UTF-8"))) {
								succeeded = damsRepository.createAuthorityRecord(saxReader.read(in)) != null;
							}
						} else {
							// Update object
							String importMode = Constants.IMPORT_MODE_ADD;
							if (replace && recordsToReplace.indexOf(subjectId) >= 0)
								importMode = Constants.IMPORT_MODE_ALL;
							succeeded = damsClient.updateObject(subjectId, rdfXml, importMode);
						}

						eventDate =  damsDateFormat.format(new Date());
						if(!succeeded){
							exeResult = false;

							if(metadataFailed.indexOf(currFile) < 0)
								failedCount++;
							metadataFailed.append(subjectId + " (" + currFile + "), \n");
							message = "Metadata import for record " + subjectId  + " failed (" + (j+1) + " of " + jLen + ") in file " + currFile + ".";
							setStatus( message ); 
							log.error(message + "\n Error RDF: \n" + graph.export(RDFStore.RDFXML_ABBREV_FORMAT));
						}else{
							recordsIngested.add(subjectId);
							message = "Metadata import for record " + subjectId  + " succeeded (" + (j+1) + " of " + jLen + ") in file " + currFile + ". ";
							setStatus(message); 
							log.info(message);
							
							processIndex++;
							status[processIndex] = succeeded;
							messages[processIndex].append(damsDateFormat.format(new Date()));
							// Ingest the source file only if metadata ingested successfully
							if(status[processIndex] && importOption.equalsIgnoreCase("metadataAndFiles")){
								uploadFiles(objModel, currFile, subjectId);
							}
						}
					
					} catch (Exception e) {
						e.printStackTrace();
						exeResult = false;

						if(metadataFailed.indexOf(currFile) < 0)
							failedCount++;
						metadataFailed.append(subjectId + " (" + currFile + "), \n");
						message = "Metadata import failed: " + e.getMessage();
						setStatus( message  + " (" +(j+1)+ " of " + jLen + ") in file " + currFile + "."); 
						log.error(message);
						
						String error = e.getMessage();
						if (error != null && error.indexOf("Invalid RDF input") >= 0) {
							messages[processIndex].append(error);
						} else {
							status[processIndex] = true;
							messages[processIndex].append(damsDateFormat.format(new Date()));
							processIndex++;
							messages[processIndex].append(error);
						}
					} finally {
						int solrRequestIndex = processNames.length - 1;
						try{
							// Update SOLR for the record.
							status[solrRequestIndex] = updateSOLR(subjectId);
							messages[solrRequestIndex].append(damsDateFormat.format(new Date()));
							log.info("SOLR update requested for " + subjectId + ": " + damsClient.getRequestURL() + " " + status[solrRequestIndex]);

						} catch(Exception e) {
							e.printStackTrace();
							exeResult = false;
							log.error("SOLR Index failed " + subjectId + ": " + e.getMessage());
							
							messages[processNames.length - 1].append(e.getMessage());
						}
						
						if(exeResult)
							exeResult = status[processIndex];

						
						String resultMessage = "http://" + Constants.CLUSTER_HOST_NAME + ".ucsd.edu/dc/object/" + subjectId.substring(subjectId.lastIndexOf("/") + 1)
								+ " - " + (status[processIndex] && status[solrRequestIndex] ? "successful" : "failed") + " - " + damsDateFormat.format(new Date());
						if (objRecords.containsKey(subjectId) || collRecords.containsKey(subjectId)) {
							String title = getTitle(objModel, subjectId);
							if (StringUtils.isBlank(title))
								title = "[Unknown Title]";

							String damsNsPrefixUri = objModel.getNsPrefixURI("dams");
							ExtendedIterator<Triple> filesIt = objModel.getGraph().find(ResourceFactory.createResource(subjectId).asNode(), 
									ResourceFactory.createProperty(damsNsPrefixUri + "hasFile").asNode(), com.hp.hpl.jena.graph.Node.ANY);
							// csv format ark report for object records with no files
							if (!filesIt.hasNext() || importOption != null && importOption.equalsIgnoreCase("metadataOnly")) {
								String sourceFile = "No associated file";
								if (filesIt.hasNext()) {
									sourceFile = getSourceFiles(doc, subjectId);
								}

								toArkReport (
										objectArkReport,
										subjectId.substring(subjectId.lastIndexOf("/") + 1), 
										(StringUtils.isNotBlank(title) ? title : "[Unknown Title]"), 
										sourceFile,
										status[processIndex] ? "successful" : "failed", 
										eventDate
										);
							}
							arkReport.append(objectArkReport.toString());

							logMessage ("\n" + title + " - " + resultMessage);
							if (!status[processIndex] || !status[solrRequestIndex]) {
								// Logging for pre-procesing - succeeded. 
								logMessage ("* Pre-processing - successful - " + preprocessedTimestamp);
								for (int k = 0; k <= processIndex; k++) {
									if (status[k] || !status[k] && status[k - 1]) {
										logMessage ("* " + processNames[k] + " - " + (status[k] ? "successful" : "failed") + " - " + messages[k].toString());
									}
								}
								
								// SOLR index request logging
								if (!status[solrRequestIndex])
									logMessage ("* " + processNames[solrRequestIndex] + " - " + (status[solrRequestIndex] ? "successful" : "failed") + " - " + messages[solrRequestIndex].toString());
							}
							
						} else {

							ingestMessages.append("\n" + resultMessage);
							if (!status[processIndex]) {
								for (int k = 0; k + 1 < processIndex; k++) {
									if (status[k] || !status[k] && status[k - 1]) {
										logMessage ("* " + processNames[k] + " - " + (status[k] ? "successful" : "failed") + " - " + messages[k].toString());
									}
								}
							}
						}
					}
					
					try{
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
						interrupted = true;
						exeResult = false;
						failedCount++;
						metadataFailed.append(subjectId + " (" + currFile + ") \n");
						message = "Metadata import interrupted for subject " + subjectId  + ". \n Error: " + e.getMessage() + "\n";
						setStatus("Canceled");
						clearSession();
						log.error(message);
						
						logMessage ("Client Cancled - " + damsDateFormat.format(new Date()));
					}
				}

			}catch(Exception e){
				e.printStackTrace();
				exeResult = false;
				failedCount++;
				message = "Import failed for " + currFile + ": " + e.getMessage();
				setStatus( message  + " (" +(i+1)+ " of " + fLen + ").");
				log.error(message);
			}
			
			setProgressPercentage( ((i + 1) * 100) / fLen);
			
			try{
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
				exeResult = false;
				interrupted = true;
				failedCount++;
				message = "Import interrupted for oject in " + currFile + ". \n Error: " + e.getMessage() + "\n";
				setStatus("Canceled");
				clearSession();
				log.error(message);
				
				messages[processIndex].append("Client canceled - " + damsDateFormat .format(new Date()));
			}
		}

		// Re-index the collection CLR when object ingested and add records added event.
		if (objRecords.size() > 0 && arkReport.length() > 0 && StringUtils.isNotBlank(collectionId)) {
			updateSOLR(collectionId, DAMSClient.RECORD_ADDED);
		}

		// write the ark report to file
		if (arkReport.length() > 0 || authorityReport.length() > 0) {
			FileOutputStream out = null;
			File arkReportFile = getArkReportFile();
			try {
				out = new FileOutputStream(arkReportFile);
				if (arkReport.length() > 0) {
					out.write(arkReport.toString().getBytes());
				} else {
					// report authoritative records created/found.
					out.write("Subject Type,Subject Term,Action,ARK\n".getBytes());
					out.write(authorityReport.toString().getBytes());
				}
			} catch (Exception e) {
				e.printStackTrace();
			} finally {
				close(out);
			}
		}
		return exeResult;
	}
	
	public int uploadFiles(Model rdf, String srcName, String subjectURL){
		String message = "";
		String oid = null;
		String cid = null;
		String fid = null;
		String fileUrl = null;
		String srcPath = null;
		String srcFileName = null;
		Statement stmt = null;
		Statement tmpStmt = null;
		Property prop = null;
		RDFNode fNode = null;
		RDFNode oNode = null;
		Map<String, String> params = null;
		List<Statement> oStmt = rdf.listStatements().toList();
		List<String> ocArkReported = new ArrayList<>();
		Map<String, Statement> fileStmts = new TreeMap<String, Statement>();
		int iLen = oStmt.size();
		// Retrieve the statements for files
		for(int l=0; l<iLen&&!interrupted;l++){
			stmt = oStmt.remove(0);
			prop = stmt.getPredicate();
			if(prop.getLocalName().equals(HAS_FILE) && stmt.getSubject().getURI().indexOf(subjectURL) >= 0){
				fileUrl = stmt.getObject().asResource().getURI();
				fileStmts.put(fileUrl, stmt);
			}
		}
		// sorted list
		oStmt = Arrays.asList(fileStmts.values().toArray(new Statement[fileStmts.size()]));
		
		boolean successful = true;
		iLen = oStmt.size();

		for(int l=0; l<iLen&&!interrupted;l++){
			if (l == 0)
				processIndex++;
			
			filesCount++;
			boolean ingested = true;
			StringBuilder ingestLog = new StringBuilder();
			stmt = oStmt.get(l);
			fNode = stmt.getObject();
			fileUrl = fNode.asResource().getURI();
			List<Statement> stmts = rdf.listStatements(fNode.asResource(), null, oNode).toList();
			String use = null;
			for(int j=0; j<stmts.size(); j++){
				// File properties. The use property will be retrieved.
				tmpStmt = stmts.get(j);
				prop = tmpStmt.getPredicate();
				String localName = prop.getLocalName();
				if(localName.equalsIgnoreCase(DFile.SOURCE_PATH)){
					srcPath = tmpStmt.getObject().asLiteral().getString();
				}else if(localName.equalsIgnoreCase(DFile.SOURCE_FILE_NAME)){
					srcFileName = tmpStmt.getObject().asLiteral().getString();
				}else if (localName.equalsIgnoreCase(DFile.USE)){
					use = tmpStmt.getObject().asLiteral().getString();
				}
			}
			
			setStatus("Ingesting file " + fileUrl + " (" + srcFileName + ", " + (l+1) + " of " + iLen + ") in " + srcName + " ... " );
			
			if(srcFileName!=null) {
				String fName = srcFileName;
				File srcFile = null;
				
				if(fName.startsWith("http")){
					// XXX URL resource, handle it for local file for now
					int idx = fName.lastIndexOf('/');
					if(idx >= 0 && fName.length() > idx + 1)
						fName = fName.substring(idx+1);
				}
				
				if(srcPath != null)
					srcFile = new File(srcPath + "/" + fName);
				
				if(srcFile == null || !srcFile.exists()){
					// Retrieve the file locally
					srcFile = filesMap.get(fName);
				}

				// Ingest the file
				DamsURI dURI = null;
				try{
					dURI = DamsURI.toParts(fileUrl, null);
					dURI = DamsURI.toParts(fileUrl, null);
					oid = dURI.getObject();
					cid = dURI.getComponent();
					fid = dURI.getFileName();
					
					if(srcFile == null){
						ingestFailedCount++;
						ingestFailed.append(fileUrl + " (" + srcFileName + ")\t\n");
						log.error("Source file for " + srcFileName + " in " + srcName + " doesn't exist. Please choose a correct file location from the dams staging area.");
						
						ingested = false;
						successful = false;
						ingestLog.append("\n    Ingest - failed - " + damsDateFormat.format(new Date()) + " - " + "Source file is not found.");
					}else{
						
						String tmpFile = srcFile.getAbsolutePath();

						params = new HashMap<String, String>();
						params.put("oid", oid);
						params.put("cid", cid);
						params.put("fid", fid);
						params.put("use", use);
						params.put("local", tmpFile);
						params.put("sourceFileName", srcFileName);
						
						// File ingest and Jhove extraction
						if(!damsClient.uploadFile(params, replace)){
							ingestFailedCount++;
							ingestFailed.append(fileUrl + " (" + srcFileName + ")\t\n");
							log.error("Error ingesting file " + fileUrl  + " (" + tmpFile + ", " + (l+1) + " of " + iLen + ") in " + srcName + ".");

							ingested = false;
							successful = false;
							ingestLog.append("\n    Ingest - failed - " + damsDateFormat.format(new Date()));
						}else{
							ingestLog.append("\n    Ingest - successful - " + damsDateFormat.format(new Date()));
							ingestLog.append("\n    JHOVE extraction - successful - " + damsDateFormat.format(new Date()));
							
							message = "Ingested file " + fileUrl + " (" + srcFileName + ", " + (l+1) + " of " + iLen + ") in " + srcName + ". ";
							log.info(message);
							setStatus(message);
							filesIngested.append(fileUrl + "\t" + srcFileName + "\t" + damsClient.getRequestURL() + "\n");
							
							// Add for SOLR update
							if(objWithFiles.indexOf(oid) < 0)
								objWithFiles.add(oid);
							
							//Create/update derivatives for images and documents PDFs
							if( isDerivativesRequired(fid, use) ) {
								try {
									boolean derCreated = false;
									if (isVideo(fid, use) || isAudio(fid, use)) {
										String deriName = "2.mp4";
										if (isAudio(fid, use))
											deriName = "2.mp3";
										
										String[] deriSizes = {deriName};
										derCreated = damsClient.updateDerivatives(oid, cid, fid, deriSizes);
									} else {
										derCreated = damsClient.updateDerivatives(oid, cid, fid, null);
									}

									if(derCreated){
										logMessage( "Created derivatives for " + fileUrl + " (" + damsClient.getRequestURL() + ").");
										
										ingestLog.append("\n    Derivative creation - - successful - " + damsDateFormat.format(new Date()));
										if(StringUtils.isNotBlank(use) && use.equalsIgnoreCase("video-source")){
											// create the .jpg thumbnail for videos from the mp4 derivative
											String[] sizes = {"4", "3"};
											derCreated = damsClient.updateDerivatives(oid, cid, "2.mp4", sizes);
											if(derCreated){
												logMessage( "Created thumbnails for video " + fileUrl + " (" + damsClient.getRequestURL() + ").");
											} else {
												derivFailedCount++;
												derivativesFailed.append(damsClient.getRequestURL() + ", \n"); 
												log.error("Failed to created thumbnails for video " + damsClient.getRequestURL() + " (" + tmpFile + ", " + (l+1) + " of " + iLen + "). ");
												
												ingested = false;
												successful = false;
												ingestLog.append("\n    Thumbnail creation - failed - " + damsDateFormat.format(new Date()));
											}
										}

										// embedded metadata for audio mp3 and video mp4 derivatives
										if ((isAudio(fid, use) || isVideo(fid, use)) && StringUtils.isNotBlank(use) && use.endsWith("-source")) {
											// extract embedded metadata
											EmbeddedMetadata embeddedMetadata = null;
											String derName = null;
											String fileUse = null;
											String commandParams = null;
											if (isAudio(fid, use)) {
												embeddedMetadata = new AudioMetadata(damsClient);
												derName = "2.mp3";
												fileUse = "audio-service";
												commandParams = Constants.FFMPEG_EMBED_PARAMS.get("mp3");
											} else {
												embeddedMetadata = new VideoMetadata(damsClient);
												derName = "2.mp4";
												fileUse = "video-service";
												commandParams = Constants.FFMPEG_EMBED_PARAMS.get("mp4");
											}

											Map<String, String> metadata = embeddedMetadata.getMetadata(oid, fileUrl);
											if(damsClient.ffmpegEmbedMetadata(oid, cid, derName, fileUse, commandParams, metadata)) {
												logMessage( "Embedded metadata for " + fileUrl + " (" + damsClient.getRequestURL() + ").");
											} else {
												derivFailedCount++;
												derivativesFailed.append(damsClient.getRequestURL() + ", \n"); 
												log.error("Failed to embedd metadata for " + damsClient.getRequestURL() + " (" + tmpFile + ", " + (l+1) + " of " + iLen + "). ");
												ingested = false;
												successful = false;
												ingestLog.append("\n    Derivative creation (embed metadata) for " + damsClient.getRequestURL() + " - failed - " + damsDateFormat.format(new Date()));
											}
										} else if (watermarking) {
											// Create watermarked derivatives fpr documents and images
											try {
												if (isImage(fid, use) && use.toLowerCase().contains("source")) {
													for (String key : Constants.WATERMARKED_DERIVATIVES.keySet()) {
														//Watermarking large image derivative types
														Watermarking watermarking = new ImageWatermarking(Constants.IMAGEMAGICK_COMMAND);
														File dstFile = watermarking.createWatermarkedDerivative(oid, cid, key, key);

														Map<String, String > fParams = toIngestParams(oid, cid, key, Constants.WATERMARKED_DERIVATIVES.get(key), dstFile.getAbsolutePath());
														damsClient.uploadFile(fParams, true);

														logMessage( "Watermarked image for " + fileUrl + " (" + damsClient.getRequestURL() + ").");
													}
												} else if (isDocument(fid, use) && use.toLowerCase().contains("source") && !fid.startsWith("1.")) {
													//Watermarked PDF from the source PDF (2.pdf) that were uploaded.
													Watermarking watermarking = new Watermarking(Constants.WATERMARK_COMMAND);

													String dfid = "1" + fid.substring(fid.lastIndexOf("."));
													File dstFile = watermarking.createWatermarkedDerivative(oid, cid, fid, dfid);

													Map<String, String > fParams = toIngestParams(oid, cid, dfid, "document-service", dstFile.getAbsolutePath());
													damsClient.uploadFile(fParams, true);

													logMessage( "Watermarked PDF for " + fileUrl + " (" + damsClient.getRequestURL() + ").");
												}
											} catch (Exception e) {
												e.printStackTrace();
												ingestFailedCount++;
												ingestFailed.append(srcFileName + " (" + fileUrl + ")\t\n");
												log.error("Failed to create watermarked derivative for " + fileUrl + " (" + srcFileName + ", " + (l+1) + " of " + iLen + ") in " + srcName + ": " + e.getMessage());
												ingested = false;
												successful = false;
												ingestLog.append("\n    Watermark derivative - failed - " + e.getMessage());
											}
										}
									} else {
										derivFailedCount++;
										derivativesFailed.append(damsClient.getRequestURL() + ", \n"); 
										log.error("Failed to created derivatives " + damsClient.getRequestURL() + " (" + tmpFile + ", " + (l+1) + " of " + iLen + "). ");
										
										ingested = false;
										successful = false;
										ingestLog.append("\n    Derivative creation - failed - " + damsDateFormat.format(new Date()));
									}
								} catch(Exception e) {
									e.printStackTrace();
									ingestFailedCount++;
									ingestFailed.append(srcFileName + " (" + fileUrl + ")\t\n");
									log.error("Failed to ingest file " + fileUrl + " (" + srcFileName + ", " + (l+1) + " of " + iLen + ") in " + srcName + ": " + e.getMessage());
									
									ingested = false;
									successful = false;
									ingestLog.append("\n    Derivative creation - failed - " + e.getMessage());
								}
							}

							// create zoomify tiles for master image files
							if ( isImage(fid, use) && fid.startsWith("1.") ) {
								try {
									boolean zoomifyTilesCreated = false;
									if (watermarking) {
										// Create zoomify tiles with the image-huge 1600x1600 watermarked derivative
										zoomifyTilesCreated = createZoomifyTiles( oid, cid, "7.jpg" );
									} else {
										zoomifyTilesCreated = createZoomifyTiles( oid, cid, fid );
									}

									if( zoomifyTilesCreated ){
										logMessage( "Created zoomify tiles " + fileUrl + " (" + damsClient.getRequestURL() + ").");
									} else {
										log.error("Failed to create zoomify tiles for " + damsClient.getRequestURL() + " (" + tmpFile + ", " + (l+1) + " of " + iLen + "). ");
										
										ingested = false;
										successful = false;
										ingestLog.append("\n    Zoomify tiles - failed - " + damsDateFormat.format(new Date()));
									}
								}catch (Exception e) {
									e.printStackTrace();
									log.error("Failed to create Zoomify tiles for " + fileUrl + " (" + srcFileName + ", " + (l+1) + " of " + iLen + ") in " + srcName + ": " + e.getMessage());

									ingested = false;
									successful = false;
									ingestLog.append("\n    Zoomify tiles creation - failed - " + e.getMessage());
								}
							}
						}
					}
				} catch(Exception e) {
					e.printStackTrace();
					
					ingestFailedCount++;
					ingestFailed.append(srcFileName + " (" + fileUrl + ")\t\n");
					log.error("Failed to ingest file " + fileUrl + " (" + srcFileName + ", " + (l+1) + " of " + iLen + ") in " + srcName + ": " + e.getMessage());
					
					ingested = false;
					successful = false;
					String error = e.getMessage();
					if (error.indexOf("Error Jhove extraction") >= 0) {
						ingestLog.append("\n    Ingest - successful - " + damsDateFormat.format(new Date()));
						ingestLog.append("\n    JHOVE extraction - failed - " + damsDateFormat.format(new Date()) + " - " + error);
					} else 
						ingestLog.append("\n    Ingest - failed - " + damsDateFormat.format(new Date()) + " - " + error);
					
				}
			}else{
				ingestFailedCount++;
				ingestFailed.append( fid + "\t\n");
				log.error("Missing sourceFileName property for file " + fileUrl + " (" + (l+1) + " of " + iLen + ") in " + srcName + ".\n");
				
				ingested = false;
				successful = false;
				ingestLog.append("\n    Ingest - failed - " + damsDateFormat.format(new Date()) + ": " + "missing sourceFileName property for file " + fileUrl + ".");
			}	
			
			try{
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
				interrupted = true;
				message = "Import interrupted in " + (fileUrl!=null?fileUrl + " (" + srcName + ")":srcName) + ". \n Error: " + e.getMessage() + "\n";
				setStatus("Canceled");
				clearSession();
				log.error(message);
				
				ingested = false;
				successful = false;
				ingestLog.append("\n    Ingest - failed - " + damsDateFormat.format(new Date()) + ": canceled by user.");
				
			}
			
			status[processIndex] = successful;
			messages[processIndex].append("\n**  " + srcFileName + " (" + fileUrl + ") - " + (ingested ? "successful" : "failed") + " - " + damsDateFormat.format(new Date()));
			if (!ingested)
				messages[processIndex].append(ingestLog.toString());

			// determine whether the current sourceFileName should be used for ark report
			String nextOcUri = "";
			if (l + 1 < iLen) {
				String nextFileUrl = oStmt.get(l + 1).getObject().asResource().getURI();
				nextOcUri = nextFileUrl.substring(0, (nextFileUrl.lastIndexOf("/") > 0 ? nextFileUrl.lastIndexOf("/") : nextFileUrl.length()));
			}

			String ocUri = fileUrl.substring(0, (fileUrl.lastIndexOf("/") > 0 ? fileUrl.lastIndexOf("/") : fileUrl.length()));
			String ocid = ocUri.replace(Constants.DAMS_ARK_URL_BASE + "/" + Constants.ARK_ORG + "/", "");
			if ( StringUtils.isNotBlank(use) && use.contains("source")
					|| oStmt.size() == 1 
					|| !ocArkReported.contains(ocUri) && !ocUri.equals(nextOcUri)) {
				// CSV format ARK Report for source files
				ocArkReported.add(ocUri);
				String title = getTitle(rdf, ocUri);
				toArkReport (
						objectArkReport, 
						ocid, 
						(StringUtils.isNotBlank(title) ? title : "[Unknown Title]"), 
						(StringUtils.isNotBlank(srcFileName) ? srcFileName : "No associated file"), 
						successful ? "successful" : "failed", 
						damsDateFormat.format(new Date())
						);
			}
		}

		messages[processIndex].insert(0, damsDateFormat.format(new Date()));

		return iLen;
	}

	public static boolean createZoomifyTiles( final String oid, final String cid, final String fid ) throws Exception {
		ZoomifyTilesConverter zoomifyConverter = new ZoomifyTilesConverter( Constants.ZOOMIFY_COMMAND );
		zoomifyConverter.setFileStoreDir(Constants.FILESTORE_DIR);
		return zoomifyConverter.createZoomifyTiles(oid, cid, fid);
	}

	/**
	 * Update the document for DAMS ARK ID
	 * @param doc
	 * @param record
	 * @param title
	 * @throws Exception
	 */
	private void updateDocument(Document doc, Node record, String field, String title, Map<String, String> props) throws Exception{
		// Skip if the record detached
		if(record.getDocument() == null && !doc.equals(record))
			return;
		
		// Subject, Authority records use mads:authoritativeLabel
		Node aboutAttr = record.selectSingleNode("@rdf:about");
		String srcUri = aboutAttr.getStringValue();
		//String nName = record.getName();
		String xPath = record.getPath();
		String elemName = xPath.substring(xPath.lastIndexOf("/")+1);
		
		String modelName = elemName;
		
		List<Map<String,String>> oids = null;
		String oid = null;
		// First perform ID lookup with mads:hasExactExternalAuthority
		String fastIdKey = "mads:hasExactExternalAuthority";
		String fastId = props.remove(fastIdKey);
		if(props != null && props.containsKey(fastIdKey)) {
			oid = idsMap.get(fastId);
			if(StringUtils.isBlank(oid)) {
				oids = lookupRecordsFromTs(fastIdKey, fastId, null, null);
				if (oids != null && oids.size() > 0) {
					oid = oids.get(0).values().iterator().next();
					idsMap.put(fastId, oid);
				}
			}
		}

		if(oid == null){
			String nKey = INFO_MODEL_PREFIX + modelName + "::" + title;
			if(props != null){
				for(Iterator<String> it=props.keySet().iterator(); it.hasNext();){
					String iKey = it.next();
					nKey += "_" + iKey + "::" + props.get(iKey);
				}
			}
			oid = idsMap.get(nKey);

			if (DAMSRepository.isAuthorityRecord(record) && record.selectSingleNode(DAMSRepository.MADS_AUTHORITATIVELABEL) != null) {
				// Matching authority records with mads:authoritativeLabel: ignoring punctuation and spaces 
				List<String> results = damsRepository.findAuthority(modelName, StringEscapeUtils.unescapeJava(title));
				if(results != null && results.size() > 0){
					oid = results.get(0);
					if (results.size() > 1){
						String duids = "";
						for(Iterator<String> it=results.iterator(); it.hasNext();)
							duids += (duids.length()>0?", ":"") + it.next();
						log.warn("Duplicate records found for " + title + " (" + field + "): " + duids + ".");
					}	

					authorityReport (record, oid, title, "match");
				}
			} else {

			//Lookup records from the triplestore, matching the required properties that are null or empty.
			oids = lookupRecordsFromTs(field, title, "\""+ modelName + "\"", props);

			if(oids != null && oids.size() > 0){
				
				String propName = null;
				String propValue = null;
				Document recDoc = null;
				Node cNode = null;
				if(props != null && props.size() > 0){
					List<Map<String,String>> oidsCopy = new ArrayList<Map<String,String>>();
					oidsCopy.addAll(oids);
					for(int i=0; i< oidsCopy.size(); i++){
						
						Collection<String> propValues = props.values();
						Map<String,String> resolution = oidsCopy.get(i);
						String rid = resolution.values().iterator().next();
						if(rid.startsWith("http")){
							if(propValues.contains(null)){
								
								recDoc = damsClient.getRecord(rid);
								for(Iterator<String> it=props.keySet().iterator(); it.hasNext();){
									propName = it.next();
									propValue = props.get(propName);
									// Test for the nodes for null properties and remove it from the result
									if(propValue == null){
										int idx = propName.indexOf("/", 1);
										if(idx > 0)
											cNode = recDoc.selectSingleNode("//" + modelName + "/" + propName);
										else
											cNode = recDoc.selectSingleNode("//" + modelName + "/" + propName);
										
										if(cNode != null){
											oids.remove(resolution);
											break;
										}
									}
								}
							}
						}else // removed internal BlankNodes from the results
							oids.remove(resolution);
					}
				}
				
				if(oids.size() > 0){
					oid = oids.get(0).values().iterator().next();
					if(oids.size() > 1){
						String duids = "";
						for(Iterator<Map<String, String>> it=oids.iterator(); it.hasNext();)
							duids += (duids.length()>0?", ":"") + it.next().values().iterator().next();
						
						log.warn("Duplicated records found for " + title + " (" + field + "): " + duids + ".");
					}	

					authorityReport (record, oid, title, "match");
				}
					
			}
			}
			
			if(oid == null){
				// Create the record
				oid = getNewId();
				aboutAttr.setText(oid);

				authorityReport (record, oid, title, "record created");
			}else{
				// Record found. Add to the map, link and remove it.
				toResourceLinking(oid, record);
			}
	
			idsMap.put(nKey, oid);
			if (StringUtils.isNotBlank(fastId))
				idsMap.put(fastId, oid);
		}else{	
			// Record added. Add linking, remove it.
			toResourceLinking(oid, record);
		}
		updateReference(doc, srcUri, oid);
	}
	
	private Map<String, String> copyrightProperties(Node record){
		return getProperties(record, getCopyrightPropNames());
	}
	
	private Map<String, String> licenseProperties(Node record){
		return getProperties(record, getLicensePropNames());
	}
	
	private Map<String, String> otherRightsProperties(Node record){
		List<String> propNames = getOtherRightsPropNames();
		String propName = "dams:relationship//dams:name";
		if(record.selectSingleNode(propName + "//mads:authoritativeLabel") != null)
			propNames.add(propName + "//mads:authoritativeLabel");
		else
			propNames.add(propName);		
		return getProperties(record, propNames);
	}
	
	private Map<String, String> relatedResourceProperties(Node record){
		return getProperties(record, getRelatedResourcePropNames());
	}
	
	private Map<String, String> sourceCaptureProperties(Node record){
		return getProperties(record, getSourceCapturePropNames());
	}
	
	private Map<String, String> noteProperties(Node record){
		return getProperties(record, getNotePropNames());
	}
	
	private Map<String, String> dateProperties(Node record){
		return getProperties(record, getDatePropNames());
	}
	
	private Map<String, String> getProperties(Node record, List<String> propNames){
		Map<String, String> props = new TreeMap<String, String>();
		String propName = null;
		String propValue = null;
		for(Iterator<String> it=propNames.iterator(); it.hasNext();){
			propValue = null;
			propName = it.next();
			Node tNode = record.selectSingleNode(propName);
			if(tNode != null){
				Node resNode = tNode.selectSingleNode("@rdf:resource");
				if(resNode != null)
					propValue = encodeIdentifier(resNode.getStringValue());
				else
					propValue = encodeLiteralValue(tNode);
			}
			
			props.put(propName, propValue);
		}
		return props;
	}
	
	private List<String> getCopyrightPropNames(){
		List<String> propNames = new ArrayList<String>();
		propNames.add("dams:copyrightStatus");
		propNames.add("dams:copyrightJurisdiction");
		propNames.add("dams:copyrightPurposeNote");
		propNames.add("dams:copyrightNote");
		propNames.add("dams:beginDate");
		propNames.add("dams:endDate");
		return propNames;
	}
	
	private List<String> getLicensePropNames(){
		List<String> propNames = new ArrayList<String>();
		propNames.add("dams:permission//dams:type");
		propNames.add("dams:permission//dams:beginDate");
		propNames.add("dams:permission//dams:endDate");
		propNames.add("dams:restriction//dams:type");
		propNames.add("dams:restriction//dams:beginDate");
		propNames.add("dams:restriction//dams:endDate");
		return propNames;
	}
	
	private List<String> getOtherRightsPropNames(){
		List<String> propNames = getLicensePropNames();
		propNames.add("dams:otherRightsNote");
		return propNames;
	}
	
	private List<String> getRelatedResourcePropNames(){
		List<String> propNames = new ArrayList<String>();
		propNames.add("dams:type");
		//propNames.add("dams:uri");
		return propNames;
	}
	
	private List<String> getNotePropNames(){
		List<String> propNames = new ArrayList<String>();
		propNames.add("dams:type");
		propNames.add("dams:displayLabel");
		return propNames;
	}
	
	private List<String> getDatePropNames(){
		List<String> propNames = new ArrayList<String>();
		propNames.add("dams:beginDate");
		propNames.add("dams:endDate");
		return propNames;
	}
	
	private List<String> getSourceCapturePropNames(){
		List<String> propNames = new ArrayList<String>();
		propNames.add("dams:imageProducer");
		propNames.add("dams:captureSource");
		propNames.add("dams:scannerManufacturer");
		propNames.add("dams:scannerModelName");
		propNames.add("dams:scanningSoftware");
		propNames.add("dams:scanningSoftwareVersion");
		return propNames;
	}

	private String encodeIdentifier(String id){
		return "<" + id + ">";
	}
	
	private String encodeLiteralValue(Node node){
		String value = null;
		if(node != null){
			value = "\"" + StringEscapeUtils.escapeJava(Normalizer.normalize(node.getText(), Normalizer.Form.NFC)).replace("'", "\\'").replace("\"", "\\\"") + "\"";
			Node langAttr = node.selectSingleNode("@xml:lang");
			if(langAttr!=null)
				value += "@" + langAttr.getStringValue();
		}
		return value;
	}
	
	/**
	 * Mint a new ark id
	 * @return
	 * @throws Exception
	 */
	public String getNewId() throws Exception{
		return toDamsUrl(damsClient.mintArk(Constants.DEFAULT_ARK_NAME));
	}
	
	/**
	 * Update record for resource linking
	 * @param url
	 * @param node
	 */
	public void toResourceLinking(String url, Node record){
		Element pNode = record.getParent();
		if(pNode.getName().endsWith("List") && !record.getName().toLowerCase().endsWith(pNode.getName().toLowerCase())){
			//List elements
			record.setName("rdf:Description");
			((Element)record).clearContent();
			((Element)record).selectSingleNode("@rdf:about").setText(toDamsUrl(url));
		}else{
			pNode.addAttribute("rdf:resource", toDamsUrl(url));
			record.detach();
		}
	}

	private List<String> sortRecords(List<String> records) {
		if (records == null || records.size() <= 1)
			return records;

		// Sort object records to the last of the list
		int count = 0;
		Collections.sort(records);
		List<String> sortedRecords = new ArrayList<>();
		for (String rec : records) {
			if (objRecords.containsKey(rec) || collRecords.containsKey(rec))
				sortedRecords.add(rec);
		    else
				sortedRecords.add (count++, rec);
		}
		return sortedRecords;
	}

	private String getTitle(Model model, String oid){
		String title = "";
		String damsNsPrefixUri = model.getNsPrefixURI("dams");
		String madsNsPrefixUri = model.getNsPrefixURI("mads");
		ExtendedIterator<Triple> titleIt = model.getGraph().find(ResourceFactory.createResource(oid).asNode(), 
				ResourceFactory.createProperty(damsNsPrefixUri + "title").asNode(), com.hp.hpl.jena.graph.Node.ANY);
		
		if (titleIt.hasNext()) {
			ExtendedIterator<Triple> titleLabelIt = model.getGraph().find(titleIt.next().getObject(), 
					ResourceFactory.createProperty(madsNsPrefixUri + "authoritativeLabel").asNode(), com.hp.hpl.jena.graph.Node.ANY);
			if (titleLabelIt.hasNext()) {
				title = titleLabelIt.next().getObject().getLiteral().getLexicalForm();
			}
		}
		return title;
	}

	/*
	 * Create the parameters map for file upload
	 * @param oid
	 * @param cid
	 * @param fid
	 * @param use
	 * @param localFile
	 * @return
	 */
	private Map<String, String> toIngestParams(String oid, String cid, String fid, String use, String localFile) {
		Map<String, String > params = new HashMap<String, String>();
		params.put("oid", oid);
		params.put("cid", cid);
		params.put("fid", fid);
		params.put("use", use);
		params.put("local", localFile);

		return params;
	}

	private void toArkReport (StringBuilder reportBuilder, String ark, String title, String fileName, String outcome, String eventDate) {
		// ARK column
		reportBuilder.append(ark);
		// title column
		reportBuilder.append("," + escapeCsvValue(title));
		// source file name column 
		reportBuilder.append("," + escapeCsvValue(fileName));
		// outcome column
		reportBuilder.append("," + outcome);
		// event date column
		reportBuilder.append("," + eventDate);
		reportBuilder.append("\n");
	}

	private String escapeCsvValue (String value) {
		String csvValue = value;
		if (StringUtils.isNotBlank(csvValue)) {
			if (csvValue.contains("\n") || csvValue.contains(",") || csvValue.contains("\"")) {
				csvValue = value.replace("\"", "\"\"");
				csvValue = "\"" + csvValue + "\"";
			}
		}
		return StringUtils.isBlank(csvValue) ? "" : csvValue;
	}

	/**
	 * Construct the URL with an id.
	 * @param arkUrl
	 * @return
	 */
	public static String toDamsUrl(String arkUrl){
		if(!arkUrl.startsWith("http")){
			String arkUrlBase = Constants.DAMS_ARK_URL_BASE;
			String arkOrg = Constants.ARK_ORG;
			arkUrl = arkUrlBase + (arkUrlBase.endsWith("/")?"":"/") + (arkUrl.indexOf('/')>0?arkUrl:arkOrg+"/"+arkUrl);
		}
		return arkUrl;
	}
	
	public void updateReference(Document doc, String srcId, String oid){
		List<Node> resNodes = doc.selectNodes("//@rdf:resource");
		for(int k=0; k<resNodes.size(); k++){
			Node nRes = resNodes.get(k);
			if(nRes.getStringValue().equals(srcId)){
				nRes.setText(oid);
			}
		}
	}
	
	public void logData(String fileName, String content) throws UnsupportedEncodingException, IOException{
		File file = new File(Constants.TMP_FILE_DIR + "/damsmanager");
		if(!file.exists())
			file.mkdir();
		FileOutputStream out = null;
		try{
			out = new FileOutputStream(file.getAbsoluteFile() + "/" + fileName);
			out.write(content.getBytes("UTF-8"));
		}finally{
			close(out);
		}
	}
	
	private void authorityReport (Node record, String oid, String title, String action) {
		String modelLable = getModelLabel(record);
		List<String> subjectsTypes = ExcelSource.getControlValues().get(SubjectTabularRecord.SUBJECT_TYPE);
		if (subjectsTypes != null && subjectsTypes.contains("Subject:" + modelLable))
			modelLable = "Subject:" + modelLable;
		title = title.startsWith("\"") ? title.substring(1, title.length()-1) : title; // remove quotes in the title that was added for sparql lookup
		authorityReport.append( modelLable + "," + escapeCsvValue(title) + "," + action + "," + escapeCsvValue(oid) + "\n");
	}

	public static String getModelLabel(Node record) {
		String model = record.getName();
		//anatomy, common name, conference name, corporate name, cruise, culturalContext, family name, genre, geographic, lithology, occupation, personal name, scientific name, series, temporal, topic
		switch (model) {
			case "CulturalContext":
				model = "culturalContext";
				break;
			case "GenreForm":
				model = "genre";
				break;
			default:
				String[] tokens = model.split("(?<=.)(?=(\\p{Upper}))");
				model = "";
				for (String token : tokens)
					model += token + " ";
				model = model.trim().toLowerCase();
				break;
			}
		return model;
	}

	/**
	 * Execution result message
	 */
	@Override
	public String getExeInfo() {
		int objectsCount = objRecords.size();
		
		if(exeResult)
			exeReport.append("Import summary: successful - Imported " + objectsCount + " objects in " + rdfFiles.length + " metadata file" + (rdfFiles.length>1?"s":"")
					+ " \n- Total " + recordsCount + " records ingested. \n- " + (filesCount==0?"No":"Total " + filesCount) + " files ingested.\n");
		else {
			exeReport.append("Import summary: failed - Found " +  objectsCount + " objet" + (objectsCount>1?"s":"") 
					+ (failedCount>0?" \n- " + failedCount + " of " + rdfFiles.length + " failed":"") + (derivFailedCount>0?" \n- Derivatives creation failed for " + derivFailedCount + " files.":"") 
					+ (solrFailedCount>0?" \n- SOLR update failed for " + solrFailedCount + " records.":"") +" \n");
			if(metadataFailed.length() > 0)
				exeReport.append("* Failed to import the following metadeta records: \n" + metadataFailed.toString());
			if(ingestFailedCount > 0)
				exeReport.append("* Failed to ingest " + ingestFailedCount + " of total " + filesCount + " file" + (filesCount>1?"s":"") + ": \n" + ingestFailed.toString() + " \n");
			if(derivativesFailed.length() > 0)
				exeReport.append("* Failed to create the following derivatives: \n" + derivativesFailed.toString());
			getSOLRReport();
		}
		
		if(objectsCount > 0){
			String key = null;
			exeReport.append("\nThe following " + objectsCount + " object" + (objectsCount>1?"s are ":" is ") + "found in the source metadata: \n");
			for(Iterator<String> it=objRecords.keySet().iterator(); it.hasNext();){
				key = it.next();
				exeReport.append("http://" + Constants.CLUSTER_HOST_NAME + ".ucsd.edu/dc/object/" + key.substring(key.lastIndexOf("/") + 1) + " \t" + objRecords.get(key) + "\n");
			}
		}
		
		int recordSize = recordsIngested.size();
		if(recordSize > 0){
			exeReport.append("\nFound " + (idsMap.size() + objectsCount) + " record" + (idsMap.size() > 1 ? "s" : "") + ". The following " + recordSize + " record" + (recordSize>1?"s are":" is") + " imported: \n");
			for(Iterator<String> it=recordsIngested.iterator(); it.hasNext();){
				exeReport.append(it.next() + "\n");
			}
		}else
			exeReport.append("\nFound " + (idsMap.size() + objectsCount) + " record" + (idsMap.size() > 1 ? "s" : "") + ". No records were imported.\n");

		if(filesIngested.length() > 0){
			log.info("\nThe following files are ingested successfully: \n" + filesIngested.toString());
		}

		if(warnings.length() > 0) {
		    exeReport.append("\nWarning(s):\n");
		    exeReport.append(warnings.toString());
		}

		log("log", "\n______________________________________________________________________________________________");
		String exeInfo = exeReport.toString();
		logMessage(exeInfo);

		return exeInfo;
	}

	/*
	 * Extract source filename of an object
	 * @param doc
	 * @param subjectUrl
	 * @return
	 */
	private String getSourceFiles(Document doc, String subjectUrl) {
		String fileName = "";
		String fileXPath = "//dams:Object[@rdf:about='" + subjectUrl
			+ "']//dams:File[contains(@rdf:about, '/1.') OR contains(dams:use, 'alternate')]";

		List<Node> files = doc.selectNodes(fileXPath);
		for (Node file : files) {
			fileName += (fileName.length() > 0 ? " | " : "") + file.valueOf("dams:sourceFileName");
		}
		return fileName;
	}
}
