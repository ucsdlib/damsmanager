package edu.ucsd.library.xdre.collection;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DamsURI;

/**
 * 
 * FileUploadHandler: to ingest and replace files.
 * @author lsitu@ucsd.edu
 */
public class FileUploadHandler extends CollectionHandler{
	private static Logger log = Logger.getLogger(FileUploadHandler.class);

	private int count = 0;
	private int failedCount = 0;
	private Map<String, String> filesMap = null;
	private List<String> ingestFailed = new ArrayList<String>();
	private List<String> derivFailed = new ArrayList<String>();
	private List<String> solrFailed = new ArrayList<String>();
	
	/**
	 * Constructor for FileUploadHandler
	 * @param damsClient
	 * @throws Exception
	 */
	public FileUploadHandler(DAMSClient damsClient, Map<String, String> filesMap) throws Exception{
		super(damsClient, null);
		this.filesMap = filesMap;
	}
	

	/**
	 * Procedure for File Upload
	 */
	public boolean execute() throws Exception {

		String message = null;
		String subjectURI = null;
		
		for(int i=0; i<itemsCount && !interrupted; i++){
			count++;
			String f = items.get(i);
			subjectURI = filesMap.get(f);
			setStatus("Uploading file " + subjectURI  + " (" + (i+1) + " of " + itemsCount + ") ... " );

			String use = null;
			DamsURI damsURI = DamsURI.toParts(subjectURI, null, "File");
			String oid = damsURI.getObject();
			String cid = damsURI.getComponent();
			String fid = damsURI.getFileName();
			
			try {
				if (damsClient.exists(damsURI.getObject(), null, null)) {
					try {
						Document doc = damsClient.getRecord(oid);
						Node fileNode = doc.selectSingleNode("//File[@rdf:about='" + subjectURI + "']");
						if (fileNode != null) {
							Node useNode = fileNode.selectSingleNode("dams:use");
							if (useNode != null)
								use = useNode.getText();
						}
					}catch (Exception e) {
						logError("Unable to retrieve file use property for object " + oid + ": " + e.getMessage());
					}
				}
						
				// Add field dateCreated, sourceFileName, sourcePath etc.
				File file = new File(Constants.DAMS_STAGING, f);
				Map<String, String> params = DAMSClient.toFileIngestParams(oid, cid, fid, file);
				params.put("local", file.getAbsolutePath());
				params.put("use", use);

	
				boolean successful = damsClient.uploadFile(params, true);
				
				if (successful) {
					
					message = "Uploaded file  " + subjectURI  + " (" + f + ").";
					setStatus(message);
					log("log", message);
	
					//Create derivatives for images and documents PDFs
					if((isImage(fid, use) || isDocument(fid, use)) 
							&& (use == null || use.endsWith("source") || use.endsWith("service") || use.endsWith("alternate"))){
						
						successful = damsClient.updateDerivatives(oid, cid, fid, null);
						if(successful){
							logMessage( "Created derivatives for " + subjectURI + " (" + damsClient.getRequestURL() + ").");
						} else {
							derivFailed.add(damsClient.getRequestURL() + ", \n"); 
							logError("Failed to created derivatives " + damsClient.getRequestURL() + " (" + (i+1) + " of " + itemsCount + ") ... " );
						}
					}
	
					if(!updateSOLR(oid)) {
						failedCount++;
						exeResult = false;
						solrFailed.add(oid);
						message = "SOLR index failed for subject " + oid  + ".";
						setStatus(message);
						logError(message);
					}
				} else {
					exeResult = false;
					ingestFailed.add(f);
					message = "Failed to ingest file " + subjectURI  + " (" + f + ").";
					setStatus(message);
					logError(message);
				}
	
				setProgressPercentage( ((i + 1) * 100) / itemsCount);
			} catch (Exception e) {
				exeResult = false;
				ingestFailed.add(f);
				message = "Failed to ingest file " + subjectURI  + " (" + f + "): \n" + e.getMessage();
				setStatus(message);
				logError(message);
			}

			try{
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				failedCount++;
				interrupted = true;
    			logError("File upload canceled for subject " + subjectURI  + ". Error: " + e1.getMessage() + ". ");
				setStatus("Canceled");
				clearSession();
			}
		}
		
		return exeResult;
	}


	/**
	 * Execute the file upload process
	 */
	public String getExeInfo() {
		if(exeResult)
			exeReport.append("File uploaded succeeded: \n ");
		else
			exeReport.append("File uploaded (" + failedCount + " of " + count + " failed): \n ");	

		exeReport.append("Total files found " + itemsCount + " (Number of files processed: " + count + "). \n");

		if (ingestFailed.size() > 0) {
			exeReport.append("Failed to ingest the following files: \n");
			for (String f : ingestFailed) {
				exeReport.append(f + " (" + filesMap.get(f) + ") \n");
			}
		}

		if (derivFailed.size() > 0) {
			exeReport.append("Failed to create derivatives for the following files: \n");
			for (String f : derivFailed) {
				exeReport.append(f + " (" + filesMap.get(f) + ") \n");
			}
		}

		if (solrFailed.size() > 0) {
			exeReport.append("Failed to update SOLR for the following objects: \n");
			for (String o : solrFailed) {
				exeReport.append(o + "\n");
			}
		}

		String exeInfo = exeReport.toString();
		log("log", exeInfo);
		return exeInfo;
	}
}
