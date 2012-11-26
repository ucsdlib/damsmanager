package edu.ucsd.library.xdre.collection;

import java.beans.Statement;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.security.auth.login.LoginException;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.omg.CosNaming.BindingIterator;

import edu.ucsd.library.jetl.DAMSUploadTaskHandler;
import edu.ucsd.library.jetl.LocalStoreUploadTaskHandler;
import edu.ucsd.library.jetl.jhove.KBDataObject;
import edu.ucsd.library.xdre.utils.Constants;

/**
 * Class LocalStoreJhoveReportHandler generate live report from Jhove
 * 
 * @author lsitu@ucsd.edu
 */
public class JhoveReportHandler extends CollectionHandler{
	private static Logger log = Logger.getLogger(JhoveReportHandler.class);
	private static String JHOVE_VALID_STATUS = "Well-Formed and valid";
    private StringBuilder message = null;
	private int totalFiles = 0;
	private int failedCount = 0;
	private int filesReported = 0;
	private int filesUpdated = 0;
	private StringBuilder objectsFailed = null;
	private SimpleDateFormat dateFormat = null;
	private SimpleDateFormat dfOpenStack = null;
	private boolean bytestreamOnly = false;
	private boolean updateFormat = false;
	private StringBuilder filesNotUpdated = null;
	private String originalSrcFileName = null;
	
	public JhoveReportHandler(String collectionId) throws Exception{
		this(collectionId, false);
	}
	
	public JhoveReportHandler(String collectionId, boolean bytestreamOnly) throws Exception{
		this.collectionId = collectionId;
		this.bytestreamOnly = bytestreamOnly;
		objectsFailed = new StringBuilder();
		message = new StringBuilder();
		filesNotUpdated = new StringBuilder();
		dateFormat = damsClient.getDamsDateFormat();
		dfOpenStack = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z");
		getCollectionData();
	}
	
	public JhoveReportHandler(String collectionId, HttpSession session) throws Exception {
		this(collectionId);
		this.session = session;
	}
    
   	public void getCollectionData() throws Exception {
   		if(bytestreamOnly){
	   		List<String> subjects = new ArrayList<String>();
	   		List<String> embargoedItems = null;
	   		if(excludeEmbargoed)
	   			embargoedItems = getEmbargoedItems(collectionId);
	   		List<String> colSubjects = damsClient.listObjects(collectionId);
	   		String subject = null;
	   		for(Iterator<String> it= colSubjects.iterator(); it.hasNext();){
	   			subject = (String)it.next();
	   			if(!excludeEmbargoed || (excludeEmbargoed && embargoedItems.indexOf(subject) < 0))
	   				subjects.add(subject);
	   		}
	   		items = subjects;
	   		
	   		//xxx
	   		//Retrieve complex object?
			//subjects.addAll(getComplexObjects(collectionId));
			itemsCount = items.size();
   		}else
   			super.init();
	}
   	
	public boolean isBytestreamOnly() {
		return bytestreamOnly;
	}

	public void setBytestreamOnly(boolean bytestreamOnly) {
		this.bytestreamOnly = bytestreamOnly;
	}

	public boolean isUpdateFormat() {
		return updateFormat;
	}

	public void setUpdateFormat(boolean updateFormat) {
		this.updateFormat = updateFormat;
	}

	/**
	 * Implements the collection handler's execute method for file count validation
	 * @throws Exception 
	 */
	public boolean execute() throws Exception{
    	String subjectId = null;
    	String arkFilePrefix = null;
    	String itemLink = null;
    	if(updateFormat)
    		log("log", "Format metadata for the following files are corrected in DAMS from the original bytestream format: ");
    	log("log", "ARK_ID\tFormat&Version\tReporting_Module\tSize(bytes)\tCheckSum_CRC32\tDate_Modified\tStatus\tSource_File");
    	for (int i = 0; i < itemsCount && !interrupted; i++) {
   			arkFilePrefix = Constants.ARK_ORG + "-";
			subjectId = collectionId + "-1-" + (i+1);
			arkFilePrefix += subjectId;
			totalFiles += 1;
   			if(!handleObject(subjectId, arkFilePrefix, i))
   				setExeResult(false);
   	        setProgressPercentage( ((i+1) * 100) / itemsCount);
   		}
		return exeResult;
	}

