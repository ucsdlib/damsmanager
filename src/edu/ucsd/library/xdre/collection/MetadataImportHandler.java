package edu.ucsd.library.xdre.collection;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

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
	protected String rdf = null;
	protected String format = null;
	protected String importMode = null;
	protected int count = 0;
	protected int failedCount = 0;
	protected RDFStore rdfStore = null;
	protected Map<String, List<DamsURI>> objects = null;
	
	/**
	 * Constructor for MetadataImportHandler
	 * @param tsUtils
	 * @param rdfXml
	 * @param operation
	 * @throws Exception
	 */
	public MetadataImportHandler(DAMSClient damsClient, String rdf, String importMode) throws Exception{
		this(damsClient, null, rdf, importMode);
	}
	
	/**
	 * Constructor for MetadataImportHandler
	 * @param damsClient
	 * @param collectionId
	 * @param rdf
	 * @param mode
	 * @throws Exception
	 */
	public MetadataImportHandler(DAMSClient damsClient, String collectionId, String rdf, String importMode) throws Exception{
		this(damsClient, null, rdf, null, importMode);
	}
	
	/**
	 * Constructor for MetadataImportHandler
	 * @param damsClient
	 * @param collectionId
	 * @param rdf
	 * @param format
	 * @param importMode
	 * @throws Exception
	 */
	public MetadataImportHandler(DAMSClient damsClient, String collectionId, String rdf, String format, String importMode) throws Exception{
		super(damsClient, collectionId);
		this.rdf = rdf;
		this.importMode = importMode;
		rdfStore = new RDFStore();
		if(format != null && format.equalsIgnoreCase(RDFStore.NTRIPLE_FORMAT))
			rdfStore.loadNTriples(rdf);
		else
			rdfStore.loadRDFXML(rdf);
		
		initHandler();
	}
	
	/**
	 * Constructor for MetadataImportHandler
	 * @param damsClient
	 * @param collectionId
	 * @param rdf
	 * @param format
	 * @param importMode
	 * @throws Exception
	 */
	public MetadataImportHandler(DAMSClient damsClient, String collectionId, RDFStore rdfStore, String importMode) throws Exception{
		super(damsClient, collectionId);
		this.rdfStore = rdfStore;
		this.importMode = importMode;
		initHandler();
	}
	
	protected void initHandler() throws Exception{
		objects = new HashMap<String, List<DamsURI>>();
		
		if(rdfStore != null){
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
		}else
			rdfStore = new RDFStore();
		
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
		DamsURI objURI = null;
		List<DamsURI> objURIs = null;
		Model rdf = null;
		Statement stmt = null;
		for(int i=0; i<itemsCount; i++){
			count++;
			subjectId = items.get(i);
			try{
				setStatus("Processing metadata for subject " + subjectId  + " (" + (i+1) + " of " + itemsCount + ") ... " ); 
				String message = "";
				boolean succeeded = false;
				objURIs = objects.get(subjectId);

				RDFStore rdfStoreOrgin = null;
				if(importMode.equals(Constants.IMPORT_MODE_SAMEPREDICATES) || importMode.equals(Constants.IMPORT_MODE_DESCRIPTIVE)){
					rdfStoreOrgin = new RDFStore();
					rdfStoreOrgin.loadRDFXML(damsClient.getMetadata(subjectId, "xml"));
				}
				
				RDFStore graph = new RDFStore();
				for(int j=0; j<objURIs.size(); j++){
					objURI = objURIs.get(j);
					rdf = rdfStore.querySubject(objURI.toString());
					if(importMode.equals(Constants.IMPORT_MODE_SAMEPREDICATES)){
						List<String> preds = rdfStore.listPredicates(objURI.toString());
						if(objURI.isFileURI()){
							//Properties replacement for files.
							Model iModel = rdfStoreOrgin.querySubject(objURI.toString());
							DFile dFileOrig = DFile.toDFile(iModel.listStatements().toList());
							StmtIterator sIt = rdf.listStatements();
							JSONObject props = new JSONObject();
							while(sIt.hasNext()){
								stmt = sIt.next();
								// Properties for update
								if(stmt.getObject().isLiteral())
									props.put(stmt.getPredicate().getLocalName(), stmt.getLiteral().getString());
							}
							dFileOrig.updateProperties(props);
							succeeded = damsClient.updateFileCharacterize(objURI.getObject(), objURI.getComponent(), objURI.getFileName(), dFileOrig.toNameValuePairs());
							if(!succeeded){
								message += "Failed to update file properties for subject " + objURI + ".\n"; 
								break;
							}
						}else{
							// Perform selective delete for same predicates replacement								
							succeeded = damsClient.selectiveMetadataDelete(objURI.getObject(), objURI.getComponent(), preds);
							if(succeeded){
								graph.merge(rdf);
							}else{
								message += "Failed to perform selective delete for subject " + objURI + ".\n"; 
								break;
							}	
						}
						
					} else if(importMode.equals(Constants.IMPORT_MODE_DESCRIPTIVE)){
						// Add subject
						graph.merge(rdf);
						if(j == objURIs.size() - 1){
							// Adding the file properties
							List<String> subjects = rdfStoreOrgin.listURISubjects();
							for(Iterator<String> it=subjects.iterator(); it.hasNext();){
								objURI = DamsURI.toParts(it.next(), subjectId);
								if(objURI.isFileURI()){
									graph.merge(rdfStoreOrgin.querySubject(objURI.toString()));
								}
							}
						}
					} else if(importMode.equals(Constants.IMPORT_MODE_ALL)){
						String objId = objURI.getObject();
						
						// Need object metadata for object replacement.
						if(objURIs.get(0).toString().equals(objId)){
							graph.merge(rdf);
						}else{
							message += "Unsupported metadata update mode all for subject " + objURI + ". Please use add or same predicate replacement instead.\n"; 
							break;
						}

					} else if(importMode.equals(Constants.IMPORT_MODE_ADD)) {
						// Add subject
						graph.merge(rdf);
					} else 
						throw new Exception ("Unhandled import mode for metadata import: " + importMode + ".");
				}
				
				
				// Update object
				if( message.length() == 0 ){
					if(importMode.equals(Constants.IMPORT_MODE_SAMEPREDICATES))
						succeeded = damsClient.updateObject(subjectId, graph.export(RDFStore.RDFXML_ABBREV_FORMAT), Constants.IMPORT_MODE_ADD);
					else if(importMode.equals(Constants.IMPORT_MODE_DESCRIPTIVE))
						succeeded = damsClient.updateObject(subjectId, graph.export(RDFStore.RDFXML_ABBREV_FORMAT), Constants.IMPORT_MODE_ALL);
					else
						succeeded = damsClient.updateObject(subjectId, graph.export(RDFStore.RDFXML_ABBREV_FORMAT), importMode);
				}else
					succeeded = false;
					
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
