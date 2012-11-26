package edu.ucsd.library.xdre.exports;

import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.collection.CollectionHandler;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * Submit objects to CDL Merritt.
 * @author lsitu
 *
 */
public class CdlIngestHandler extends CollectionHandler
{
	public static String INSERT_STATUS_TRACKING_SQL = "INSERT INTO DAMS_TRACKING (SUBJECT, SUBMITED_DATE, USER_ID, LOCATION) VALUES (?,TO_DATE(?,'" + "'yyyy-MM-dd''T''HH:mm:ss'" + "'),?,'Merritt')";
	private static Logger log = Logger.getLogger(CdlIngestHandler.class);
	
	private static Map<String, List<String>> manifestMap = null;
	private int userId = -1;
	private int operationType = 0;
	private String cdlGroupId = null;
	private String message = "";
	private int failedsCount = 0;

	private String metsFeeder = null;
	private String cdlAccount = null;
	private String cdlAuthCode = null; 
	
	public CdlIngestHandler(DAMSClient damsClient, String collectionId, int userId, int operationType) throws Exception{
		super(damsClient, collectionId);
		this.userId = userId;
		this.operationType = operationType;
		initHandler();
	}
	
	public CdlIngestHandler(DAMSClient damsClient, String collectionId, int userId, int operationType, String metsFeeder, String cdlAccount, String cdlAuthCode) throws Exception{
		this(damsClient, collectionId, userId, operationType);
		this.metsFeeder = metsFeeder;
		this.cdlAccount = cdlAccount;
		this.cdlAuthCode = cdlAuthCode;
	}
	
	private synchronized void initHandler() throws Exception{
		
		//XXX
		//Retrieve the cdlGroupId for the collection
	    cdlGroupId = damsClient.getMetadata(collectionId, null);
	    if(cdlGroupId == null || cdlGroupId.length() == 0){
	    	interrupted = true;
	    	message = "CDL Access Group ID is not found for collection " + collectionTitle;
	    	throw new Exception(message);
	    }
	    if(manifestMap == null)
	    	manifestMap = new HashMap<String, List<String>>();
	}
	
	public String getMetsFeeder() {
		return metsFeeder;
	}

	public void setMetsFeeder(String metsFeeder) {
		this.metsFeeder = metsFeeder;
	}

	public String getCdlAccount() {
		return cdlAccount;
	}

	public void setCdlAccount(String cdlAccount) {
		this.cdlAccount = cdlAccount;
	}

	public String getCdlAuthCode() {
		return cdlAuthCode;
	}

	public void setCdlAuthCode(String cdlAuthCode) {
		this.cdlAuthCode = cdlAuthCode;
	}

	public String getCdlGroupId() {
		return cdlGroupId;
	}

	public void setCdlGroupId(String cdlGroupId) {
		this.cdlGroupId = cdlGroupId;
	}

