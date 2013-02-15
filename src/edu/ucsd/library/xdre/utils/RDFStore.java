package edu.ucsd.library.xdre.utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.hp.hpl.jena.rdf.model.AnonId;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Property;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.ResIterator;
import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.shared.PrefixMapping;

/**
 * JENA RDFStore
 * @author lsitu
 *
 */
public class RDFStore {
	public static final String RDFXML_FORMAT = "RDF/XML";
	public static final String RDFXML_ABBREV_FORMAT = "RDF/XML-ABBREV";
	public static final String NTRIPLE_FORMAT = "N-TRIPLE";
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
	
	/**
	 * Create Property
	 * @param prop
	 * @return
	 */
	public Property createProperty( String prop ) {
		if(prop == null)
			return null;
		return rdfModel.createProperty(prop);
	}
	
	/**
	 * Load RDF string into the RDF store
	 * @param rdfXml
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	public Model loadRDFXML(String rdfXml) throws UnsupportedEncodingException, IOException{
		InputStream in = null;
		try{
			in = toInputStream (Normalizer.normalize(rdfXml, Normalizer.Form.NFC));
			return load(in, RDFXML_FORMAT);
		}finally{
			if(in != null){
				in.close();
				in = null;
			}
		}
	}
	
	/**
	 * Load NTriples into the RDFStore
	 * @param nTriples
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	public Model loadNTriples(String nTriples) throws UnsupportedEncodingException, IOException{
		InputStream in = null;
		try{
			in = toInputStream (nTriples);
			return load(in, NTRIPLE_FORMAT);
		}finally{
			if(in != null){
				in.close();
				in = null;
			}
		}
	}
	
	/**
	 * Construct an InputStream 
	 * @param rdf
	 * @return
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	public InputStream toInputStream (String data) throws UnsupportedEncodingException, IOException{
		InputStream in = null;
		OutputStream out = null;
		try{
			out = new ByteArrayOutputStream();
			out.write(data.getBytes("UTF-8"));
			in = new ByteArrayInputStream(((ByteArrayOutputStream)out).toByteArray());
		}finally{
			if(out != null){
				out.close();
				out = null;
			}
		}
		return in;
	}
	
	/**
	 * Load RDF from an InputStream
	 * @param in
	 * @param format
	 * @return
	 */
	public Model load(InputStream in, String format){
		return 	rdfModel.read(in, null, format);
	}
	
	/**
	 * List all URL subjects
	 * @return
	 */
	public List<String> listURISubjects(){
		List<String> subjects = new ArrayList<String>();
		ResIterator resIt = rdfModel.listSubjects();
		while(resIt.hasNext()){
			Resource res = resIt.next();
			if(res.isURIResource()){
				subjects.add(res.getURI());
			}
		}
		
		return subjects;
	}
	
	/**
	 * List all URL subjects
	 * @return
	 */
	public List<Resource> listURIResources(){
		List<Resource> subjects = new ArrayList<Resource>();
		ResIterator resIt = rdfModel.listSubjects();
		while(resIt.hasNext()){
			Resource res = resIt.next();
			if(res.isURIResource()){
				subjects.add(res);
			}
		}
		
		return subjects;
	}
	
	/**
	 * Export a RDF subject
	 * @param subjectURI
	 * @param format
	 * @return
	 */
	public String exportSubject(String subjectURI, String format ){
		Model resultModel = querySubject(subjectURI);
		//System.out.println(subjectURI + ":\n");
		//resultModel.write(System.out);
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		resultModel.write(out, format);
		return out.toString();
	}
	
	/**
	 * Query a subject
	 * @param objId
	 * @return
	 */
	public Model querySubject(String objId){
		Resource res = rdfModel.createResource(objId);
		return res.getModel();
	}
	
	/**
	 * Retrieve the predicates a subject contains in prefix format like dams:collection
	 * @param subject
	 * @return
	 */
	public List<String> listPredicates(String subject){
		List<String> pres = new ArrayList<String>();
		String sid = null;
		String ns = null;
		Statement stmt = null;
		Resource res = null;
		StmtIterator stmtIt = rdfModel.listStatements();
		PrefixMapping prefixMap = rdfModel.lock();
		
		while (stmtIt.hasNext()){
			stmt = stmtIt.next();
			sid = stmt.getSubject().toString();
			if(sid.equals(subject)){
				res = stmt.getPredicate();
				ns = prefixMap.getNsURIPrefix(res.getNameSpace()) + ":" + res.getLocalName();
				if(pres.indexOf(ns) < 0)
					pres.add(ns);
			}
		}
		return pres;
	}
	
	
	/**
	 * Remove the triples for components
	 * @throws Exception
	 */
	public void excludeComponents() throws Exception{
		DamsURI damsURI = null;
		String subId = null;
		ResIterator resIt = rdfModel.listSubjects();;
		while(resIt.hasNext()){
			Resource res = resIt.next();
			if(res.isURIResource()){
				subId = res.getURI();
				damsURI = DamsURI.toParts(subId, null);
				if(damsURI.isComponentURI()){
					rdfModel.remove(querySubject(subId));
				}
			}
		}
	}
	
	/**
	 * Remove the elements that is not in the predicates list
	 * @param preds
	 */
	public void trimStatements(List<String> preds){
		String prep = null;
		Statement stmt = null;
		Property prop = null;
		RDFNode rdfNode = null;
		boolean keep = false;
		List<Statement> stmts = rdfModel.listStatements().toList();
		for(int i=0; i<stmts.size(); i++){
			stmt = stmts.get(i);
			if(stmt.getSubject().isURIResource()){
				keep = false;
				prop = stmt.getPredicate();
				prep = rdfModel.getNsURIPrefix(prop.getNameSpace()) + ":" + prop.getLocalName();
				for(Iterator<String> pIt=preds.iterator(); pIt.hasNext();){
					if(prep.indexOf(pIt.next()) >= 0){
						keep = true;
						break;
					}
				}
				
				if(!keep){
					rdfModel.remove(stmt);
					rdfNode = stmt.getObject();
					if(rdfNode.isAnon()){
						rdfModel.remove(rdfNode.getModel());
					}
				}
			}
		}
	}
	
	/**
	 * Merge two graphs
	 * @param rdfStore
	 * @return
	 */
	public Model mergeObjects(RDFStore iStore){
		rdfModel = rdfModel.union(iStore.rdfModel);
		Map<String, String> nsPrefixMap = rdfModel.getNsPrefixMap();
		nsPrefixMap.putAll(iStore.rdfModel.getNsPrefixMap());
		rdfModel.setNsPrefixes(nsPrefixMap);
		return rdfModel;
	}
	
	/**
	 * Output the graph
	 * @param out
	 * @param format
	 */
	public void write(OutputStream out, String format){
		rdfModel.write(out, format);
	}
}
