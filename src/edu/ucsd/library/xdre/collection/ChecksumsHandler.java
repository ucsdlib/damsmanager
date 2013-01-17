package edu.ucsd.library.xdre.collection;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DFile;
import edu.ucsd.library.xdre.utils.FileURI;

/**
 * 
 * ChecksumsHandler: perform checksum validation.
 * @author lsitu@ucsd.edu
 */
public class ChecksumsHandler extends CollectionHandler{
	private static Logger log = Logger.getLogger(ChecksumsHandler.class);

	private int count = 0;
	private int failedCount = 0;
	private int totalFiles = 0;
	private String checksumDate = null;
	
	/**
	 * Constructor for ChecksumsHandler
	 * @param damsClient
	 * @throws Exception
	 */
	public ChecksumsHandler(DAMSClient damsClient) throws Exception{
		this(damsClient, null);
	}
	
	/**
	 * Constructor for ChecksumsHandler
	 * @param damsClient
	 * @param collectionId
	 * @throws Exception
	 */
	public ChecksumsHandler(DAMSClient damsClient, String collectionId) throws Exception{
		this(damsClient, collectionId, null);
	}
	
	/**
	 * Constructor for ChecksumsHandler
	 * @param damsClient
	 * @param collectionId
	 * @param chechsumDate
	 * @throws Exception
	 */
	public ChecksumsHandler(DAMSClient damsClient, String collectionId, String chechsumDate) throws Exception{
		super(damsClient, collectionId);
		this.checksumDate = checksumDate;
	}

	/**
	 * Procedure for checksums validation
	 */
	public boolean execute() throws Exception {

		String eMessage;
		String subjectURI = null;
		
		for(int i=0; i<itemsCount; i++){
			count++;
			subjectURI = items.get(i);
			try{
				setStatus("Processing checksums validation for subject " + subjectURI  + " (" + (i+1) + " of " + itemsCount + ") ... " ); 
				String message = "";
				DFile dFile = null;
				List<DFile> files = damsClient.listObjectFiles(subjectURI);
				for(Iterator<DFile> it=files.iterator(); it.hasNext();){
					totalFiles++;
					dFile = it.next();
					FileURI fileURI = FileURI.toParts(dFile.getId(), subjectURI);
					boolean suceeded = damsClient.checksum(fileURI.getObject(), fileURI.getFileName(), fileURI.getFileName());
					if(!suceeded){
						failedCount++;
						exeResult = false;
						String iMessage = "Checksums validation for subject " + subjectURI  + " failed ";
						iMessage += "(" + (i+1) + " of " + itemsCount + "): ";
						setStatus( iMessage + message.replace("\n", "<br/>")); 
						log("log", iMessage + message);
						log.info(iMessage + message);
					}else{
						String iMessage = "Checksums validation for subject " + subjectURI  + " succeeded (" + (i+1) + " of " + itemsCount + "): ";
						setStatus( iMessage + message.replace("\n", "<br/>")); 
						log("log", iMessage + message);
						log.info(iMessage + message);
					}
				}
			} catch (Exception e) {
				failedCount++;
				e.printStackTrace();
				exeResult = false;
				eMessage = "Checksums validation failed: " + e.getMessage();
				setStatus( eMessage  + "(" +(i+1)+ " of " + itemsCount + ")"); 
				log("log", eMessage );
				log.info(eMessage );
			}
			setProgressPercentage( ((i + 1) * 100) / itemsCount);
			
			try{
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				failedCount++;
        		exeResult = false;
    			eMessage = "Checksums validation interrupted for subject " + subjectURI  + ". \n Error: " + e1.getMessage() + "\n";
				setStatus("Canceled");
				clearSession();
				log("log", eMessage.replace("\n", ""));
				log.info(eMessage, e1);
				break;
			}
		}
		
		return exeResult;
	}


	/**
	 * Execution result message
	 */
	public String getExeInfo() {
		if(exeResult)
			exeReport.append("Checksums validation succeeded. \n ");
		else
			exeReport.append("Checksums validation failed (" + failedCount + " of " + totalFiles + " failed): \n ");	
		exeReport.append("Total items found " + itemsCount + ". Number of items processed " + count + ". Number of files exists " + totalFiles + ".\n");
		String exeInfo = exeReport.toString();
		log("log", exeInfo);
		return exeInfo;
	}
}
