package edu.ucsd.library.xdre.collection;

import java.io.File;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DFile;
import edu.ucsd.library.xdre.utils.DamsURI;

/**
 * 
 * FileCountValidaionHandler: perform checksum validation.
 * @author lsitu@ucsd.edu
 */
public class FileCountValidaionHandler extends CollectionHandler{
	private static Logger log = Logger.getLogger(FileCountValidaionHandler.class);

	protected int count = 0;
	protected int missingObjectsCount = 0;
	protected int missingFilesCount = 0;
	protected int failedCount = 0;
	protected int masterTotal = 0;
	protected int filesTotal = 0;
	protected int ingestedCount = 0;
	protected int ingestFailedCount = 0;
	protected int derivFailedCount = 0;
	protected StringBuilder missingObjects = new StringBuilder();
	protected StringBuilder missingFiles = new StringBuilder();
	protected StringBuilder duplicatedFiles = new StringBuilder();
	protected StringBuilder ingestFails = new StringBuilder();
	protected StringBuilder derivFails = new StringBuilder();
	protected Document filesDoc = null;
	private boolean ingestFile = false;
	private String[] filesPaths = null;
	private Map<String, File> filesMap = new HashMap<String, File>();
	
	/**
	 * Constructor for FileCountValidaionHandler
	 * @param damsClient
	 * @throws Exception
	 */
	public FileCountValidaionHandler(DAMSClient damsClient) throws Exception{
		this(damsClient, null);
	}
	
	/**
	 * Constructor for FileCountValidaionHandler
	 * @param damsClient
	 * @param collectionId
	 * @throws Exception
	 */
	public FileCountValidaionHandler(DAMSClient damsClient, String collectionId) throws Exception{
		super(damsClient, collectionId);
		if(collectionId != null)
			filesDoc = damsClient.getCollectionFiles(collectionId);
	}

	public boolean isIngestFile() {
		return ingestFile;
	}

	public void setIngestFile(boolean ingestFile) {
		this.ingestFile = ingestFile;
	}

	public String[] getFilesPaths() {
		return filesPaths;
	}

	public void setFilesPaths(String[] filesPaths) {
		this.filesPaths = filesPaths;
	}

