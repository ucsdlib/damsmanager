package edu.ucsd.library.xdre.tab;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;

import javax.xml.transform.TransformerException;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;

public class ModsRecord implements Record {
    protected String id;
    protected Document rdf;
	
	public ModsRecord(File xsl, File xml, String[] collectionURIs, String unitURI, 
			String copyrightStatus, String copyrightJurisdiction, String copyrightOwner,
			String program, String access, String endDate) throws ParseException, IOException, DocumentException, TransformerException {
		XsltSource xsltSource = new XsltSource( xsl, xml );
		Record record = xsltSource.nextRecord();
		this.id = record.recordID();
		this.rdf = record.toRDFXML();
		
		//Assign ID for component and files with symbols /CID or /FID 
		assignIDs();
		RecordUtil.addRights(rdf, unitURI, collectionURIs, copyrightStatus, copyrightJurisdiction, copyrightOwner, program, access, endDate);
	}
	
	public ModsRecord(File xsl, InputStream in, String sourceID, String[] collectionURIs, String unitURI, 
			String copyrightStatus, String copyrightJurisdiction, String copyrightOwner,
			String program, String access, String endDate) throws ParseException, IOException, DocumentException, TransformerException {
		XsltSource xsltSource = new XsltSource( xsl, sourceID, in );
		Record record = xsltSource.nextRecord();
		this.id = record.recordID();
		this.rdf = record.toRDFXML();
		
		//Assign component ID and file ID for symbols CID, FID 
		assignIDs();
		RecordUtil.addRights(rdf, unitURI, collectionURIs, copyrightStatus, copyrightJurisdiction, copyrightOwner, program, access, endDate);
	}

	public void addRights (String unitURI, String[] collectionURIs,
	        String copyrightStatus, String copyrightJurisdiction, String copyrightOwner,
	        String program, String access, String endDate){
		RecordUtil.addRights(rdf, unitURI, collectionURIs, copyrightStatus, copyrightJurisdiction, copyrightOwner, program, access, endDate);
	}

	@Override
	public String recordID() {
		return id;
	}

	@Override
	public Document toRDFXML() throws ParseException {
		return rdf;
	}
	
	/*
	 * Assign component ID and file ID.
	 */
	private void assignIDs() {
		int comSize = rdf.selectNodes("//dams:Component/@rdf:about").size();
		List<Node> nodes = rdf.selectNodes("//dams:Component/@rdf:about[contains(., '/CID')]");
		int cid = comSize - nodes.size() + 1;
		for (int j=0; j<nodes.size();j++) {
			Node rdfAbout = nodes.get(j);
			//Assigned CID for components: http://library.ucsd.edu/ark:/20775/OID/CID
			rdfAbout.setText(rdfAbout.getStringValue().replace("/CID", "/" + cid));
			Node cNode = rdfAbout.getParent();
			//Assigned FID for files
			List<Node> fNodes = cNode.selectNodes("dams:hasFile/dams:File/@rdf:about");
			for (int k=0; k<fNodes.size(); k++) {
				Node fidNode = fNodes.get(k);
				String fid = fidNode.getStringValue();
				if (fid.endsWith("/FID")) {
					String fileExt = "";
					Node fileNameNode = fidNode.getParent().selectSingleNode("dams:sourceFileName");
					if (fileNameNode != null) {
						String fileName = fileNameNode.getText().trim();
						if (fileName.indexOf(".") > 0) {
							fileExt = fileName.substring(fileName.indexOf("."));
						}
					}
					
					// $oid/CID/FID | $oid/FID
					if (fid.indexOf("/CID") > 0 && fid.indexOf("/FID") > 0) {
						fid = fid.replace("/CID", "/" + cid + "").replace("/FID", "/" + (k + 1) + fileExt);
					} else {
						fid = fid.replace("/FID", "/" + cid + "/" + (k + 1) + fileExt);
					}
					fidNode.setText(fid);
				}
			}

			cid += 1;
		} 
	}
}