	public boolean execute() throws Exception {
		String subjectId = null;
		boolean success = false;
		String eMessage = "";
		String itemLink = "";
		int batchId = 0;

		boolean cdlCreated = false;
		boolean cdlUpdated = false;
	
		List<String> batchItems = new ArrayList<String>();
		String batchIdPrefix = collectionId + "-";
		try{
			List<String> userIdList = getMerrittBatch(collectionId);
			if(userIdList!= null){
				setExeResult(false);
				message += "Conflicting request. " + collectionTitle + " is sending to Merritt by " + userIdList.get(0) + " at this time.";
			}else{
				//Lock collection for Merritt ingestion
				userIdList = new ArrayList<String>();
				userIdList.add(""+userId);
				addMerrittBatch(collectionId, userIdList);
				//General items in a collection
				for(int i=0; i<itemsCount && !interrupted; i++){
					try{
						subjectId = (String) items.get(i);
						
						itemLink = getDDOMReference(subjectId);
							
						if((operationType != 0 && !cdlUpdated) || (!cdlCreated && operationType == 0)){
						
							setStatus("Adding object to " + (metsFeeder==null?"DPR":metsFeeder.toUpperCase()) + " for " + itemLink + " (" + (i + 1) + " of " + itemsCount + ") ...");
							if(metsFeeder != null && metsFeeder.equalsIgnoreCase("merritt")){
								batchItems.add(subjectId);
								
								if(batchItems.size()%Constants.MERRITT_BATCH_SIZE == 0){
									success = merrittIngest(batchIdPrefix + ++batchId, batchItems, cdlCreated);
									batchItems = new ArrayList<String>();
								}
							}
						}
						
					} catch (Exception e) {
						e.printStackTrace();
						setExeResult(false);
						eMessage = itemLink + ". Error: " + e.getMessage().replace(cdlAuthCode, "xxxxxx");
						String iMessagePrefix = "CDL objects sending failed with ";
						setStatus(iMessagePrefix + eMessage + "<br/>");
						log("log", iMessagePrefix + eMessage);
						log.info(iMessagePrefix + eMessage, e);
						batchItems = new ArrayList<String>();
					}
					
		   			try{
		   				if(exeResult)
		   					Thread.sleep(10);
		   			} catch (InterruptedException e) {
						e.printStackTrace();
		        		setExeResult(false);
		        		eMessage = e.getMessage();
		    			String iMessagePrefix = "CDL object sending interrupted with " + itemLink + ". \n";
						setStatus("Canceled");
						clearSession();
						log("log", iMessagePrefix + eMessage + "\n");
						log.info(iMessagePrefix + eMessage, e);
						interrupted = true;
					}
		   			
					setProgressPercentage( ((i + 1) * 100) / getFilesCount());
					
				}
			}
			
			if(batchItems.size() > 0){
				try{
					success = merrittIngest(batchIdPrefix + ++batchId, batchItems, cdlCreated);
				}catch (Exception e) {
					e.printStackTrace();
					setExeResult(false);
					eMessage = itemLink + ". Error: " + e.getMessage().replace(cdlAuthCode, "xxxxxx");
					String iMessagePrefix = "CDL objects sending failed with ";
					setStatus(iMessagePrefix + eMessage + "<br/>");
					log("log", iMessagePrefix + eMessage);
					log.info(iMessagePrefix + eMessage, e);
				}
			}
		}finally{
			//Clear user lock.
			addMerrittBatch(collectionId, null);
		}
		return exeResult;
	}
	
	private String feedMerritt(String batchId, List<String> itemsBatch) throws Exception{
		String manifestUrl = Constants.CLUSTER_HOST_NAME + "damsmanager/merritt.do?batchId=" + batchId;
		String merrittFeederUrl = Constants.CDL_MERRITT_URL+ "?userID=" + cdlAccount + "&authCode=" + cdlAuthCode + "&accessGroupID=" + cdlGroupId + "&manifestURL=" + URLEncoder.encode(manifestUrl, "UTF-8");
		String resultMessage = null;
		try{
			
			/** FIXME test only */
			System.out.println("Merrir URL -> " + merrittFeederUrl.replace(cdlAuthCode, "xxxxxx"));
			setStatus("Submitting " + itemsBatch.size() + " items in batch " +  batchId + " to Merritt. Please wait ...");
			addMerrittBatch(batchId, itemsBatch);

			resultMessage = damsClient.getContentBodyAsString(merrittFeederUrl);
			
			resultMessage = resultMessage.replace(cdlAuthCode, "xxxxxx");

			System.out.println(merrittFeederUrl + " -> " + resultMessage);
			
			addMerrittBatch(batchId, null);
		}catch(Exception e){
			String iMessage = e.getMessage();
			if(iMessage != null && iMessage.length() > 0){
				throw new Exception(iMessage.replace(cdlAuthCode, "xxxxxx"));
			}else
				throw e;
		}
		return resultMessage;
	}
	
	public boolean parseMerrittResult(String resultMessage){
		return resultMessage.indexOf("QUEUED") > 0;
	}
	
	public String getExeInfo() {
		String exeInfo = "";
		if(exeResult)
			exeInfo = "CDL objects sending for " + collectionTitle + " succeded. \n-Total files processed: " + itemsCount + "\n" + message;
		else
			exeInfo = "CDL object sending result for " + collectionTitle + ": " + failedsCount + " of " + itemsCount + " failed: + \n" + message;
		log("log", exeInfo);
		return exeInfo;

	}
	
	public boolean merrittIngest(String batchId, List<String> batchItems, boolean cdlCreated) throws Exception{
		boolean success = false;

		String resultMessage = null;
		resultMessage = feedMerritt(batchId, batchItems);
		success = parseMerrittResult(resultMessage);
		
		if(!success){
			failedsCount += batchItems.size();
			exeResult = false;
			message += "Failed to ingest " + batchId + ". Message: " + resultMessage;
		}
		return success;
	}
	
	
	public static synchronized List<String> getMerrittBatch(String batchId){
		if(manifestMap != null)
			return manifestMap.get(batchId);
		else 
			return null;
	}
	
	public static synchronized void addMerrittBatch(String batchId, List<String> items){
		manifestMap.put(batchId, items);
	}
}
