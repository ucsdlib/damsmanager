package edu.ucsd.library.xdre.exports;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;

import edu.ucsd.library.xdre.collection.CollectionHandler;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.NameSpaceUtil;

/**
 * Class RDFMetadaExportHandler exports metadata for a collection in RDF format 
 *
 * @author lsitu@ucsd.edu
 */
public class NTriplesMetadataExportHandler extends CollectionHandler {
	
	private static Logger log = Logger.getLogger(NTriplesMetadataExportHandler.class);
	public static int MAX_ITEMS_LOAD = 500;
	private OutputStream out = null;
	private File outputFile = null;
	private List<String> namespaces = null;
	private boolean translated = false;
	private boolean componentsIncluded = false;;
	//private String nTriplesFile = null;

	public NTriplesMetadataExportHandler(File outputFile) throws Exception{
		this.outputFile = outputFile;
		this.out = new FileOutputStream(outputFile);
	}
	
	public NTriplesMetadataExportHandler(DAMSClient damsClient, String collectionId, File outputFile) throws Exception{
		super(damsClient, collectionId);
		this.outputFile = outputFile;
		this.out = new FileOutputStream(outputFile);
	}
	
	public boolean execute() throws Exception {
		int count = 0;
		int size = items.size();
		String subjectId = null;
		String rdfXml = null;
		String itemLink = null;
		
		InputStream in = null;
		Model model = null;
		List<String> nsLabels = new ArrayList<String>();
		OutputStream rdfOut = null;
		try{
			do{
				try{
					if(rdfOut == null)
						rdfOut = new ByteArrayOutputStream();
					startRDFDocument(rdfOut);
					for(int i=count; i<size; i++){
						subjectId = (String)items.get(i);
						itemLink = getDDOMReference(subjectId);
						setStatus("Preparing NTriples export for " + itemLink + " (" + (i+1) + " of " + size + ")");
						if(namespaces != null && namespaces.size()>0 && nsLabels.size() == 0){
							Map<String, String> nsMap = damsClient.getPredicates();
							String nsLabel = null;
							String nsPredicate = null;
							for(int j=0; j<namespaces.size(); j++){
								nsPredicate = namespaces.get(j);
								nsLabel = (String)nsMap.get(nsPredicate);
								nsLabel = (nsLabel==null?nsPredicate:nsLabel);
								nsLabels.add(nsLabel);
							}
						}
						rdfXml = damsClient.samePredicateExport(subjectId, nsLabels);
						if(rdfXml != null && rdfXml.length() > 0){
							rdfOut.write(Normalizer.normalize(rdfXml, Normalizer.Form.NFC).getBytes("UTF-8"));
						}
						
						setProgressPercentage( ((i+1) * 100) / size);
						count++;
						if(count % MAX_ITEMS_LOAD == 0)
							break;
					}
					endRDFDocument(rdfOut);
					model = ModelFactory.createDefaultModel();
					in = new ByteArrayInputStream(((ByteArrayOutputStream)rdfOut).toByteArray());
					model.read(in, Constants.DAMS_ARK_URL_BASE + Constants.ARK_ORG);
					model.write(out, "N-TRIPLE");
				}finally{
					if(in != null){
						try{
							in.close();
						}catch (IOException e){}
						in = null;
					}
					if(rdfOut != null){	
						try{
							rdfOut.close();
							((ByteArrayOutputStream)rdfOut).reset();
						}catch (IOException e){}
					}
				}
			}while(count < size);
			rdfOut = null;
		}catch (Exception e){
			exeResult = false;
			exeReport.append("\nInternal error: " + e.getMessage());
			throw e;
		}
		return exeResult;
	}

	public void startRDFDocument(OutputStream out) throws IOException{
		out.write("<?xml version=\"1.0\" encoding=\"UTF-8\"?>".getBytes("UTF-8"));
		if(translated)
			out.write("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns:mods=\"http://www.loc.gov/mods/v3\" xmlns:mix=\"http://www.loc.gov/mix/\" xmlns:pre=\"http://www.loc.gov/standards/premis/v1\" xmlns:rts=\"http://cosimo.stanford.edu/sdr/metsrights/\" xmlns:xdre=\"http://libraries.ucsd.edu/ark:/20775/bb3448106g/xdre.rdf#\" xmlns:file=\"http://libraries.ucsd.edu/dams/\" xmlns:mets=\"http://www.loc.gov/METS/\" xmlns=\"http://libraries.ucsd.edu/dams/xmlns/\">".getBytes("UTF-8"));
		else
			out.write("<rdf:RDF xmlns:rdf=\"http://www.w3.org/1999/02/22-rdf-syntax-ns#\" xmlns=\"http://libraries.ucsd.edu/ark:/20775/\">".getBytes("UTF-8"));
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
	
	public void endRDFDocument(OutputStream out) throws IOException{
		out.write("</rdf:RDF>".getBytes("UTF-8"));
	}
	
	public File getOutputFile(){
		return outputFile;
	}
	
	public String getExeInfo() {
		String exeInfo = "";
		if(exeResult)
			exeInfo += "\n" + "Your NTriples metadata file " + (collectionId==null?"":" for " + collectionTitle) + " is ready." + (outputFile!=null?" Please download it from <a href=\"" + Constants.CLUSTER_HOST_NAME.replace("http://", "https://").replace(":8080/", ":8443/") + "damsmanager/downloadLog.do?sessionId=" + session.getId() + "&file=" + outputFile.getName() + "\">here</a>.":"");
		else
			exeInfo = "NTriples exportation failed. Error: " + exeReport + "\n";
		return exeInfo;
	}
	
	public File getNTriplesFile(){
		return outputFile;
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