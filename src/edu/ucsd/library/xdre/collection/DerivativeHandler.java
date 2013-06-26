package edu.ucsd.library.xdre.collection;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;

import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DFile;
import edu.ucsd.library.xdre.utils.DamsURI;
import edu.ucsd.library.xdre.utils.FFMPEGConverter;

/**
 * Class DerivativeHandler creates thumbnails/derivatives 
 * 
 * @author lsitu@ucsd.edu
 */
public class DerivativeHandler extends CollectionHandler{

	private static Logger log = Logger.getLogger(DerivativeHandler.class);
	private String[] sizes = null;
	private String file = null; // Source file for derivative creation
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
	 * Access the source file name that use for derivative creation
	 * @return
	 */
	public String getFile() {
		return file;
	}

	/**
	 * Setter for the source file fid | cid/fid for derivative creation
	 * @param file
	 */
	public void setFile(String file) {
		this.file = file;
	}

	/**
	 * Access the frameNo property
	 * @return
	 */
	public String getFrameNo() {
		return frameNo;
	}

	/**
	 * Frame number that designated for derivative creation
	 * @param frameNo
	 */
	public void setFrameNo(String frameNo) {
		frameDefault = false;
		this.frameNo = frameNo;
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
			boolean updateSOLR = false;
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
	        	//Check for derivative created for source and service files
				String sizs2create = "";
				String sizs2replace = "";
				String fileID = dFile.getId();
				
				// Create derivative for the designated source file or master image files (source/alternative?), service video files (mp4) etc.
	        	if((file!=null && file.length()>0 && fileID.endsWith(file)) || 
	        			(file==null || file.length()==0) && use != null && !(use.startsWith("audio") || use.startsWith("data")) && (use.endsWith(Constants.SOURCE) || use.endsWith(Constants.ALTERNATE) || (use.endsWith(Constants.SERVICE) && !use.startsWith(Constants.IMAGE) && !(reqSizes.length==1 && reqSizes[0].equals("v"))))){
		        	totalFiles += 1;
		        	DamsURI fileURI = DamsURI.toParts(fileID, dFile.getObject());
	        		String derId = null;
	        		String mfId = fileURI.getFileName();
	        		for(int j=0; j<reqSizes.length; j++){
	        			if(use.endsWith(Constants.SOURCE) && (use.startsWith("video") || mfId.equals("1.mov") || mfId.equals("1.avi")) && reqSizes[j].equals("v"))
	        				derId = fileURI.toString().replace("/"+mfId, "/"+"2.mp4");
	        			else
	        				derId = fileURI.toString().replace("/"+mfId, "/"+reqSizes[j]+".jpg");
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
	        			if(handleFile(subjectId, fileURI.getComponent(), fileURI.getFileName(), derSize, true, i))
	        				updateSOLR = true;
	        		}
	        	}
	        }
        	
			// Updated SOLR
			if(updateSOLR && !updateSOLR(subjectId))
				failedsCount++;
			
	        setProgressPercentage( ((i + 1) * 100) / itemsCount);
	        
