package edu.ucsd.library.xdre.tab;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;

import javax.xml.transform.TransformerException;

import org.dom4j.Document;
import org.dom4j.DocumentException;

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
		RecordUtil.addRights(rdf, unitURI, collectionURIs, copyrightStatus, copyrightJurisdiction, copyrightOwner, program, access, endDate);
	}
	
	public ModsRecord(File xsl, InputStream in, String sourceID, String[] collectionURIs, String unitURI, 
			String copyrightStatus, String copyrightJurisdiction, String copyrightOwner,
			String program, String access, String endDate) throws ParseException, IOException, DocumentException, TransformerException {
		XsltSource xsltSource = new XsltSource( xsl, sourceID, in );
		Record record = xsltSource.nextRecord();
		this.id = record.recordID();
		this.rdf = record.toRDFXML();
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
}