	/**
	 * Procedure for file count validation
	 */
	public boolean execute() throws Exception {
		if(ingestFile && filesPaths != null){
			File file = null;
			// List the source files
			for(int i=0; i<filesPaths.length; i++){
				file = new File(filesPaths[i]);
				if(file.exists()){
					listFile(filesMap, file);
				}
			}
		}
		
		String subjectId = null;
		String fileId = null;
		DamsURI damsURI = null;
		String use = null;
		String oid = null;
		String cid = null;
		String fid = null;
		for(int i=0; i<itemsCount; i++){

			boolean masterExists = false;
			boolean missing = false;
			boolean duplicated = false;
			boolean updateSOLR = false;
			count++;
			subjectId = items.get(i);
			try{
				setStatus("Processing file count validation for object " + subjectId  + " (" + (i+1) + " of " + itemsCount + ") ... " ); 
				DFile dFile = null;
				int duSize = 0;
				List<DFile> files = damsClient.listObjectFiles(subjectId);
				for(Iterator<DFile> it=files.iterator(); it.hasNext();){
					filesTotal++;
					dFile = it.next();
					use = dFile.getUse();
					
					// Check file existence
					fileId = dFile.getId();
					damsURI = DamsURI.toParts(fileId, subjectId);
					oid = damsURI.getObject();
					cid = damsURI.getComponent();
					fid = damsURI.getFileName();
					
					// Files has no file extension
					if(fid==null && cid != null){
						fid = cid;
						cid = null;
						damsURI.setFileName(fid);
						damsURI.setComponent(cid);
					}
					// Check source and alternate master files 
					if((fid!=null && (fid.equals("1") || fid.startsWith("1."))) || (use!=null && (use.endsWith(Constants.SOURCE) 
							|| (use.endsWith(Constants.SERVICE) && !use.startsWith(Constants.IMAGE)) || use.endsWith(Constants.ALTERNATE)))){

						masterTotal++;
						masterExists = true;
						
						// Ingest the file from staging or from the original source path
						// Count it as missing when ingest failed.
						if(ingestFile && !damsClient.exists(oid, cid, fid)){
							if(ingestFile(dFile))
								updateSOLR = true;
						}
						
						String srcFileName = dFile.getSourceFileName();
						if(srcFileName != null){
							List<DamsURI> duFiles = DAMSClient.getFiles(filesDoc, null, srcFileName);
							if((duSize=duFiles.size()) > 1){
								String[] checksums = new String[duSize];
								for(int j=0; j<duSize; j++){
									checksums[j] = getChecksum(duFiles.get(j));
								}
								
								// Check checksums for duplication
								boolean du = false;
								String duItems = "";
								for(int j=0; j<duSize; j++){
									for(int k=j+1; k<duSize; k++){
										if(checksums[j].equals(checksums[k]) && duplicatedFiles.indexOf(duFiles.get(j).toString())<0){
											duItems += duFiles.get(j) + ", " + duFiles.get(k) + ", ";
											du = true;
										}
									}
								}
								if(du){
									duplicated = true;;
									duplicatedFiles.append(duItems.substring(0, duItems.length()-2) + "\n");
									logError("Duplicated files found: " + duItems);
								}
							}
						}
					}
					
					if(!damsClient.exists(oid, cid, fid)){
						missingFilesCount++;
						missing = true;
						missingFiles.append(fileId + "\t" + (missingFilesCount%10==0?"\n":""));
						logError("File " + fileId + " (" + damsClient.getRequestURL() + ") doesn't exist.");
					}
				}
				if(!masterExists || missing || duplicated){
					failedCount++;
					if(!masterExists){
						missingObjectsCount++;
						missingObjects.append(subjectId + "\t" + (missingObjectsCount%10==0?"\n":""));
						logError("No master files exist: " + subjectId);
					}
				}
			} catch (Exception e) {
				failedCount++;
				e.printStackTrace();
				logError("File count validation failed " + (damsURI==null?": ":damsURI+": ") + e.getMessage());
			}
			
			// Updated SOLR
			if(updateSOLR && !updateSOLR(subjectId))
				failedCount++;

			setProgressPercentage( ((i + 1) * 100) / itemsCount);
			
			try{
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				failedCount++;
    			logError("File count validation canceled on object " + subjectId  + ".");
				setStatus("Canceled");
				clearSession();
				break;
			}
		}
		
		return exeResult;
	}
	
	private String getChecksum(DamsURI damsURI){
		String checksum = "";
		String checkSumXPath = DAMSClient.DOCUMENT_RESPONSE_ROOT_PATH + "/files/value[id='" + damsURI.toString() + "']/crc32checksum";
		Node checksumNode = filesDoc.selectSingleNode(checkSumXPath);
		if(checksumNode != null)
			checksum = checksumNode.getText();
		return checksum;
	}
	
