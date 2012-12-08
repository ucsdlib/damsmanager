package edu.ucsd.library.xdre.collection;

import java.beans.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.omg.CosNaming.BindingIterator;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * Class FileCountHandler handles file count validation operation
 * with collection master files count, duplicated files check, and
 * mixed file format check
 * 
 * @author lsitu@ucsd.edu
 */
public class FileCountHandler extends CollectionHandler{
	private static Logger log = Logger.getLogger(FileCountHandler.class);
	private int expectedFiles = -1;	
    private String message = "";
    private String preFileExt = null;
    private String duplicatedMessage = "";
    private String duplicatedFileIds = "";
    private String fileStoreMissing = "";
	private String differentFileExts = "";
	private int fileStoreCount = 0;
	private int fileStoreFailedCount = 0;
	private int totalFiles = 0;
	//boolean interrupted = false;
	
	public FileCountHandler(DAMSClient damsClient, String collectionId, int expectedFiles) throws Exception{
		super(damsClient, collectionId);
		this.expectedFiles = expectedFiles;
	}
	
	public FileCountHandler(DAMSClient damsClient, String collectionId, String collectionName, int expectedFiles, HttpSession session) throws Exception {
		this(damsClient, collectionId, expectedFiles);
		this.session = session;
	}
	

	/**
	 * Implements the collection handler's execute method for file count validation
	 * @throws Exception 
	 * @throws Exception 
	 */
	public boolean execute() throws Exception{
    	String subjectId = null;
    	String arkFilePrefix = Constants.ARK_ORG + "-";
    	totalFiles = (int) itemsCount;
    	String compId = null;
    	String fileId = "1";
   		for (int i = 0; i < itemsCount && !interrupted; i++) {
 
   			subjectId = items.get(i);
   			int complexObjectCount = queryComplexObject(subjectId);
   			if(complexObjectCount > 0)
   				handleComplexObject(subjectId, complexObjectCount, i);
   			else
	   			if(!handleObject(subjectId, compId, fileId, i))
	   				setExeResult(false);
   	        setProgressPercentage( ((i+1) * 100) / itemsCount);
   		}
   		
   		if(totalFiles !=  fileStoreCount)
   			setExeResult(false);
   		
		return exeResult;
	}

	public String getExeInfo() {

		message += " - Number of objects found: " + itemsCount + "\n";
		if( fileStoreCount != totalFiles){
			fileStoreMissing = fileStoreMissing.replace("\t", " ");
			message += " - Expected number of master files in Local Sotre: " + totalFiles + "\n";
			message += " - Number of master files found in LocalSotre: " + fileStoreCount + "\n";
			if(fileStoreMissing !=null && fileStoreMissing.length() > 0)
				message += " - Master files doesn't exist in Local Sotre: " + fileStoreMissing + "\n";
		}
		
		if(duplicatedFileIds != null && duplicatedFileIds.length() > 0){
			duplicatedMessage = duplicatedFileIds.replace("\t", " ");
			message += " - Potential duplicated files with the same checksum: " + duplicatedMessage + " \n ";
		}
		
        if(differentFileExts != null && differentFileExts.length() > 0){
        	differentFileExts = differentFileExts.replace("\t", " ");
			message += " - Files with different extension: " + differentFileExts + "\n";
        }
        
        String mHeader = "File validation for " + collectionTitle;
		if(getExeResult())
			message = mHeader + " succeeded: \n" + message + (duplicatedFileIds.length()==0?" - No duplicate files found.":"") + " \n " + (differentFileExts.length()==0?" - All files have the same file extension.":"") + "\n";
		else
			message = mHeader + " failed: \n" + message;
		log("log", message);
		return message;
	}
	
	private String getSubjectIds(List tmpList){
		String subjectIds = "";
		for(int j=0; j<tmpList.size(); j++){
			Object obj = tmpList.get(j);
			//XXX

		}
		return subjectIds;
	}
	
	private boolean handleComplexObject(String subjectId, int complexObjectCount, int itemIndex) throws Exception{
		boolean successful = true;
		int compId = 0;
		String fileId = "1";
		for(int i=0; i<complexObjectCount && !interrupted; i++){
			compId = 1 + 1;
			if(!handleObject(subjectId, ""+compId, fileId, itemIndex))
				successful = false;
		}
		return successful;
	}
	
