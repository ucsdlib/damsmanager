package edu.ucsd.library.xdre.collection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DFile;
import edu.ucsd.library.xdre.utils.DamsURI;

/**
 * 
 * JhoveReportHandler: create a report for Jhove technical metadata.
 * @author lsitu@ucsd.edu
 */
public class JhoveReportHandler extends CollectionHandler{
	protected static Logger log = Logger.getLogger(JhoveReportHandler.class);

	public static final String ADDJHOVE = "addJhove";
	public static final String BYTESTREAM = "bytestream";
	public static final String DURATION = "duration";
	
	protected int count = 0;
	protected int failedCount = 0;
	protected int filesReported = 0;
	protected int filesUpdated = 0;
	protected int masterCount = 0;
	protected boolean bytestreamFilesOnly = false;
	private String jhoveUpdate = null;
	private StringBuilder filesNotUpdated = new StringBuilder();
	
	/**
	 * Constructor for JhoveReportHandler
	 * @param damsClient
	 * @throws Exception
	 */
	public JhoveReportHandler(DAMSClient damsClient) throws Exception{
		this(damsClient, null);
	}
	
	/**
	 * Constructor for ChecksumsHandler
	 * @param damsClient
	 * @param collectionId
	 * @throws Exception
	 */
	public JhoveReportHandler(DAMSClient damsClient, String collectionId) throws Exception{
		this(damsClient, collectionId, false);
	}
	
	/**
	 * Constructor for ChecksumsHandler
	 * @param damsClient
	 * @param collectionId
	 * @param updateFormat
	 * @throws Exception
	 */
	public JhoveReportHandler(DAMSClient damsClient, String collectionId, boolean bytestreamFilesOnly) throws Exception{
		super(damsClient, collectionId);
		this.bytestreamFilesOnly = bytestreamFilesOnly;
		 if(bytestreamFilesOnly && (collectionId == null || collectionId.length() == 0)){
			 // Report all bytestream format files in DAMS
			 items = listAllItems();
		 }
	}