	public boolean ingestFile(DFile dFile){
		String message = "";
		String oid = null;
		String cid = null;
		String fid = null;

		String fileUrl = dFile.getId();
		String use = dFile.getUse();
		String srcFileName = dFile.getSourceFileName();
		String srcPath = dFile.getSourcePath();
		boolean successful = false;
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
					ingestFailedCount++;
					ingestFails.append(fileUrl + " (" + (srcPath==null?"":srcPath+"/"+fName) + "), \n");
					logError("Source file for " + fileUrl + " (" + srcFileName + ") doesn't exist. \nPlease make sure the file is deposited in dams staging and the location is selected for ingestion.");
				}else{
					// Ingest the file
					DamsURI dURI = null;
					String tmpFile = srcFile.getAbsolutePath();
					try{
						dURI = DamsURI.toParts(fileUrl, null);
						Map<String, String> params = new HashMap<String, String>();
						oid = dURI.getObject();
						cid = dURI.getComponent();
						fid = dURI.getFileName();
						
						params.put("oid", oid);
						params.put("cid", cid);
						params.put("fid", fid);
						params.put("use", use);
						params.put("local", tmpFile);
						params.put("sourceFileName", srcFileName);
						successful = damsClient.createFile(params);
						if(!successful){
							ingestFailedCount++;
							ingestFails.append(fileUrl + " (" + tmpFile + "), \n");
							logError("Error ingesting file " + fileUrl  + " (" + tmpFile + ").");
						}else{
							ingestedCount++;
							message = "Ingested file " + fileUrl + " (" + tmpFile + "). ";
							log.info(message);
							logMessage(message);
							
							//Create derivatives for images and documents PDFs
							if((isImage(fid, use) || isDocument(fid, use)) 
									&& (use == null || use.endsWith("source") || use.endsWith("service") || use.endsWith("alternate") || use.endsWith("Master"))){
								
								successful = damsClient.createDerivatives(oid, cid, fid, null);
								if(successful){
									logMessage( "Created derivatives for " + fileUrl + " (" + damsClient.getRequestURL() + ").");
								} else {
									derivFailedCount++;
									derivFails.append(damsClient.getRequestURL() + ", \n"); 
									logError("Failed to created derivatives " + damsClient.getRequestURL() + "(" + srcFileName + "). ");
								}
							}
						}
					}catch(Exception e){
						e.printStackTrace();
						ingestFailedCount++;
						ingestFails.append(fileUrl + " (" + tmpFile + "), \n");
						logError("Failed to ingest file " + fileUrl + " (" + tmpFile + "): " + e.getMessage());
					}
				}
			}
		}else{
			ingestFailedCount++;
			ingestFails.append( fid + ", \n");
			logError("Missing sourceFileName property for file " + fileUrl + ".");
		}
		return successful;
	}
	
	/**
	 * Execution result message
	 */
	public String getExeInfo() {
		String missingObjectsMessage = " object" + (missingObjectsCount>1?"s have ":" has ") + " no master files";
		String missingFilesMessage = " file" + (missingFilesCount>1?"s are ":" is ") + " missing from " + damsClient.getFileStore();
		String ingestedMessage = " file" + (ingestedCount>1?"s are ":" is ") + " ingested";
		String ingestFailedMessage = " file" + (ingestFailedCount>1?"s are ":" is ") + " failed to be ingested";
		String derivFailedMessage = " file" + (derivFailedCount>1?"s are ":" is ") + " failed for derivatives creation";
		if(exeResult)
			exeReport.append("File count validation succeeded: \n");
		else
			exeReport.append("File count validation (" + failedCount + " of " + itemsCount + " objects failed for validation" + (missingObjectsCount>0?"; " + missingObjectsCount + missingObjectsMessage:"") + (missingFilesCount>0?"; " + missingFilesCount + missingFilesMessage:"") + (ingestFailedCount>0?"; " + ingestFailedCount + ingestFailedMessage:"") + (derivFailedCount>0?"; " + derivFailedCount + derivFailedMessage:"") + "): \n");	
		exeReport.append("Total files found " + filesTotal + ". \nNumber of objects found " + itemsCount + ". \nNumber of objects processed " + count  + ". \nNumber of source, service and alternate files " + masterTotal + ".\n" + (ingestedCount > 0?ingestedCount+ingestedMessage+".\n":""));
		if(duplicatedFiles.length() > 0)
			exeReport.append("\nThe following files are duplicated: \n" + duplicatedFiles.toString());
		
		if(missingObjects.length() > 0)
			exeReport.append("\nThe following " + missingObjectsMessage + " : \n" + missingObjects.toString());
		
		if(missingFiles.length() > 0)
			exeReport.append("\nThe following " + missingFilesMessage + " : \n" + missingFiles.toString());
		
		if(ingestFails.length() > 0)
			exeReport.append("\nThe following " + ingestFailedMessage + " : \n" + ingestFails.toString());
		
		if(derivFails.length() > 0)
			exeReport.append("\nThe following " + derivFailedMessage + " : \n" + derivFails.toString());
		
		// Add solr report message
		exeReport.append(getSOLRReport());
		String exeInfo = exeReport.toString();
		log("log", exeInfo);
		return exeInfo;
	}
}
