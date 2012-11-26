package edu.ucsd.library.xdre.collection;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;

import javax.security.auth.login.LoginException;

import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * Class MetaDataFileUploadHandler handles the uplaoding of metadata file to FileStore
 * 
 * @author lsitu@ucsd.edu
 */
public class MetaDataStreamUploadHandler extends CollectionHandler{

	private static Logger log = Logger.getLogger(MetaDataStreamUploadHandler.class);
	private boolean replaceOrNot = false;
	private String fileType = null;
	private String fileExt = null;
	private boolean jhoveUpload = false;
	
    private StringBuilder message = new StringBuilder();
    private int counter = 0;
    private int failedsCount = 0;
    private int updatedCount = 0;
    
	public MetaDataStreamUploadHandler(String fileType, boolean replaceOrNot) 
	    	throws Exception {
		this(null, null, fileType, replaceOrNot);
	}
	
	public MetaDataStreamUploadHandler(DAMSClient damsClient, String collectionId, String fileType, boolean replaceOrNot) 
	        throws Exception {
		super(damsClient, collectionId);
		this.fileType = fileType;
		this.replaceOrNot = replaceOrNot;
		initHandler();
	}

	private void initHandler() throws Exception{
		if("RDF".equalsIgnoreCase(fileType)){
			if(jhoveUpload)
				fileExt = "-jhove.xml";
			else
				fileExt = "-rdf.xml";

		}else if("METS".equalsIgnoreCase(fileType)){
			fileExt = "-mets.xml";
		}else
			throw new Exception("Unknown file type for File Store backup: " + fileType);
	}
	
	/**
	 * Implements the collection handler's execute method for
	 * RDF Xml file creation and LocalStore upload
	 * @throws Exception 
	 * @throws Exception 
	 * @throws Exception 
	 * @throws Exception 
	 * @throws FileExistedException 
	 * @throws IOException 
	 * @throws URISyntaxException 
	 */
	public boolean execute()throws Exception{
    	String subjectId = null;
	    String itemLink = "";
		String eMessage ="";
	    String eMessagePrefix = fileType + " upload failed with object ";

	    if(!("RDF".equalsIgnoreCase(fileType) || "METS".equalsIgnoreCase(fileType) || "JSON".equalsIgnoreCase(fileType)))
	    	throw new Exception("Unknown file type for File Store upload: " + fileType);
    	
	   		for (int i = 0; i < itemsCount && !interrupted; i++) {   				
	   			subjectId = collectionId + "-1-" + (i+1);
	   			uploadFile(subjectId, i);
		   			
	   	        try{
	   				Thread.sleep(20);
	   	        } catch (InterruptedException e) {
	   				setExeResult(false);
	   				interrupted = true;
	   				eMessage = itemLink + ". Error: " + e.getMessage();
	   				System.out.println(eMessagePrefix + eMessage);
	   				setStatus("Canceled");
	   				clearSession();
	   				log("log", eMessagePrefix + eMessage);
	   				log.info(eMessagePrefix + eMessage, e);
	   			}
	   	        counter++;		
	   	        setProgressPercentage( ((i + 1) * 100) / getFilesCount());

	   		}
    	
       return exeResult;
	}

	private boolean uploadFile(String subjectId, int idx) throws Exception{
		boolean successful = false;

		String data = "";
		String eMessage = "";
		String eMessagePrefix = fileType + " upload failed with object ";
		String itemLink = getDDOMReference(subjectId);

	  	setStatus("Processing " + fileType + " uploading for " + itemLink  + " ... " 
  			     + (idx + 1) + " of " + getFilesCount() + " in " + collectionTitle);
	  	
	  	int numTry = 1;
	  	boolean successTry = false;
		boolean uploaded = false;
		boolean updated = false;

		if((!replaceOrNot && !uploaded) || (replaceOrNot && !updated)){
			numTry = 1;
			successTry = false;
			do{
				try{
					data = damsClient.getMetadata(subjectId, fileType);
	    			successTry = true;	
				} catch (LoginException e){
					e.printStackTrace();
					if(numTry == maxTry){
						setExeResult(false);
						eMessage = subjectId + ". FileStoreAuthException: " + e.getMessage();
						String iMessagePrefix = "Upload " + fileType + " failed for ";
						message.append(eMessagePrefix + eMessage + "\n");
						setStatus(iMessagePrefix + eMessage);
						log("log", iMessagePrefix + eMessage );
						log.error(iMessagePrefix + eMessage, e);
					}
				} catch (Exception e) {
					if(numTry==maxTry){
		   				failedsCount++;
		   	        	setExeResult(false);
		   	        	eMessage = itemLink + ". Error: " + e.getMessage() + "\n";
		   	        	message.append(eMessagePrefix + eMessage + "\n");
		   				setStatus(eMessagePrefix + eMessage);
		   				System.out.println(eMessagePrefix + eMessage + "\n");
		   				log("log", eMessagePrefix + eMessage);
		   				log.error(eMessagePrefix + eMessage, e);
					}
				}
				
		  	}while(!successTry && numTry++<maxTry && !interrupted);
			
			if(data != null && data.length() > 0){
    			String arkFileName = Constants.ARK_ORG + "-" + subjectId + fileExt;
    			String[] parts = toFileParts(arkFileName);
    			boolean exists = damsClient.exists(parts[1], parts[2]);
    			if(replaceOrNot || !exists){
    				InputStream in = null;
    				try{
    					in = new ByteArrayInputStream(data.getBytes("UTF-8"));
		    			if(exists)
		    				successful = damsClient.updateFile(parts[1], parts[2], in, data.length());
		    			else
		    				successful = damsClient.createFile(parts[1], parts[2], in, data.length());
    				}finally{
    					if(in != null){
    						in.close();
    						in = null;
    					}
    				}
	    			
	    			if(successful){
		    			updatedCount++;
	    			}else{
	    				failedsCount++;
		   	        	setExeResult(false);
		   	        	eMessage = itemLink + ". Error: file " + arkFileName + " doesn't exist.\n";
		   	        	message.append(eMessagePrefix + eMessage + "\n");
		   				setStatus(eMessagePrefix + eMessage);
		   				System.out.println(eMessagePrefix + eMessage + "\n");
		   				log("log", eMessagePrefix + eMessage);
		   				log.error(eMessagePrefix + eMessage);
	    			}
	    			
    			}
			}
		}	
		return successful;	
	}
	
	public String getExeInfo() {
		String exeInfo = "";
		if(getExeResult())
			exeInfo = fileType + " files for " + collectionTitle + " uploaded to File Store successfully (Total processed: " + counter + "; Updated: " + updatedCount + ").\n" + message.toString();
		else
			exeInfo = fileType + " files File Store upload for " + collectionTitle + " failed  (Total processed: " + counter + "; Updated: " + updatedCount + "): "
			         + failedsCount + " of " + counter + " failed: \n" + message.toString();
		log("log", exeInfo);
		return exeInfo;
	}

	
	public boolean isJhoveUpload() {
		return jhoveUpload;
	}

	public void setJhoveUpload(boolean jhoveUpload) {
		this.jhoveUpload = jhoveUpload;
	}	
}
