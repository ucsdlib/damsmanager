package edu.ucsd.library.xdre.collection;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;


/**
 * Class ChecksumHandler handles checksum operation
 * for master files in Local File Store
 * 
 * @author lsitu@ucsd.edu
 */
public class ChecksumHandler extends CollectionHandler{

	private static Logger log = Logger.getLogger(ChecksumHandler.class);
	
    private Date dateRechecked = null;
    private int counter = 0;
    private int failedsCount = 0;
    private int totalFiles = 0;

	public ChecksumHandler(DAMSClient damsClient, String collectionId, Date date) throws Exception {
		super(damsClient, collectionId);
		this.dateRechecked = date;
		totalFiles = items.size();	
	}
	
	public synchronized void increaseCounter(){
		counter++;
	}
	
	public String getExeInfo() {
		String exeInfo = "";
		if(getExeResult())
			exeInfo = "All checksums for " + collectionTitle + " are valid. \n - Total objects processed: " + counter + " (" + totalFiles + " master files)\n" + exeReport.toString();
		else
			exeInfo = "Execution result for " + collectionTitle + ": " + failedsCount + " of " + counter + " failed. \n" + exeReport.toString();
		log("log", exeInfo);
		return exeInfo;
	}
	
	public String convertCrcChecksum(String originalChecksum){
		// convert original checksum from hexadecimal to decimal
		long longChecksum = -1;
		try{
		   longChecksum =  Long.parseLong(originalChecksum, 16);
		}catch (NumberFormatException e){
			return originalChecksum;
		}
		return new Long(longChecksum).toString();
	}
	
	private boolean handleComplexObject(String complexObjectId, int complexObjectCount, int itemIndex) throws Exception{
		String subjectId = null;
    	String arkFilePrefix = null;
    	boolean successful = true;
   		for (int i = 0; i < complexObjectCount && !interrupted; i++) {
   			arkFilePrefix = Constants.ARK_ORG + "-";
   			subjectId = complexObjectId + "-1-" + (i+1);
   			arkFilePrefix += subjectId;
   		    //Item reference from DDOM Viewer
   		    String itemLink = getDDOMReference(subjectId);     
   		    setStatus("Processing checksum for " + itemLink  + " ... " 
	   			     + (itemIndex + 1) + " of " + itemsCount + " in " + collectionTitle);

			if(!handleObject(subjectId, arkFilePrefix, itemIndex))
				successful = false; 			
   		}
   		
   		return successful;
	}
	
