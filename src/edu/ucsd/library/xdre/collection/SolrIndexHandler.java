package edu.ucsd.library.xdre.collection;

import java.io.File;
import java.util.List;

import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * SolrIndexHandler
 * @author lsitu
 *
 */
public class SolrIndexHandler extends CollectionHandler
{
	private static Logger log = Logger.getLogger(SolrIndexHandler.class);
	private boolean replaceOrNot = false;
	private String message = "";
	private int counter = 0;
	private int failedsCount = 0;
	private int updatedCount = 0;
	private int skipCount = 0;
	private boolean interrupted = false;
	private String solrCore = null;

	public SolrIndexHandler(DAMSClient damsClient, String collectionId, int userId, boolean replaceOrNot) throws Exception{
		super(damsClient, collectionId);
		this.replaceOrNot = replaceOrNot;
	}
	
	public SolrIndexHandler(DAMSClient damsClient, List<String> items, int userId, boolean replaceOrNot) throws Exception{
		this.items = items;
		this.replaceOrNot = replaceOrNot;
	}

	public String getSolrCore() {
		return solrCore;
	}

	public void getSolrCore(String solrCore) {
		this.solrCore = solrCore;
	}

	public String getExeInfo() {
		String exeInfo = "";
		if(exeResult && failedsCount<=0)
			exeInfo = "SOLR indexes for " + collectionTitle + " succeeded. \n-Total processed: " + counter + " ( Number of items updated: " + updatedCount + (skipCount>0?", number of items skit: " + skipCount:"") + " ).\n" + message;
		else
			exeInfo = "Execution result for " + collectionTitle + ": " + failedsCount + " of " + counter + " failed ( updated: " + updatedCount + (updatedCount + skipCount>0?", skip: " + skipCount:"") + " ).\n" + message;
		log("log", exeInfo);
		return exeInfo;

	}
	
	public boolean execute() throws Exception{
		String subjectId = null;
		
		String eMessage = "";
		String itemLink = "";

		System.out.println("SOLR Indexer collection " +  collectionId + "...");

		
		for(int i=0; i<itemsCount && !interrupted; i++){
			subjectId = (String) items.get(i);
			
			itemLink = getDDOMReference(subjectId);
			
			setStatus("SOLR Indexer processing " + itemLink + " (" + (i+1) + " of " + itemsCount + ")...");

			System.out.println("SOLR handler indexing " + subjectId );
			int numTry = 1;
			boolean successTry = false;
			do{
				try{
					successTry = damsClient.solrUpdate(subjectId);
				}catch(Exception e){
					//Log error.
					if(numTry == maxTry){
		   				eMessage = itemLink + ". Error: " + e.getMessage();
		   				String iMessagePrefix = "SOLR indexing failed with ";
		   				setStatus(iMessagePrefix + eMessage + "<br/>");
		   				log("log", iMessagePrefix + eMessage);
		   				log.info(iMessagePrefix + eMessage, e);
					}
					e.printStackTrace();
				}
			}while(!successTry && numTry++ < maxTry);
			
			if(!successTry){
				failedsCount++;
   				setExeResult(false);
   				eMessage = itemLink + ".";
   				String iMessagePrefix = "SOLR indexing failed with ";
   				setStatus(iMessagePrefix + eMessage + "<br/>");
   				log("log", iMessagePrefix + eMessage);
   				log.info(iMessagePrefix + eMessage);
			}else
				updatedCount++;

					
   	        counter++;		
   			setProgressPercentage( ((i + 1) * 100) / getFilesCount());
		}
		
		return exeResult;
	}
}
