package edu.ucsd.library.xdre.collection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.security.auth.login.LoginException;

import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DFile;
import edu.ucsd.library.xdre.utils.FileURI;

/**
 * Class DerivativeHandler creates thumbnails/derivatives 
 * 
 * @author lsitu@ucsd.edu
 */
public class DerivativeHandler extends CollectionHandler{

	private static Logger log = Logger.getLogger(DerivativeHandler.class);
	private String[] sizes = null;
	boolean replace = false;
	//Using frameNo -1 to diable the page/frame option
	private String frameNo = null;
	private boolean frameDefault = true;
	
    private int failedsCount;
    private int updatedCount;
    private int skipCount;
    private int createdCount;
    private int counter = 0;
    private int totalFiles = 0;
	
	public DerivativeHandler(DAMSClient damsClient, String collectionId, String[] sizes, boolean replace) 
	        throws Exception {
		super(damsClient, collectionId);
		this.sizes = sizes;		
		this.replace = replace;
	}

	/**
	 * Implements the collection handler's execute method for
	 * LocalStore derivative creation
	 * @throws TripleStoreException 
	 */
	public boolean execute() throws Exception{
	    	String itemLink = null;
	    	totalFiles = 0;

			for (int i = 0; i < itemsCount && !interrupted; i++) {        
		        String subjectId = null;
		        counter++;

	        	subjectId = (String) items.get(i);
	        	setStatus("Processing derivation for subject " + subjectId  + " (" + (i+1) + " of " + itemsCount + ") ... " ); 
	        	itemLink = getDDOMReference(subjectId);
		        List<DFile> dFiles = damsClient.listObjectFiles(subjectId);
		        DFile dFile = null;

		        String[] reqSizes = Constants.DEFAULT_DERIVATIVES.split(",");
		        if(sizes != null)
		        	reqSizes = sizes;  	
		        for(Iterator<DFile> it=dFiles.iterator(); it.hasNext();){
		        	dFile = it.next();
		        	String use = dFile.getUse();
		        	//Check for derivative created for service files
    				String sizs2create = "";
    				String sizs2replace = "";
		        	if(use != null && use.endsWith("-service")){
			        	totalFiles += 1;
			        	FileURI fileURI = FileURI.toParts(dFile.getId(), dFile.getObject());
		        		String derId = null;
		        		for(int j=0; j<reqSizes.length; j++){
		        			derId = fileURI.toString().replace("/"+fileURI.getFileName(), "/"+reqSizes[j]+".jpg");
		        			if(!isFileExists(derId, dFiles)){
		        				sizs2create += reqSizes[j] + ",";
		        			}else{
		        				sizs2replace += reqSizes[j] + ",";
		        			}
		        		}
		        		
		        		int len = sizs2create.length();
		        		sizs2create = len>0?sizs2create.substring(0, len-1):"";	        		
		           		len = sizs2replace.length();
		        		sizs2replace = len>0?sizs2replace.substring(0, len-1):"";
		        		
		        		String[] derSize = null;
		        		// Perform create with POST to DAMS
		        		if( sizs2create.length() > 0 ){
		        			derSize = sizs2create.split(",");
		        			if(reqSizes.length == derSize.length)
		        				derSize = sizes;
		        			handleFile(subjectId, fileURI.getComponent(), fileURI.getFileName(), derSize, false, i);
		        		}else if (!replace)
		        			skipCount++;
		        		
		        		// Replace option. Perform replace with PUT to DAMS for derivatives that were created
		        		if (replace && sizs2replace.length() > 0){
		        			derSize = sizs2replace.split(",");
		        			if(reqSizes.length == sizs2replace.split(",").length)
		        				derSize = sizes;
		        			handleFile(subjectId, fileURI.getComponent(), fileURI.getFileName(), derSize, true, i);
		        		}
		        	}
		        }
	        	//counter++;
		        setProgressPercentage( ((i + 1) * 100) / itemsCount);
		        
	        	try{	        		
	        		Thread.sleep(10);
	        	} catch (InterruptedException e1) {
	        		interrupted = true;
	        		setExeResult(false);
	    			String eMessage = "Derivative creation canceled on " + itemLink + " ( " + (i + 1) + " of " + itemsCount + ").";
	    			String iMessagePrefix = "Derivative creation interrupted with ";
					System.out.println(iMessagePrefix + eMessage);
					setStatus("Canceled");
					clearSession();
					log("log", iMessagePrefix + eMessage);
					log.info(iMessagePrefix + eMessage, e1);
				}
			}

		return exeResult;
	}

