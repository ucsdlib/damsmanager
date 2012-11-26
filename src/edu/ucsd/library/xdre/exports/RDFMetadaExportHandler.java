package edu.ucsd.library.xdre.exports;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * Class RDFMetadaExportHandler exports metadata for a collection in RDF format 
 *
 * @author lsitu@ucsd.edu
 */
public class RDFMetadaExportHandler extends edu.ucsd.library.xdre.collection.CollectionHandler {
	
	private static Logger log = Logger.getLogger(RDFMetadaExportHandler.class);
	private OutputStream out = null;
	private File outputFile = null;
	private List<String> namespaces = null;
	private boolean translated = false;
	private boolean componentsIncluded = false;

	public RDFMetadaExportHandler(File outputFile) throws Exception{
		this.outputFile = outputFile;
		this.out = new FileOutputStream(outputFile);
	}
	
	public RDFMetadaExportHandler(DAMSClient damsClient, String collectionId, File outputFile) throws Exception{
		super(damsClient, collectionId);
		this.outputFile = outputFile;
		this.out = new FileOutputStream(outputFile);
	}
	
	public RDFMetadaExportHandler(DAMSClient damsClient, String collectionId, OutputStream out) throws Exception{
		super(damsClient, collectionId);
		this.out = out;
	}
	
	public boolean execute() throws Exception {
		int size = items.size();
		String subjectId = null;
		String rdfXml = null;
		String itemLink = null;
		startRDFDocument();
		try{
			for(int i=0; i<size; i++){
				subjectId = (String)items.get(i);
				itemLink = getDDOMReference(subjectId);
				setStatus("Preparing rdf export for " + itemLink + " (" + (i+1) + " of " + itemsCount + ")");
				rdfXml = damsClient.getMetadata(subjectId, DAMSClient.DataFormat.rdf.toString());
				if(rdfXml == null || rdfXml.length() == 0){
					rdfXml = "<rdf:Description rdf:about=\"http://libraries.ucsd.edu/ark:/20775/" + subjectId + "\" />";
					log("log", "Subject not found: " + subjectId);
				}else
					log("log", "Exported metadata for subject: " + subjectId);
				out.write(rdfXml.getBytes("UTF-8"));
				setProgressPercentage( ((i+1) * 100) / itemsCount);
				
	        	try{	        		
	        		Thread.sleep(10);
	        	} catch (InterruptedException e) {
	        		interrupted = true;
	        		setExeResult(false);

	    			String eMessage = "RDF export canceled on " + itemLink + " ( " + i + " of " + itemsCount + ").";
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
		}
		return true;
	}

	public void startRDFDocument() throws IOException{
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n".getBytes("UTF-8"));
		if(translated)
			out.write("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:mods=\"http://www.loc.gov/mods/v3\" xmlns:mix=\"http://www.loc.gov/mix/\" xmlns:pre=\"http://www.loc.gov/standards/premis/v1\" xmlns:rts=\"http://cosimo.stanford.edu/sdr/metsrights/\" xmlns:xdre=\"http://libraries.ucsd.edu/ark:/20775/bb3448106g/xdre.rdf#\" xmlns:file=\"http://libraries.ucsd.edu/dams/\" xmlns:mets=\"http://www.loc.gov/METS/\" xmlns=\"http://libraries.ucsd.edu/dams/xmlns/\">\n".getBytes("UTF-8"));
		else
			out.write("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns=\"http://libraries.ucsd.edu/ark:/20775/\">\n".getBytes("UTF-8"));
	}
	
	public void releaseResource(){
		try {
			super.releaseResource();
			if(outputFile!=null && out != null)
				close(out);
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
		String exeInfo = "\n" + "RDF metadata " + (collectionId==null?"":" for " + collectionTitle) + " is ready." + (outputFile!=null?" Please download it from <a href=\"" + Constants.CLUSTER_HOST_NAME.replace("http://", "https://").replace(":8080/", ":8443/") + "damsmanager/downloadLog.do?sessionId=" + session.getId() + "&file=" + outputFile.getName() + "\">here</a>.":"");
		return exeInfo;
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
