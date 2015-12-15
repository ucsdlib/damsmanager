package edu.ucsd.library.xdre.collection;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DFile;

/**
 * 
 * FileReportHandler: report files in a collection
 * @author lsitu@ucsd.edu
 */
public class FileReportHandler extends CollectionHandler{
	private static Logger log = Logger.getLogger(FileReportHandler.class);
	
	protected int count = 0;
	protected int filesReported = 0;
	protected int masterCount = 0;
	protected int failedCount = 0;
	
	/**
	 * Constructor for JFilesReportHandler
	 * @param damsClient
	 * @throws Exception
	 */
	public FileReportHandler(DAMSClient damsClient) throws Exception{
		this(damsClient, null);
	}
	
	/**
	 * Constructor for FileReportHandler
	 * @param damsClient
	 * @param collectionId
	 * @throws Exception
	 */
	public FileReportHandler(DAMSClient damsClient, String collectionId) throws Exception{
		super(damsClient, collectionId);
	}

	/**
	 * Procedure for creating file report
	 */
	public boolean execute() throws Exception {

		String message;
		String subjectURI = null;

		log("log", this.collectionTitle);
    	log("log", "File Name\tFile URL\tFile Use");
		for(int i=0; i<itemsCount; i++){
			subjectURI = items.get(i);
			boolean updateSolr = false;
			try{
				setStatus("Processing file report for subject " + subjectURI  + " (" + (i+1) + " of " + itemsCount + ") ... " ); 
				DFile dFile = null;
	
				String use = null;
				List<DFile> files = damsClient.listObjectFiles(subjectURI);
				
				for(Iterator<DFile> it=files.iterator(); it.hasNext();){
					count++;
					dFile = it.next();
					use = dFile.getUse();
					try{
						setStatus("Processing file report for file " + dFile.getId()  + " (" + (i+1) + " of " + itemsCount + ") ... " ); 
						if (use != null && (use.toLowerCase().endsWith("-source") || use.toLowerCase().endsWith("-alternate")) || dFile.getId().indexOf("/1.") > 0) {
							masterCount++;
							log("log", dFile.getSourceFileName() + "\t" + dFile.getId() + "\t" + dFile.getUse());
						}
					} catch (Exception e) {
						failedCount++;
						e.printStackTrace();
						exeResult = false;
						message = "File report failed: " + e.getMessage();
						setStatus(message  + "(" +(i+1)+ " of " + itemsCount + ")"); 
						log.info(message );
					}
				}
			} catch (Exception e) {
				failedCount++;
				e.printStackTrace();
				exeResult = false;
				message = "File report failed: " + e.getMessage();
				setStatus(message  + "(" +(i+1)+ " of " + itemsCount + ")"); 
				log.info(message );
			}

    		
			// Updated SOLR
			if(updateSolr && !updateSOLR(subjectURI)){
				failedCount++;
			}
			
			setProgressPercentage( ((i + 1) * 100) / itemsCount);
			
			try{
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				//failedCount++;
        		exeResult = false;
    			message = "File report canceled on subject " + subjectURI  + ".";
				log.info(message, e1);
				setStatus("Canceled");
				clearSession();
				break;
			}
		}
		
		return exeResult;
	}
	
	public boolean isMasterFile(String use){
		return use != null && (use.endsWith(Constants.SOURCE) || use.endsWith(Constants.ALTERNATE));
	}
	

	/**
	 * Execution result message
	 */
	public String getExeInfo() {
		if (exeResult)
			exeReport.append("File Report for collection '" + collectionTitle.substring(0, collectionTitle.lastIndexOf(" [")) 
				+ "' is created and sent through mail. Total master files found: " + masterCount);
		else
			exeReport.append("Failed to generate File Report for collection '"
				+ collectionTitle.substring(0, collectionTitle.lastIndexOf(" [")) + "'. Total master files found: " + masterCount );
		String exeInfo = exeReport.toString();
		log("log", exeInfo);
		return exeInfo;
	}
}