	public String getExeInfo() {
		String message = "";
			if(exeResult && failedsCount <=0){
				if(replace)
					message += " updated ";
				else
					message += " created ";
				message += " for " + collectionTitle + ": " + "created " + createdCount + ", " + (replace?"updated " + updatedCount + ", ":"") + "skit " + skipCount + ", total " + totalFiles + " master files in " + itemsCount + " objects.\n"; 
			}else{
				message = "Execution result for derivative creation " 
					+ " in " + collectionTitle + ": " + "created " + createdCount + ", " + (replace?"updated " + updatedCount  + ", ":"") + "skit " + skipCount + ", failed " + failedsCount + " (Total " + counter + " of " + itemsCount + " objects processed). \n";
			}
		log("log", message);
		return message;
	}


	public String getFrameNo() {
		return frameNo;
	}

	public void setFrameNo(String frameNo) {
		frameDefault = false;
		this.frameNo = frameNo;
	}
	
	private boolean handleFile(String subjectId, String compId, String fileName, String[] derSizes, boolean update, int itemIndex) throws Exception{
        
        //Item reference from DDOM Viewer
		String derivFullName = "";
	    String itemLink = getDDOMReference(subjectId);
	    String message = "Preprocessing derivative creation for " + itemLink + " (" 
			+ (itemIndex + 1) + " of " + getFilesCount() + ") ... ";
	    setStatus(message);
	    
	    String eMessage ="";
        String eMessagePrefix = "Derivative creation failed with ";
        
    	if(frameDefault){
        	if(fileName.toLowerCase().endsWith("jpg") || fileName.endsWith("png") || fileName.endsWith("gif"))
        		frameNo = "-1";
        	else if((fileName.toLowerCase().endsWith("mp4") || fileName.endsWith("avi") || fileName.endsWith("mov")))
        		//Use frame 100 to create derivatives for videos as default.
            	frameNo = "100";
    	}

    	message = "Generating derivative(s) for " + itemLink + " ...   " 
			+ (itemIndex + 1) + " of " + getFilesCount() + " in '" + collectionTitle + "'.";
    	setStatus(message);
		
		boolean successful = false;
		try {
			// Derivative Replacement implementation ???
			if(update) {
				successful = damsClient.updateDerivatives(subjectId, compId, fileName, derSizes, frameNo, update);				
			} else
				successful = damsClient.createDerivatives(subjectId, compId, fileName, derSizes, frameNo);
			if(!successful){
				failedsCount += 1;
        		setExeResult(false);
        		message = "Failed to create derivative " + derivFullName + " for " + itemLink 
					+ (itemIndex + 1) + " of " + getFilesCount() + " in " +collectionTitle + ").";
        		log("log", message);
        		setStatus(message);
			} else {
				if(update)
					updatedCount++;
				else
					createdCount++;
				message = "Derivative " + derivFullName + " for " + itemLink + " generated (" 
					+ (itemIndex + 1) + " of " + getFilesCount() + " in " + collectionTitle + ").";
			    setStatus(message);
			}

		} catch (LoginException e){
			e.printStackTrace();
			exeResult = false;
			failedsCount += 1;
			eMessage = subjectId + ". FileStoreAuthException: " + e.getMessage();
			String iMessagePrefix = "Derivative creation failed with ";
			setStatus(iMessagePrefix + eMessage);
			log("log", iMessagePrefix + eMessage );
			log.error(iMessagePrefix + eMessage, e);
		} catch (Exception e) {
			e.printStackTrace();
			failedsCount += 1;
    		exeResult = false;
			eMessage = "Failed to create " + derivFullName + ". Error: " + e.getMessage();
			setStatus(eMessagePrefix + eMessage);
			System.out.println(eMessagePrefix + eMessage + "\n");
			log("log", eMessagePrefix + eMessage);
			log.error(eMessagePrefix + eMessage, e);
		}
    	
    	try{	        		
    		Thread.sleep(10);
    	} catch (InterruptedException e1) {
    		interrupted = true;
    		exeResult = false;
    		failedsCount += 1;
			eMessage = "Derivative creation canceled on " + itemLink + " ( " + itemIndex + " of " + itemsCount + ").";
			String iMessagePrefix = "Derivative creation interrupted with ";
			System.out.println(iMessagePrefix + eMessage);
			setStatus("Canceled");
			clearSession();
			log("log", iMessagePrefix + eMessage);
			log.info(iMessagePrefix + eMessage, e1);
		}
                	
		return successful;
	}
	
	public boolean isFileExists(String fileId, List<DFile> dFiles){
		DFile dFile = null;
		for(Iterator<DFile> it=dFiles.iterator(); it.hasNext();){
			dFile = it.next();
			if(dFile.getId().equals(fileId)){
				return true;
			}
		}
		return false;
	}
}