	public String getExeInfo() {
		String report = "";
		String iMessage = "Number of objects found: " + itemsCount + "; Total files processed: " + totalFiles + "; Total Files reported: " + filesReported + (updateFormat?"; Total Files updated: "+filesUpdated:"");
        String mHeader = "Jhove report " + ((collectionId!=null&&collectionId.length()==10)?"for "+ collectionTitle:"");
		if(getExeResult())
			report = mHeader + " succeeded: \n" + iMessage + ".\n" + message.toString();
		else{
			report = mHeader + " failed. \n" + iMessage + ".\nNumber of files failed " + failedCount + ": \n" + objectsFailed.toString() + "\nErrors: " + message.toString() + "\n";
		}
		
		if(updateFormat && filesNotUpdated.length()>0){
			log("log", "\n*************************************************************************************************************************************");
			log("log", "\nThe following " + (filesReported-filesUpdated)+ " BYSTREAM format files haven't being fixed by XDRE Manager: \n");
			log("log", "\n*************************************************************************************************************************************");
			log("log", filesNotUpdated.toString());
		}
		log("log", report);
		return report;
	}
	
	private boolean handleComplexObject(String complexObjectId, int complexObjectCount, int itemIndex) throws Exception{
		boolean successful = true;
		for(int i=0; i<complexObjectCount && !interrupted; i++){
			String arkFilePrefix = Constants.ARK_ORG + "-";
   			String subjectId = complexObjectId + "-1-" + (i+1);
   			arkFilePrefix += subjectId;
			if(!handleObject(subjectId, arkFilePrefix, itemIndex))
				successful = false;
   			totalFiles += 1;
		}
		return successful;
	}
	