	private boolean handleObject(String subjectId, String arkFilePrefix, int itemIdex) throws Exception{
    	String fExt = null;
    	String arkFileName = null;
    	boolean successful = false;
	   
		String eMessage ="";
	    String eMessagePrefix = "<br/>Checksum failed with object ";
	    
	    //Item reference from DDOM Viewer
	    String itemLink = getDDOMReference(subjectId);     
	    
	    int numTry = 1;
	    boolean successTry = false;
	    fExt = getFileExtension(subjectId); 
	    
        if(fExt == null){
			eMessage = "Error: Unable to determine file extension for subject " + subjectId;
        }
		        
        if(eMessage == null || eMessage.length() == 0){
			        	
	        boolean checksumPassed = false;
	        String dateChecked = null;
	        Date lastCheckedDate = null;
	        
	        arkFileName = arkFilePrefix + fExt;
        	numTry = 1;
   			successTry = false;
   			
   			//XXX
   			dateChecked = damsClient.getMetadata(subjectId, null); 
	       			
   			SimpleDateFormat dateFormat = damsClient.getDamsDateFormat();
   			if(dateChecked != null){
   				dateChecked = (dateChecked.startsWith("b") && dateChecked.indexOf(' ') == 10)?dateChecked.substring(10):dateChecked;
   				try {
   					lastCheckedDate = (Date) dateFormat.parse(dateChecked);
   				} catch (ParseException e) {
   					try {
	   					lastCheckedDate = (Date) damsClient.getDamsDateFormatAlt().parse(dateChecked);
	   				} catch (ParseException ee) {
	   					String err = "Force checksum for " + itemLink + ". Invalid date format(" + dateChecked + ").";
	   					eMessage = err ;
	   					System.out.println(err);
	   					log("log", eMessage);
	   					log.error(eMessage, ee);
	   					lastCheckedDate = null;
	   				}
   				}
   			}
		   	        
			if(lastCheckedDate == null || lastCheckedDate.before(dateRechecked)){
				numTry = 1;
				successTry = false;
		   		do{
		   			try{
						//Invoke CRC32 checksum
		   				String[] parts = toFileParts(arkFileName);
		   				
		   				
		   				//XXX
		   				//CRC32 checksum in triplrstore
						String checksumValueCrc32 = ""; 
	
						if(checksumValueCrc32 != null && checksumValueCrc32.length() > 0){
	   	        			String newChecksumCrc32 = damsClient.checksum(parts[1], parts[2]); 
	        				checksumPassed = checksumValueCrc32.equals(newChecksumCrc32);
	   	        			
		   	        		if(!checksumPassed)
		   	        			exeReport.append("Checksum values do not match - Current checksum: " + newChecksumCrc32 + ", Original checksum: " + checksumValueCrc32 + "\n");	   	        		
						}else
							exeReport.append("CRC32 checksum values isn't available. \n");
		   	        	successTry = true;
				    } catch (Exception e){
						e.printStackTrace();
						if(numTry==maxTry){
							setExeResult(false);
							eMessage = itemLink + ". Error: " + e.getMessage();
							String iMessagePrefix = "Checksum validation failed with ";
							System.out.println(iMessagePrefix + eMessage);
							setStatus(iMessagePrefix + eMessage + "<br/>");
							log("log", iMessagePrefix + eMessage);
							log.info(iMessagePrefix + eMessage, e);
						}
			   		}
				    if(!successTry && numTry < maxTry){
						try{
							Thread.sleep(10);
					    }catch (InterruptedException e2) {
					   		interrupted = true;
					   		successful = false;
							setExeResult(false);
							eMessage = arkFileName + ". Error: " + e2.getMessage();
							String iMessagePrefix = "Checksum interrupted with ";
							System.out.println(iMessagePrefix + eMessage);
							setStatus("Canceled");
							clearSession();
							log("log", iMessagePrefix + eMessage);
							log.info(iMessagePrefix + eMessage, e2);
						}
				    }
			    }while(!successTry && numTry++ < maxTry && !interrupted);
	
   		       	if(!checksumPassed){
   		       		failedsCount++;
   		       		setExeResult(false);
   		       		eMessage = "Checksum for local store file " + itemLink + " is not valid."; 
   		       		exeReport.append(eMessage + "\n");
   		       		setStatus(eMessage);
   		       		log("log", eMessage);
   		       	}else{
   		       		successful = true;
   		       	}
   			}else
   				successful = true;
        }else{
        	setExeResult(false);
        	failedsCount++;
        	setStatus(eMessagePrefix + eMessage);
			log("log", eMessagePrefix + eMessage);
			log.error(eMessagePrefix + eMessage);
        }
		try{
			Thread.sleep(10);
	    }catch (InterruptedException e2) {
	   		interrupted = true;
	   		successful = false;
			setExeResult(false);
			eMessage = arkFileName + ". Error: " + e2.getMessage();
			String iMessagePrefix = "Checksum interrupted with ";
			System.out.println(iMessagePrefix + eMessage);
			setStatus("Canceled");
			clearSession();
			log("log", iMessagePrefix + eMessage);
			log.info(iMessagePrefix + eMessage, e2);
		}
	    return successful;
	}
	
	/*
	 * Task implementation for checksuming.
	 */
	public void invokeTask(String subjectId, int idx) throws Exception{
		String arkFilePrefix = Constants.ARK_ORG + "-";
		String itemLink = getDDOMReference(subjectId);     
		setStatus("Processing checksum for " + itemLink  + " ... " 
		   		  + (idx + 1) + " of " + getFilesCount() + " in " + collectionTitle);
		setProgressPercentage( (idx * 100) / itemsCount);
		
		arkFilePrefix += subjectId + "-1-1";
		long complexObjectCount = queryComplexObject(subjectId);
		if(complexObjectCount >= 0){
			totalFiles += (complexObjectCount - 1);
			//Handle complex object
			handleComplexObject(subjectId, (int)complexObjectCount, idx);
		}else
			handleObject(subjectId, arkFilePrefix, idx);			
		increaseCounter();
	}	
}
