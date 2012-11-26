package edu.ucsd.library.xdre.utils;

import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.activation.FileDataSource;
import javax.security.auth.login.LoginException;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import com.hp.hpl.jena.graph.Triple;

/**
 * DAMSClient perform operations through the DAMS REST API
 * 
 * @author lsitu
 * 
 */
public class DAMSClient {
	public static final String DOCUMENT_RESPONSE__ROOT_PATH = "/response";
	public static final String DAMS_ARK_URL_BASE = "http://libraries.ucsd.edu/ark:/";
	public static final String DAMS_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	public static final String DAMS_DATE_FORMAT_ALT = "MM-dd-yyyy HH:mm:ss";
	public static enum DataFormat {rdf, xml, json, mets, html};
	private static final Logger log = Logger.getLogger(DAMSClient.class);

	private Properties prop = null; // Properties for DAMS REST API
	private String storageURL = null; // DAMS REST URL
	private DefaultHttpClient client = null; // Httpclient object
	private HttpRequestBase request = null; // HTTP request
	private HttpResponse response = null; // HTTP response
	private HttpContext httpContext = null;
	private SimpleDateFormat damsDateFormat = null;
	private SimpleDateFormat damsDateFormatAlt = null;
	private String fileStore = null;
	private String tripleStore = null;
	private String resultFormat = null;

	/**
	 * Constructor for DAMSClient.
	 * 
	 * @param storageURL
	 * @throws IOException
	 * @throws LoginException
	 */
	public DAMSClient() throws IOException, LoginException {
		// disable retries and timeouts
		BasicHttpParams params = new BasicHttpParams();
		params.setParameter("http.socket.timeout", new Integer(0));
		params.setParameter("http.connection.timeout", new Integer(0));
		DefaultHttpRequestRetryHandler x = new DefaultHttpRequestRetryHandler(0, false);
		client = new DefaultHttpClient(new PoolingClientConnectionManager(), params);
		client.setHttpRequestRetryHandler(x);
		this.damsDateFormat = new SimpleDateFormat(DAMS_DATE_FORMAT);
		this.damsDateFormatAlt = new SimpleDateFormat(DAMS_DATE_FORMAT_ALT);
	}
	
	/**
	 * Construct a DAMSClient object using storage URL.
	 * 
	 * @param storageURL
	 * @throws IOException
	 * @throws LoginException
	 */
	public DAMSClient(String storageURL) throws IOException, LoginException {
		this();
		if(!storageURL.endsWith("/"))
			storageURL += "/";
		this.storageURL = storageURL;

	}

	/**
	 * Construct DAMSClient object and authenticate using the auth account
	 * information provided.
	 * 
	 * @param account
	 * @param out
	 * @throws IOException
	 * @throws LoginException
	 */
	public DAMSClient(Properties prop) throws IOException, LoginException {
		this();
		this.prop = prop;
		if (prop != null && prop.size() > 0) {
			storageURL = prop.getProperty("storageUrl");
			if(!storageURL.endsWith("/"))
				storageURL += "/";
		}
	}

	/**
	 * Mint an ark
	 * 
	 * @return
	 * @throws LoginException
	 * @throws IOException
	 */
	public String mintArk(String name) throws LoginException, IOException {
		String ark = "";
		//String url = storageURL + "next_id?format=json" + (name!=null&&name.length()>0?"&name=" + name:"");
		String url = getDAMSRestURL("next_id", null, null, null, "json") + (name!=null&&name.length()>0?"&name=" + name:"");
		HttpPost post = new HttpPost(url);
		JSONObject resObj = this.getJSONResult(post);
		if(resObj != null){
			JSONArray jsonArr = (JSONArray) resObj.get("ids");
			if(jsonArr!=null && jsonArr.size() > 0)
				ark = (String) jsonArr.get(0);
		}else{
			throw new IOException("Error mint ARK: Response body is empty.");
		}
		return ark;
	}
	
