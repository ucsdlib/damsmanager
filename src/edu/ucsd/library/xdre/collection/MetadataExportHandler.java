package edu.ucsd.library.xdre.collection;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;

import com.hp.hpl.jena.rdf.model.Resource;

import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.RDFStore;

/**
 * 
 * MetadataExportHandler: export metadata for a single item or in batch
 * with/without predicates limitation
 * @author lsitu@ucsd.edu
 */
public class MetadataExportHandler extends CollectionHandler{
	private static Logger log = Logger.getLogger(MetadataExportHandler.class);

	//private Map subjectNSMap = null;
	private List<String> predicates = null;
	private boolean components = true;
	private int count = 0;
	private int failedCount = 0;
	private RDFStore rdfStore = null;
	private String format = null;
	private String fileUri = null;
	private OutputStream out = null;

	/**
	 * Constructor for MetadataExportHandler
	 * @param damsClient
	 * @throws Exception
	 */
	public MetadataExportHandler(DAMSClient damsClient, OutputStream out) throws Exception{
		this(damsClient, null, RDFStore.RDFXML_FORMAT, out);
	}
	
	/**
	 * Constructor for MetadataExportHandler
	 * @param damsClient
	 * @throws Exception
	 */
	public MetadataExportHandler(DAMSClient damsClient, List<String> items, String format, OutputStream out) throws Exception{
		this(damsClient, null, null, false, format, out);
		this.items = items;
	}
	
	/**
	 * Constructor for MetadataExportHandler
	 * @param damsClient
	 * @param collectionId
	 * @param predicates
	 * @param components
	 * @throws Exception
	 */
	public MetadataExportHandler(DAMSClient damsClient, String collectionId, List<String> predicates, boolean components, String format, OutputStream out) throws Exception{
		super(damsClient, collectionId);
		this.predicates = predicates;
		this.components = components;
		this.format = format;
		this.out = out;
		initHandler();
	}
	
	private void initHandler() throws DocumentException, UnsupportedEncodingException, IOException{
		rdfStore = new RDFStore();
	}

	public String getFileUri() {
		return fileUri;
	}

	public void setFileUri(String fileUri) {
		this.fileUri = fileUri;
	}

	public boolean isComponents() {
		return components;
	}

	/**
	 * Set to true to include components descriptive metadata in the export.
	 */
	public void setComponents(boolean components) {
		this.components = components;
	}

	/**
	 * Procedure to populate the RDF metadata
	 */
	public boolean execute() throws Exception {

		String subjectId = null;
		RDFStore iStore = null;
		Resource res = null;
		for(int i=0; i<itemsCount; i++){
			count++;
			subjectId = items.get(i);
			try{
				setStatus("Processing metadata export for subject " + subjectId  + " (" + (i+1) + " of " + itemsCount + ") ... " ); 
				iStore =  new RDFStore();
				iStore.loadRDFXML(damsClient.getMetadata(subjectId, "xml"));
				// Same predicates export
				if(predicates.size() > 0 || !components){
					trimStatements(iStore);
					// Merge the resources
					List<Resource> resIds = iStore.listURIResources();
					for(Iterator<Resource> it=resIds.iterator(); it.hasNext();){
						res = it.next();
						rdfStore.merge(iStore.querySubject(res.getURI()));
					}
				}else
					rdfStore.mergeObjects(iStore);
				
				logMessage("Exported metadata for subject " + subjectId + ".");
			} catch (Exception e) {
				failedCount++;
				e.printStackTrace();
				logError("Metadata export failed (" +(i+1)+ " of " + itemsCount + "): " + e.getMessage());
			}
			setProgressPercentage( ((i + 1) * 100) / itemsCount);

			try{
				Thread.sleep(10);
			} catch (InterruptedException e1) {
				failedCount++;
    			logError("Metadata export interrupted for subject " + subjectId  + ". Error: " + e1.getMessage() + ".");
				setStatus("Canceled");
				clearSession();
				break;
			}
		}
		
		rdfStore.write(out, format);
		return exeResult;
	}
	
	/**
	 * Remove the unwanted statements
	 * @throws Exception
	 */
	public void trimStatements(RDFStore iStore) throws Exception{
		if(!components)
			iStore.excludeComponentsAndFiles();
		if(predicates.size() > 0)
			iStore.trimStatements(predicates);
	}

	/**
	 * Execution result message
	 */
	public String getExeInfo() {
		exeReport.append((format.startsWith("RDF/XML")?"RDF/XML":format) + " metadata export ");
		if(exeResult)
			exeReport.append((collectionId==null?"":" for " + getCollectionTitle()) + " is ready" + (fileUri!=null?" for <a href=\"" + fileUri + "\">download</a>":"") + ":\n");
		else
			exeReport.append("failed (" + failedCount + " of " + count + " failed): \n ");	
		exeReport.append("- Total items found " + itemsCount + ". \n- Number of items processed " + count + ".");
		String exeInfo = exeReport.toString();
		logMessage(exeInfo);
		return exeInfo;
	}
}
