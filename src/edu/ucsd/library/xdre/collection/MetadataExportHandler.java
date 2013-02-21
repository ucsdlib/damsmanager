package edu.ucsd.library.xdre.collection;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.List;

import org.apache.log4j.Logger;
import org.dom4j.DocumentException;

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
	private boolean components = false;
	private int count = 0;
	private int failedCount = 0;
	private RDFStore rdfStore = null;
	private String format = null;
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

	/**
	 * Procedure to populate the RDF metadata
	 */
	public boolean execute() throws Exception {

		String subjectId = null;
		RDFStore iStore = null;
		for(int i=0; i<itemsCount; i++){
			count++;
			subjectId = items.get(i);
			try{
				setStatus("Processing metadata export for subject " + subjectId  + " (" + (i+1) + " of " + itemsCount + ") ... " ); 
				iStore =  new RDFStore();
				iStore.loadRDFXML(damsClient.getMetadata(subjectId, "xml"));
				// Implement the same predicates export
				if(predicates.size() > 0 || !components){
					trimStatements(iStore);
					//iStore.write(System.out, RDFStore.RDFXML_ABBREV_FORMAT);
				}
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
			iStore.excludeComponents();
		if(predicates.size() > 0)
			iStore.trimStatements(predicates);
	}

	/**
	 * Execution result message
	 */
	public String getExeInfo() {
		if(exeResult)
			exeReport.append("\n" + "RDF metadata " + (collectionId==null?"":" for " + getCollectionTitle()) + " is ready:");
		else
			exeReport.append("Metadata export failed (" + failedCount + " of " + count + " failed): \n ");	
		exeReport.append("Total items found " + itemsCount + ". Number of items processed " + count + ".\n");
		String exeInfo = exeReport.toString();
		logMessage(exeInfo);
		return exeInfo;
	}
}
