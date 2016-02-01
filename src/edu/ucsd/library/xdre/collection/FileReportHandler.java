package edu.ucsd.library.xdre.collection;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;

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

			try{
				setStatus("Processing file report for subject " + subjectURI  + " (" + (i+1) + " of " + itemsCount + ") ... " ); 
	
				String use = "";
				String fileName = "";
				
				Document doc = damsClient.getRecord(subjectURI);
				// retrieve filenames stored in the filename/local identifiers 
				List<Node> nodes = doc.selectNodes("//dams:Note[dams:type='identifier' and (dams:displayLabel='filename' or dams:displayLabel='Filename' or dams:displayLabel='local')]/rdf:value", ".");

				List<String> fileIdentifiers = new ArrayList<>();
				if (nodes.size() > 0) {
					for(Node node : nodes){
						fileIdentifiers.add(node.getText());
					}
				} 
				
				// dams:sourceFileName stored on time ingest, could be lost overtime
				nodes = doc.selectNodes("//dams:File[contains(dams:use, '-source') or contains(dams:use, '-alternate') or contains(@rdf:about, '/1.')]");
				for(int j = 0; j < nodes.size(); j++) {
					masterCount++;

					Node node = nodes.get(j);
					Node srcFileNode = node.selectSingleNode("dams:sourceFileName");
					Node useNode = node.selectSingleNode("dams:use");
					String fid = node.selectSingleNode("@rdf:about").getStringValue();
					if (useNode != null)
						use = useNode.getStringValue();

					if (srcFileNode != null)
						fileName = srcFileNode.getStringValue();

					// ark string filename, need to be extracted from the filename/local indentifiers
					if (fileName.indexOf("20775-") >= 0) {
						// use the filename/local identifier when there is one master file presented
						if (fileIdentifiers.size() == 1 && fileIdentifiers.size() == nodes.size()) {
							fileName = fileIdentifiers.get(0);
						} else {
							// more than one files that can't be matched it to the file elements
							if (j < fileIdentifiers.size()) {
								fid = subjectURI;
								use = "";
								// use the values in the filename/local identifiers
								fileName = fileIdentifiers.get(j);
							} else {
								// use value "Filename not found" if no filename/local identifier presented
								fileName = "Filename not found";
							}
						}
					}

					log("log", fileName + "\t" + fid + "\t" +use);
				}

				// List all other values in filename/local identifiers 
				if (fileIdentifiers.size() > nodes.size()) {
					for (int j = nodes.size(); j < fileIdentifiers.size(); j++) {
						log("log", fileIdentifiers.get(j) + "\t" + subjectURI + "\t" + "");
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