	/**
	 * Procedure for creating Jhove report
	 */
	public boolean execute() throws Exception {

		String message;
		String subjectURI = null;
		
    	log("log", "ARK_ID\tFormat&Version\tSize(bytes)\tCheckSum_CRC32\tDate_Modified\tDuration\tStatus\tSource_File");
		for(int i=0; i<itemsCount; i++){
			subjectURI = items.get(i);
			try{
				setStatus("Processing Jhove report for subject " + subjectURI  + " (" + (i+1) + " of " + itemsCount + ") ... " ); 
				DFile dFile = null;
				DFile dFileTmp = null;
				DamsURI fileURI = null;
				String formatName = null;
				String formatNameTmp = null;
				String duration = null;
				String durationTmp = null;
				String oSrcFileName = null;
				String use = null;
				List<DFile> files = damsClient.listObjectFiles(subjectURI);
				
				for(Iterator<DFile> it=files.iterator(); it.hasNext();){
					count++;
					dFile = it.next();
					use = dFile.getUse();
					try{
						setStatus("Processing Jhove report for file " + dFile.getId()  + " (" + (i+1) + " of " + itemsCount + ") ... " ); 
						if((jhoveUpdate != null && jhoveUpdate.equalsIgnoreCase("addJhove")) || isMasterFile(use)){
							if(isMasterFile(use))
								masterCount++;
							// Jhove report for bytestream format files only
							if(bytestreamFilesOnly) 
								formatName = dFile.getFormatName();
							
							if(!bytestreamFilesOnly || (bytestreamFilesOnly && (formatName ==null || formatName !=null&&formatName.equalsIgnoreCase(BYTESTREAM)))){
								oSrcFileName = dFile.getSourceFileName();
								duration = dFile.getDuration();
								fileURI = DamsURI.toParts(dFile.getId(), subjectURI);
								dFileTmp = damsClient.extractFileCharacterize(fileURI.getObject(), fileURI.getComponent(), fileURI.getFileName());
				    			// Update Jhove
						    	if(jhoveUpdate != null && jhoveUpdate.length() > 0){
						    		boolean updateJhove = false;
						    		List<NameValuePair> optionalParams = new ArrayList<NameValuePair>();
						    		if (jhoveUpdate.equalsIgnoreCase("addJhove")){
						    			updateJhove = true;
										// Overwrite the jhove metadta with the original properties.
										Map<String, String> props = dFile.toProperties();
										Map<String, String> newProps = dFileTmp.toProperties();
										String key = null;
										String value = null;
										for(Iterator<String> pit=props.keySet().iterator(); pit.hasNext();){
											key = pit.next();
											value = props.get(key);
											if(value != null && value.length() > 0)
												newProps.put(key, value);
										}
										
										//file properties to update
										for(Iterator<String> pit=newProps.keySet().iterator(); pit.hasNext();){
											key = pit.next();
											value = newProps.get(key);
											if(value != null){
												if(key.equals(DFile.SOURCE_PATH))
													value = value.replace("\\", "/");
												addProperty(optionalParams, key, value);
											}
										}
						    		} else {
						    			// Update file properties selectively
							    		boolean addDuration = true;
							    		List<NameValuePair> paramsOrg = dFile.toNameValuePairs();
							    		formatNameTmp = dFileTmp.getFormatName();
							    		durationTmp = dFileTmp.getDuration();
							    		for(int j=0; j<paramsOrg.size(); j++){
							    			NameValuePair optParam = paramsOrg.get(j);
							    			String paramName = optParam.getName();
								    		
								    		if(jhoveUpdate.equalsIgnoreCase(BYTESTREAM) && formatNameTmp != null && !formatNameTmp.equalsIgnoreCase(BYTESTREAM) 
								    				&& !formatNameTmp.equals(formatName) && (paramName.equals(DFile.FORMAT_NAME) || paramName.equals(DFile.FORMAT_VERSION))){
									    		// Format and formatVersion extracted from Jhove	
								    			if(paramName.equals(DFile.FORMAT_NAME)){
								    				addProperty(optionalParams, paramName, formatNameTmp);
								    				addProperty(optionalParams, DFile.FORMAT_VERSION, dFileTmp.getFormatVersion());
								    			}
									    		updateJhove = true;
								    		}else if(jhoveUpdate.equalsIgnoreCase(DURATION) && paramName.equals(DFile.DURATION)){
								    			addDuration = false;
								    			if(durationTmp != null)
								    				addProperty(optionalParams, paramName, durationTmp);
								    			if(duration == null || !durationTmp.equals(duration))
								    				updateJhove = true;
											}else{
												// Overwrite other properties that don't need update except format to force Jhove extraction. 
												optionalParams.add(paramsOrg.get(j));
								    		}
							    		}
						    		
						    		
							    		// Duration that need to be extracted and save
							    		if(jhoveUpdate.equalsIgnoreCase(DURATION) && durationTmp != null && addDuration){
							    			addProperty(optionalParams, DFile.DURATION, durationTmp);
							    			updateJhove = true;
							    		}
						    		}
							    		
						    		// Save Jhove
						    		if(updateJhove){
							    		damsClient.updateFileCharacterize(fileURI.getObject(), fileURI.getComponent(), fileURI.getFileName(), optionalParams);
							    		log("log", dFile.getId() + "\t" + dFileTmp.getFormatName() + " " + dFileTmp.getFormatVersion() + "\t" + dFileTmp.getSize() + "\t" + dFileTmp.getCrc32checksum() + "\t" + dFileTmp.getDateCreated() + "\t" + dFileTmp.getDuration() + "\t" + dFileTmp.getStatus() + "\t" + (oSrcFileName==null?" ":oSrcFileName));
							    		filesUpdated++;
							    		
										// Updated SOLR
										if(!updateSOLR(subjectURI)){
											failedCount++;
											jhoveErrorReport(dFile.getId() + "\t \t \t \t \t \tError: failed to updated SOLR for " + subjectURI + "\t ");
										}
	
						    		}else{
						    			filesNotUpdated.append(dFileTmp.getId() + "\t" + dFileTmp.getFormatName() + " " + dFileTmp.getFormatVersion() + "\t" + dFileTmp.getSize() + "\t" + dFileTmp.getCrc32checksum() + "\t" + dFileTmp.getDateCreated() + "\t" + dFileTmp.getDuration() + "\t" + dFileTmp.getStatus() + "\t" + (oSrcFileName==null?" ":oSrcFileName));
						    		}
						    		
						    	}else
									log("log", dFile.getId() + "\t" + dFileTmp.getFormatName() + " " + dFileTmp.getFormatVersion() + "\t" + dFileTmp.getSize() + "\t" + dFileTmp.getCrc32checksum() + "\t" + dFileTmp.getDateCreated() + "\t" + dFileTmp.getDuration() + "\t" + dFileTmp.getStatus() + "\t" + (oSrcFileName==null?" ":oSrcFileName));
								
								filesReported++;
							}
						}
					} catch (Exception e) {
						failedCount++;
						e.printStackTrace();
						exeResult = false;
						message = "Jhove report failed: " + e.getMessage();
						setStatus(message  + "(" +(i+1)+ " of " + itemsCount + ")"); 
						jhoveErrorReport(fileURI + "\t \t \t \t \t \tError: " + message + "\t ");
						log.info(message );
					}
				}
			} catch (Exception e) {
				failedCount++;
				e.printStackTrace();
				exeResult = false;
				message = "Jhove report failed: " + e.getMessage();
				setStatus(message  + "(" +(i+1)+ " of " + itemsCount + ")"); 
				jhoveErrorReport(subjectURI + "\t \t \t \t \t \tError: " + message + "\t ");
				log.info(message );
			}

			setProgressPercentage( ((i + 1) * 100) / itemsCount);
			
			try{
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				//failedCount++;
        		exeResult = false;
    			message = "Jhove report canceled on subject " + subjectURI  + ".";
				//jhoveErrorReport(subjectURI + "\t \t \t \t \t \tError: " + message + "\t ");
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
	
	public void addProperty(List<NameValuePair> params, String propName, String propValue){
		if(propValue != null)
			params.add(new BasicNameValuePair(propName, propValue));
	}

	public String getJhoveUpdate() {
		return jhoveUpdate;
	}

	public void setJhoveUpdate(String jhoveUpdate) {
		this.jhoveUpdate = jhoveUpdate;
	}

	public void jhoveErrorReport(String errorMessage){
		if(jhoveUpdate != null && jhoveUpdate.length() > 0)
			filesNotUpdated.append(errorMessage + "\n");
		else
			log("log", errorMessage);
	}

	/**
	 * Execution result message
	 */
	public String getExeInfo() {
		String iMessage = "Number of objects found: " + itemsCount + " \nTotal master files: " + masterCount + " \nTotal Files reported: " + filesReported + (jhoveUpdate!=null&&jhoveUpdate.length()>0?" \nTotal Files updated: "+filesUpdated:"");
        String mHeader = "\nFile characterize/Jhove report " + ((collectionId!=null&&collectionId.length()==10)?"for "+collectionTitle:"");
		if(exeResult)
			exeReport.append(mHeader + " succeeded: \n" + iMessage + "\n");
		else{
			exeReport.append(mHeader + failedCount + " of " + count + " failed: \n" + iMessage + "\n" );
		}
		
		if(jhoveUpdate != null && jhoveUpdate.length() > 0 && filesNotUpdated.length()>0){
			log("log", "\n*************************************************************************************************************************************");
			log("log", "\n" + jhoveUpdate + " for the following " + failedCount + " files haven't being updated by DAMS Manager: \n");
			log("log", "\n*************************************************************************************************************************************");
			log("log", filesNotUpdated.toString());
		}
		// Add solr report message
		exeReport.append(getSOLRReport());
		String exeInfo = exeReport.toString();
		log("log", exeInfo);
		return exeInfo;
	}
}