	/**
	 * Retrieve the tripleStores in use.
	 * @return
	 * @throws IOException 
	 * @throws LoginException 
	 */
	public List<String> listTripleStores() throws LoginException, IOException{
		//String url = storageURL + "system/triplestores?format=json";
		String url = getDAMSRestURL("system", "triplestores", null, null, "json");
		HttpGet get = new HttpGet(url);
		JSONObject resObj = getJSONResult(get);
		JSONArray jsonArr = (JSONArray) resObj.get("triplestores");
		List<String> triplestores = new ArrayList<String>();
		for(int i=0; i<jsonArr.size(); i++)
			triplestores.add((String) jsonArr.get(i));
		return triplestores;
	}
	
	/**
	 * Retrieve the fileStores in use.
	 * @return
	 * @throws IOException 
	 * @throws LoginException 
	 */
	public List<String> listFileStores() throws LoginException, IOException{
		//String url = storageURL + "system/filestores?format=json";
		String url = getDAMSRestURL("system", "filestores", null, null, "json");
		HttpGet get = new HttpGet(url);
		JSONObject resObj = getJSONResult(get);
		JSONArray jsonArr = (JSONArray) resObj.get("filestores");
		List<String> filestores = new ArrayList<String>();
		for(int i=0; i<jsonArr.size(); i++)
			filestores.add((String) jsonArr.get(i));
		return filestores;
	}
	
	/**
	 * Retrieve the collections in DAMS
	 * @return
	 * @throws IOException 
	 * @throws LoginException 
	 * @throws ClientProtocolException 
	 */
	public Map<String, String> listCollections() throws ClientProtocolException, LoginException, IOException{
		Map<String, String> map = null;
		//String url = storageURL + "collections?format=json";
		String url = getDAMSRestURL("collections", null, null, null, "json");
		url = appendStorageInfo(url);
		HttpGet get = new HttpGet(url);
		JSONObject resObj = getJSONResult(get);
		JSONArray colArr = (JSONArray) resObj.get("collections");
		JSONObject col = null;
		map = new TreeMap<String, String>();
		for(Iterator it= colArr.iterator(); it.hasNext();){
			col = (JSONObject)it.next();
			map.put((String)col.get("title"), stripID((String)col.get("collection")));
		}

		return map;
	}	

	/**
	 * Get a list of objects in a collection.
	 * 
	 * @throws LoginException
	 * @throws DocumentException 
	 **/
	public List<String> listObjects(String collectionId) throws IOException, LoginException, DocumentException {
		//String[] objects = {"bb01010101"};

		String url = getDAMSRestURL("collections", collectionId, null, null, null);
		//System.out.println("listObjects: " + url);
		HttpGet get = new HttpGet(url);
		Document doc = getXMLResult(get);
		List<Node> objectNodes = doc.selectNodes(DOCUMENT_RESPONSE__ROOT_PATH + "/objects/value");
		Node valNode= null;
		List<String> objectsList = new ArrayList<String>();
		for(Iterator it= objectNodes.iterator(); it.hasNext();){
			valNode = (Node)it.next();
			 objectsList.add(stripID(valNode.getText()));
		}
		return objectsList;
	}
	

	/**
	 * Count number of objects in a collection
	 * @param collectionId
	 * @return
	 * @throws ClientProtocolException
	 * @throws LoginException
	 * @throws IOException
	 */
	public long countObjects(String collectionId) throws ClientProtocolException, LoginException, IOException{
		JSONObject resObj = null;
		String url = getDAMSRestURL("collections", collectionId, null, "count", "json");
		//System.out.println("countObjects: " + url);
		HttpGet get = new HttpGet(url);
		resObj = getJSONResult(get);
		return ((Long)resObj.get("count")).longValue();
	}
	
	/**
	 * Transform an object with xsl
	 * @param object
	 * @param xsl
	 * @param recursive
	 * @param destFileId
	 * @return
	 * @throws ClientProtocolException
	 * @throws LoginException
	 * @throws IOException
	 */
	public InputStream transform(String object, String xsl, boolean recursive, String destFileId) throws ClientProtocolException, LoginException, IOException{
		String url = getDAMSRestURL("objects", object, null, null, null);
		HttpPost post = new HttpPost(url);
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("xsl", xsl));
		nameValuePairs.add(new BasicNameValuePair("recursive", String.valueOf(recursive)));
		if(destFileId != null && destFileId.length() > 0)
			nameValuePairs.add(new BasicNameValuePair("dest", destFileId));
		
