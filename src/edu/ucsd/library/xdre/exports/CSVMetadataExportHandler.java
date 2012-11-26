package edu.ucsd.library.xdre.exports;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.collection.CollectionHandler;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * Class  CSVMetadaExportHandler exports metadata for a collection in CSV format 
 *
 * @author lsitu@ucsd.edu
 */
public class CSVMetadataExportHandler extends CollectionHandler {
	
	private static Logger log = Logger.getLogger(CSVMetadataExportHandler.class);
	private OutputStream out = null;
	private File outputFile = null;
	private List<String> namespaces = null;
	private boolean translated = false;
	private boolean componentsIncluded = false;
	private String xslSource = null;
	private String csvFile = null;

	public CSVMetadataExportHandler(DAMSClient damsClient, File outputFile, String xslSource) throws Exception{
		this(damsClient, null, outputFile, xslSource);
	}
	
	public CSVMetadataExportHandler(DAMSClient damsClient, String collectionId, File outputFile, String xslSource) throws Exception{
		super(damsClient, collectionId);
		this.outputFile = outputFile;
		this.xslSource = xslSource;
		this.out = new FileOutputStream(outputFile);
	}
	
	public boolean execute() throws Exception {

		String subjectId = null;
		String rdfXml = null;
		String itemLink = null;
		startRDFDocument();
		try{
			if(collectionId != null && collectionId.length() == 10)
				items.add(0, collectionId);
			int size = items.size();
			for(int i=0; i<size; i++){
				subjectId = (String)items.get(i);
				itemLink = getDDOMReference(subjectId);
				setStatus("Preparing CSV export for " + itemLink + " (" + (i+1) + " of " + size + ")");
				rdfXml = damsClient.getMetadata(subjectId, DAMSClient.DataFormat.rdf.toString());
				if(rdfXml == null || rdfXml.length() == 0){
					rdfXml = "<rdf:Description rdf:about=\"http://libraries.ucsd.edu/ark:/20775/" + subjectId + "\" />";
					log("log", "Subject not found: " + subjectId);
				}else{
					log("log", "Exported metadata for subject: " + subjectId);
				}

				out.write(rdfXml.getBytes("UTF-8"));
				setProgressPercentage( ((i+1) * 100) / size);
				
	        	try{	        		
	        		Thread.sleep(10);
	        	} catch (InterruptedException e) {
	        		interrupted = true;
	        		setExeResult(false);

	    			String eMessage = "CSV export canceled on " + itemLink + " ( " + i + " of " + itemsCount + ").";
	    			String iMessagePrefix = "Derivative creation interrupted with ";
					System.out.println(iMessagePrefix + eMessage);
					setStatus("Canceled");
					clearSession();
					log("log", iMessagePrefix + eMessage);
					log.info(iMessagePrefix + eMessage, e);
				}
			}
		}finally{
			endRDFDocument();
			if(outputFile!=null && out != null){
				close(out);
				out = null;
			}
		}
		
		String fileName = outputFile.getAbsolutePath();
		csvFile = outputFile.getParent() + File.separatorChar + (collectionId!=null?collectionId:items.get(0)) + "-" + (System.currentTimeMillis()/1000)  + ".csv";
		transformCSV(csvFile, fileName, xslSource, collectionId);
		return true;
	}

	public void startRDFDocument() throws IOException{
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes("UTF-8"));
		if(translated)
			out.write("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:mods=\"http://www.loc.gov/mods/v3\" xmlns:mix=\"http://www.loc.gov/mix/\" xmlns:pre=\"http://www.loc.gov/standards/premis/v1\" xmlns:rts=\"http://cosimo.stanford.edu/sdr/metsrights/\" xmlns:xdre=\"http://libraries.ucsd.edu/ark:/20775/bb3448106g/xdre.rdf#\" xmlns:file=\"http://libraries.ucsd.edu/dams/\" xmlns:mets=\"http://www.loc.gov/METS/\" xmlns=\"http://libraries.ucsd.edu/dams/xmlns/\">".getBytes("UTF-8"));
		else
			out.write("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns=\"http://libraries.ucsd.edu/ark:/20775/\">".getBytes("UTF-8"));
	}
	
	public void releaseResource(){
		try {
			super.releaseResource();
		} catch (Exception e1) {
			e1.printStackTrace();
		}finally{
			out = null;
		}
	}
	
	public void endRDFDocument() throws IOException{
		out.write("</rdf:RDF>".getBytes("UTF-8"));
	}
	
	public File getOutputFile(){
		return outputFile;
	}
	
	public String getExeInfo() {
		File csvFileObj = new File(csvFile);
		String csvFileName = csvFileObj.getName();
		String exeInfo = "\n" + "Your .csv metadata file " + (collectionId==null?"":" for " + collectionTitle) + " is ready." + (outputFile!=null?" Please download it from <a href=\"" + Constants.CLUSTER_HOST_NAME.replace("http://", "https://").replace(":8080/", ":8443/") + "damsmanager/downloadLog.do?sessionId=" + session.getId() + "&file=" + csvFileName + "\">here</a>.":"");
		return exeInfo;
	}
	
	public File getCSVFile(){
		return new File(csvFile);
	}

	public List<String> getNamespaces() {
		return namespaces;
	}

	public void setNamespaces(List<String> namespaces) {
		this.namespaces = namespaces;
	}

	public boolean isTranslated() {
		return translated;
	}

	public void setTranslated(boolean translated) {
		this.translated = translated;
	}

	public boolean isComponentsIncluded() {
		return componentsIncluded;
	}

	public void setComponentsIncluded(boolean componentsIncluded) {
		this.componentsIncluded = componentsIncluded;
	}

}
