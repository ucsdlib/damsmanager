package edu.ucsd.library.xdre.imports;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
 * DFDAMS4ImportHandler: Import objects (metadata and files) in DAMS4 RDF/XML format.
 * @author lsitu@ucsd.edu
 */
public class RDFDAMS4ImportHandler extends MetadataImportHandler{

	private static Logger log = Logger.getLogger(RDFDAMS4ImportHandler.class);

	public static final String INFO_MODEL_PREFIX = "info:fedora/afmodel:Dams";
	public static final String HAS_FILE = "hasFile";
	private Map<String, String> idsMap = new HashMap<String, String>();
	private Map<String, File> filesMap = new HashMap<String, File>();
	
	private DAMSClient damsClient = null;
	private String importOption = null;
	private File[] rdfFiles = null;
	private String[] filesPaths = null;

	private int objectsCount = 0;
	private int recordsCount = 0;
	private int filesCount = 0;
	private int ingestFailedCount = 0;
	private int derivFailedCount = 0;
	private StringBuilder ingestFailed = new StringBuilder();
	private StringBuilder metadataFailed = new StringBuilder();
	private StringBuilder derivativesFailed = new StringBuilder();
	
	/**
	 * Constructor
	 * @param damsClient
	 * @param rdf
	 * @param mode
	 * @throws Exception
	 */
	public RDFDAMS4ImportHandler(DAMSClient damsClient, File[] rdfFiles, String importOption) throws Exception {
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
					String srcId = null;
					Node nUri = nodes.get(j);
					String iUri = nUri.getStringValue();
					Node parentNode = nUri.getParent();
					String nName = parentNode.getName();
					
					if(iUri.endsWith("/COL") || !(iUri.startsWith("http") && iUri.indexOf("/ark:/") > 0)){
						// Assign ARK
						
						if(nName.endsWith("Object")){
							objectsCount++;
							oid = idsMap.get(iUri);
							// Assign new ARK
							if(oid == null){
								oid = getNewId();
								idsMap.put(iUri, oid);
							}
							nUri.setText(oid);
							
							updateReference(doc, iUri, oid);
							
						} else if (nName.endsWith("Component") || nName.endsWith("File")){
							damsURI = DamsURI.toParts(iUri, null);
							srcId = damsURI.getObject();
							oid = idsMap.get(srcId);
							if(oid == null){
								oid = getNewId();
								idsMap.put(srcId, oid);
							}
							damsURI.setObject(oid);
							nUri.setText(damsURI.toString());
							// XXX
							// Assign cid and fid for Component and FIle if required
							
						} else {
							String field = null;
							Node tNode = null;
							String xPath = null;
							if (nName.endsWith("Collection")){
								// Retrieve the Collection record
								field = "title_tesim";
								xPath = "dams:title/dams:Title/rdf:value";
								tNode = parentNode.selectSingleNode(xPath);
							} else if (nName.endsWith("Subject") || nName.endsWith("Name") || nName.endsWith("Topic") || nName.endsWith("GenreForm")){
								// Subject, Authority records use mads:authoritativeLabel
								field = "name_tesim";
								xPath = "mads:authoritativeLabel";
								tNode = parentNode.selectSingleNode(xPath);
							} else {
								// Other records like Role, Language etc. use rdf:value
								field = "value_tesim";
								xPath = "rdf:value";
								tNode = parentNode.selectSingleNode(xPath);
	
							}
							if(tNode == null){
								throw new Exception("Element " + xPath + " is missing from the " + nName + " record " + iUri + " in file " + currFile + ".");
							}
							updateDocument(doc, parentNode, field, tNode.getText());
						}
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
				
				for (int j=0; j<items.size()&&!interrupted; j++){
					graph = new RDFStore();
					recordsCount++;
					// Add subject
					subjectId = items.get(j);
					try{
						setStatus("Processing metadata for subject " + subjectId  + " (" + (i+1) + " of " + fLen + ") ... " ); 
						boolean succeeded = false;
						objURIs = objects.get(subjectId);
	
						for(int k=0; k<objURIs.size(); k++){
							objURI = objURIs.get(k);
							rdf = rdfStore.querySubject(objURI.toString());
							graph.merge(rdf);
						}
						
						// Update object
						System.out.println(j + " ingesting record " + subjectId + ":\n" + graph.export(RDFStore.RDFXML_ABBREV_FORMAT) + "\n\n");
						
						succeeded = damsClient.updateObject(subjectId, graph.export(RDFStore.RDFXML_ABBREV_FORMAT), Constants.IMPORT_MODE_ADD);
							
						if(!succeeded){
							if(metadataFailed.indexOf(currFile) < 0)
								failedCount++;
							metadataFailed.append(subjectId + " (" + currFile + "), \n");
							message = "Metadata import for subject " + subjectId  + " failed (" + (i+1) + " of " + fLen + ").";
							setStatus( message ); 
							logError(message + "\n Error RDF: \n" + graph.export(RDFStore.RDFXML_ABBREV_FORMAT));
						}else{
							message = "Metadata import for subject " + subjectId  + " succeeded (" + (i+1) + " of " + fLen + "). ";
							setStatus(message); 
							logMessage(message);
							log.warn(message);
						}
					
					} catch (Exception e) {
						e.printStackTrace();
						if(metadataFailed.indexOf(currFile) < 0)
							failedCount++;
						metadataFailed.append(subjectId + " (" + currFile + "), \n");
						message = "Metadata import failed: " + e.getMessage();
						setStatus( message  + " (" +(i+1)+ " of " + fLen + ")"); 
						logError(message);
					}
					
					setProgressPercentage( ((i + 1) * 100) / fLen);
					
					try{
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
						interrupted = true;
						failedCount++;
						metadataFailed.append(subjectId + " (" + currFile + ") \n");
						message = "Metadata population interrupted for subject " + subjectId  + ". \n Error: " + e.getMessage() + "\n";
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
				setStatus( message  + " (" +(i+1)+ " of " + fLen + ")");
				logError(message);
			}
			
			setProgressPercentage( ((i + 1) * 100) / fLen);
			
			try{
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
				interrupted = true;
				failedCount++;
				message = "Import interrupted for oject file " + currFile + ". \n Error: " + e.getMessage() + "\n";
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
		for(int l=0; l<oStmt.size()&&!interrupted;l++){
			stmt = oStmt.get(l);
			prop = stmt.getPredicate();
			if(prop.getLocalName().equals(HAS_FILE)){
				filesCount++;
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
				
				setStatus("Ingesting file " + fileUrl + " [" + srcFileName + "] in " + srcName + " ... " );
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
							logError("Source file for " + srcFileName + " doesn't exist. Please choose a correct stage file location.");
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
									logError("Error ingesting file " + fileUrl  + " (" + tmpFile + ")" + " in " + srcName + ".");
								}else{
									message = "Ingested file " + fileUrl + " (" + tmpFile + ") in " + srcName + ". ";
									log.info(message);
									logMessage(message);
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
											logError("Failed to created derivatives " + damsClient.getRequestURL() + "(" + srcFileName + "). ");
										}
									}
								}
							}catch(Exception e){
								e.printStackTrace();
								ingestFailedCount++;
								ingestFailed.append(fileUrl + " (" + tmpFile + "), \n");
								logError("Failed to ingest file " + fileUrl + " (" + tmpFile + ") in " + srcName + ": " + e.getMessage());
							}
						}
					}
				}else{
					ingestFailedCount++;
					ingestFailed.append( fid + ", \n");
					logError("Missing sourceFileName property for file " + fileUrl + " in " + srcName + ".");
				}	
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
	private void updateDocument(Document doc, Node record, String field, String title) throws Exception{
		// Subject, Authority records use mads:authoritativeLabel
		Node aboutAttr = record.selectSingleNode("@rdf:about");
		String srcUri = aboutAttr.getStringValue();
		String nName = record.getName();
		String nKey = INFO_MODEL_PREFIX+nName + "::" + title;
		String oid = idsMap.get(nKey);
		// Retrieve the record
		if(oid == null){
			oid = lookupRecord(field, title, record.getName());
			if(oid == null){
				// Create the record
				oid = getNewId();
				aboutAttr.setText(oid);
				idsMap.put(nKey, oid);
				System.out.println("Found new redord " + srcUri + " (" + oid + ": " + field + " -- " + title);
				
			}else{
				// Record found. Add linking, remove it.
				toResourceLinking(oid, record);
			}			
		}else{	
			// Record added. Add linking, remove it.
			toResourceLinking(oid, record);
		}

		updateReference(doc, srcUri, oid);
	}
	
	/**
	 * Mint a new ark id
	 * @return
	 * @throws Exception
	 */
	public String getNewId() throws Exception{
		return toDamsUrl(damsClient.mintArk(null));
	}
	
	/**
	 * Update record for resource linking
	 * @param url
	 * @param node
	 */
	public void toResourceLinking(String url, Node record){
		Element pNode = record.getParent();
		pNode.addAttribute("rdf:resource", toDamsUrl(url));
		record.detach();
	}
	
	/**
	 * Construct the URL with an id.
	 * @param arkUrl
	 * @return
	 */
	public String toDamsUrl(String arkUrl){
		if(!arkUrl.startsWith("http")){
			String arkUrlBase = "http://library.ucsd.edu/ark:";//Constants.DAMS_ARK_URL_BASE;
			arkUrl = arkUrlBase + (arkUrlBase.endsWith("/")?"":"/") + (arkUrl.indexOf('/')>0?arkUrl:"20775/" + arkUrl);
		}
		return arkUrl;
	}
	
	public void updateReference(Document doc, String srcId, String oid){
		List<Node> resNodes = doc.selectNodes("//@rdf:resource");
		for(int k=0; k<resNodes.size(); k++){
			Node nRes = resNodes.get(k);
			if(nRes.getStringValue().equals(srcId)){
				nRes.setText(oid);
				//System.out.println("Res Node: " + nRes.getParent().asXML());
			}
		}
	}
	
	public void logData(String fileName, String content) throws FileNotFoundException{
		File file = new File(Constants.TMP_FILE_DIR + "/damsmanager");
		if(!file.exists())
			file.mkdir();
		PrintWriter writer = null;
		try{
			writer = new PrintWriter(file.getAbsoluteFile() + "/" + fileName);
			writer.write(content);
		}finally{
			close(writer);
		}
	}
	
	/**
	 * Look up record from dams
	 * @param value
	 * @param modelName
	 * @return
	 * @throws Exception
	 */
	public String lookupRecord(String field, String value, String modelName) throws Exception{
		String modelParam = "\"" + INFO_MODEL_PREFIX + modelName + "\"";
		// XXX Customize for Simple Subject and Complex Subject for now 
		//if(modelName.endsWith("Topic"))
		//	modelParam = "(" + modelParam +  " OR \"" + INFO_MODEL_PREFIX + "ComplexSubject\")";
		String query = "q=" + URLEncoder.encode("name_tesim:\"" + value + "\" AND has_model_ssim:" + modelParam, "UTF-8") + "&fl=id&fl=has_model_ssim";
		Document doc = damsClient.solrLookup(query);
		int numFound = Integer.parseInt(doc.selectSingleNode("/response/result/@numFound").getStringValue());
		if(numFound <= 0)
			return null;
		else
			return ((Node)doc.selectNodes("/response/result/doc/str[@name='id']").get(0)).getText();
	}
	
	/**
	 * Execution result message
	 */
	public String getExeInfo() {
		if(exeResult)
			exeReport.append("Successful imported " + objectsCount + " objets in " + rdfFiles.length + " metadata files: \n - Total " + recordsCount + " records ingested. \n - Total " + filesCount + " files ingested. ");
		else {
			exeReport.append("Import failed ( found " +  objectsCount + " objets; Total " + recordsCount + " records" + (failedCount>0?"; " + failedCount + " of " + rdfFiles.length + " failed":"") + (derivFailedCount>0?"; Derivatives creation failed for " + derivFailedCount + " files.":"") +"): \n ");
			if(ingestFailedCount > 0)
				exeReport.append(" - " + ingestFailedCount + " of " + filesCount + " files failed: \n" + ingestFailed.toString() + " \n");
			if(metadataFailed.length() > 0)
				exeReport.append(" - Failed to import the following metadeta records: \n" + metadataFailed.toString());
			if(derivativesFailed.length() > 0)
				exeReport.append(" - Failed to create the following derivatives: \n" + derivativesFailed.toString());
		}
		String exeInfo = exeReport.toString();
		log("log", exeInfo);
		return exeInfo;
	}
}
