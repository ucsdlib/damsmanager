package edu.ucsd.library.xdre.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;

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
	private Map<String, List<DamsURI>> objects = null;
	
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
	
	private void initHandler() throws Exception{
		objects = new HashMap<String, List<DamsURI>>();
		rdfStore = new RDFStore();
		if(format != null && format.equalsIgnoreCase(RDFStore.NTRIPLE_FORMAT))
			rdfStore.loadNTriples(rdf);
		else
			rdfStore.loadRDFXML(rdf);
		
		List<String> sItems = rdfStore.listURISubjects();
		Collections.sort(sItems);
		int iSize = sItems.size();
		
		// Object list
		String objId = null;
		DamsURI objURI = null;
		List<DamsURI> objURIs = null;
		for(int i=0; i<iSize; i++){
			objURI = DamsURI.toParts(sItems.get(i), null);
			objId = objURI.getObject();
			objURIs = objects.get(objId);
			if(objURIs == null){
				objURIs = new ArrayList<DamsURI>();
				objects.put(objId , objURIs);
			}
			objURIs.add(objURI);
		}
		items = Arrays.asList(objects.keySet().toArray(new String[objects.size()]));
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
		DamsURI objURI = null;
		List<DamsURI> objURIs = null;
		Model rdf = null;
		String rdfXml = null;
		for(int i=0; i<itemsCount; i++){
			count++;
			subjectId = items.get(i);
			try{
				setStatus("Processing metadata for subject " + subjectId  + " (" + (i+1) + " of " + itemsCount + ") ... " ); 
				String message = "";
				boolean succeeded = false;
				objURIs = objects.get(subjectId);
				//DamsURI objURI = DamsURI.toParts(subjectId, null);

				if (importMode != null){
					RDFStore graph = new RDFStore();
					for(int j=0; j<objURIs.size(); j++){
						objURI = objURIs.get(j);
						rdf = rdfStore.querySubject(objURI.toString());
						if(importMode.equals(Constants.IMPORT_MODE_SAMEPREDICATES)){
							//objURI = objURIs.get(j);
							rdfXml = new RDFStore(rdf).export(RDFStore.RDFXML_FORMAT);
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
							
						} else if(importMode.equals(Constants.IMPORT_MODE_DESCRIPTIVE)){
							DamsURI damsURI = null;
							if(objURI.isFileURI())
								throw new Exception("File characterize are not allowed for descriptive metadata import: " + subjectId);
							// XXX
							// Replace descriptive metadata, need to regenerate the technical metadata for files ???
							List<DFile> dFiles = damsClient.listObjectFiles(subjectId);
							succeeded = damsClient.updateObject(subjectId, rdfXml, Constants.IMPORT_MODE_ALL);
							// regenerate the technical metadata for files
							if(succeeded){							
								for(Iterator<DFile> it=dFiles.iterator(); succeeded && it.hasNext();){
									dFile = it.next();
									damsURI = DamsURI.toParts(dFile.getId(), subjectId);
									succeeded = damsClient.updateFileCharacterize(damsURI.getObject(), damsURI.getComponent(), damsURI.getFileName(), dFile.toNameValuePairs());
								}
							}
						} else if(importMode.equals(Constants.IMPORT_MODE_ALL)){
							String objId = objURI.getObject();
							
							// Object replacement: Need object metadata.
							if(objURIs.get(0).toString().equals(objId)){
								graph.merge(rdf);
							}else{
								message += "Unsupported metadata update mode all for subject " + objURI + ". Please use add or same predicate replacement instead."; 
								break;
							}
							if(j == objURIs.size()-1){
								succeeded = damsClient.updateObject(subjectId, graph.export(RDFStore.RDFXML_ABBREV_FORMAT), importMode);
							}
						} else if(importMode.equals(Constants.IMPORT_MODE_ADD)) {
							graph.merge(rdf);
							if(j == objURIs.size()-1)
								succeeded = damsClient.updateObject(subjectId, graph.export(RDFStore.RDFXML_ABBREV_FORMAT), importMode);
						} else 
							throw new Exception ("Unhandled import mode for metadata import: " + importMode + ".");
					}
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
