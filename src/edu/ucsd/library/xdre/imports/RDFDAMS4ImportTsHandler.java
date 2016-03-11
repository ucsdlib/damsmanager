package edu.ucsd.library.xdre.imports;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DFile;
import edu.ucsd.library.xdre.utils.DamsURI;
import edu.ucsd.library.xdre.utils.RDFStore;

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
	private StringBuilder objectArkReport = null;

	private boolean replace = false;

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
		for(int i=0; i<fLen&&!interrupted; i++){
			if (i == 0) {
				logMessage ("Object Import status:\n[Object title]   -   [URI]   -   [Status]   -   [Timestamp]");
				toArkReport (arkReport, "ARK", "Title", "File name", "Outcome", "Event date");
			}

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
					if (iUri.endsWith("/COL") || !(iUri.startsWith("http") && iUri.indexOf("/ark:/") > 0)){
						// Assign ARK
						if(nName.endsWith("Object") || nName.endsWith("Component") || nName.endsWith("File") || (((Element)parentNode).isRootElement() || (parentNode.getParent().isRootElement() && parentNode.getParent().getName().equals("RDF")))){
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
								} else if(nName.endsWith(LANGUAGE)){
									field = "mads:code";
									xPath = "mads:code";
									tNode = parentNode.selectSingleNode(xPath);
									if(tNode == null){
										field = "mads:authoritativeLabel";
										xPath = "mads:authoritativeLabel";
										tNode = parentNode.selectSingleNode(xPath);
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
									// Mapping for mads:isMemberOfMADSScheme
									String madsScheme = null;
									Node madsSchemeNode = parentNode.selectSingleNode("mads:isMemberOfMADSScheme");
									if(madsSchemeNode != null){
										Node msValueNode = madsSchemeNode.selectSingleNode("@rdf:resource");
										if (msValueNode != null){
											madsScheme = madsSchemeNode.getStringValue();
											props.put("mads:isMemberOfMADSScheme", "<" + madsScheme + ">");
										}else if ((madsSchemeNode = madsSchemeNode.selectSingleNode("mads:MADSScheme")) != null && madsSchemeNode.hasContent()){
											if ((msValueNode=madsSchemeNode.selectSingleNode("mads:code")) != null){
												madsScheme = encodeLiteralValue(msValueNode);
												props.put("mads:isMemberOfMADSScheme/mads:code", madsScheme);
											}else if((msValueNode=madsSchemeNode.selectSingleNode("rdfs:label")) != null){
												madsScheme = encodeLiteralValue( msValueNode);
												props.put("mads:isMemberOfMADSScheme/rdfs:label", madsScheme);
											}
										}else{
											props.put("mads:isMemberOfMADSScheme/rdfs:label", "\"\"");
										}
									}else{
										props.put("mads:isMemberOfMADSScheme/rdfs:label", null);
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
						
						// Update object
						//log.info(j + " ingesting record " + subjectId + ":\n" + graph.export(RDFStore.RDFXML_ABBREV_FORMAT) + "\n\n");
						String importMode = Constants.IMPORT_MODE_ADD;
						if (replace && recordsToReplace.indexOf(subjectId) >= 0)
							importMode = Constants.IMPORT_MODE_ALL;
						succeeded = damsClient.updateObject(subjectId, graph.export(RDFStore.RDFXML_ABBREV_FORMAT), importMode);

						
						// Logging for Object RDF/XML validation
						status[processIndex] = succeeded;
						messages[processIndex].append(damsDateFormat.format(new Date()));

						eventDate =  damsDateFormat.format(new Date());
						if(!succeeded){
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
						
						if(metadataFailed.indexOf(currFile) < 0)
							failedCount++;
						metadataFailed.append(subjectId + " (" + currFile + "), \n");
						message = "Metadata import failed: " + e.getMessage();
						setStatus( message  + " (" +(j+1)+ " of " + jLen + ") in file " + currFile + "."); 
						log.error(message);
						
						String error = e.getMessage();
						if (error.indexOf("Invalid RDF input") >= 0) {
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
							if (!filesIt.hasNext()) {
								toArkReport (
										arkReport, 
										subjectId.substring(subjectId.lastIndexOf("/") + 1), 
										(StringUtils.isNotBlank(title) ? title : "[Unknown Title]"), 
										"No associated file", 
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

		// write the ark report to file
		if (arkReport.length() > 0) {
			FileOutputStream out = null;
			File arkReportFile = getArkReportFile();
			try {
				out = new FileOutputStream(arkReportFile);
				out.write(arkReport.toString().getBytes());
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
							if((isImage(fid, use) || isDocument(fid, use) || isVideo(fid, use) || isAudio(fid, use))
									&& (use == null || use.endsWith("source") || use.endsWith("service") || use.endsWith("alternate"))) {
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

	/**
	 * Update the document for DAMS ARK ID
	 * @param doc
	 * @param record
	 * @param title
	 * @throws Exception
	 */
	private void updateDocument(Document doc, Node record, String field, String title, Map<String, String> props) throws Exception{
		// Skip if the record detached
		if(record.getDocument() == null)
			return;
		
		// Subject, Authority records use mads:authoritativeLabel
		Node aboutAttr = record.selectSingleNode("@rdf:about");
		String srcUri = aboutAttr.getStringValue();
		//String nName = record.getName();
		String xPath = record.getPath();
		String elemName = xPath.substring(xPath.lastIndexOf("/")+1);
		
		String modelName = elemName;
		String nKey = INFO_MODEL_PREFIX + modelName + "::" + title;
		if(props != null){
			for(Iterator<String> it=props.keySet().iterator(); it.hasNext();){
				String iKey = it.next();
				nKey += "_" + iKey + "::" + props.get(iKey);
			}
		}
		String oid = idsMap.get(nKey);

		if(oid == null){
			//Lookup records from the triplestore, matching the required properties that are null or empty.
			List<Map<String,String>> oids = lookupRecordsFromTs(field, title, "\""+ modelName + "\"", props);
			if(oids != null && oids.size() > 0){
				
				String propName = null;
				String propValue = null;
				Document recDoc = null;
				Node cNode = null;
				if(props != null){
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
				}
					
			}
			
			if(oid == null){
				// Create the record
				oid = getNewId();
				aboutAttr.setText(oid);
			}else{
				// Record found. Add to the map, link and remove it.
				toResourceLinking(oid, record);
			}
			idsMap.put(nKey, oid);
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
			exeReport.append("\nThe following " + recordSize + " record" + (recordSize>1?"s are":" is") + " imported: \n");
			for(Iterator<String> it=recordsIngested.iterator(); it.hasNext();){
				exeReport.append(it.next() + "\n");
			}
		}else
			exeReport.append("\nNo records were imported.\n");

		if(filesIngested.length() > 0){
			log.info("\nThe following files are ingested successfully: \n" + filesIngested.toString());
		}
		
		log("log", "\n______________________________________________________________________________________________");
		String exeInfo = exeReport.toString();
		logMessage(exeInfo);

		return exeInfo;
	}
}
