package edu.ucsd.library.xdre.imports;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;

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
	private List<String> recordsIngested = new ArrayList<String>();
	private List<String> objWithFiles = new ArrayList<String>();
	private StringBuilder ingestFailed = new StringBuilder();
	private StringBuilder metadataFailed = new StringBuilder();
	private StringBuilder derivativesFailed = new StringBuilder();
	private StringBuilder filesIngested = new StringBuilder();
	
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
	
	/**
	 * Procedure to populate the RDF metadata and ingest the files
	 */
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
		
		String oid = null;
		int fLen = rdfFiles.length;
		String currFile = null;
		SAXReader saxReader = new SAXReader();
		for(int i=0; i<fLen&&!interrupted; i++){
			currFile = rdfFiles[i].getName();
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
							if(tNode == null && !field.equals("dams:licenseNote")){
								throw new Exception("Element " + xPath + " is missing from the " + nName + " record " + iUri + " in file " + currFile + ".");
							}
							
							updateDocument(doc, parentNode, field, encodeLiteralValue(tNode), props);
						}
					}else if(nName.endsWith("Object")){
						objRecords.put(iUri, currFile);
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
				Model rdf = rdfStore.loadRDFXML(dams4Rdf);
				initHandler();
				
				Model iRdf = null;
				int jLen = items.size();
				//System.out.println(currFile + " records found: " + jLen);
				for (int j=0; j<jLen&&!interrupted; j++){
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
							graph.merge(iRdf);
						}
						
						// Update object
						//log.info(j + " ingesting record " + subjectId + ":\n" + graph.export(RDFStore.RDFXML_ABBREV_FORMAT) + "\n\n");
						
						succeeded = damsClient.updateObject(subjectId, graph.export(RDFStore.RDFXML_ABBREV_FORMAT), Constants.IMPORT_MODE_ADD);
							
						if(!succeeded){
							if(metadataFailed.indexOf(currFile) < 0)
								failedCount++;
							metadataFailed.append(subjectId + " (" + currFile + "), \n");
							message = "Metadata import for record " + subjectId  + " failed (" + (j+1) + " of " + jLen + ") in file " + currFile + ".";
							setStatus( message ); 
							logError(message + "\n Error RDF: \n" + graph.export(RDFStore.RDFXML_ABBREV_FORMAT));
						}else{
							recordsIngested.add(subjectId);
							message = "Metadata import for record " + subjectId  + " succeeded (" + (j+1) + " of " + jLen + ") in file " + currFile + ". ";
							setStatus(message); 
							logMessage(message);
							log.info(message);
							
							// Update SOLR fre records ingested.
							updateSOLR(subjectId);
						}
					
					} catch (Exception e) {
						e.printStackTrace();
						if(metadataFailed.indexOf(currFile) < 0)
							failedCount++;
						metadataFailed.append(subjectId + " (" + currFile + "), \n");
						message = "Metadata import failed: " + e.getMessage();
						setStatus( message  + " (" +(j+1)+ " of " + jLen + ") in file " + currFile + "."); 
						logError(message);
					}
					
					try{
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
						interrupted = true;
						failedCount++;
						metadataFailed.append(subjectId + " (" + currFile + ") \n");
						message = "Metadata import interrupted for subject " + subjectId  + ". \n Error: " + e.getMessage() + "\n";
						setStatus("Canceled");
						clearSession();
						logError(message);
					}
				}
				
				// Ingest the source file
				if(importOption.equalsIgnoreCase("metadataAndFiles")){
					uploadFiles(rdf, currFile);
				}

			}catch(Exception e){
				e.printStackTrace();
				failedCount++;
				message = "Import failed for " + currFile + ": " + e.getMessage();
				setStatus( message  + " (" +(i+1)+ " of " + fLen + ").");
				logError(message);
			}finally{
				// Update SOLR for files uploaded
				int iLen = objWithFiles.size();
				for (int j=0; j<iLen&&!interrupted; j++){
					updateSOLR(objWithFiles.get(j));
				}
			}
			
			setProgressPercentage( ((i + 1) * 100) / fLen);
			
			try{
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
				interrupted = true;
				failedCount++;
				message = "Import interrupted for oject in " + currFile + ". \n Error: " + e.getMessage() + "\n";
				setStatus("Canceled");
				clearSession();
				logError(message);
			}
		}
		return exeResult;
	}
	
	public void uploadFiles(Model rdf, String srcName){
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
		int iLen = oStmt.size();
		// Retrieve the statements for files
		for(int l=0; l<iLen&&!interrupted;l++){
			stmt = oStmt.remove(0);
			prop = stmt.getPredicate();
			if(prop.getLocalName().equals(HAS_FILE)){
				oStmt.add(stmt);
			}
		}
		
		iLen = oStmt.size();
		for(int l=0; l<iLen&&!interrupted;l++){
			filesCount++;
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
					if(srcFile == null){
						exeResult = false;
						logError("Source file for " + srcFileName + " in " + srcName + " doesn't exist. Please choose a correct file location from the dams staging area.");
					}else{
						// Ingest the file
						DamsURI dURI = null;
						boolean ingested = false;
						String tmpFile = srcFile.getAbsolutePath();
						try{
							dURI = DamsURI.toParts(fileUrl, null);
							oid = dURI.getObject();
							cid = dURI.getComponent();
							fid = dURI.getFileName();
							
							params = new HashMap<String, String>();
							params.put("oid", oid);
							params.put("cid", cid);
							params.put("fid", fid);
							params.put("use", use);
							params.put("local", tmpFile);
							params.put("sourceFileName", srcFileName);
							ingested = damsClient.createFile(params);
							if(!ingested){
								ingestFailedCount++;
								ingestFailed.append(fileUrl + " (" + tmpFile + "), \n");
								logError("Error ingesting file " + fileUrl  + " (" + tmpFile + ", " + (l+1) + " of " + iLen + ") in " + srcName + ".");
							}else{
								message = "Ingested file " + fileUrl + " (" + tmpFile + ", " + (l+1) + " of " + iLen + ") in " + srcName + ". ";
								log.info(message);
								logMessage(message);
								filesIngested.append(fileUrl + "\t" + srcFileName + "\t" + damsClient.getRequestURL() + "\n");
								
								// Add for SOLR update
								if(objWithFiles.indexOf(oid) < 0)
									objWithFiles.add(oid);
								// Remove the hasFile property from the record which was ingested during file ingestion.
								//rdf.remove(stmt);
								//rdf.remove(stmts);
								
								//Create derivatives for images and documents PDFs
								if((isImage(fid, use) || isDocument(fid, use)) 
										&& (use == null || use.endsWith("source") || use.endsWith("service") || use.endsWith("alternate"))){
									
									boolean derCreated = damsClient.createDerivatives(oid, cid, fid, null);
									if(derCreated){
										logMessage( "Created derivatives for " + fileUrl + " (" + damsClient.getRequestURL() + ").");
									} else {
										derivFailedCount++;
										derivativesFailed.append(damsClient.getRequestURL() + ", \n"); 
										logError("Failed to created derivatives " + damsClient.getRequestURL() + " (" + tmpFile + ", " + (l+1) + " of " + iLen + "). ");
									}
								}
							}
						}catch(Exception e){
							e.printStackTrace();
							ingestFailedCount++;
							ingestFailed.append(fileUrl + " (" + tmpFile + "), \n");
							logError("Failed to ingest file " + fileUrl + " (" + tmpFile + ", " + (l+1) + " of " + iLen + ") in " + srcName + ": " + e.getMessage());
						}
					}
				}
			}else{
				ingestFailedCount++;
				ingestFailed.append( fid + ", \n");
				logError("Missing sourceFileName property for file " + fileUrl + " (" + (l+1) + " of " + iLen + ") in " + srcName + ".");
			}	
			
			try{
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
				interrupted = true;
				message = "Import interrupted in " + (fileUrl!=null?fileUrl + " (" + srcName + ")":srcName) + ". \n Error: " + e.getMessage() + "\n";
				setStatus("Canceled");
				clearSession();
				logError(message);
			}
		}
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
						
						System.out.println("Duplicated records found for " + title + " (" + field + "): " + duids + ".");
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
	
	/**
	 * Construct the URL with an id.
	 * @param arkUrl
	 * @return
	 */
	public String toDamsUrl(String arkUrl){
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
	public String getExeInfo() {
		int objectsCount = objRecords.size();
		if(exeResult)
			exeReport.append("Successfully imported " + objectsCount + " objets in " + rdfFiles.length + " metadata file" + (rdfFiles.length>1?"s":"") + ": \n - Total " + recordsCount + " records ingested. \n - " + (filesCount==0?"No":"Total " + filesCount) + " files ingested. ");
		else {
			exeReport.append("Import failed ( Found " +  objectsCount + " objets; Total " + recordsCount + " record" + (recordsCount>1?"s":"") + (failedCount>0?"; " + failedCount + " of " + rdfFiles.length + " failed":"") + (derivFailedCount>0?"; Derivatives creation failed for " + derivFailedCount + " files.":"") + (solrFailedCount>0?"; SOLR update failed for " + solrFailedCount + " records.":"") +"): \n ");
			if(ingestFailedCount > 0)
				exeReport.append(" - " + ingestFailedCount + " of " + filesCount + " file" + (filesCount>1?"s":"") + " failed: \n" + ingestFailed.toString() + " \n");
			if(metadataFailed.length() > 0)
				exeReport.append(" - Failed to import the following metadeta records: \n" + metadataFailed.toString());
			if(derivativesFailed.length() > 0)
				exeReport.append(" - Failed to create the following derivatives: \n" + derivativesFailed.toString());
			getSOLRReport();
		}
		
		if(objectsCount > 0){
			String key = null;
			exeReport.append("The following " + objectsCount + " object" + (objectsCount>1?"s are ":" is ") + "found in the source metadata: \n");
			for(Iterator<String> it=objRecords.keySet().iterator(); it.hasNext();){
				key = it.next();
				exeReport.append(key + " \t" + objRecords.get(key) + "\n");
			}
		}
		
		int recordSize = recordsIngested.size();
		if(recordSize > 0){
			exeReport.append("The following " + recordSize + " record" + (recordSize>1?"s are":" is") + " imported: \n");
			for(Iterator<String> it=recordsIngested.iterator(); it.hasNext();){
				exeReport.append(it.next() + "\n");
			}
		}else
			exeReport.append("No records were imported.\n");
		String exeInfo = exeReport.toString();
		log("log", exeInfo);
		if(filesIngested.length() > 0){
			log("log", "\nThe following files are ingested successfully: \n" + filesIngested.toString());
		}
		return exeInfo;
	}
}