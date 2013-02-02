package edu.ucsd.library.xdre.collection;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DFile;
import edu.ucsd.library.xdre.utils.DamsURI;

/**
 * 
 * FileCountValidaionHandler: perform checksum validation.
 * @author lsitu@ucsd.edu
 */
public class FileCountValidaionHandler extends CollectionHandler{
	private static Logger log = Logger.getLogger(FileCountValidaionHandler.class);

	protected int count = 0;
	protected int missingObjectsCount = 0;
	protected int missingFilesCount = 0;
	protected int failedCount = 0;
	protected int masterTotal = 0;
	protected int filesTotal = 0;
	protected StringBuilder missingObjects = new StringBuilder();
	protected StringBuilder missingFiles = new StringBuilder();
	protected StringBuilder duplicatedFiles = new StringBuilder();
	protected Document filesDoc = null;
	
	/**
	 * Constructor for FileCountValidaionHandler
	 * @param damsClient
	 * @throws Exception
	 */
	public FileCountValidaionHandler(DAMSClient damsClient) throws Exception{
		this(damsClient, null);
	}
	
	/**
	 * Constructor for FileCountValidaionHandler
	 * @param damsClient
	 * @param collectionId
	 * @throws Exception
	 */
	public FileCountValidaionHandler(DAMSClient damsClient, String collectionId) throws Exception{
		super(damsClient, collectionId);
		if(collectionId != null)
			filesDoc = damsClient.getCollectionFiles(collectionId);
	}

	/**
	 * Procedure for file count validation
	 */
	public boolean execute() throws Exception {

		String eMessage = "";
		String subjectId = null;
		String fileId = null;
		DamsURI DamsURI = null;
		String use = null;
		for(int i=0; i<itemsCount; i++){

			boolean masterExists = false;
			boolean missing = false;
			count++;
			subjectId = items.get(i);
			try{
				setStatus("Processing file count validation for subject " + subjectId  + " (" + (i+1) + " of " + itemsCount + ") ... " ); 
				DFile dFile = null;
				int duSize = 0;
				List<DFile> files = damsClient.listObjectFiles(subjectId);
				for(Iterator<DFile> it=files.iterator(); it.hasNext();){
					filesTotal++;
					dFile = it.next();
					use = dFile.getUse();
					
					// Check file existence
					fileId = dFile.getId();
					DamsURI = DamsURI.toParts(fileId, subjectId);

					if(!damsClient.exists(DamsURI.getObject(), DamsURI.getComponent(), DamsURI.getFileName())){
						missingFilesCount++;
						missing = true;
						missingFiles.append(fileId + "\t" + (missingFilesCount%10==0?"\n":""));
						logError("File " + fileId + " doesn't exists.");
					}
					// Check source and alternate master files 
					if(use.endsWith(Constants.SOURCE) || use.endsWith(Constants.ALTERNATE)){
						masterTotal++;
						masterExists = true;
						List<DamsURI> duFiles = DAMSClient.getFiles(filesDoc, null, dFile.getSourceFileName());
						if((duSize=duFiles.size()) > 1){
							String[] checksums = new String[duSize];
							for(int j=0; j<duSize; j++){
								checksums[j] = getChecksum(duFiles.get(j));
							}
							
							// Check checksums for duplication
							boolean duplicated = false;
							String duItems = "";
							for(int j=0; j<duSize; j++){
								for(int k=j+1; k<duSize; k++){
									if(checksums[j].equals(checksums[k]) && duplicatedFiles.indexOf(duFiles.get(j).toString())<0){
										duItems += duFiles.get(j) + ", " + duFiles.get(k) + ", ";
										duplicated = true;
									}
								}
							}
							if(duplicated){
								failedCount++;
								duplicatedFiles.append(duItems.substring(0, duItems.length()-2) + "\n");
								logError("Duplicated files found: " + duItems);
							}
						}
					}
				}
				if(!masterExists || missing){
					failedCount++;
					if(!masterExists){
						missingObjectsCount++;
						missingObjects.append(subjectId + "\t" + (missingObjectsCount%10==0?"\n":""));
						logError("No master files exist: " + subjectId);
					}
				}
			} catch (Exception e) {
				failedCount++;
				e.printStackTrace();
				logError("File count validation failed: " + e.getMessage());
			}
			setProgressPercentage( ((i + 1) * 100) / itemsCount);
			
			try{
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				failedCount++;
    			logError("File count validation interrupted for subject " + subjectId  + ". Error: " + e1.getMessage() + ".");
				setStatus("Canceled");
				clearSession();
				break;
			}
		}
		
		return exeResult;
	}
	
	private String getChecksum(DamsURI damsURI){
		String checksum = "";
		String checkSumXPath = DAMSClient.DOCUMENT_RESPONSE_ROOT_PATH + "/files/value[id='" + damsURI.toString() + "']/crc32checksum";
		Node checksumNode = filesDoc.selectSingleNode(checkSumXPath);
		if(checksumNode != null)
			checksum = checksumNode.getText();
		return checksum;
	}
	
	/**
	 * Execution result message
	 */
	public String getExeInfo() {
		String missingObjectsMessage = " object" + (missingObjectsCount>1?"s have ":" has ") + " no master files";
		String missingFilesMessage = " file" + (missingFilesCount>1?"s are ":" is ") + " missing from " + damsClient.getFileStore();
		if(exeResult)
			exeReport.append("File count validation succeeded. \n ");
		else
			exeReport.append("File count validation (" + failedCount + " of " + itemsCount + " failed" + (missingObjectsCount>0?", " + missingObjectsCount + missingObjectsMessage:"") + (missingFilesCount>0?", " + missingFilesCount + missingFilesMessage:"") + "): \n ");	
		exeReport.append("Total files found " + filesTotal + ". \nNumber of objects found " + itemsCount + ". \nNumber of objects processed " + count  + ". \nNumber of source and alternate master files detected " + masterTotal + ".\n");
		if(duplicatedFiles.length() > 0)
			exeReport.append("\nThe following files are duplicated: \n" + duplicatedFiles.toString());
		
		if(missingObjects.length() > 0)
			exeReport.append("\nThe following object" + missingObjectsMessage + " : \n" + missingObjects.toString() );
		
		if(missingFiles.length() > 0)
			exeReport.append("\nThe following object" + missingFilesMessage + " : \n" + missingFiles.toString() );
		
		String exeInfo = exeReport.toString();
		log("log", exeInfo);
		return exeInfo;
	}
}
