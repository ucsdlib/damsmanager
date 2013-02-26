package edu.ucsd.library.xdre.imports;

import java.io.File;
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
	//private Map<String, String> collsTitleMap = null;
	private Map<String, String> idsMap = new HashMap<String, String>();
	private Map<String, File> filesMap = new HashMap<String, File>();
	//private Map<String, Map<String, List<String>>> objectsMap = new HashMap<String, Map<String, List<String>>>();
	
	private DAMSClient damsClient = null;
	private String importMode = null;
	private File[] rdfFiles = null;
	private String[] srcLocations = null;
	private int recordsCount = 0;
	private int filesCount = 0;
	private int ingestFailedCount = 0;
	private StringBuilder ingestFailed = new StringBuilder();
	private StringBuilder metadataFailed = new StringBuilder();
	
	/**
	 * Constructor
	 * @param damsClient
	 * @param rdf
	 * @param mode
	 * @throws Exception
	 */
	public RDFDAMS4ImportHandler(DAMSClient damsClient, File[] rdfFiles, String importMode) throws Exception {
		super(damsClient, null);
		this.damsClient = damsClient;
		this.rdfFiles = rdfFiles;
		this.importMode = importMode;
	}

	public String[] getSrcLocations() {
		return srcLocations;
	}

	public void setSrcLocations(String[] srcLocations) {
		this.srcLocations = srcLocations;
	}

	/**
	 * Procedure to populate the RDF metadata and ingest the files
	 */
	public boolean execute() throws Exception {

		if(srcLocations != null){
			File file = null;
			// List the source files
			for(int i=0; i<srcLocations.length; i++){
				file = new File(srcLocations[i]);
				if(file.exists()){
					listFile(file);
				}
			}
		}
		
		String message = "";
		Document doc = null;
		DamsURI damsURI = null;
		
		String oid = null;
		Map<String, List<String>> parts = new HashMap<String, List<String>>();
		int fLen = rdfFiles.length;
		String currFile = null;
		SAXReader saxReader = new SAXReader();
		for(int i=0; i<fLen; i++){
			currFile = rdfFiles[i].getAbsolutePath();
			setStatus("Processing metadata in file " + currFile + " (" + (i+1) + " of " + fLen + ") ... " );
			try{
				doc = saxReader.read(currFile);
				List<Node> nodes = doc.selectNodes("//@rdf:about");
				for(int j=0; j<nodes.size(); j++){
					String srcId = null;
					Node nUri = nodes.get(j);
					String iUri = nUri.getStringValue();
					Node parentNode = nUri.getParent();
					String nName = parentNode.getName();
					
					if(!(iUri.startsWith("http") && iUri.indexOf("/ark:/") > 0)){
						// Assign ARK
						
						if(nName.endsWith("Object")){
							oid = idsMap.get(iUri);
							// Assign new ARK
							if(oid == null){
								oid = getNewId();
								idsMap.put(iUri, oid);
							}
							nUri.setText(oid);
							
							updateReference(doc, iUri, oid);
							
						} else if (nName.endsWith("Component") || nName.endsWith("File")){
							//System.out.println("Processing " + nName + " " + iUri);
							damsURI = DamsURI.toParts(iUri, null);
							srcId = damsURI.getObject();
							oid = idsMap.get(srcId);
							if(oid == null){
								oid = getNewId();
								idsMap.put(srcId, oid);
							}
							damsURI.setObject(oid);
							nUri.setText(damsURI.toString());
							//System.out.println("Processing " + nName + " " + nUri.getParent().asXML().substring(0, 200));
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
								//System.out.println(parentNode.asXML());
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
				
				rdfStore = new RDFStore();
				Model rdf = rdfStore.loadRDFXML(doc.asXML().replace(" http://lccn.loc.gov/", "http://lccn.loc.gov/"));
				initHandler();
				
				// Ingest the source file
				String cid = null;
				String fid = null;
				String srcPath = null;
				String srcFileName = null;
				Statement stmt = null;
				Statement tmpStmt = null;
				Property prop = null;
				RDFNode fNode = null;
				RDFNode oNode = null;
				List<Statement> oStmt = rdf.listStatements().toList();
				for(int l=0; l<oStmt.size();l++){
					stmt = oStmt.get(l);
					prop = stmt.getPredicate();
					if(prop.getLocalName().equals(HAS_FILE)){
						filesCount++;
						fNode = stmt.getObject();
						fid = fNode.asResource().getURI();
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
									logError("Source file " + srcFileName + " doesn't exist. File location is not provided or not valid.");
								}else{
									// Ingest the file
									DamsURI fileURI = null;
									boolean ingested = false;
									String tmpFile = srcFile.getAbsolutePath();
									try{
										fileURI = DamsURI.toParts(fid, null);
										ingested = damsClient.createFile(fileURI.getObject(), fileURI.getComponent(), fileURI.getFileName(), tmpFile, use);
										if(!ingested){
											ingestFailedCount++;
											ingestFailed.append(fid + "(" + tmpFile + "), \n");
											logError("Failed to ingest file " + fid + " (" + tmpFile + ").");
										}else{
											message = "Ingested file " + fileURI.toString() + "(" + fid + "). ";
											log.info(message);
											logMessage(message);
											// Remove the hasFile property from the record which was ingested during file ingestion.
											rdf.remove(stmt);
											rdf.remove(stmts);
										}
									}catch(Exception e){
										e.printStackTrace();
										ingestFailedCount++;
										ingestFailed.append(fid + "(" + tmpFile + "), \n");
										logError("Failed to ingest file " + fid + " (" + tmpFile + "): " + e.getMessage());
									}
								}
							}
						}else{
							ingestFailedCount++;
							ingestFailed.append( fid + ", \n");
							logError("Source file name is missing for file " + fid + ".");
						}	
					}
				}
				
				// Ingest the records
				String subjectId = null;
				DamsURI objURI = null;
				List<DamsURI> objURIs = null;
				RDFStore graph = new RDFStore();
				for (int j=0; j<items.size(); j++){
					recordsCount++;
					// Add subject
					subjectId = items.get(i);
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
						System.out.println(graph.export(RDFStore.RDFXML_ABBREV_FORMAT));
						succeeded = damsClient.updateObject(subjectId, graph.export(RDFStore.RDFXML_ABBREV_FORMAT), importMode);
							
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
							log.info(message);
						}
					
					} catch (Exception e) {
						e.printStackTrace();
						if(metadataFailed.indexOf(currFile) < 0)
							failedCount++;
						metadataFailed.append(subjectId + " (" + currFile + "), \n");
						message = "Metadata import failed: " + e.getMessage();
						setStatus( message  + "(" +(i+1)+ " of " + fLen + ")"); 
						logError(message);
					}
					setProgressPercentage( ((i + 1) * 100) / fLen);
					
					try{
						Thread.sleep(10);
					} catch (InterruptedException e) {
						e.printStackTrace();
						failedCount++;
						metadataFailed.append(subjectId + " (" + currFile + ") \n");
						message = "Metadata population interrupted for subject " + subjectId  + ". \n Error: " + e.getMessage() + "\n";
						setStatus("Canceled");
						clearSession();
						logError(message);
						break;
					}
				}
				
				
				PrintWriter writer = new PrintWriter(rdfFiles[i].getParentFile().getParent() + "/" + "_" + rdfFiles[i].getName());
				writer.write(rdfStore.export(RDFStore.RDFXML_ABBREV_FORMAT));
				writer.close();

			}catch(Exception e){
				e.printStackTrace();
				failedCount++;
				message = "Import failed for oject file " + currFile + ": " + e.getMessage();
				setStatus( message  + "(" +(i+1)+ " of " + fLen + ")");
				logError(message);
			}
		}
		return exeResult;
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
			}else{
				Element pNode = record.getParent();
				pNode.addAttribute("rdf:resource", toDamsUrl(oid));
				// Record exist. Remove it.
				record.detach();
			}
		}else{
			// Record exist. Remove it.
			Element pNode = record.getParent();
			pNode.addAttribute("rdf:resource", toDamsUrl(oid));
			record.detach();
		}
		updateReference(doc, srcUri, oid);
	}
	
	/**
	 * List all the files recursively.
	 * @param file
	 */
	private void listFile(File file){
		String fName = null;
		File tmpFile = null;
		File[] files = file.listFiles();
		for(int i=0; i<files.length; i++){
			tmpFile = files[i];
			if(tmpFile.isDirectory())
				listFile(tmpFile);
			else {
				fName = tmpFile.getName();
				if(filesMap.get(fName) != null){
					logError("Duplicate source file name found: " + tmpFile.getAbsoluteFile() + "(" + filesMap.get(fName).getAbsolutePath() + ").");
				}else
					filesMap.put(fName, tmpFile);
			}	
		}
	}
	
	public String getNewId() throws Exception{
		return toDamsUrl(damsClient.mintArk(null));
	}
	
	public String toDamsUrl(String arkUrl){
		if(!arkUrl.startsWith("http"))
			arkUrl = Constants.DAMS_ARK_URL_BASE + (arkUrl.indexOf('/')>0?arkUrl:Constants.ARK_ORG+ "/" + arkUrl);
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
	
	/**
	 * Look up record from dams
	 * @param value
	 * @param modelName
	 * @return
	 * @throws Exception
	 */
	public String lookupRecord(String field, String value, String modelName) throws Exception{
		String modelParam = "\"" + INFO_MODEL_PREFIX + modelName + "\"";
		// XXX Customize for Simple Subject and Complex Subject 
		if(modelName.endsWith("Topic"))
			modelParam = "(" + modelParam +  " OR \"" + INFO_MODEL_PREFIX + "ComplexSubject\")";
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
			exeReport.append("Import succeeded (Total " + rdfFiles.length + " metadata files imported): \n - Total " + recordsCount + " records ingested. \n - Total " + filesCount + " files ingested. ");
		else {
			exeReport.append("Import failed (" + failedCount + " of " + rdfFiles.length + " failed): \n ");
			exeReport.append(" - " + ingestFailed + " of " + filesCount + " files failed: \n" + ingestFailed.toString() + " \n");
			exeReport.append(" - Failed to import the following metadeta records: \n" + metadataFailed.toString());
		}
		String exeInfo = exeReport.toString();
		log("log", exeInfo);
		return exeInfo;
	}
}
