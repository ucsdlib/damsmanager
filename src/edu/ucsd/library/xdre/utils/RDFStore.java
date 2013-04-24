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

import com.hp.hpl.jena.query.Query;
import com.hp.hpl.jena.query.QueryExecutionFactory;
import com.hp.hpl.jena.query.QueryFactory;
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
	
	public RDFStore(Model rdfModel){
		this.rdfModel  = rdfModel;
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
		String[] uriParts = prop.split(":");
		if(uriParts.length == 2){
			return rdfModel.createProperty(rdfModel.getNsPrefixURI(uriParts[0]), uriParts[1]);
		}else
			return rdfModel.createProperty(prop);
	}
	
	
	/**
	 * Add Statement
	 * @param stmt
	 */
	public void addStatement(Statement stmt){
		rdfModel.add(stmt);
	}
	
	/**
	 * Add list of Statements
	 * @param stmts
	 */
	public void addStatements(List<Statement> stmts){
		rdfModel.add(stmts);
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
	 * Retrieve the property
	 * @param uri
	 * @param prodName
	 * @return
	 */
	public String getProperty(String uri, String prodName){
		return getProperty(createResource(uri), createProperty(prodName));
	}
	
	/**
	 * Retrieve the property
	 * @param res
	 * @param prod
	 * @return
	 */
	public String getProperty(Resource res, Property prod){
		String value = null;
		Statement stmt = rdfModel.getProperty(res, prod);
		if(stmt != null){
			RDFNode rn = stmt.getObject();
			if(rn.isLiteral())
				value = rn.asLiteral().getString();
			else
				value = rn.asResource().getURI().toString();
		}
		return value;
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
		Model resModel = querySubject(subjectURI);
		return new RDFStore(resModel).export(format);
	}
	
	/**
	 * Query a subject
	 * @param objId
	 * @return
	 */
	public Model querySubject(String objId){
		Query query = QueryFactory.create("DESCRIBE <" + objId + ">") ;
		return QueryExecutionFactory.create(query, rdfModel).execDescribe() ;
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
		StmtIterator stmtIt = null;
		try{
			stmtIt = rdfModel.listStatements();
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
		}finally{
			if(stmtIt != null){
				stmtIt.close();
				stmtIt = null;
			}
		}
		return pres;
	}
	
	
	/**
	 * Remove the triples for components and files
	 * @throws Exception
	 */
	public void excludeComponentsAndFiles() throws Exception{
		DamsURI damsURI = null;
		String subId = null;
		ResIterator resIt = rdfModel.listSubjects();;
		while(resIt.hasNext()){
			Resource res = resIt.next();
			if(res.isURIResource()){
				subId = res.getURI();
				damsURI = DamsURI.toParts(subId, null);
				if(damsURI.isFileURI() || damsURI.isComponentURI()){
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
		boolean keep = false;
		StmtIterator stmtIt = null;
		try{
			stmtIt = rdfModel.listStatements();
			List<Statement> stmts = stmtIt.toList();
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
						removeStatement(stmt, true);
					}
				}
			}
		}finally{
			if(stmtIt != null){
				stmtIt.close();
				stmtIt = null;
			}
		}
	}
	
	/**
	 * Remove the statement. When tree is set to true, remove the BlankNode structure
	 * @param stmt
	 * @param tree
	 */
	public void removeStatement(Statement stmt, boolean tree){
		rdfModel.remove(stmt);
		if(tree){
			RDFNode rdfNode = stmt.getObject();
			if(rdfNode.isAnon()){
				RDFNode objNode = null;
				StmtIterator stmtIt = null;
				try{
					stmtIt = rdfModel.listStatements(rdfNode.asResource(), null, objNode);
					List<Statement> stmts = stmtIt.toList();
					for(int i=0; i<stmts.size(); i++){
						removeStatement(stmts.get(i), tree);
					}
				}finally{
					if(stmtIt != null){
						stmtIt.close();
						stmtIt = null;
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
		return merge(iStore.rdfModel);
	}
	
	/**
	 * Merge two graphs
	 * @param iModel
	 * @return
	 */
	public Model merge(Model iModel){
		rdfModel = rdfModel.union(iModel);
		Map<String, String> nsPrefixMap = rdfModel.getNsPrefixMap();
		nsPrefixMap.putAll(iModel.getNsPrefixMap());
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
	
	/**
	 * Output the graph
	 * @param format
	 * @return
	 */
	public String export(String format){
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		write(out, format);
		return out.toString();
	}
}