        	try{	        		
        		Thread.sleep(10);
        	} catch (InterruptedException e1) {
        		interrupted = true;
				logError("Derivative creation canceled on " + itemLink + " ( " + (i + 1) + " of " + itemsCount + ").");
				setStatus("Canceled");
				clearSession();
				break;
			}
		}

		return exeResult;
	}

	public String getExeInfo() {
		exeReport.append("Derivatives ");
		if(exeResult && failedsCount <=0){
			if(replace)
				exeReport.append(" updated ");
			else
				exeReport.append(" created ");
			exeReport.append((collectionTitle==null?"":" for " + collectionTitle) + ": " + "created " + createdCount + ", " + (replace?"updated " + updatedCount + ", ":"") + "skit " + skipCount + ", " + totalFiles + " file" + (totalFiles>1?"s":"") + " processed for derivative creation for " + counter + " object" + (counter>1?"s":"") + ".\n"); 
		}else{
			exeReport.append("Derivative creation result" 
				+ (collectionTitle==null?"":" for " + collectionTitle) + ": " + "created " + createdCount + ", " + (replace?"updated " + updatedCount  + ", ":"") + "skit " + skipCount + ", failed " + failedsCount + " (Total " + itemsCount +  " item" + (itemsCount>1?"s":"") + " found. " + totalFiles + " file" + (totalFiles>1?"s are":"is") + " processed for derivative creation for " + counter + " object" + (counter>1?"s":"") + ".\n");
		}
		
		// Add SOLR report message
		exeReport.append(getSOLRReport());
		String message = exeReport.toString();
		log("log", message);
		return message;
	}
	
	private boolean handleFile(String oid, String cid, String fid, String[] derSizes, boolean update, int itemIndex) throws Exception{
        
        //Item reference from DDOM Viewer
		String derivFullName = "";
	    String itemLink = getDDOMReference(oid);
	    String message = "Preprocessing derivative creation for " + itemLink + " (" 
			+ (itemIndex + 1) + " of " + getFilesCount() + ") ... ";
	    setStatus(message);
	    
	    String eMessage ="";
        String eMessagePrefix = "Derivative creation failed with ";

    	message = "Generating derivative(s) for " + itemLink + " ...   " 
			+ (itemIndex + 1) + " of " + getFilesCount() + " in '" + collectionTitle + "'.";
    	setStatus(message);
		
		boolean successful = false;
		try {
	    	if(frameDefault){
	        	if(fid.toLowerCase().endsWith("jpg") || fid.endsWith("png") || fid.endsWith("gif"))
	        		frameNo = "-1";
	        	else if((fid.toLowerCase().endsWith("mp4") || fid.endsWith("avi") || fid.endsWith("mov")))
	        		//Use frame 100 to create derivatives for videos as default.
	            	frameNo = "100";
	    	}
	    	
	    	List<String> dSizes = new ArrayList<String>();//Arrays.asList(derSizes.clone());
	    	dSizes.addAll(Arrays.asList(derSizes.clone()));
	    	int vIdx = -1;
	    	if((vIdx=dSizes.indexOf("v")) >= 0){
	    		// MP4 derivative processing
	    		dSizes.remove(vIdx);
	    		derSizes = dSizes.size()>0?dSizes.toArray(new String[dSizes.size()]):new String[0];
	    		
	    		String dfid = "2.mp4";
	    		derivFullName = oid + "/" + (cid!=null?cid+"/":"") + dfid;
	    		if(!update && damsClient.exists(oid, cid, dfid)){
	    			logError("Derivative " + derivFullName + " exists.");
	    		}else{
	    			File dst = null;
	    			try{
		    			dst = createMp4Derivatives(oid, cid, fid, dfid);
		    			if(dst != null){
		    				// Upload the mp4
		    				Map<String, String> params = new HashMap<String, String>();
		    				params.put("oid", oid);
		    				params.put("cid", cid);
		    				params.put("fid", dfid);
		    				params.put("local", dst.getAbsolutePath());
		    				params.put(DFile.USE, "video-service");
		    				String fs = damsClient.getFileStore();
		    				if(fs != null)
		    					params.put("fs", fs);
		    				successful = damsClient.uploadFile(params, replace); 
		    			}else
		    				successful = false;
	    			}finally{
	    				if(dst != null && dst.exists()){
	    					// Cleanup temp files
	    					try {
	    						dst.delete();
	    					} catch ( Exception e ) {
	    						e.printStackTrace();
	    					}
	    					dst = null;
	    				}
	    			}
	    		}
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
	    	}
	    	
	    	if(derSizes.length > 0){
				// JPEG Derivative Creation/Replacement
				if(update) {
					successful = damsClient.updateDerivatives(oid, cid, fid, derSizes, frameNo, update);				
				} else
					successful = damsClient.createDerivatives(oid, cid, fid, derSizes, frameNo);
	    	
				derivFullName = oid + "/" + (cid!=null?cid+"/":"") + derSizes + ".jpg";
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
	    	}
		} catch (LoginException e){
			e.printStackTrace();
			exeResult = false;
			failedsCount += 1;
			eMessage = oid + ". FileStoreAuthException: " + e.getMessage();
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
	
	/**
	 * Function to create the mp4 derivatives
	 * @param oid
	 * @param cid
	 * @param mfid
	 * @param dfid
	 * @return
	 * @throws Exception 
	 */
	public File createMp4Derivatives(String oid, String cid, String mfid, String dfid) throws Exception{
		FFMPEGConverter converter = new FFMPEGConverter();
		if(Constants.FFMPEG_COMMAND != null && Constants.FFMPEG_COMMAND.length() > 0)
			converter.setCommand(Constants.FFMPEG_COMMAND);
		return converter.createDerivative(DAMSClient.stripID(oid), cid, mfid, dfid, Constants.VIDEO_SIZE);
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
