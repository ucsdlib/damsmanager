package edu.ucsd.library.xdre.collection;

import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DFile;
import edu.ucsd.library.xdre.utils.DamsURI;

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
	private Date checksumDate = null;
	
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
	public ChecksumsHandler(DAMSClient damsClient, String collectionId, Date checksumDate) throws Exception{
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
				DFile dFile = null;
				List<DFile> files = damsClient.listObjectFiles(subjectURI);
				for(Iterator<DFile> it=files.iterator(); it.hasNext();){
					totalFiles++;
					dFile = it.next();
					DamsURI damsURI = DamsURI.toParts(dFile.getId(), subjectURI);
					boolean suceeded = damsClient.checksum(damsURI.getObject(), damsURI.getComponent(), damsURI.getFileName());
					if(!suceeded){
						failedCount++;
						logError("Checksums validation for subject " + subjectURI  + " failed (" + (i+1) + " of " + itemsCount + "). ");
					}else{
						logMessage("Checksums validation for subject " + subjectURI  + " succeeded (" + (i+1) + " of " + itemsCount + "). ");
					}
				}
			} catch (Exception e) {
				failedCount++;
				e.printStackTrace();
				logError("Checksums validation failed (" +(i+1)+ " of " + itemsCount + "): " + e.getMessage());
			}
			setProgressPercentage( ((i + 1) * 100) / itemsCount);
			
			try{
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				failedCount++;
    			logError("Checksums validation interrupted for subject " + subjectURI  + ". Error: " + e1.getMessage());
				setStatus("Canceled");
				clearSession();
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
