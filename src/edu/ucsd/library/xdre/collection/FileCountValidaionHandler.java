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
	private StringBuilder filesIngested = new StringBuilder();
	protected Document filesDoc = null;
	private boolean ingestFile = false;
	private boolean dams4FileRename = false;
	private String[] filesPaths = null;
	private Map<String, File> filesMap = new HashMap<String, File>();
	private Map<String, String> dersMap = getDams4n3derivativesMap();
	
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

	public boolean isDams4FileRename() {
		return dams4FileRename;
	}

	public void setDams4FileRename(boolean dams4FileRename) {
		this.dams4FileRename = dams4FileRename;
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
					damsURI = DamsURI.toParts(fileId, subjectId, DamsURI.FILE);
					oid = damsURI.getObject();
					cid = damsURI.getComponent();
					fid = damsURI.getFileName();
					
					// Rename DAMS3 files to DAMS4 naming convention
					if(dams4FileRename && !damsClient.exists(oid, cid, fid)){
						String dams3FileName = getDams3FileName(oid, cid, fid);
						String dams4FileName = getDams4FileName(oid, cid, fid);
						String fileDir = Constants.FILESTORE_DIR + "/" + pairPath(DAMSClient.stripID(oid));
						File dams3File = new File(fileDir, dams3FileName);
						if(dams3File.exists()){
							File dams4File = new File(fileDir, dams4FileName);
							dams3File.renameTo(dams4File);
							logMessage("Renamed DAMS3 file " + dams3File.getPath() + " to " + dams4File.getPath());
						}
					}
					
					// Check source and alternate master files 
					if((fid!=null && (fid.equals("1") || fid.startsWith("1."))) || (use!=null && (use.endsWith(Constants.SOURCE) 
							|| use.endsWith(Constants.ALTERNATE)))){

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
	
	public String pairPath(String value){
		String path = "";
		for(int i=0; i<value.length(); i+=2){
			path += value.substring(i, (i+2<value.length()?i+2:value.length())) + "/";
		}
		return path;
	}
	
	public String getDams4FileName(String oid, String cid, String fid){
		return Constants.ARK_ORG + "-" + DAMSClient.stripID(oid) + "-" + (cid!=null&&cid.length()>0?cid:"0") + "-" + fid;
	}
	
	public String getDams3FileName(String oid, String cid, String fid){
		return Constants.ARK_ORG + "-" + DAMSClient.stripID(oid) + "-" + toDams3FileConvention(cid, fid);
	}
	
	private String toDams3FileConvention(String cid, String fid){
		String dams3Name = "1";
		if(cid != null && cid.length() > 0){
			//Complex object
			dams3Name += "-" + cid;
		}
		
		//files like xml in ETD and derivatives like jpeg, mp3, mp4 etc.
		String fName = dersMap.get(fid);
		if(fName == null){
			if(cid != null && cid.length() > 0 && fid.startsWith("1.")){
				//master file
				fName = fid.substring(1);
			}else
				//Unknown file???
				fName = fid;
		}
		
		dams3Name += "-" + fName;
		return dams3Name;
	}
	
	public static Map<String, String> getDams4n3derivativesMap(){
		Map<String, String> map = new HashMap<String, String>();
		if(Constants.DERIVATIVES_LIST != null){
			String[] pairs = Constants.DERIVATIVES_LIST.split(",");
			for(int i=0; i<pairs.length; i++){
				String[] pair = pairs[i].split(":");
				map.put(pair[0], pair[1]);
			}
		}
		return map;	
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
			}
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
						filesIngested.append(fileUrl + "\t" + tmpFile + "\t" + damsClient.getRequestURL() + "\n");
						
						//Create derivatives for images and documents PDFs
						if((isImage(fid, use) || isDocument(fid, use)) 
								&& (use == null || use.endsWith("source") || use.endsWith("alternate"))){
							
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
			
		}else{
			ingestFailedCount++;
			ingestFails.append( fileUrl + ", \n");
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
			exeReport.append("File count validation ( fails count " + failedCount + (missingObjectsCount>0?"; " + missingObjectsCount + missingObjectsMessage:"") + (missingFilesCount>0?"; " + missingFilesCount + missingFilesMessage:"") + (ingestFailedCount>0?"; " + ingestFailedCount + ingestFailedMessage:"") + (derivFailedCount>0?"; " + derivFailedCount + derivFailedMessage:"") + "): \n");	
		exeReport.append("Total files found " + filesTotal + ". \nNumber of objects found " + itemsCount + ". \nNumber of objects processed " + count  + ". \nNumber of source and/or alternate files " + masterTotal + ".\n" + (ingestedCount > 0?ingestedCount+ingestedMessage+".\n":""));
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
		if(filesIngested.length() > 0){
			log("log", "\nThe following files are ingested successfully: \n" + filesIngested.toString());
		}
		return exeInfo;
	}
}
