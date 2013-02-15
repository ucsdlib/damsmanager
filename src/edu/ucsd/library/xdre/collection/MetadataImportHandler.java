package edu.ucsd.library.xdre.collection;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DFile;
import edu.ucsd.library.xdre.utils.DamsURI;
import edu.ucsd.library.xdre.utils.RDFStore;

/**
 * 
 * MetadataImportHandler: Import metadata - single item or batch.
 * @author lsitu@ucsd.edu
 */
public class MetadataImportHandler extends CollectionHandler{
	private static Logger log = Logger.getLogger(MetadataImportHandler.class);

	//private Map subjectNSMap = null;
	private String rdf = null;
	private String format = null;
	private String importMode = null;
	private int count = 0;
	private int failedCount = 0;
	private RDFStore rdfStore = null;
	
	/**
	 * Constructor for MetadataImportHandler
	 * @param tsUtils
	 * @param rdfXml
	 * @param operation
	 * @throws Exception
	 */
	public MetadataImportHandler(DAMSClient damsClient, String rdf, String mode) throws Exception{
		this(damsClient, null, rdf, mode);
	}
	
	/**
	 * Constructor for MetadataImportHandler
	 * @param damsClient
	 * @param collectionId
	 * @param rdf
	 * @param mode
	 * @throws Exception
	 */
	public MetadataImportHandler(DAMSClient damsClient, String collectionId, String rdf, String mode) throws Exception{
		this(damsClient, null, rdf, null, mode);
	}
	
	/**
	 * Constructor for RDFLoadingHandler
	 * @param tsUtils
	 * @param collectionId
	 * @param rdfXml
	 * @param operation
	 * @throws Exception
	 */
	public MetadataImportHandler(DAMSClient damsClient, String collectionId, String rdf, String format, String importMode) throws Exception{
		super(damsClient, collectionId);
		this.rdf = rdf;
		this.importMode = importMode;
		initHandler();
	}
	
	private void initHandler() throws DocumentException, UnsupportedEncodingException, IOException{
		rdfStore = new RDFStore();
		if(format != null && format.equals(RDFStore.NTRIPLE_FORMAT))
			rdfStore.loadNTriples(rdf);
		else
			rdfStore.loadRDFXML(rdf);
		items = rdfStore.listURISubjects();
		itemsCount = items.size();
	}

	public void setImportMode(String importMode) {
		this.importMode = importMode;
	}
	
	/**
	 * Procedure to populate the RDF metadata
	 */
	public boolean execute() throws Exception {

		String eMessage;
		String subjectId = null;
		DFile dFile = null;
		for(int i=0; i<itemsCount; i++){
			count++;
			subjectId = items.get(i);
			try{
				setStatus("Processing metadata for subject " + subjectId  + " (" + (i+1) + " of " + itemsCount + ") ... " ); 
				String message = "";
				boolean succeeded = false;
				DamsURI objURI = DamsURI.toParts(subjectId, null);
				String rdfXml = rdfStore.exportSubject(subjectId, RDFStore.RDFXML_FORMAT);
				if(importMode != null){
					if(importMode.equals(Constants.IMPORT_MODE_SAMEPREDICATES)){
						if(objURI.isFileURI()){
							//Properties replacement for files. Need to regenerate it with fileCharacterize
							dFile = DFile.toDFile(rdfXml);
							List<DFile> dFiles = damsClient.listObjectFiles(objURI.getObject());
							DFile dFileTmp = null;
							for(Iterator<DFile> it=dFiles.iterator(); it.hasNext();){
								dFileTmp = it.next();
								if(subjectId.equals(dFileTmp)){
									//Update the properties that need replace.
									dFileTmp.updateValues(dFile);
									dFile = dFileTmp;
									break;
								}
							}
							succeeded = damsClient.updateFileCharacterize(objURI.getObject(), objURI.getComponent(), objURI.getFileName(), dFile.toNameValuePairs());
						}else{
							// Perform same predicates replacement
							List<String> preds = rdfStore.listPredicates(subjectId);
							succeeded = damsClient.selectiveMetadataDelete(objURI.getObject(), objURI.getComponent(), preds);
							if(succeeded)
								succeeded = damsClient.updateObject(objURI.getObject(), rdfXml, Constants.IMPORT_MODE_ADD);
						}
						
					}else if(importMode.equals(Constants.IMPORT_MODE_ALL)){
						DamsURI damsURI = DamsURI.toParts(subjectId, null);
						
						if(damsURI.isFileURI()){
							System.out.println("Regenerate technical metadata:" + rdfXml);
							// regenerate the technical metadata for files
							dFile = DFile.toDFile(rdfXml);
							// Replace descriptive metadata plus technical metadata for files
							succeeded = damsClient.updateFileCharacterize(damsURI.getObject(), damsURI.getComponent(), damsURI.getFileName(), dFile.toNameValuePairs());
						}else
							succeeded = damsClient.updateObject(subjectId, rdfXml, importMode);
					} else
						succeeded = damsClient.updateObject(subjectId, rdfXml, importMode);

				}else
					throw new Exception ("Import mode is required.");
					
				if(!succeeded){
					failedCount++;
					exeResult = false;
					String iMessage = "Metadata import for subject " + subjectId  + " failed ";
					iMessage += "(" + (i+1) + " of " + itemsCount + "): ";
					setStatus( iMessage + message.replace("\n", "<br/>")); 
					log("log", iMessage + message);
					log.info(iMessage + message);
				}else{
					String iMessage = "Metadata import for subject " + subjectId  + " succeeded (" + (i+1) + " of " + itemsCount + "): ";
					setStatus( iMessage + message.replace("\n", "<br/>")); 
					log("log", iMessage + message);
					log.info(iMessage + message);
				}
			
			} catch (Exception e) {
				failedCount++;
				e.printStackTrace();
				exeResult = false;
				eMessage = "Metadata import failed: " + e.getMessage();
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
    			eMessage = "Metadata population interrupted for subject " + subjectId  + ". \n Error: " + e1.getMessage() + "\n";
				setStatus("Canceled");
				clearSession();
				log("log", eMessage.replace("\n", ""));
				log.info(eMessage, e1);
				break;
			}
		}
		return exeResult;
	}

	/**
	 * Execution result message
	 */
	public String getExeInfo() {
		if(exeResult)
			exeReport.append("Metadata import succeeded. \n ");
		else
			exeReport.append("Metadata import failed (" + failedCount + " of " + count + " failed): \n ");	
		exeReport.append("Total items found " + itemsCount + ". Number of items processed " + count + ".\n");
		String exeInfo = exeReport.toString();
		log("log", exeInfo);
		return exeInfo;
	}
}
