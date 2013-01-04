package edu.ucsd.library.xdre.collection;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * 
 * SOLRIndexHandler: single item or batch solr update.
 * @author lsitu@ucsd.edu
 */
public class SOLRIndexHandler extends CollectionHandler{
	private static Logger log = Logger.getLogger(SOLRIndexHandler.class);

	private int count = 0;
	private int failedCount = 0;
	private boolean indexUpdate = false;
	
	/**
	 * Constructor for SOLRIndexHandler
	 * @param damsClient
	 * @throws Exception
	 */
	public SOLRIndexHandler(DAMSClient damsClient) throws Exception{
		this(damsClient, null);
	}
	
	/**
	 * Constructor for SOLRIndexHandler
	 * @param damsClient
	 * @param collectionId
	 * @throws Exception
	 */
	public SOLRIndexHandler(DAMSClient damsClient, String collectionId) throws Exception{
		this(damsClient, collectionId, false);
	}
	
	/**
	 * Constructor for SOLRIndexHandler
	 * @param damsClient
	 * @param collectionId
	 * @param indexUpdate
	 * @throws Exception
	 */
	public SOLRIndexHandler(DAMSClient damsClient, String collectionId, boolean indexUpdate) throws Exception{
		super(damsClient, collectionId);
		this.indexUpdate = indexUpdate;
	}
	
	public boolean isIndexUpdate() {
		return indexUpdate;
	}

	public void setIndexUpdate(boolean indexUpdate) {
		this.indexUpdate = indexUpdate;
	}

	/**
	 * Procedure for SOLR index
	 */
	public boolean execute() throws Exception {

		String eMessage;
		String subjectURI = null;
		
		for(int i=0; i<itemsCount; i++){
			count++;
			subjectURI = items.get(i);
			try{
				setStatus("Processing SOLR index for subject " + subjectURI  + " (" + (i+1) + " of " + itemsCount + ") ... " ); 
				boolean suceeded = damsClient.solrUpdate(subjectURI);
				if(!suceeded){
					failedCount++;
					exeResult = false;
					String iMessage = "SOLR index for subject " + subjectURI  + " failed ";
					iMessage += "(" + (i+1) + " of " + itemsCount + "): ";
					setStatus( iMessage); 
					log("log", iMessage);
					log.info(iMessage);
				}else{
					String iMessage = "SOLR index for subject " + subjectURI  + " succeeded (" + (i+1) + " of " + itemsCount + "). ";
					setStatus( iMessage); 
					log("log", iMessage);
					log.info(iMessage);
				}
			
			} catch (Exception e) {
				failedCount++;
				e.printStackTrace();
				exeResult = false;
				eMessage = "SOLR index failed: " + e.getMessage();
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
    			eMessage = "SOLR index interrupted for subject " + subjectURI  + ". \n Error: " + e1.getMessage() + "\n";
				setStatus("Canceled");
				clearSession();
				log("log", eMessage.replace("\n", ""));
				log.info(eMessage, e1);
				break;
			}
		}
		
		// Update/Delete the items that are not in the collection, or not in the batch of collections.
		if(indexUpdate && exeResult && collectionId != null){
			setStatus("Cleaning up SOLR index  ... " );
			String subject = null;
			String[] collections = collectionId.split(",");
			List<String> colItems = null;
			for(int i=0; i<collections.length; i++){
				String colTitle = collectionsMap.get(collections[i]);
				List<String> solrItems = damsClient.solrListObjects(null, colTitle);
				int solrCount = solrItems.size();
				int count = 0;
				if(collections.length > 1)
					colItems = listItems(collections[i], damsClient);
				else
					colItems = items;
				
				// Perform solr cleaning up when more items is found in SOLR for the collection
				if(solrCount > colItems.size()){
					for(Iterator<String> it=solrItems.iterator();it.hasNext();){
						count++;
						subject = it.next();
						if(colItems.indexOf(subject) <= 0){
							setStatus("Processing SOLR cleaning up item " + subjectURI  + " for collection " + colTitle + " (" + count + " of " + solrCount + ") ... " ); 
							boolean suceeded = damsClient.solrUpdate(subjectURI);
							if(!suceeded){
								exeResult = false;
								String iMessage = "SOLR index cleaning up failed " + " for subject " + subjectURI  + ".\n";
								setStatus( iMessage.replace("\n", "<br/>")); 
								log("log", iMessage);
								log.info(iMessage);
							}else{
								String iMessage = "SOLR index cleaning up succeedded " + " for subject " + subjectURI  + ".\n";
								setStatus( iMessage.replace("\n", "<br/>")); 
								log("log", iMessage);
								log.info(iMessage);
							}
						}else
							setStatus("Cleaning up SOLR at  " + count + " of " + solrCount + " for collection " + colTitle + "." ); 
						try{
							Thread.sleep(10);
						} catch (InterruptedException e1) {
			        		exeResult = false;
			    			eMessage = "SOLR index cleaning up interrupted ... \n";
							setStatus("Canceled");
							clearSession();
							log("log", eMessage.replace("\n", ""));
							log.info(eMessage, e1);
							break;
						}
					}
				}
			}
		}
		
		return exeResult;
	}


	/**
	 * Execution result message
	 */
	public String getExeInfo() {
		if(exeResult)
			exeReport.append("SOLR index succeeded. \n ");
		else
			exeReport.append("SOLR index failed (" + failedCount + " of " + count + " failed): \n ");	
		exeReport.append("Total items found " + itemsCount + ". Number of items updated " + count + ".\n");
		String exeInfo = exeReport.toString();
		log("log", exeInfo);
		return exeInfo;
	}
}
