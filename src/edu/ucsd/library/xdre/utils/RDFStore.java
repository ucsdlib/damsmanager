package edu.ucsd.library.xdre.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;

import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * JENA RDFStore
 * @author lsitu
 *
 */
public class RDFStore {
	private Model rdfModel = null;
	public RDFStore(){
		rdfModel = ModelFactory.createDefaultModel();
	}
	
	/**
	 * Create a Statement
	 * @param subject
	 * @param predicate
	 * @param object
	 * @param isLiteral
	 * @return
	 */
	public Statement createStatement (String subject, String predicate, String object, boolean isLiteral)
	{
		Resource s = createResource( subject );
		Property p = rdfModel.createProperty( predicate );
		RDFNode  o = isLiteral ? rdfModel.createLiteral(object):createResource(object);
		return rdfModel.createStatement(s, p, o);
	}
	
	/**
	 * Create Resource
	 * @param id
	 * @return
	 */
	public Resource createResource( String id ) {
		Resource res = null;
		if ( id != null ) {
			if ( id != null && id.startsWith("_:") ) {
				res = rdfModel.createResource( new AnonId(id) );
			}
			else if ( id != null ) {
				res = rdfModel.createResource( id );
			}
		}
		return res;
	}
	
	public Model loadRDFXML(String rdfXml) throws UnsupportedEncodingException, IOException{
		InputStream in = null;
		try{
			in = toInputStream (Normalizer.normalize(rdfXml, Normalizer.Form.NFC));
			return load(in, "RDF/XML");
		}finally{
			if(in != null){
				in.close();
				in = null;
			}
		}
	}
	
	public Model loadNTriples(String nTriples) throws UnsupportedEncodingException, IOException{
		InputStream in = null;
		try{
			in = toInputStream (nTriples);
			return load(in, "N-TRIPLE");
		}finally{
			if(in != null){
				in.close();
				in = null;
			}
		}
	}
	
	public InputStream toInputStream (String rdf) throws UnsupportedEncodingException, IOException{
		InputStream in = null;
		OutputStream out = null;
		try{
			out = new ByteArrayOutputStream();
			out.write(rdf.getBytes("UTF-8"));
			in = new ByteArrayInputStream(((ByteArrayOutputStream)out).toByteArray());
		}finally{
			if(out != null){
				out.close();
				out = null;
			}
		}
		return in;
	}
	
	public Model load(InputStream in, String format){
		return 	rdfModel.read(in, null, format);
	}
}