package edu.ucsd.library.xdre.tab;

import java.io.File;
import java.io.InputStream;
import java.text.ParseException;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.QName;

/**
 * A Marc/Mods record constructed from the Roger/AT form
 * @author lsitu
 * Since Sep. 25, 2014 
 */
public class ModsRecord implements Record {
    protected String id;
    protected Document rdf;
	
	public ModsRecord(File xsl, File xml, Map<String, String> collections, String unitURI, 
			String copyrightStatus, String copyrightJurisdiction, String copyrightOwner,
			String program, String access, String beginDate, String endDate) 
					throws Exception {
		XsltSource xsltSource = new XsltSource( xsl, xml );
		Record record = xsltSource.nextRecord();
		this.id = record.recordID();
		this.rdf = record.toRDFXML();
		
		//Assign ID for component and files with symbols /CID or /FID 
		assignIDs();
		RecordUtil.addRights(rdf, unitURI, collections, copyrightStatus, copyrightJurisdiction,
				copyrightOwner, program, access, beginDate, endDate);
	}
	
	public ModsRecord(File xsl, InputStream in, String sourceID, Map<String, String> collections, String unitURI, 
			String copyrightStatus, String copyrightJurisdiction, String copyrightOwner,
			String program, String access, String beginDate, String endDate) throws Exception {
		XsltSource xsltSource = new XsltSource( xsl, sourceID, in );
		Record record = xsltSource.nextRecord();
		this.id = record.recordID();
		this.rdf = record.toRDFXML();
		
		//Assign component ID and file ID for symbols CID, FID 
		assignIDs();
		RecordUtil.addRights(rdf, unitURI, collections, copyrightStatus, copyrightJurisdiction, 
				copyrightOwner, program, access, beginDate, endDate);
	}

	public void addRights (String unitURI, Map<String, String> collections,
	        String copyrightStatus, String copyrightJurisdiction, String copyrightOwner,
	        String program, String access, String beginDate, String endDate){
		RecordUtil.addRights(rdf, unitURI, collections, copyrightStatus, copyrightJurisdiction, 
				copyrightOwner, program, access, beginDate, endDate);
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
		
		// assigned file id for simple files
		List<Node> fNodes = rdf.selectNodes("//dams:hasFile/dams:File/@rdf:about[contains(., '/FID')]");
		for (int k=0; k<fNodes.size(); k++) {
			Node fidNode = fNodes.get(k);
			String fid = fidNode.getStringValue();
			String fileExt = "";
			Node fileNameNode = fidNode.getParent().selectSingleNode("dams:sourceFileName");
			if (fileNameNode != null) {
				String fileName = fileNameNode.getText().trim();
				if (fileName.indexOf(".") > 0) {
					fileExt = fileName.substring(fileName.indexOf("."));
				}
			}
			fid = fid.replace("/FID", "/" + (k + 1) + fileExt);			
			fidNode.setText(fid);
		}
	}
	
	/**
	 * Adding master file(s) for bib/Roger records: a PDF, a TIFF, or a PDF + a ZIP file 
	 * @param cid
	 * @param files
	 * @param fileUseMap
	 */
	public void addFiles(int cid, List<File> files, Map<String, String> fileUseMap) {
		Element o = (Element) rdf.selectSingleNode("//dams:Object");
		String objUri = o.selectSingleNode("@rdf:about").getStringValue();
		for (int i = 0; i< files.size(); i++) {
			String fileName = files.get(i).getName();
			String fileUse = fileUseMap.get(fileName);
			int idx = fileName.indexOf(".");
			String fileUri = objUri + "/" + (cid == 0 ? cid + "/" : "") + (i + 1) + (idx > 0 ? fileName.substring(fileName.indexOf(".")) : "");
			Element f = o.addElement("dams:hasFile").addElement("dams:File");
			f.addAttribute(new QName("about",  rdf.getRootElement().getNamespaceForPrefix("rdf")), fileUri);
			f.addElement("dams:sourceFileName").setText(fileName);
			if (!StringUtils.isBlank(fileUse)) {
				f.addElement("dams:use").setText(fileUse);
			}
		}
	}
}
