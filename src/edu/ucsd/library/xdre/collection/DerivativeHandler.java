package edu.ucsd.library.xdre.collection;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.security.auth.login.LoginException;

import org.apache.commons.lang3.StringUtils;
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
	@Override
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
				String fileID = dFile.getId();

	        	DamsURI fileURI = DamsURI.toParts(fileID, dFile.getObject());

				// Create derivative for the designated source file or master image files (source/alternative?), service video files (mp4) etc.
	        	if( isDerivativesRequired(fileURI.getFileName(), use) ){
		        	totalFiles += 1;
					
	    	    	List<String> sizesList = new ArrayList<String>();
	    	    	if(reqSizes != null)
	    	    		sizesList.addAll(Arrays.asList(reqSizes));	        		

        			String oid = fileURI.getObject();
        			String cid = fileURI.getComponent();
        			String fid = fileURI.getFileName(); // source file id
        			String dfid = null; // derivative file id
	        		List<String> sizes2create = new ArrayList<>();
	        		List<String> sizes2replace = new ArrayList<>();

	        		// handle video and audio derivatives creation
	        		if (sizesList.indexOf("v") >= 0) {

	        			sizesList.remove(sizesList.indexOf("v"));
	        			boolean isVideo = isVideo(fileID, use);
		        		if ( isVideo || isAudio(fileID, use) ) {
		        			if ( isVideo ) {
		        				dfid = "2.mp4";
		        			} else {
		        				dfid = "2.mp3";
		        			}
	
		        			if (!damsClient.exists(oid, cid, dfid)) {
		        				sizes2create.add(dfid);
		        			} else {
			        			sizes2replace.add(dfid);
		        			}
		        		} else {
		        			exeResult = false;
		        			logError("Unknown source " + fileID + " for audio/video derivative creation.");
		        		}
	        		}

	        		// handle jpeg thumbnails/derivatives creation
	        		if (sizesList.size() > 0 && !isAudio(fileID, use)) {
	        			
		        		for(int j=0; j<sizesList.size(); j++) {
		        			String derName = sizesList.get(j);
		        			dfid = derName + ".jpg";

		        			if (isVideo(fileID, use)) {
		        				// use the .mp4 derivative for jpeg thumbnail creation if one exists 
		        				if (damsClient.exists(oid, cid, "2.mp4"))
		        					fid = "2.mp4";
		        			}

		        			if(!damsClient.exists(oid, cid, dfid)){
		        				sizes2create.add(derName);
		        			}else{
		        				sizes2replace.add(derName);
		        			}
		        		}
		        	}

	        		// Replace option. Perform replace with PUT to DAMS for derivatives that were created
	        		if ( sizes2create.size() > 0 ) {
	        			if(handleFile(oid, cid, fid, use, sizes2create, false, i))
	        				updateSOLR = true;
	        		} else if (!replace)
	        			skipCount++;
	        		
	        		// Replace option. Perform replace with PUT to DAMS for derivatives that were created
	        		if (replace && sizes2replace.size() > 0){
	        			if(handleFile(oid, cid, fid, use, sizes2replace, true, i))
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

	@Override
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
	
	private boolean handleFile(String oid, String cid, String fid, String use, List<String> derSizes, boolean update, int itemIndex) throws Exception{
        
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

	    	if(derSizes != null && derSizes.size() > 0){
				// JPEG Derivative Creation/Replacement
				if(update) {
					successful = damsClient.updateDerivatives(oid, cid, fid, derSizes.toArray(new String[derSizes.size()]), frameNo, update);				
				} else
					successful = damsClient.createDerivatives(oid, cid, fid, derSizes.toArray(new String[derSizes.size()]), frameNo);
		    	
				if (isAudio(fid, use)) {
					// add embedded metadata for mp3 derivatives
					String fileUrl = oid + (StringUtils.isNotBlank(cid) ? "/" + cid : "") + "/2.mp3";
					if(damsClient.ffmpegEmbedMetadata(oid, cid, "2.mp3", "audio-service")) {
						logMessage( "Embedded metadata for audio " + fileUrl + " (" + damsClient.getRequestURL() + ").");
					} else {
						successful = false;
						message = "Derivative creation (embed metadata) - failed - " + damsDateFormat.format(new Date());
						log("log", message);
						setStatus(message);
					}
				}
				
				derivFullName = oid + "/" + (cid!=null?cid+"/":"") + derSizes;
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
	 * Function to create the mp3 derivatives
	 * @param oid
	 * @param cid
	 * @param mfid
	 * @param dfid
	 * @return
	 * @throws Exception 
	 */
	public File createMp3Derivatives(String oid, String cid, String mfid, String dfid) throws Exception{
		FFMPEGConverter converter = new FFMPEGConverter();
		if(Constants.FFMPEG_COMMAND != null && Constants.FFMPEG_COMMAND.length() > 0)
			converter.setCommand(Constants.FFMPEG_COMMAND);
		return converter.createDerivative(DAMSClient.stripID(oid), cid, mfid, dfid, Constants.FFMPEG_AUDIO_PARAMS);
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
		return converter.createDerivative(DAMSClient.stripID(oid), cid, mfid, dfid, Constants.FFMPEG_VIDEO_PARAMS);
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
