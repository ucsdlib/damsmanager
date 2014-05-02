package edu.ucsd.library.xdre.collection;

import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * 
 * FilestoreSerializationHandler: serialize rdf/xml to filestore for single item or in batch.
 * @author lsitu@ucsd.edu
 */
public class FilestoreSerializationHandler extends CollectionHandler{
	private static Logger log = Logger.getLogger(FilestoreSerializationHandler.class);

	private int count = 0;
	private int failedCount = 0;
	private int colCount = 0;
	private StringBuilder faileds = new StringBuilder();
	
	/**
	 * Constructor for FilestoreSerializationHandler
	 * @param damsClient
	 * @throws Exception
	 */
	public FilestoreSerializationHandler(DAMSClient damsClient) throws Exception{
		this(damsClient, null);
	}
	
	/**
	 * Constructor for FilestoreSerializationHandler
	 * @param damsClient
	 * @param collectionId
	 * @throws Exception
	 */
	public FilestoreSerializationHandler(DAMSClient damsClient, String collectionId) throws Exception{
		super(damsClient, collectionId);
	}

	/**
	 * Procedure to serialize objects rdf/xml to filestore
	 */
	public boolean execute() throws Exception {

		String subjectURI = null;
		String message = null;
		String filestore = damsClient.getFileStore();
		filestore = filestore==null?"filestore":filestore;
		if(collectionId != null && collectionId.length() > 0){
			// Update SOLR for the collection record
			String[] colIds = collectionId.split(",");
			for(int i=0; i< colIds.length; i++){
				String colId = colIds[i];
				if( colId != null && !colId.equalsIgnoreCase("all")){
					colCount++;
					try{
						setStatus("Adding collection record " + colId  + " for filestore serialization ... " );
						if(!damsClient.serialize2disk(colId)){
							faileds.append(colId + "; \n");
							failedCount++;
							exeResult = false;
							message = "Failed to serialized collection record " + colId + " to " + filestore + ".";
							exeReport.append(message);
							log.error(message);
							log("log", message);
						}else{
							count++;
							message = "Serialized collection record " + colId + " to " + filestore + ".";
							log.info(message);
							log("log", message);
						}
					} catch (Exception e){
						faileds.append(colId + "; \n");
						failedCount++;
						exeResult = false;
						message = "Failed to serialize collection record " + colId + " to " + filestore + ": " + e.getMessage() + ";";
						log.error(message, e);
						log("log", message);
					}
				}
			}
		}
		
		for(int i=0; i<itemsCount && !interrupted; i++){
			
			subjectURI = items.get(i);
			setStatus("Processing filestore serialization for subject " + subjectURI  + " (" + (i+1) + " of " + itemsCount + ") ... " );
			try{
				if(!damsClient.serialize2disk(subjectURI)){
					faileds.append(subjectURI + "; \n");
					failedCount++;
					exeResult = false;
					message = "Failed to serialized record " + subjectURI + " to " + filestore + ".";
					log.error(message);
					log("log", message);
				}else{
					count++;
					message = "Serialized record " + subjectURI + " to " + filestore + ".";
					log.info(message);
					log("log", message);
				}
			} catch (Exception e){
				faileds.append(subjectURI + "; \n");
				failedCount++;
				exeResult = false;
				message = "Failed to serialize record " + subjectURI + " to " + filestore + ": " + e.getMessage() + ";";
				log.error(message, e);
				log("log", message);
			}
			setProgressPercentage( ((i + 1) * 100) / (itemsCount+colCount));
			
			try{
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				failedCount++;
				interrupted = true;
    			logError("Filestore serialization canceled on subject " + subjectURI  + ". Error: " + e1.getMessage() + ". ");
				setStatus("Canceled");
				clearSession();
			}
		}
		
		return exeResult;
	}


	/**
	 * Execution result message
	 */
	public String getExeInfo() {
		if(exeResult)
			exeReport.append("Filestore serialization succeeded: \n ");
		else
			exeReport.append("Filestore serialization failed (" + failedCount + " of " + (failedCount + count) + " failed): \n " + faileds.toString());	
		exeReport.append("Number of records serialized " + count + " (Total " + (itemsCount + colCount) +  " records).");
		String exeInfo = exeReport.toString();
		log("log", exeInfo);
		return exeInfo;
	}
}
