package edu.ucsd.library.xdre.collection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DFile;
import edu.ucsd.library.xdre.utils.FileURI;

/**
 * 
 * JhoveReportHandler: create a report for Jhove technical metadata.
 * @author lsitu@ucsd.edu
 */
public class JhoveReportHandler extends CollectionHandler{
	protected static Logger log = Logger.getLogger(JhoveReportHandler.class);

	protected int count = 0;
	protected int failedCount = 0;
	protected int filesReported = 0;
	protected int filesUpdated = 0;
	protected boolean bytestreamFormatOnly = false;
	protected boolean updateFormat = false;
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
	public JhoveReportHandler(DAMSClient damsClient, String collectionId, boolean updateFormat) throws Exception{
		super(damsClient, collectionId);
		this.updateFormat = updateFormat;
	}

	/**
	 * Procedure for creating Jhove report
	 */
	public boolean execute() throws Exception {

		String eMessage;
		String subjectURI = null;
		
    	log("log", "ARK_ID\tFormat&Version\tSize(bytes)\tCheckSum_CRC32\tDate_Modified\tStatus\tSource_File");
		for(int i=0; i<itemsCount; i++){
			count++;
			subjectURI = items.get(i);

			try{
				setStatus("Processing Jhove report for subject " + subjectURI  + " (" + (i+1) + " of " + itemsCount + ") ... " ); 
				DFile dFile = null;
				DFile dFileTmp = null;
				String formatName = null;
				String formatNameTmp = null;
				String oSrcFileName = null;
				String use = null;
				List<DFile> files = damsClient.listObjectFiles(subjectURI);
				for(Iterator<DFile> it=files.iterator(); it.hasNext();){
					dFile = it.next();
					use = dFile.getUse();
					if(use != null && use.endsWith(Constants.SERVICE) || use.endsWith(Constants.SOURCE)){
						// Jhove report for bytestream format files only
						if(bytestreamFormatOnly) 
							formatName = dFile.getFormatName();
						
						if(!bytestreamFormatOnly || (bytestreamFormatOnly && (formatName ==null || formatName !=null&&formatName.equalsIgnoreCase("bytestream")))){
							oSrcFileName = dFile.getSourceFileName();
							FileURI fileURI = FileURI.toParts(dFile.getId(), subjectURI);
							dFileTmp = damsClient.extractFileCharacterize(fileURI.getObject(), fileURI.getComponent(), fileURI.getFileName());
					    	if(updateFormat){
					    		formatNameTmp = dFileTmp.getFormatName();
					    		if(formatNameTmp != null && !formatNameTmp.equalsIgnoreCase("bytestream") && !formatNameTmp.equals(formatName)){
					    			// Save Jhove
						    		List<NameValuePair> paramsOrg = dFile.toNameValuePairs();
						    		//Remove the properties for format and formatVersion
						    		List<NameValuePair> optionalParams = new ArrayList<NameValuePair>();
						    		for(int j=0; j<paramsOrg.size(); j++){
						    			NameValuePair optParam = paramsOrg.get(j);
						    			String paramName = optParam.getName();
						    			if(!(paramName.equals("formatName") || paramName.equals("formatVersion")))
						    				optionalParams.add(paramsOrg.get(j));
						    		}
						    		damsClient.saveFileCharacterize(fileURI.getObject(), fileURI.getComponent(), fileURI.getFileName(), optionalParams);
						    		log("log", dFile.getId() + "\t" + dFileTmp.getFormatName() + " " + dFileTmp.getFormatVersion() + "\t" + dFileTmp.getSize() + "\t" + dFileTmp.getCrc32checksum() + "\t" + dFileTmp.getDateCreated() + "\t" + dFileTmp.getStatus() + "\t" + (oSrcFileName==null?" ":oSrcFileName));
						    		filesUpdated++;
					    		}else{
					    			filesNotUpdated.append(dFileTmp.getId() + "\t" + dFileTmp.getFormatName() + " " + dFileTmp.getFormatVersion() + "\t" + dFileTmp.getSize() + "\t" + dFileTmp.getCrc32checksum() + "\t" + dFileTmp.getDateCreated() + "\t" + dFileTmp.getStatus() + "\t" + (oSrcFileName==null?" ":oSrcFileName));
					    		}
					    			
					    	}else
								log("log", dFile.getId() + "\t" + dFileTmp.getFormatName() + " " + dFileTmp.getFormatVersion() + "\t" + dFileTmp.getSize() + "\t" + dFileTmp.getCrc32checksum() + "\t" + dFileTmp.getDateCreated() + "\t" + dFileTmp.getStatus() + "\t" + (oSrcFileName==null?" ":oSrcFileName));
							
							filesReported++;
						}
					}
				}

			} catch (Exception e) {
				failedCount++;
				e.printStackTrace();
				exeResult = false;
				eMessage = "Jhove report failed: " + e.getMessage();
				setStatus( eMessage  + "(" +(i+1)+ " of " + itemsCount + ")"); 
				jhoveErrorReport(subjectURI + "\t \t \t \t \tError" + eMessage + "\t ");
				log.info(eMessage );
			}
			setProgressPercentage( ((i + 1) * 100) / itemsCount);
			
			try{
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				failedCount++;
        		exeResult = false;
    			eMessage = "Jhove report interrupted for subject " + subjectURI  + ". \n Error: " + e1.getMessage() + "\n";
				setStatus("Canceled");
				clearSession();
				jhoveErrorReport(subjectURI + "\t \t \t \t \tError" + eMessage + "\t ");
				log.info(eMessage, e1);
				break;
			}
		}
		
		return exeResult;
	}

	public boolean isBytestreamFormatOnly() {
		return bytestreamFormatOnly;
	}

	public void setBytestreamFormatOnly(boolean bytestreamFormatOnly) {
		this.bytestreamFormatOnly = bytestreamFormatOnly;
	}
	
	public boolean isUpdateFormat() {
		return updateFormat;
	}

	public void setUpdateFormat(boolean updateFormat) {
		this.updateFormat = updateFormat;
	}

	public void jhoveErrorReport(String errorMessage){
		if(updateFormat)
			filesNotUpdated.append(errorMessage);
		else
			log("log", errorMessage);
	}

	/**
	 * Execution result message
	 */
	public String getExeInfo() {
		String iMessage = "Number of objects found: " + itemsCount + " \nTotal master/service files processed: " + count + " \nTotal master/service Files reported: " + filesReported + (updateFormat?"; \nTotal Files updated: "+filesReported:"");
        String mHeader = "\nFile characterize/Jhove report " + ((collectionId!=null&&collectionId.length()==10)?"for "+collectionTitle:"");
		if(exeResult)
			exeReport.append(mHeader + " succeeded: \n" + iMessage + "\n");
		else{
			exeReport.append(mHeader + failedCount + " of " + itemsCount + " failed: " + iMessage + "\n" );
		}
		
		if(updateFormat && filesNotUpdated.length()>0){
			log("log", "\n*************************************************************************************************************************************");
			log("log", "\nThe following " + (filesReported-filesUpdated)+ " BYTESTREAM format files haven't be fixed: \n");
			log("log", "\n*************************************************************************************************************************************");
			log("log", filesNotUpdated.toString());
		}
		String exeInfo = exeReport.toString();
		log("log", exeInfo);
		return exeInfo;
	}
}
