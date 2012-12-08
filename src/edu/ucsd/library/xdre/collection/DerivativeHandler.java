package edu.ucsd.library.xdre.collection;

import javax.security.auth.login.LoginException;

import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * Class LocalStoreDerivativeHandler creats derivative LocalStore 
 * 
 * @author lsitu@ucsd.edu
 */
public class DerivativeHandler extends CollectionHandler{

	private static Logger log = Logger.getLogger(DerivativeHandler.class);
	private String[] sizes = null;
	boolean replaceOrNot = false;
	//Using frameNo -1 to diable the page/frame option
	private int frameNo = 0;
	private boolean frameDefault = true;
	
    private int failedsCount;
    private int updatedCount;
    private int skipCount;
    private int counter = 0;
    private int totalFiles = 0;
	
	public DerivativeHandler(DAMSClient damsClient, String collectionId, String[] sizes, boolean replaceOrNot) 
	        throws Exception {
		super(damsClient, collectionId);
		this.sizes = sizes;		
		this.replaceOrNot = replaceOrNot;
	}

	/**
	 * Implements the collection handler's execute method for
	 * LocalStore derivative creation
	 * @throws TripleStoreException 
	 */
	public boolean execute() throws Exception{
	    	String itemLink = null;
	    	totalFiles = itemsCount;

			for (int k = 0; k < totalFiles && !interrupted; k++) {        
		        String subjectId = null;
		        String compId = null;
		        String masterFileId = "1";
		        counter++;

	        	subjectId = (String) items.get(k);
	        	
	        	itemLink = getDDOMReference(subjectId);

	        	long complexObjectCount = 0;
	    		
	    		complexObjectCount = queryComplexObject(subjectId);
	    		
   				if(complexObjectCount >= 0){
   					totalFiles += (complexObjectCount - 1);
   					//Handle complex object
   					handleComplexObject(subjectId, (int)complexObjectCount, k);
   					//counter++;
   					continue;
   				}
		        
		        handleObject(subjectId, compId, masterFileId, k, true);
	        	//counter++;
		        setProgressPercentage( ((k + 1) * 100) / getFilesCount());
		        
	        	try{	        		
	        		Thread.sleep(10);
	        	} catch (InterruptedException e1) {
	        		interrupted = true;
	        		setExeResult(false);
	    			String eMessage = "Derivative creation canceled on " + itemLink + " ( " + (k + 1) + " of " + itemsCount + ").";
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
				if(replaceOrNot)
					message += " updated ";
				else
					message += " created ";
				message += " for " + collectionTitle + ". \n"+ counter + " objects processed: " + (replaceOrNot?"updated " + updatedCount + ", ":"") + "skit " + skipCount + ", total " + totalFiles + " master files.\n"; 
			}else{
				message = "Execution result for derivative creation " 
					+ " in " + collectionTitle + ": " + failedsCount + " of " + totalFiles + " files failed (Total " + counter + " of " + itemsCount + " objects processed:  " + (replaceOrNot?"updated " + updatedCount  + ", ":"") + "skit " + skipCount + "). \n";
			}
		log("log", message);
		return message;
	}


	public int getFrameNo() {
		return frameNo;
	}

	public void setFrameNo(int frameNo) {
		frameDefault = false;
		this.frameNo = frameNo;
	}

	private boolean handleComplexObject(String subjectId, int complexObjectCount, int itemIndex) 
			throws Exception{
		String compId = null;
		String fileId = "1";
		boolean successful = true;
		
		for(int i=0; i<complexObjectCount && !interrupted; i++){
			//Create derivatives for the PDF files in ETD only
			if(collectionId != null && collectionId.equals(Constants.COLLECTION_ETD) && i>0)
				continue;
			compId = "" + (i+1);
	    	if(!handleObject(subjectId, compId, fileId, itemIndex, false))
	    		successful = false;
		    try{
   				Thread.sleep(10);
   	        }catch (InterruptedException e2) {
   	        	interrupted = true;
   	        	successful = false;
   				setExeResult(false);
   				String eMessage = subjectId + ". Error: " + e2.getMessage();
   				String iMessagePrefix = "Derivative creation interrupted with ";
   				System.out.println(iMessagePrefix + eMessage);
   				setStatus("Canceled");
   				clearSession();
   				log("log", iMessagePrefix + eMessage);
   				log.info(iMessagePrefix + eMessage, e2);
   			}		
   			setProgressPercentage( (itemIndex * 100) / itemsCount);	 
		}
		return successful;
	}
	
	private boolean handleObject(String subjectId, String compId, String fileId, int itemIndex, boolean countUpdate) throws Exception{
        
        //Item reference from DDOM Viewer
		String masterFileName = "";
		String derivFullName = "";
	    String itemLink = getDDOMReference(subjectId);
	    String message = "Preprocessing derivative creation for " + itemLink + " (" 
			+ (itemIndex + 1) + " of " + getFilesCount() + ") ... ";
	    setStatus(message);
	    
	    String eMessage ="";
        String eMessagePrefix = "Derivative creation failed with ";
        boolean handled = true;
    	String fExt = null;
	  	int numTry = 1;
	  	boolean successTry = false;
	  	fExt = getFileExtension(subjectId, compId, fileId);

        if(fExt == null){
    		eMessage = "Error: Unable to determine file extension for subject " + subjectId + (compId!=null&&compId.length()>0?"/"+compId:"") + (fileId!=null&&fileId.length()>0?"/"+fileId:"");
        }else{
    
	        masterFileName = fileId + fExt;
	        if(collectionId != null && collectionId.equals(Constants.COLLECTION_SHOTSOFWAR)){ 
	        	//Shots of the War collection uses the master-edited file.
	        	String masterEditedFileName = "4" + fExt;
	        	String fullArkMasterEdited = Constants.ARK_ORG + "-" + subjectId + "-" + masterEditedFileName;
	        	//String[] parts = toFileParts(fullArkMasterEdited); 
	        	if(damsClient.exists(subjectId, compId, fileId)){
	        		masterFileName = masterEditedFileName;
	        		log("log", "Choosing master-edited file " + fullArkMasterEdited + " for derivative creation.");
	        	}
	        }else if(collectionId != null && collectionId.equals(Constants.COLLECTION_UNIVERSITYCOMMUNICATIONNSEWSRELEASE)){
	        	//Simple object with master 1-1.xml and access PDF 1-2.pdf in University Communications News Releases. 
	        	masterFileName = "2.pdf";
	        }else if(fExt.endsWith("avi") || fExt.endsWith("mov")){
	        	//Use derivative mp4 for thumbnail creation
	        	masterFileName = "2.mp4";
	        }
	        
        	if(frameDefault){
	        	if(fExt.toLowerCase().endsWith("jpg") || fExt.endsWith("png") || fExt.endsWith("gif"))
	        		frameNo = -1;
	        	else if((fExt.toLowerCase().endsWith("mp4") || fExt.endsWith("avi") || fExt.endsWith("mov")))
	        		//Use frame 100 to create derivatives for videos as default.
	            	frameNo = 100;
        	}
		}

        if(eMessage == null || eMessage.length() == 0){
        	message = "Generating derivative(s) for " + itemLink + " ...   " 
				+ (itemIndex + 1) + " of " + getFilesCount() + " in '" + collectionTitle + "'.";
        	setStatus(message);
			String derFileName = "";
			if(sizes.length > 0){
				for(int i=0; i<sizes.length && !interrupted; i++){
					derFileName += derFileName.length()>0?",":""+derFileName;
				}
			}
				
        	//for(int i=0; i<sizes.length && !interrupted; i++){
        		boolean successful = false;
      		  	numTry = 1;
      		  	successTry = false;
      		  	do{
	        		try {
	        			boolean generated = false;
	        			boolean updated = false;
	        	
			        	if((!replaceOrNot && !generated) || (replaceOrNot && !updated)){
			
		        			boolean deriExists = damsClient.exists(subjectId, compId, masterFileName);
		        			successful = deriExists;
		        			if(!successful || replaceOrNot){
		        				successful = false;
		        				if(deriExists){
		        					successful = damsClient.updateDerivatives(subjectId, compId, masterFileName, derFileName, ""+frameNo);
		        				}else
		        					successful = damsClient.createDerivatives(subjectId, compId, masterFileName, derFileName, ""+frameNo);
		        			}
							if(!successful){
								failedsCount += 1;
			            		setExeResult(false);
			            		message = "Failed to create derivative " + derivFullName + " for " + itemLink 
			    					+ (itemIndex + 1) + " of " + getFilesCount() + " in " +collectionTitle + ").";
			            		log("log", message);
				        		setStatus(message);
							}
								
		        		    if(replaceOrNot  && countUpdate)
					        	updatedCount++;
		        		    
							message = "Derivative " + derivFullName + " for " + itemLink + " generated (" 
		    					+ (itemIndex + 1) + " of " + getFilesCount() + " in " + collectionTitle + ").";
		        		    setStatus(message);

			        	}else{
			        		skipCount++;
			        		successful = true;
			        	}
			        	successTry = true;
	        		} catch (LoginException e){
						e.printStackTrace();
						if(numTry == maxTry){
							setExeResult(false);
							failedsCount += 1;
							eMessage = subjectId + ". FileStoreAuthException: " + e.getMessage();
							String iMessagePrefix = "Derivative creation failed with ";
							setStatus(iMessagePrefix + eMessage);
							log("log", iMessagePrefix + eMessage );
							log.error(iMessagePrefix + eMessage, e);
						}
					} catch (Exception e) {
						e.printStackTrace();
		   				if(numTry==maxTry){
		   					handled = false;
		   					failedsCount += 1;
		            		setExeResult(false);
		            		StackTraceElement[] trace = e.getStackTrace();
		                	String traceInfo = "";
		                	if(trace != null && trace.length > 0){
		                		int printLines = trace.length > 2?2:trace.length;
		                		for(int j=0; j<printLines; j++)
		                			traceInfo += "@" + trace[j] + "; ";
		                	}
		        			eMessage = "Failed to create " + derivFullName + ". Error: " + e.getMessage() + " -- " + traceInfo;
		        			setStatus(eMessagePrefix + eMessage + traceInfo);
							System.out.println(eMessagePrefix + eMessage + "\n" + traceInfo);
							log("log", eMessagePrefix + eMessage + " -- " + traceInfo);
							log.error(eMessagePrefix + eMessage, e);
		   				}
	        		}
	        	if(!successful)
	        		handled = false;
	        	
	        	try{	        		
	        		Thread.sleep(10);
	        	} catch (InterruptedException e1) {
	        		handled = false;
	        		interrupted = true;
	        		setExeResult(false);
	        		failedsCount += 1;
	    			eMessage = "Derivative creation canceled on " + itemLink + " ( " + itemIndex + " of " + itemsCount + ").";
	    			String iMessagePrefix = "Derivative creation interrupted with ";
					System.out.println(iMessagePrefix + eMessage);
					setStatus("Canceled");
					clearSession();
					log("log", iMessagePrefix + eMessage);
					log.info(iMessagePrefix + eMessage, e1);
				}
      		  }while(!successTry && numTry++<maxTry && !interrupted);
        	//}
                	
        }else{
        	handled = false;
        	setExeResult(false);
        	failedsCount += 1;
    	    setStatus(eMessagePrefix + eMessage);
			System.out.println(eMessagePrefix + eMessage + "\n");
			log("log", eMessagePrefix + eMessage);
			log.error(eMessagePrefix + eMessage);
        	try{	        		
        		Thread.sleep(10);
        	} catch (InterruptedException e1) {
        		handled = false;
        		interrupted = true;
        		setExeResult(false);
    			eMessage = "Derivative creation canceled on " + itemLink + " ( " + itemIndex + " of " + itemsCount + ").";
    			String iMessagePrefix = "Derivative creation interrupted with ";
				System.out.println(iMessagePrefix + eMessage);
				setStatus("Canceled");
				clearSession();
				log("log", iMessagePrefix + eMessage);
				log.info(iMessagePrefix + eMessage, e1);
			}
        }
		return handled;
	}
}