	private boolean handleObject(String subjectId, String compId, String fileId, int itemCount) throws Exception {
        //Item reference from DDOM Viewer
	    String itemLink = getDDOMReference(subjectId);
	    setStatus("Processing File Count Validation for " + itemLink + " (" + (itemCount+1) + " of " + itemsCount + ")");
	    String eMessage = "";
	    String fileName = null;
	    BindingIterator bit = null;
    	List tmpList = null;
    	boolean successful = true;
		
		String fExt = null;
		int numTry = 0;
		boolean reTry = true;
		while(reTry && numTry++ < maxTry && !interrupted){
			fExt = getFileExtension(subjectId, compId, fileId);
			reTry = false;
		}
        if(fExt == null){	        	
        	//The moving_image collection
        	if(collectionId.equals("bb3550355p")){
        		fExt = ".avi";
        	}else{
    			eMessage = ": Unable to determine the file extension.";
    			setStatus(eMessage);
   				String iMessagePrefix = "File count validation failed with " + itemLink;
   				System.out.println(iMessagePrefix + eMessage);
   				setStatus(message += iMessagePrefix + eMessage  + "\n");
   				log("log", iMessagePrefix + eMessage);
   				log.info(iMessagePrefix + eMessage + "\n");
    			setExeResult(false);
    			return false;
        	}
        }
        
        
		numTry = 1;
        fileName = fileId + fExt;
		String fileURLPath = damsClient.toUrlPath(subjectId, compId, fileName);
        //String[] parts = toFileParts(arkFileName);
		do{
   			try{
				if (damsClient.exists(subjectId, compId, fileName)){
					successful = false;
					fileStoreFailedCount++;
					if(fileStoreFailedCount%5 == 0)
						fileStoreMissing +=  fileURLPath + "\n";
					else
						fileStoreMissing += fileURLPath + "\t";
					
					String iMessagePrefix = "Error: FileStore master file missing " + itemLink;
					setStatus(iMessagePrefix + "\n");
					log("log", iMessagePrefix);
		        	setExeResult(false);
		        	System.out.println(" FileStoreMissing " + fileURLPath);
		        }else{
		        	fileStoreCount++;
		        }
	        	reTry = false;
   			} catch (LoginException e){
				e.printStackTrace();
				if(numTry == maxTry){
					setExeResult(false);
					eMessage = subjectId + ". FileStoreAuthException: " + e.getMessage();
					String iMessagePrefix = "File count validation failed with ";
					setStatus(iMessagePrefix + eMessage);
					log("log", iMessagePrefix + eMessage );
					log.error(iMessagePrefix + eMessage, e);
				}
			}
		}while(numTry++ < maxTry && !interrupted);
		   			
		numTry = 1;
		do{
			//xxx
   			String itemData = damsClient.getMetadata(subjectId, null);
   			
   			if(fileName != null){
   				tmpList = damsClient.listObjects(itemData);
   				//Check for duplicated file name
   				
   				if(tmpList.size() > 1){
   					boolean duplicated = false;
   					String checksum = "";
   					List<String> checksums = new ArrayList<String>();
   					int duplicatedSize = tmpList.size();
   					for(int i=0; i<duplicatedSize; i++){
   						//XXX 
   						//chechsums from the list of objects
   						if(checksum == null)
   							checksum = "";
   						checksums.add(checksum);
   					}
   					
   					
   					for(int i=0; i<duplicatedSize - 1; i++){
   						duplicated = false;
   						checksum = checksums.remove(i);
   						int idx = checksums.indexOf(checksum);
   						if(idx >= 0){
   							duplicated = true;
   							//checksums.add(i, checksum);
   							break;
   						}else{
   							tmpList.remove(i);
   							i--;
   						}
   					}
   					if(duplicated){
	   					successful = false;
	   					duplicatedFileIds += "\n" + getSubjectIds(tmpList);
	   					
	   					String iMessagePrefix = "Error: duplicated files found for " + getSubjectIds(tmpList);
	   	   				//message += iMessagePrefix;
	   	   				setStatus(iMessagePrefix.replace("\t", " ")  + "\n");
	   	   				log("log", iMessagePrefix);
   					}
   				}
   			}
   		}while(numTry++ < maxTry && !interrupted);

		try{
			Thread.sleep(10);
		} catch (InterruptedException e) {
			interrupted = true;
			successful = false;
			setExeResult(false);
			eMessage = fileURLPath + ". Error: " + e.getMessage();
			String iMessagePrefix = "File count validation interrupted with ";
			System.out.println(iMessagePrefix + eMessage);
			setStatus("Canceled");
			clearSession();
			log("log", iMessagePrefix + eMessage );
			log.info(iMessagePrefix + eMessage, e);				
		}
		return successful;
	}
}