	private boolean handleObject(String subjectId, String arkFilePrefix, int itemCount) throws Exception {
        //Item reference from DDOM Viewer
	    String itemLink = getDDOMReference(subjectId);
	    setStatus("Processing Jhove report for " + itemLink + " (" + (itemCount+1) + " of " + itemsCount + ")");
	    String eMessage = "";
	    String arkFileName = null;
    	boolean successful = true;
        
		try{
			Thread.sleep(10);
		} catch (InterruptedException e) {
			interrupted = true;
			successful = false;
			setExeResult(false);
        	failedCount++;
        	objectsFailed.append(arkFileName + (failedCount%50==0?"\n":","));
			eMessage = arkFileName + ". Error: " + e.getMessage();
			String iMessagePrefix = "Jhove report interrupted with ";
			message.append(iMessagePrefix + eMessage + "\n");
			setStatus("Canceled");
			clearSession();
			jhoveErrorReport( iMessagePrefix + eMessage );
			//log("log", iMessagePrefix + eMessage );
			log.info(iMessagePrefix + eMessage, e);				
		}
		
		
		//xxx
		//Retrieve bytestream objects
        /*if(bytestreamOnly){
        	String format = tsUtils.getLiteralValue(subjectId, TripleStoreConstants.PRE_FORMATNAME_ARK);
        	if(format == null || !format.equalsIgnoreCase("bytestream"))
        		return true;
        }*/
        
        filesReported++;
        
		String fExt = null;
		int numTry = 0;
		boolean reTry = true;
		while(reTry && numTry++ < maxTry && !interrupted){
				fExt = getFileExtension(subjectId);
				reTry = false;
		}
        if(fExt == null){	        	
        	//The moving_image collection
        	if(collectionId.equals("bb3550355p")){
        		fExt = ".avi";
        	} if(collectionId.equals("bb3209056n")){ //bb3209056n Shots of War
        		fExt = ".tif";
        	}else{
        		successful = false;
    			setExeResult(false);
	        	failedCount++;
	        	objectsFailed.append(subjectId + (failedCount%50==0?"\n":"\t"));
    			eMessage = ": Unable to determine the file extension.";
   				String iMessagePrefix = "Jhove report failed with " + itemLink;
   				setStatus(iMessagePrefix + eMessage  + "\n");
   				message.append(iMessagePrefix + eMessage + "\n");
   				jhoveErrorReport(subjectId + "\t \t \t \t \t \tError" + eMessage + "\t ");
				//log("log", subjectId + "\t \t \t \t \t \tError" + eMessage + "\t ");
   				log.info(iMessagePrefix + eMessage + "\n");
    			return false;
        	}
        }
        
        arkFileName = arkFilePrefix + fExt;
        String[] parts = toFileParts(arkFileName);
		numTry = 1;
		do{
   			try{
				if (!damsClient.exists(parts[1], parts[2])){
					successful = false;
		        	setExeResult(false);
		        	failedCount++;
		        	objectsFailed.append(arkFileName + (failedCount%50==0?"\n":"\t"));
					String iMessagePrefix = "Error: FileStore master file missing " + itemLink;
		        	message.append(iMessagePrefix + "\n");
					setStatus(iMessagePrefix + "\n");
					jhoveErrorReport(arkFileName + "\t \t \t \t \t \tError: FileStore master file missing." + "\t" + (originalSrcFileName==null?" ":originalSrcFileName));
					//log("log", arkFileName + "\t \t \t \t \t \tError: FileStore master file missing." + "\t" + (originalSrcFileName==null?" ":originalSrcFileName));
		        	log.info(" FileStoreMissing " + arkFileName);
		        	return false;
		        }
	        	reTry = false;
   			} catch (LoginException e){
				if(numTry == maxTry){
					successful = false;
					setExeResult(false);
		        	failedCount++;
		        	objectsFailed.append(arkFileName + (failedCount%50==0?"\n":"\t"));
					eMessage = arkFileName + ". FileStoreAuthException: " + e.getMessage();
					String iMessagePrefix = "Jhove report failed with ";
					setStatus(iMessagePrefix + eMessage);
					message.append(iMessagePrefix + eMessage + "\n");
					jhoveErrorReport(arkFileName + "\t \t \t \t \t \tError: " + e.getMessage() + ".\t" + (originalSrcFileName==null?" ":originalSrcFileName));
					//log("log", arkFileName + "\t \t \t \t \t \tError: " + e.getMessage() + ".\t" + (originalSrcFileName==null?" ":originalSrcFileName));
					log.error(iMessagePrefix + eMessage, e);
				}
			}
			
		} while (numTry++ < maxTry && !interrupted);
		
		//xxx
		//Retrieve Jhove 
		/*if(updateFormat){
			if(jhoveStatus.equalsIgnoreCase(JHOVE_VALID_STATUS) && !formatName.equalsIgnoreCase("bytestream")){
				filesUpdated++;
				updateJhove(subjectId, kobj);
				log("log", arkFileName + "\t" + kobj.getFormat() + " " + kobj.getVersion() + "\t" + kobj.getReportingModule() + "\t" + kobj.getSize() + "\t" + kobj.getCheckSum_CRC32() + "\t" + dateFormat.format(kobj.getDateModified()) + "\t" + kobj.getStatus() + "\t" + (originalSrcFileName==null?" ":originalSrcFileName));
			}else
				filesNotUpdated.append(arkFileName + "\t" + kobj.getFormat() + " " + kobj.getVersion() + "\t" + kobj.getReportingModule() + "\t" + kobj.getSize() + "\t" + kobj.getCheckSum_CRC32() + "\t" + dateFormat.format(kobj.getDateModified()) + "\t" + kobj.getStatus() + "\t" + (originalSrcFileName==null?" ":originalSrcFileName) + "\n");
		}else
			log("log", arkFileName + "\t" + kobj.getFormat() + " " + kobj.getVersion() + "\t" + kobj.getReportingModule() + "\t" + kobj.getSize() + "\t" + kobj.getCheckSum_CRC32() + "\t" + dateFormat.format(kobj.getDateModified()) + "\t" + kobj.getStatus() + "\t" + (originalSrcFileName==null?" ":originalSrcFileName));
			*/
		return successful;
	}
	
	private void jhoveErrorReport(String errorMessage){
		if(updateFormat)
			filesNotUpdated.append(errorMessage);
		else
			log("log", errorMessage);
	}
	
	private void updateJhove(String subjectId, KBDataObject kobj) throws Exception{
		//xxx
		//Update Jhove
	}
}