		post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		try{
			execute(post);
		}finally{
			post.reset();
		}
		return new HttpContentInputStream(response.getEntity().getContent(), request);
	}
	
	/**
	 * Get a list of files in an object.
	 * 
	 * @throws LoginException
	 **/
	public List<String> listFiles(String object) throws IOException,
			LoginException {
		// XXX
		//Implementation to retrieve the files in an object container/folder
		return null;
	}
	
	/**
	 * Retrieve list of the items has the same source file name
	 * @param srcFileName
	 * @return
	 */
	public List<String> getObjects(String srcFileName){
		//XXX
		
		String[] objects = {"bb01010101"};
		return Arrays.asList(objects);
	}
	
	
	/**
	 * Perform crc32 file checksum
	 * @param subjectId
	 * @param fileName
	 * @return
	 */
	public String checksum (String subjectId, String fileName){
		//XXX
		return null;
	}
	
	/**
	 * Create derivative.
	 * 
	 * @param object
	 * @param fileName
	 *            Source file name.
	 * @param derName
	 *            List of derivative name.
	 * @param sizeWH
	 * @param frameNo
	 * @return
	 */
	public boolean createDerivative(String subjectId, String srcFileName, String derFileName, String[] sizeWH, int frameNo) {
		int status = -1;
		// XXX
		return status == 200;
	}

	/**
	/**
	 * Update derivative.
	 * 
	 * @param object
	 * @param fileName
	 *            Source file name.
	 * @param derName
	 *            List of derivative name.
	 * @param sizeWH
	 * @param frameNo
	 * @return
	 */
	public boolean updateDerivative(String subjectId, String srcFileName, String derFileName, String[] sizeWH, int frameNo) {
		int status = -1;
		// XXX
		return status == 200;
	}

	/**
	 * Push a record in SOLR.
	 * 
	 * @param object
	 * @return
	 */
	public boolean solrIndex(String object, boolean update) {
		// XXX
		int status = -1;
		return status == 200;
	}
	
	/**
	 * Update a record in SOLR.
	 * 
	 * @param object
	 * @return
	 */
	public boolean solrUpdate(String object, boolean update) {
		// XXX
		int status = -1;
		return status == 200;
	}

	/**
	 * Remove a record from SOLR.
	 * 
	 * @param object
	 * @return
	 */
	public boolean solrDelete(String object) {
		// XXX
		int status = -1;
		return status == 200;
	}

	/**
	 * Retrieve the metadata elements of an object with the top level predicates
	 * provided.
	 * 
	 * @param object
	 * @param predicates
	 * @return
	 */
	public String samePredicateExport(String object, List<String> predicates) {
		int status = -1;
		// XXX
		return null;
	}

	/**
	 * Replace the metadata elements with the same top level predicates
	 * submitted.
	 * 
	 * @param object
	 * @param metadata
	 * @return
	 */
	public boolean samePredicateReplace(String object, String metadata) {
		// XXX
		int status = -1;
		return status == 200;
	}

	/**
	 * Retrieve metatada of an object
	 * @param object
	 * @param format
	 * @return
	 * @throws ClientProtocolException
	 * @throws LoginException
	 * @throws IOException
	 */
	public String getMetadata(String object, String format)
			throws ClientProtocolException, LoginException, IOException {
		//String url = storageURL + "objects/" + toUrlPath(object, null) + (format!=null?"?format=" + format:"");
		String url = getDAMSRestURL("objects", object, null, null, format);
		//System.out.println("getMetadata: " + url);
		HttpGet get = new HttpGet(url);
		try {
			execute(get);
		} finally {
			get.reset();
		}
		return EntityUtils.toString(response.getEntity());
	}

	/**
	 * Update technical metadata with Jhove extraction and additional
	 * NameValuePairs
	 * 
	 * @param object
	 * @param fileName
	 * @param optionalParams
	 *            additional NameValuePairs that can add to the file.
	 * @return
	 * @throws ClientProtocolException
	 * @throws LoginException
	 * @throws IOException
	 */
	public boolean createTechnicalMetadata(String object, String fileName,
			List<NameValuePair> optionalParams) throws ClientProtocolException,
			LoginException, IOException {
		//String url = storageURL + "files/" + toUrlPath(object, fileName) + "/characterize";
		String url = getDAMSRestURL("files", object, fileName, "characterize", null);
		HttpPost post = new HttpPost(url);
		post.setEntity(new UrlEncodedFormEntity(optionalParams));
		int status = -1;
		try {
			status = execute(post);
		} finally {
			post.reset();
		}

		return status == 200;
	}

	/**
	 * Update technical metadata with Jhove extraction and additional
	 * NameValuePairs
	 * 
	 * @param object
	 * @param fileName
	 * @param optionalParams
	 *            additional NameValuePairs that can add to the file.
	 * @return
	 * @throws ClientProtocolException
	 * @throws LoginException
	 * @throws IOException
	 */
	public boolean updateTechnicalMetadata(String object, String fileName,
			List<NameValuePair> optionalParams) throws ClientProtocolException,
			LoginException, IOException {
		//String url = storageURL + "files/" + toUrlPath(object, fileName) + "/characterize";
		String url = getDAMSRestURL("files", object, fileName, "characterize", null);
		HttpPut put = new HttpPut(url);
		put.setEntity(new UrlEncodedFormEntity(optionalParams));
		int status = -1;
		try {
			status = execute(put);
		} finally {
			put.reset();
		}

		return status == 200;
	}
	
	/**
	 * Test whether a object or file exists.
	 * 
	 * @param object
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public boolean exists(String object, String fileName) throws Exception {
		boolean exists = false;

		//String url = storageURL;
		String url = null;
		
		if (object != null && (object = object.trim()).length() > 0) {
			String path = "objects";
			if (fileName != null && (fileName = fileName.trim()).length() > 0)
				path = "files";
				
			url = getDAMSRestURL(path, object, fileName, "exists", null);
			/*if (fileName != null && (fileName = fileName.trim()).length() > 0) {
				url += "files/" + toUrlPath(object, fileName) + "/exists";
			} else {
				url += "objects/" + toUrlPath(object, null);
			}*/
		} else {
			throw new Exception("Object identifier must be specified");
		}

		//url = appendStorageInfo(url);
		//System.out.println("exists: " + url);
		HttpGet request = new HttpGet(url);
		int status = -1;
		try {
			status = execute(request);
			if (status == 200) {
				exists = true;
			} else
				log.error("Unexpected status for exists check: " + status
						+ ": " + url);
		} catch(FileNotFoundException e){
			exists = false;
		} finally {
			request.reset();
		}
		return exists;
	}

	/**
	 * Get a list of predicates.
	 * 
	 * @throws LoginException
	 * @throws DocumentException 
	 **/
	public Map<String, String> getPredicates() throws IOException, LoginException, DocumentException {
		Map<String, String> predicates = new HashMap<String, String>();
		//String url = storageURL + "system/predicates";
		String url = getDAMSRestURL("system", null, null, "predicates", null);
		HttpGet get = new HttpGet(url);
		Document doc = getXMLResult(get);
		List<Node> nodes = doc.selectNodes(DOCUMENT_RESPONSE__ROOT_PATH + "/predicates/value");
		Node node = null;
		
		for(Iterator<Node> it=nodes.iterator(); it.hasNext();){
			node = (Node)it.next();
			predicates.put(stripID(node.selectSingleNode("@key").getStringValue()), node.getText());
		}
		return predicates;
	}

	/**
	 * Retrieve a file and copy it to another location.
	 * 
	 * @throws LoginException
	 **/
	public int download(String object, String fileName, String destFile)
			throws IOException, LoginException {

		InputStream in = null;
		OutputStream out = null;
		int bytesRead = -1;
		try {
			in = read(object, fileName);
			out = new FileOutputStream(destFile);
			bytesRead = copy(in, out);
		} finally {
			if (in != null) {
				in.close();
				in = null;
			}
			if (out != null) {
				out.close();
				out = null;
			}
		}
		log.info(bytesRead + " bytes written to " + destFile);
		return bytesRead;
	}

	/**
	 * Initiate an InputStream of a file or metadata of an object with an object
	 * ID and fileName.
	 * 
	 * @param object
	 * @param fileName
	 * @return
	 * @throws DAMSException
	 * @throws IOException
	 * @throws LoginException
	 */
	public InputStream read(String object, String fileName) throws IOException,
			LoginException {
		/*String url = storageURL;
		if (fileName != null && (fileName = fileName.trim()).length() > 0)
			url += "files/" + toUrlPath(object, fileName);
		else
			url += "objects/" + toUrlPath(object, null);*/
		String path = "objects";
		if (fileName != null && (fileName = fileName.trim()).length() > 0)
			path = "files";
		String url = getDAMSRestURL(path, object, fileName, null, null);
		url = appendStorageInfo(url);
		log.info("url: " + url);
		HttpGet get = new HttpGet(url);
		execute(get);

		return new HttpContentInputStream(response.getEntity().getContent(), get);
	}

	
	/**
	 * Create a file.
	 * 
	 * @param object
	 * @param fileName
	 * @param srcFile
	 * @return
	 * @throws IOException
	 * @throws LoginException
	 */
	public boolean createFile(String object, String fileName, String srcFile) throws IOException, LoginException {
		//String url = storageURL + "files/" + toUrlPath(object, fileName);
		//url = appendStorageInfo(url);
		String url = getDAMSRestURL("files", object, fileName, null, null);
		HttpPost post = new HttpPost(url);
		int status = -1;
		try {
			File file = new File(srcFile);
			MultipartEntity ent = new MultipartEntity();
			ent.addPart("fileName", new StringBody(file.getName()));
			String contentType = new FileDataSource(srcFile).getContentType();
			FileBody fileBody = new FileBody(file, contentType);
			ent.addPart("file", fileBody);
			post.setEntity(ent);
			status = execute(post);
		} finally {
			post.reset();
		}
		return status == 201 ;
	}
	
	/**
	 * Create a file
	 * @param object
	 * @param fileName
	 * @param in
	 * @param len
	 * @return
	 * @throws IOException
	 * @throws LoginException
	 */
	public boolean createFile(String object, String fileName, InputStream in, long len) throws IOException, LoginException {
		//String url = storageURL + "files/" + toUrlPath(object, fileName);
		//url = appendStorageInfo(url);
		String url = getDAMSRestURL("files", object, fileName, null, null);
		HttpPost post = new HttpPost(url);
		int status = -1;
		try {
			String contentType = new FileDataSource(fileName).getContentType();
			//InputStreamEntity ent = new InputStreamEntity(in, len, ContentType.create(contentType));
			InputStreamBody inputBody = new InputStreamBody(in, contentType);
			MultipartEntity ent = new MultipartEntity();
			ent.addPart("file", inputBody);
			post.setEntity(ent);
			status = execute(post);
		} finally {
			post.reset();
		}
		return status == 201 ;
	}

	/**
	 * Replace a file.
	 * 
	 * @param object
	 * @param fileName
	 * @param in
	 * @param len
	 * @return
	 * @throws IOException
	 * @throws LoginException
	 */
	public boolean updateFile(String object, String fileName, String srcFile) throws IOException, LoginException {
		//String url = storageURL + "files/" + toUrlPath(object, fileName);
		//url = appendStorageInfo(url);
		String url = getDAMSRestURL("files", object, fileName, null, null);
		HttpPut put = new HttpPut(url);
		int status = -1;
		try {
			File file = new File(srcFile);
			MultipartEntity ent = new MultipartEntity();
			ent.addPart("fileName", new StringBody(file.getName()));
			String contentType = new FileDataSource(srcFile).getContentType();
			FileBody fileBody = new FileBody(file, contentType);
			ent.addPart("file", fileBody);
			put.setEntity(ent);
			status = execute(put);
		} finally {
			put.reset();
		}
		return status == 200;
	}
	
	/**
	 * Replace a file.
	 * 
	 * @param object
	 * @param fileName
	 * @param in
	 * @param len
	 * @return
	 * @throws IOException
	 * @throws LoginException
	 */
	public boolean updateFile(String object, String fileName, InputStream in, long len) throws IOException, LoginException {
		//String url = storageURL + "files/" + toUrlPath(object, fileName);
		//url = appendStorageInfo(url);
		String url = getDAMSRestURL("files", object, fileName, null, null);
		HttpPut put = new HttpPut(url);
		int status = -1;
		try {
			String contentType = new FileDataSource(fileName).getContentType();
			//InputStreamEntity ent = new InputStreamEntity(in, len, ContentType.create(contentType));
			InputStreamBody inputBody = new InputStreamBody(in, contentType);
			MultipartEntity ent = new MultipartEntity();
			ent.addPart("file", inputBody);
			put.setEntity(ent);
			status = execute(put);
		} finally {
			put.reset();
		}
		return status == 200;
	}


	/**
	 * Create the object with the triples.
	 * 
	 * @param object
	 * @param triples
	 * @return
	 * @throws ClientProtocolException
	 * @throws LoginException
	 * @throws IOException
	 */
	public boolean createObject(String object, List<Triple> triples)
			throws ClientProtocolException, LoginException, IOException {
		//String url = storageURL + "objects/" + toUrlPath(object, null);
		//url = appendStorageInfo(url);
		String url = getDAMSRestURL("objects", object, null, null, null);
		HttpPost request = new HttpPost(url);
		// XXX
		// Create the object with the triples.

		int status = -1;
		try {
			status = execute(request);
		} finally {
			request.reset();
		}
		return status == 200;
	}

	/**
	 * Create the object with the triples.
	 * 
	 * @param object
	 * @param triples
	 * @return
	 * @throws ClientProtocolException
	 * @throws LoginException
	 * @throws IOException
	 */
	public boolean updateObject(String object, List<Triple> triples)
			throws ClientProtocolException, LoginException, IOException {
		//String url = storageURL + "objects/" + toUrlPath(object, null);
		//url = appendStorageInfo(url);
		String url = getDAMSRestURL("objects", object, null, null, null);
		HttpPost request = new HttpPost(url);
		// XXX
		// Create the object with the triples.

		int status = -1;
		try {
			status = execute(request);
		} finally {
			request.reset();
		}
		return status == 200;
	}

	/**
	 * Add metadata to an object
	 * 
	 * @param object
	 * @param triples
	 * @return
	 * @throws ClientProtocolException
	 * @throws LoginException
	 * @throws IOException
	 */
	public boolean addMetadata(String object, List<Triple> triples)
			throws ClientProtocolException, LoginException, IOException {
		//String url = storageURL + toUrlPath(object, null);
		//url = appendStorageInfo(url);
		String url = getDAMSRestURL("objects", object, null, null, null);
		HttpPut request = new HttpPut(url);
		// XXX
		// Update triples

		int status = -1;
		try {
			status = execute(request);
		} finally {
			request.reset();
		}
		return status == 200;
	}

	/**
	 * Delete an object or a file.
	 * 
	 * @param object
	 * @param file
	 *            If not null, delete this file.
	 * @throws Exception
	 **/
	public boolean delete(String object, String fileName) throws Exception {
		String url = null;
		String path = "objects";
		//String url = storageURL;
		if (object != null && (object = object.trim()).length() > 0) {
			if (fileName != null && (fileName = fileName.trim()).length() > 0)
				path = "files";
			url = getDAMSRestURL(path, object, fileName, null, null);
			/*if (fileName != null && (fileName = fileName.trim()).length() > 0) {
				url += "files/" + toUrlPath(object, fileName);
			} else {
				url += "objects/" + toUrlPath(object, null);
			}*/
		} else {
			throw new Exception("Object identifier must be specified");
		}

		//url = appendStorageInfo(url);
		HttpDelete del = new HttpDelete(url);
		int status = -1;
		try {
			status = execute(del);
		} finally {
			del.reset();
		}
		return status == 200;
	}

	/**
	 * Execute a http request
	 * @param request
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws LoginException
	 */
	public int execute(HttpRequestBase request) throws ClientProtocolException,
			IOException, LoginException {

		this.request = request;
		if (httpContext != null) {
			response = client.execute(request, httpContext);
		} else {
			response = client.execute(request);
		}
		int status = response.getStatusLine().getStatusCode();
		//200 - OK: Success, object/file exists
		//201 - Created: File/object created successfully
		if (!(status == 200 || status == 201)) {
			
			//403 - Forbidden: Deleting non-existing file, using POST to update or PUT to create
			if (status == 403) {  
				log.info("HTTP status: " + status + ". Login required: "
						+ request.getMethod() + " " + request.getURI() + ".");
				throw new LoginException("HTTP Status: " + status);
				
			//404 - Not Found: Object/file does not exist 
			} else if (status == 404) { 
				log.info("HTTP status 404. Resource not found: "
						+ request.getMethod() + " " + request.getURI() + ".");
				throw new FileNotFoundException(
						"HTTP status 404. Resource not found: "
								+ request.getMethod() + " " + request.getURI()
								+ ".");
				
			//500 - Internal Error: Other errors
			}  else if (status == 500) {  
				logError();
				
			//502 - Unavailable: Too many uploads 
			} else if (status == 502) { 
				logError();
				
			//Unknown status code
			} else { 
				logError();
			}
		}
		return status;
	}
	
	/**
	 * Construct the URL for the DAMS REST API
	 * @param path -
	 * 			objects, files, collections, system, client etc.
	 * @param object
	 * @param fileName
	 * @param function -
	 * 			function exist, fixity, transform, files, export, triplestores, filestores, predicates, index etc.
	 * @param format
	 * 			xml, html, json, ntriple, etc.
	 * @return
	 */
	private String getDAMSRestURL(String path, String object, String fileName, String function, String format){
		return appendStorageInfo(storageURL + path + toUrlPath(object, fileName) + (function!=null&&function.length()>0?"/" + function:"") + (format!=null&&format.length()>0?"?format="+format:""));
	}
	
	/**
	 * Execute http request for JSON format
	 * @param request
	 * @return
	 * @throws ClientProtocolException
	 * @throws LoginException
	 * @throws IOException
	 */
	public JSONObject getJSONResult(HttpRequestBase request) throws ClientProtocolException, LoginException, IOException{
		JSONObject resObj = null;
		InputStream in = null;
		Reader reader = null;
		try {
			execute(request);
		
			in = response.getEntity().getContent();
			resObj = (JSONObject) JSONValue.parse(new InputStreamReader(in));
		}finally{
			request.reset();
			close(in);
			close(reader);
		}
		return resObj;
	}
	
	/**
	 * Execute http request for XML format
	 * @param request
	 * @return
	 * @throws ClientProtocolException
	 * @throws LoginException
	 * @throws IOException
	 * @throws DocumentException 
	 */
	public Document getXMLResult(HttpRequestBase request) throws ClientProtocolException, LoginException, IOException, DocumentException{
		Document resObj = null;
		InputStream in = null;
		SAXReader reader = null;
		try {
			reader = new SAXReader();
			execute(request);
		
			in = response.getEntity().getContent();
			resObj = reader.read(in);
		}finally{
			request.reset();
			close(in);
		}
		return resObj;
	}

	/**
	 * Request URL
	 * @return
	 */
	public String getRequestURL() {
		return request.getURI().toString();
	}

	/**
	 * Append fileStore and tripleStore parameters to the URL
	 * 
	 * @param url
	 */
	private String appendStorageInfo(String url) {
		String storageParams = "";
		if (fileStore != null)
			storageParams = "fs=" + fileStore;
		if (tripleStore != null)
			storageParams = (storageParams.length() > 0 ? "&" : "") + "ds=" + tripleStore;
		if (resultFormat != null)
			storageParams = (storageParams.length() > 0 ? "&" : "") + "format=" + resultFormat;
		if (storageParams.length() > 0) {
			int idx = url.indexOf('?');
			if (idx < 0)
				url += "?" + storageParams;
			else if (idx == url.length() - 1)
				url += storageParams;
			else
				url += "&" + storageParams;
		}
		return url;
	}
	
	/**
	 * Convert object ID and the file name to DAMS REST URL path: objectId/[componentId/]/fileName
	 * @param objectId
	 * @param fileName
	 * @return
	 */
	public String toUrlPath(String objectId, String fileName){
		if(fileName == null || fileName.length() == 0)
			fileName = "";
		else
			fileName = "/" + (fileName.startsWith("0-")?fileName.substring(2):fileName);
		if(objectId == null  || objectId.length() == 0)
			objectId = "";
		String path = (objectId + fileName).replace("-", "/");
		return path.length()>0?"/"+path:"";
	}

	/**
	 * Retrieve the HTTP content from a URL provided.
	 * 
	 * @param url
	 * @return
	 * @throws IOException
	 * @throws LoginException
	 */
	public String getContentBodyAsString(String url) throws IOException,
			LoginException {
		HttpGet get = new HttpGet(url);
		try {
			execute(get);
		} finally {
			get.reset();
		}
		return EntityUtils.toString(response.getEntity());
	}

	/**
	 * Log response header and body information.
	 * 
	 * @throws IOException
	 * @throws LoginException
	 */
	public void logError() throws IOException, LoginException {
		int status = response.getStatusLine().getStatusCode();

		System.out.println(request.getRequestLine());
		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			System.out.print(headers[i] + "; ");
		}
		System.out.println("  status: " + status);
		if (!request.getMethod().equals("HEAD")) {
			System.out.print(EntityUtils.toString(response.getEntity()));
		}
		System.out.flush();
		throw new IOException("Unexpected HTTP Status: " + status);
	}

	/**
	 * Copy data from an input stream to an output stream.
	 * 
	 * @throws IOException
	 **/
	public static int copy(InputStream in, OutputStream out) throws IOException {
		int len = 0;
		int bytesRead = 0;
		byte[] buf = new byte[5096];
		while ((bytesRead = in.read(buf)) > 0) {
			out.write(buf, 0, bytesRead);
			len += bytesRead;
		}
		return len;
	}

	/**
	 * Close up IO resources
	 * @param closeable
	 */
	public void close(Closeable closeable){
		if(closeable != null){
			try{
				closeable.close();
			}catch (IOException ioe){
				ioe.printStackTrace();
			}
			closeable = null;
		}
	}
	
	/**
	 * Get the fileStore name
	 * 
	 * @return
	 */
	public String getFileStore() {
		return fileStore;
	}

	/**
	 * Set the fileStore name
	 * 
	 * @return
	 */
	public void setFileStore(String fileStore) {
		this.fileStore = fileStore;
	}

	/**
	 * Set the tripleStore name
	 * 
	 * @return
	 */
	public void setTripleStore(String tripleStore) {
		this.tripleStore = tripleStore;
	}

	/**
	 * Get the tripleStore name
	 * 
	 * @return
	 */
	public String getTripleStore() {
		return tripleStore;
	}

	/**
	 * Get format for the result returned.
	 * @return
	 */
	public String getResultFormat() {
		return resultFormat;
	}

	/**
	 * Set the format for the response.
	 * @param resultFormat
	 */
	public void setResultFormat(String resultFormat) {
		this.resultFormat = resultFormat;
	}

	/**
	 * Date format used in DAMS.
	 * 
	 * @return
	 */
	public SimpleDateFormat getDamsDateFormat() {
		return damsDateFormat;
	}
	
	/**
	 * Old date format used in DAMS in the past.
	 * 
	 * @return
	 */
	public SimpleDateFormat getDamsDateFormatAlt() {
		return damsDateFormatAlt;
	}


	/**
	 * Timestamp for dams
	 * @return
	 */
	public String getCurrentTimestamp(){
		return damsDateFormat.format(Calendar.getInstance());
	}
	
	/**
	 * Strip the object ID from an ARK URL
	 * @param arkURL
	 * @return
	 */
	public static String stripID (String arkURL){
		return arkURL.substring(arkURL.lastIndexOf("/")+1);
	}
	

	/**
	 * Wrapper class HttpContentInputStream
	 * 
	 */
	class HttpContentInputStream extends InputStream {

		private InputStream in = null;
		private HttpRequestBase request = null;

		public HttpContentInputStream(InputStream in, HttpRequestBase request) {
			this.in = in;
			this.request = request;
		}

		/**
		 * Overwrite close
		 * 
		 * @throws IOException
		 */
		@Override
		public void close() throws IOException {
			try {
				// Close input cursor
				in.close();
			} finally {
				// Reset http request
				request.reset();
			}
		}

		@Override
		public int read() throws IOException {
			return in.read();
		}
	}

}
