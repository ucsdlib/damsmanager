package edu.ucsd.library.xdre.utils;

import java.io.ByteArrayInputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

import javax.activation.FileDataSource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.login.LoginException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.ParseException;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
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

import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.Statement;


/**
 * DAMSClient provide basic functions to consume operations through the DAMS REST API
 * 
 * @author lsitu
 * 
 */
public class DAMSClient {
	public static final String DOCUMENT_RESPONSE_ROOT_PATH = "/response";
	public static final String DAMS_ARK_URL_BASE = "http://libraries.ucsd.edu/ark:/";
	public static final String DAMS_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	public static final String DAMS_DATE_FORMAT_ALT = "MM-dd-yyyy HH:mm:ss";
	public static final String XPATH_HASCOMPONENT = "/rdf:RDF/dams:Object/dams:hasComponent";
	public static final int MAX_SIZE = 1000000;
	public static enum DataFormat {rdf, xml, json, mets, html};
	private static final Logger log = Logger.getLogger(DAMSClient.class);
	
	private static SimpleDateFormat damsDateFormat = new SimpleDateFormat(DAMS_DATE_FORMAT);
	private static SimpleDateFormat damsDateFormatAlt = new SimpleDateFormat(DAMS_DATE_FORMAT_ALT);

	private String storageURL = null; // DAMS REST URL
	private HttpClient client = null; // Httpclient object
	private HttpRequestBase request = null; // HTTP request
	private HttpResponse response = null; // HTTP response
	private HttpContext httpContext = null;
	private String fileStore = null;
	private String tripleStore = null;
	private String solrURLBase = null; // SOLR URL
	
	/**
	 * Construct a DAMSClient object using storage URL.
	 * 
	 * @param storageURL
	 * @throws IOException
	 * @throws LoginException
	 */
	public DAMSClient(String storageURL) throws IOException, LoginException {
		this(storageURL, Constants.DAMS_STORAGE_USER, Constants.DAMS_STORAGE_PWD);
	}
	
	/**
	 * Construct a DAMSClient object using storage URL, username and password.
	 * 
	 * @param storageURL
	 * @throws IOException
	 * @throws LoginException
	 */
	public DAMSClient(String storageURL, String user, String password) throws IOException, LoginException {
		if(storageURL.endsWith("/"))
			storageURL = storageURL.substring(0, storageURL.length() -1);
		this.storageURL = storageURL;
		client = createHttpClient(user, password);
	}

	/**
	 * Construct DAMSClient object and authenticate using the dams repository properties
	 * information provided.
	 * 
	 * @param account
	 * @param out
	 * @throws IOException
	 * @throws LoginException
	 */
	public DAMSClient(Properties props) throws IOException, LoginException {
		this((String)props.get("xdre.damsRepo"), (String)props.get("xdre.damsRepo.user"), (String)props.get("xdre.damsRepo.pwd"));
	}
    
    /**
     * Create HttpClient with PreemptiveAuth when user and password provide 
     * @param userName
     * @param password
     * @return
     */
    private HttpClient createHttpClient(String userName, String password) {
		// disable timeouts
		BasicHttpParams params = new BasicHttpParams();
		params.setParameter( "http.socket.timeout",     new Integer(0) );
		params.setParameter( "http.connection.timeout", new Integer(0) );
      
		DefaultHttpClient client = new DefaultHttpClient( getConnectionManager(), params );

		// disable retries
		DefaultHttpRequestRetryHandler x = new DefaultHttpRequestRetryHandler(0, false);
		client.setHttpRequestRetryHandler( x );
        
        if (userName != null && userName.length() > 0) {
            client.getCredentialsProvider().setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(userName, password)
            );
            
            httpContext = new BasicHttpContext();

            // Generate BASIC scheme object and stick it to the local execution context
            BasicScheme basicAuth = new BasicScheme();
            httpContext.setAttribute("preemptive-auth", basicAuth);

            // Add as the first request interceptor
            client.addRequestInterceptor(new PreemptiveAuthInterceptor(), 0);
        }

        return client;
    }
    
    /**
     * Initiate ClientConnectionManager for http and https connections
     * @return
     */
    private ClientConnectionManager getConnectionManager(){

        ClientConnectionManager ccm = new PoolingClientConnectionManager();
		try {
			// Accept all SSL certificates
        	X509TrustManager tm = new X509TrustManager() {
            	public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException { }
            	public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException { }
            	public X509Certificate[] getAcceptedIssuers() { return null; }
        	};

			SSLContext ctx = SSLContext.getInstance("TLS");
        	ctx.init(null, new TrustManager[]{tm}, null);
        	SSLContext.setDefault(ctx);
        	
        	SSLSocketFactory ssf = new SSLSocketFactory(ctx,SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
        	SchemeRegistry sr = ccm.getSchemeRegistry();
        	sr.register(new Scheme("https", 443, ssf));
        	sr.register(new Scheme("http", 80, new PlainSocketFactory()));
			
		} catch ( Exception ex ){
			ex.printStackTrace();
		}
		return ccm;
    }

	/**
	 * Mint an ark ID
	 * 
	 * @return
	 * @throws Exception 
	 */
	public String mintArk(String name) throws Exception {
		String ark = "";
		//String url = storageURL + "next_id?format=json" + (name!=null&&name.length()>0?"&name=" + name:"");
		String url = getDAMSFunctionURL("next_id", null, "json") + (name!=null&&name.length()>0?"&name=" + name:"");
		HttpPost post = new HttpPost(url);
		JSONObject resObj = getJSONResult(post);
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
	 * @throws Exception 
	 */
	public List<String> listTripleStores() throws Exception{
		//String url = storageURL + "system/triplestores?format=json";
		String url = getDAMSFunctionURL("system", "triplestores", "json");
		HttpGet get = new HttpGet(url);
		JSONObject resObj = getJSONResult(get);
		JSONArray jsonArr = (JSONArray) resObj.get("triplestores");
		List<String> triplestores = new ArrayList<String>();
		for(int i=0; i<jsonArr.size(); i++)
			triplestores.add((String) jsonArr.get(i));
		return triplestores;
	}
	
	/**
	 * Retrieve the default triplestore.
	 * @return
	 * @throws Exception 
	 */
	public String defaultTriplestore() throws Exception{
		String url = getDAMSFunctionURL("system", "triplestores", "json");
		HttpGet get = new HttpGet(url);
		JSONObject resObj = getJSONResult(get);
		return  (String)resObj.get("defaultTriplestore");
	}
	
	/**
	 * Retrieve the fileStores in use.
	 * @return
	 * @throws Exception 
	 */
	public List<String> listFileStores() throws Exception{
		//String url = storageURL + "system/filestores?format=json";
		String url = getDAMSFunctionURL("system", "filestores", "json");
		HttpGet get = new HttpGet(url);
		JSONObject resObj = getJSONResult(get);
		JSONArray jsonArr = (JSONArray) resObj.get("filestores");
		List<String> filestores = new ArrayList<String>();
		for(int i=0; i<jsonArr.size(); i++)
			filestores.add((String) jsonArr.get(i));
		return filestores;
	}
	
	/**
	 * Retrieve the default filestore.
	 * @return
	 * @throws Exception 
	 */
	public String defaultFilestore() throws Exception{
		String url = getDAMSFunctionURL("system", "filestores", "json");
		HttpGet get = new HttpGet(url);
		JSONObject resObj = getJSONResult(get);
		return (String)resObj.get("defaultFilestore");
	}

	/**
	 * Retrieve the user information
	 * @param clientId
	 * @return
	 * @throws Exception
	 */
	public JSONObject getUserInfo(String user) throws Exception{
		String url = getDAMSFunctionURL("client", "info", "json") + (user!=null&&user.length()>0?"&user=" + user:"");
		HttpGet get = new HttpGet(url);
		return getJSONResult(get);
	}
	
	/**
	 * Retrieve the units in DAMS
	 * /api/units
	 * @return
	 * @throws Exception 
	 */
	public Map<String, String> listUnits() throws Exception{
		Map<String, String> map = null;
		String url = getUnitsURL(null, null, "json");
		HttpGet get = new HttpGet(url);
		JSONObject resObj = getJSONResult(get);
		JSONArray colArr = (JSONArray) resObj.get("units");
		JSONObject col = null;
		map = new TreeMap<String, String>();
		for(Iterator it= colArr.iterator(); it.hasNext();){
			col = (JSONObject)it.next();
			// unit title, unit URL
			map.put((String)col.get("name"), (String)col.get("unit"));
		}

		return map;
	}
	
	/**
	 * Get a list of objects in a unit.
	 * /api/units/unitId
	 * @throws Exception 
	 **/
	public List<String> listUnitObjects(String unitId) throws Exception {
		String url = getUnitsURL(unitId, null, "xml");
		HttpGet get = new HttpGet(url);
		Document doc = getXMLResult(get);
		List<Node> objectNodes = doc.selectNodes(DOCUMENT_RESPONSE_ROOT_PATH + "/objects/value/obj");
		Node valNode= null;
		List<String> objectsList = new ArrayList<String>();
		for(Iterator it= objectNodes.iterator(); it.hasNext();){
			valNode = (Node)it.next();
			objectsList.add(valNode.getText());
		}
		return objectsList;
	}
	
	/**
	 * Get a list of files in a unit.
	 * /api/units/unitId/files 
	 * @throws Exception 
	 **/
	public List<DFile> listUnitFiles(String unitId) throws Exception {
		JSONObject resObj = null;
		String url = getUnitsURL(unitId, "files", "json");
		HttpGet get = new HttpGet(url);
		resObj = getJSONResult(get);
		JSONArray jsonArr = (JSONArray) resObj.get("files");
		List<DFile> files = new ArrayList<DFile>();
		for(int i=0; i<jsonArr.size(); i++)
			files.add(DFile.toDFile((JSONObject)jsonArr.get(i)));
		return files;
	}
	
	/**
	 * Get the files in a unit.
	 * /api/units/unitId/files 
	 * @throws Exception 
	 **/
	public Document getUnitFiles(String unitId) throws Exception {
		Document doc = null;
		String url = getUnitsURL(unitId, "files", "xml");
		HttpGet get = new HttpGet(url);
		doc = getXMLResult(get);
		return doc;
	}
	
	/**
	 * Retrieve the collections in DAMS
	 * @return
	 * @throws Exception 
	 */
	public Map<String, String> listCollections() throws Exception{
		Map<String, String> map = null;
		//String url = storageURL + "collections?format=json";
		String url = getCollectionsURL(null, null, "json");
		HttpGet get = new HttpGet(url);
		JSONObject resObj = getJSONResult(get);
		JSONArray colArr = (JSONArray) resObj.get("collections");
		JSONObject col = null;
		map = new TreeMap<String, String>();
		for(Iterator<JSONObject> it= colArr.iterator(); it.hasNext();){
			col = it.next();
			// Collection title, collection URL
			String title = (String)col.get("title");
			String colId = (String)col.get("collection");
			if(map.get(title) != null)
				title += " (" + stripID(colId) + ")";
			map.put(title, colId);
		}

		return map;
	}	

	/**
	 * Get a list of objects in a collection.
	 * @throws Exception 
	 **/
	public List<String> listObjects(String collectionId) throws Exception {
		String url = getCollectionsURL(collectionId, null, "xml");
		HttpGet get = new HttpGet(url);
		Document doc = getXMLResult(get);
		List<Node> objectNodes = doc.selectNodes(DOCUMENT_RESPONSE_ROOT_PATH + "/objects/value/obj");
		Node valNode= null;
		List<String> objectsList = new ArrayList<String>();
		for(Iterator it= objectNodes.iterator(); it.hasNext();){
			valNode = (Node)it.next();
			objectsList.add(valNode.getText());
		}
		return objectsList;
	}
	
	/**
	 * Get the files in a collection.
	 * /api/collections/collId/files 
	 * @throws Exception 
	 **/
	public Document getCollectionFiles(String collectionId) throws Exception {
		Document doc = null;
		String url = getCollectionsURL(collectionId, "files", "xml");
		HttpGet get = new HttpGet(url);
		doc = getXMLResult(get);
		return doc;
	}
	

	/**
	 * Count number of objects in a collection
	 * @param collectionId
	 * @return
	 * @throws Exception 
	 */
	public long countObjects(String collectionId) throws Exception{
		JSONObject resObj = null;
		String url = getCollectionsURL(collectionId, "count", "json");
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
	 * @throws Exception 
	 */
	public InputStream transform(String object, String xsl, boolean recursive, String destFileId) throws Exception{
		String format = null;
		String url = getObjectsURL(object, null, null, format);
		HttpPost post = new HttpPost(url);
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("xsl", xsl));
		nameValuePairs.add(new BasicNameValuePair("recursive", String.valueOf(recursive)));
		if(destFileId != null && destFileId.length() > 0)
			nameValuePairs.add(new BasicNameValuePair("dest", destFileId));
		
		post.setEntity(new UrlEncodedFormEntity(nameValuePairs));
		int status = -1;
		try{
			status = execute(post);
			if (!(status == 200 || status == 201))
				handleError(format);
		}finally{
			post.releaseConnection();
		}
		return new HttpContentInputStream(response.getEntity().getContent(), request);
	}
	
	/**
	 * Get a list of files in an object.
	 * @throws Exception 
	 **/
	public List<DFile> listObjectFiles(String object) throws Exception {
		JSONObject resObj = null;
		String url = getObjectsURL(object, null, "files", "json");
		HttpGet get = new HttpGet(url);
		resObj = getJSONResult(get);
		JSONArray jsonArr = (JSONArray) resObj.get("files");
		List<DFile> files = new ArrayList<DFile>();
		for(int i=0; i<jsonArr.size(); i++)
			files.add(DFile.toDFile((JSONObject)jsonArr.get(i)));
		return files;
	}
	
	/**
	 * Retrieve the file ID from the original source filename
	 * @param srcFileName
	 * @param srcPath
	 * @param collectionId
	 * @return
	 * @throws Exception 
	 */
	public List<DamsURI> retrieveFileURI(String srcFileName, String srcPath, String collectionId, String unitId) throws Exception{
		HttpGet req = null;
		List<DamsURI> fileURIs = null;
		String url = "";
		if(collectionId == null && unitId == null){
			// Retrieve object from SOLR
		}else{
			if(collectionId != null)
				url = getCollectionsURL(collectionId, "files", "xml");
			else
				url = getUnitsURL(unitId, "files", "xml");
		}

		try{

			req = new HttpGet(url);
			Document doc = getXMLResult(req);
			fileURIs = getFiles(doc, srcPath, srcFileName);
		}finally{
			req.releaseConnection();
		}
		return fileURIs;
	}
		
	/**
	 * Perform file checksum validation
	 * @param subjectId
	 * @param fileName
	 * @return
	 * @throws Exception 
	 */
	public boolean checksum (String object, String compId, String fileName) throws Exception{
		// http://gimili.ucsd.edu:8080/dams/api/files/bb01010101/1/1.tif/fixity?format=json
		// {"crc32":"5ffa698b","md5":"f69c4f85cafbe05e55068cedd4353146","statusCode":200,"request":"\/files\/bb01010101\/1\/1.tif\/fixity","status":"OK"}
		JSONObject resObj = null;
		String url = getFilesURL(object, compId, fileName, "fixity", "json");
		HttpGet get = new HttpGet(url);
		resObj = getJSONResult(get);
		String status = (String) resObj.get("status");
		return status != null && status.equalsIgnoreCase("ok");
	}
	
	/**
	 * Create derivative.
	 * 
	 * @param object
	 * @param compId
	 * @param fileName
	 *            Source file name.
	 * @param size
	 * 			List of derivative names: 2 (768x768), 3(450x450), 4(150x150), and 5(65x65) or actually size like 150x150.
	 * @return
	 * @throws Exception 
	 */
	public boolean createDerivatives(String object, String compId, String fileName, String[] sizes, String frame) throws Exception {
		return updateDerivatives(object, compId, fileName, sizes, frame, false);
	}

	/**
	 * Create derivative.
	 * 
	 * @param object
	 * @param compId
	 * @param fileName
	 *            Source file name.
	 * @param size
	 * 			List of derivative names: 2 (768x768), 3(450x450), 4(150x150), and 5(65x65) or actually size like 150x150.
	 * @return
	 * @throws Exception 
	 */
	public boolean createDerivatives(String object, String compId, String fileName, String[] sizes) throws Exception {

		return createDerivatives(object, compId, fileName, sizes, null);
	}

	/**
	 * Update derivative.
	 * 
	 * @param object
	 * @param compId
	 * @param fileName
	 *            Source file name.
	 * @param size
	 *            List of derivative names: 2 (768x768), 3(450x450), 4(150x150), and 5(65x65) or actually size like 150x150.
	 * @param frame
	 * @return
	 * @throws Exception 
	 */
	public boolean updateDerivatives(String object, String compId, String fileName, String[]  sizes, String frame, boolean replace) throws Exception {
		String format = null;
		String url = getFilesURL(object, compId, fileName, "derivatives", format);
		HttpEntityEnclosingRequestBase req = new HttpPut(url);
		String paramsStr = "";
		if(sizes != null && sizes.length > 0){
			paramsStr = "size=";
			for(int i=0; i<sizes.length; i++)
				paramsStr += sizes[i]+",";
			paramsStr = paramsStr.substring(0, paramsStr.length()-1);
		}
		if(frame != null && frame.length() > 0)
			paramsStr += paramsStr.length()>0?"&frame=" + frame:"";
		
		if(paramsStr != null && paramsStr.length() > 0)
			url += (url.indexOf('?')>0?"&":"?") + paramsStr;
		
		if(replace)
			req = new HttpPut(url);
		else
			req = new HttpPost(url);		

		int status = -1;
		boolean success = false;
		try {
			status = execute(req);
			success = (status == 200 || status == 201);
			if (!success)
				handleError(format);
		} finally {
			req.releaseConnection();
		}

		return success;
	}
	
	/**
	 * Update derivatives.
	 * 
	 * @param object
	 * @param compId
	 * @param fileName
	 *            Source file name.
	 * @param size
	 *            List of derivative names: 2 (768x768), 3(450x450), 4(150x150), and 5(65x65) or actually size like 150x150.
	 * @return
	 * @throws Exception 
	 */
	public boolean updateDerivatives(String object, String compId, String fileName, String[]  sizes) throws Exception {

		return updateDerivatives(object, compId, fileName, sizes, null, true);
	}

	
	/**
	 * Push or Update a record in SOLR.
	 * 
	 * @param object
	 * @return
	 * @throws IOException 
	 * @throws LoginException 
	 * @throws ClientProtocolException 
	 * @throws DocumentException 
	 * @throws IllegalStateException 
	 */
	public boolean solrUpdate(String object) throws Exception {
		//POST /objects/bb1234567x/index
		String format = "json";
		String url = getObjectsURL(object, null, "index", format);
		HttpPost post = new HttpPost(url);
		int status = -1;
		boolean success = false;
		try {
			status = execute(post);
			success=(status == 200 || status == 201);
			if (!success)
				handleError(format);
		} finally {
			post.releaseConnection();
		}

		return success;
	}

	/**
	 * Remove a record from SOLR.
	 * 
	 * @param object
	 * @return
	 * @throws DocumentException 
	 * @throws IOException 
	 * @throws LoginException 
	 * @throws IllegalStateException 
	 */
	public boolean solrDelete(String object) throws Exception {
		//DELETE /objects/bb1234567x/index
		String format = "json";
		String url = getObjectsURL(object, null, "index", format);
		HttpDelete del= new HttpDelete(url);
		int status = -1;
		boolean success = false;
		try {
			status = execute(del);
			success= (status == 200 || status == 201);
			if (!success)
				handleError(format);
		} finally {
			del.releaseConnection();
		}
		return success;
	}
	
	/**
	 * List objects for items in a unit and/or collection
	 * @param unit
	 * @param collection
	 * @return
	 * @throws Exception
	 */
	public List<String> solrListObjects(String unitTitle, String collectionTitle) throws Exception {
		int rows = 1000;
		int start = 0;
		boolean hasMore = false;
		List<String> items = new ArrayList<String>();
		//Retrieve objects in SOLR recursively
		do{
			hasMore = appendSOLRItems(items, unitTitle, collectionTitle, start, rows);
			start = rows + start;
		}while(hasMore );
		
		return items;
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
	
	public List<RightsAction> getUnitEmbargoeds(String unitId) throws Exception{
		String url = getUnitsURL(unitId, "embargo", "xml");
		HttpGet get = new HttpGet(url);
		Document doc = getXMLResult(get);
		System.out.println(doc.asXML());
		List<Node> objectNodes = doc.selectNodes(DOCUMENT_RESPONSE_ROOT_PATH + "/embargoed/value");
		Node valNode= null;
		Node oidNode = null;
		Node startDateNode = null;
		Node endDateNode = null;
		
		List<RightsAction> objectsList = new ArrayList<RightsAction>();
		for(Iterator<Node> it= objectNodes.iterator(); it.hasNext();){
			valNode = (Node)it.next();
			startDateNode = valNode.selectSingleNode("startDate");
			endDateNode = valNode.selectSingleNode("endDate");
			oidNode = valNode.selectSingleNode("oid");
			objectsList.add(new RightsAction(null, startDateNode==null?null:startDateNode.getText(), endDateNode==null?null:endDateNode.getText(), null, oidNode.getText()));
		}
		return objectsList;
	}
	
	public List<RightsAction> getCollectionEmbargoeds(String collectionId) throws Exception{
		String url = getCollectionsURL(collectionId, "embargo", "xml");
		HttpGet get = new HttpGet(url);
		Document doc = getXMLResult(get);
		System.out.println(doc.asXML());
		List<Node> objectNodes = doc.selectNodes(DOCUMENT_RESPONSE_ROOT_PATH + "/embargoed/value");
		Node valNode= null;
		Node oidNode = null;
		Node startDateNode = null;
		Node endDateNode = null;
		
		List<RightsAction> objectsList = new ArrayList<RightsAction>();
		for(Iterator<Node> it= objectNodes.iterator(); it.hasNext();){
			valNode = (Node)it.next();
			startDateNode = valNode.selectSingleNode("startDate");
			endDateNode = valNode.selectSingleNode("endDate");
			oidNode = valNode.selectSingleNode("oid");
			objectsList.add(new RightsAction(null, startDateNode==null?null:startDateNode.getText(), endDateNode==null?null:endDateNode.getText(), null, oidNode.getText()));
		}
		return objectsList;
	}

	/**
	 * Retrieve metatada of an object
	 * @param object
	 * @param format
	 * @return
	 * @throws Exception 
	 */
	public String getMetadata(String object, String format)
			throws Exception {
		String url = getObjectsURL(object, null, null, format);
		HttpGet req = new HttpGet(url);
		int status = -1;
		try {
			status = execute(req);
			if (status != 200)
				handleError(format);
			return EntityUtils.toString(response.getEntity());
		} finally {
			req.releaseConnection();
		}
	}

	/**
	 * Extract technical metadata with Jhove extraction and additional
	 * NameValuePairs
	 * 
	 * @param object
	 * @param fileName
	 * @param optionalParams
	 *            additional NameValuePairs that can add to the file.
	 * @return
	 * @throws Exception 
	 */
	public boolean saveFileCharacterize(String object, String compId, String fileName,
			List<NameValuePair> optionalParams) throws Exception {
		String format = null;
		String url = getFilesURL(object, compId, fileName, "characterize", format);
		HttpPost req = new HttpPost(url);
		req.setEntity(toMultiPartEntity(optionalParams));
		int status = -1;
		boolean success = false;
		try {
			status = execute(req);
			success= (status == 200 || status == 201);
			if (!success)
				handleError(format);
		} finally {
			req.releaseConnection();
		}

		return success;
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
	 * @throws Exception 
	 */
	public boolean updateFileCharacterize(String object, String compId, String fileName,
			List<NameValuePair> optionalParams) throws Exception {
		String format = null;
		String url = getFilesURL(object, compId, fileName, "characterize", format);
		HttpPut req = new HttpPut(url);
		//req.setEntity(new UrlEncodedFormEntity(optionalParams));
		req.setEntity(toMultiPartEntity(optionalParams));
		int status = -1;
		boolean success = false;
		try {
			status = execute(req);
			success= (status == 200 || status == 201);
			if (!success)
				handleError(format);
		} finally {
			req.releaseConnection();
		}

		return success;
	}
	
	/**
	 * Extract Jhove but not save to the triplestore
	 * @param object
	 * @param fileName
	 * @return
	 * @throws Exception 
	 */
	public DFile extractFileCharacterize(String object, String compId, String fileName) throws Exception {
		String url = getFilesURL(object, compId, fileName, "characterize", "json");
		HttpGet req = new HttpGet(url);
		DFile dfile = null;
		try {
			JSONObject mData = (JSONObject)getJSONResult(req).get("characterization");
			if (mData != null)
				dfile = DFile.toDFile(mData);
		} finally {
			req.releaseConnection();
		}

		return dfile;
	}
	
	/**
	 * Test whether a object or file exists.
	 * 
	 * @param object
	 * @param fileName
	 * @return
	 * @throws Exception
	 */
	public boolean exists(String object, String compId, String fileName) throws Exception {
		boolean exists = false;
		String format = null;
		String url = null;
		
		if (object != null && (object = object.trim()).length() > 0) {
			if (fileName != null && (fileName = fileName.trim()).length() > 0)
				url = getFilesURL(object, compId, fileName, "exists", format);
			else
				url = getObjectsURL(object, compId, "exists", format);
		} else {
			throw new Exception("Object identifier must be specified");
		}
		HttpGet req = new HttpGet(url);
		int status = -1;
		try {
			status = execute(req);
			if (status == 200) {
				exists = true;
			} else if (status == 404) 
				exists = false;
			else
				handleError(format);
		} catch(FileNotFoundException e){
			exists = false;
		} finally {
			req.releaseConnection();
		}
		return exists;
	}

	/**
	 * Get the list of predicates in use.
	 * @throws Exception 
	 **/
	public Map<String, String> getPredicates() throws Exception {
		Map<String, String> predicates = new HashMap<String, String>();
		String url = getDAMSFunctionURL("system", "predicates", "xml");
		HttpGet get = new HttpGet(url);
		Document doc = getXMLResult(get);
		List<Node> nodes = doc.selectNodes(DOCUMENT_RESPONSE_ROOT_PATH + "/predicates/value");
		Node node = null;
		
		for(Iterator<Node> it=nodes.iterator(); it.hasNext();){
			node = (Node)it.next();
			predicates.put(node.selectSingleNode("@key").getStringValue(), node.getText());
		}
		return predicates;
	}

	/**
	 * Retrieve a file and copy it to another location.
	 * @throws Exception 
	 **/
	public int download(String object, String compId, String fileName, String destFile)
			throws Exception {

		InputStream in = null;
		OutputStream out = null;
		int bytesRead = -1;
		try {
			in = read(object, compId, fileName);
			out = new FileOutputStream(destFile);
			bytesRead = copy(in, out);
		} finally {
			close(in);
			close(out);
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
	 * @throws Exception 
	 * @throws DAMSException
	 */
	public InputStream read(String object, String compId, String fileName) throws Exception {
		String format = null;
		String url = null;
		if (fileName != null && (fileName = fileName.trim()).length() > 0)
			url = getFilesURL(object, compId, fileName, null, format);
		else
			url = getObjectsURL(object, compId, null, format);
		HttpGet get = new HttpGet(url);
		int status = -1;
		try{
			status = execute(get);
			if(status != 200)
				handleError(format);
		}finally{
			get.releaseConnection();
		}

		return new HttpContentInputStream(response.getEntity().getContent(), get);
	}
	
	/**
	 * Create a new file.
	 * @param object
	 * @param compId
	 * @param fileName
	 * @param srcFile
	 * @param use
	 * @return
	 * @throws Exception
	 */
	public boolean createFile(String object, String compId, String fileName, String srcFile, String use) throws Exception {
		Map<String, String> params = new HashMap<String, String>();
		params.put("oid", object);
		params.put("cid", compId);
		params.put("fid", fileName);
		params.put("local", srcFile);
		params.put("use", use);
		
		// Add field dateCreated, sourceFileName, sourcePath etc.
		File file = new File(srcFile);
		params.put("sourcePath", file.getParent());
		params.put("sourceFileName", file.getName());
		params.put("dateCreated", damsDateFormat.format(file.lastModified()));
		return createFile(params);
	}
	
	/**
	 * Create a new file.
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public boolean createFile(Map<String, String> params) throws Exception {
		return uploadFile(params, false);
	}
	
	/**
	 * Create, update/replace a file.
	 * @param params
	 * @param replace
	 * @return
	 * @throws Exception
	 */
	public boolean uploadFile(Map<String, String>params, boolean replace) throws Exception {

		//Retrieve the app parameters that need processing
		String oid = params.remove("oid");
		String cid = params.remove("cid");
		String fid = params.remove("fid");
		String srcFile = params.remove("local");
		if(srcFile == null)
			throw new Exception("Parameter local for the source file is missing.");
		
		String format = params.get("format");
		String url = getFilesURL(oid, cid, fid, null, format);
		int status = -1;
		boolean success = false;
		HttpEntityEnclosingRequestBase req = null;
		try {
			if(replace && exists(oid, cid, fid)) {
				req = new HttpPut(url);
			} else {
				req = new HttpPost(url);
			}
			
			
			String pName = null;
			String pValue = null;
			MultipartEntity ent = toMultiPartEntity(srcFile);
			for(Iterator<String> it=params.keySet().iterator(); it.hasNext();){
				pName = it.next();
				pValue = params.get(pName);
				if(pValue != null && (pValue=pValue.trim()).length() > 0)
					ent.addPart(pName, new StringBody(pValue));
			}
			
			req.setEntity(ent);
			status = execute(req);
			success = (status == 200 || status == 201);
			if(!success)
				handleError(format);
		} finally {
			req.releaseConnection();
		}
		return success;
	}
	
	/**
	 * Create, update/replace a file.
	 * 
	 * @param object
	 * @param fileName
	 * @param in
	 * @param len
	 * @return
	 * @throws Exception 
	 */
	public boolean uploadFile(String object, String compId, String fileName, InputStream in, long len, boolean replace) throws Exception {
		HttpEntityEnclosingRequestBase req = null;
		String format = null;
		String url = getFilesURL(object, compId, fileName, null, format);
		int status = -1;
		boolean success = false;
		try {
			
			if(replace && exists(object, compId, fileName)){
				req = new HttpPut(url);
			} else {
				req = new HttpPost(url);
			}
			req.setEntity(toMultiPartEntity(in, fileName));
			status = execute(req);
			success = (status == 200 || status == 201);
			if(!success)
				handleError(format);
		} finally {
			req.releaseConnection();
		}
		return success;
	}

	/**
	 * Create, update/replace an object with a object RDF XML.
	 * 
	 * @param object
	 * @param triples
	 * @return
	 * @throws Exception 
	 */
	public boolean updateObject(String object, String xml, String mode)
			throws Exception {
		
		String format = null;
		HttpEntityEnclosingRequestBase req = null;
		String url = getObjectsURL(object, null, null, format);
		if(exists(object, null, null)) {
			req = new HttpPut(url);
		} else {
			req = new HttpPost(url);
		}
	
		ByteArrayInputStream in = null;
		int status = -1;
		boolean success = false;
		try {
			in = new ByteArrayInputStream(xml.getBytes());
			MultipartEntity ent = toMultiPartEntity(in, "rdf.xml");
			if(mode != null)
				ent.addPart("mode", new StringBody(mode));
			req.setEntity(ent);
			status = execute(req);
			success = (status == 200 || status == 201);
			if(!success)
				handleError(format);
		} finally {
			req.releaseConnection();
			close(in);
		}
		return success;
	}

	/**
	 * Add metadata to an object
	 * 
	 * @param object
	 * @param triples
	 * @return
	 * @throws Exception 
	 */
	public boolean addMetadata(String object, List<Statement> stmts)
			throws Exception {
		String format = null;
		HttpEntityEnclosingRequestBase req = null;
		String url = getObjectsURL(object, null, null, format);

		/*
		List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
		nameValuePairs.add(new BasicNameValuePair("subject", object));
		nameValuePairs.add(new BasicNameValuePair("adds", toJSONString(stmts)));
		nameValuePairs.add(new BasicNameValuePair("mode", "add"));
		*/
		int status = -1;
		boolean success = false;
		try {
			if(exists(object, null, null)) {
				req = new HttpPut(url);
			} else {
				req = new HttpPost(url);
			}
			MultipartEntity ent = new MultipartEntity();
			ent.addPart("subject", new StringBody(object));
			ent.addPart("adds", new StringBody(toJSONString(stmts)));
			ent.addPart("mode", new StringBody(Constants.IMPORT_MODE_ADD));
			req.setEntity(ent);
			status = execute(req);
			success= (status == 200 || status == 201);
			if(!success)
				handleError(format);
		} finally {
			req.releaseConnection();
		}
		return success;
	}
	
	/**
	 * Add metadata elements to an object
	 * 
	 * @param object
	 * @param triples
	 * @return
	 * @throws Exception 
	 */
	public boolean addMetadata(String object, String xml)
			throws Exception {
		List<Statement> stmts = new ArrayList<Statement>();
		// XXX
		// Convert xml metadata elements to Statement
		
		
		return  addMetadata(object, stmts);
	}

	/**
	 * Perform selective predicates deletion
	 * @param object
	 * @param compId
	 * @param predicates
	 * @return
	 * @throws ClientProtocolException
	 * @throws LoginException
	 * @throws IOException
	 * @throws IllegalStateException
	 * @throws DocumentException
	 */
	public boolean selectiveMetadataDelete(String object, String compId, List<String> predicates) throws ClientProtocolException, LoginException, IOException, IllegalStateException, DocumentException{
		String format = null;
		String url = null;
		HttpDelete del = null;
		url = getObjectsURL(object, compId, "selective", format);
		String params = url.indexOf("?")>0?"&":"?";
		for(Iterator<String> it=predicates.iterator();it.hasNext();)
			params += "predicate=" + it.next() + "&";
		del = new HttpDelete(url+params.substring(0, params.length()-1));
		int status = -1;
		boolean success = false;
		try {
			status = execute(del);
			success= (status == 200 || status == 201);
			if(!success)
				handleError(format);
		}finally{
			del.releaseConnection();
		}
		return success;
	}
	
	/**
	 * Delete an object or a file.
	 * 
	 * @param object
	 * @param file
	 *            If not null, delete this file.
	 * @throws Exception
	 **/
	public boolean delete(String object, String compId, String fileName) throws Exception {
		String format = null;
		String url = null;
		if (object != null && (object = object.trim()).length() > 0) {
			if (fileName != null && (fileName = fileName.trim()).length() > 0)
				url = getFilesURL(object, compId, fileName, null, format);
			else 
				url = getObjectsURL(object, compId, null, format);
		} else {
			throw new Exception("Object identifier must be specified");
		}

		HttpDelete del = new HttpDelete(url);
		int status = -1;
		boolean success = false;
		try {
			status = execute(del);
			success= (status == 200 || status == 201);
			if(!success)
				handleError(format);
		} finally {
			del.releaseConnection();
		}
		return success;
	}
	
	/**
	 * List the master service files in an object.
	 * @return List of top level component node
	 * @throws Exception 
	 * @throws DocumentException 
	 */
	public List<DFile> listServiceFiles(String object) throws DocumentException, Exception{
		List<DFile> mFiles = new ArrayList<DFile>();
		List<DFile> dFiles = listObjectFiles(object);
		DFile dFile = null;
		String use = null;
		for(Iterator<DFile> it=dFiles.iterator(); it.hasNext();){
			dFile = it.next();
			use = dFile.getUse();
			if (use != null && use.endsWith("-service"))
				mFiles.add(dFile);
		}
		return mFiles;
	}

	/**
	 * Execute a http request
	 * @param request
	 * @return
	 * @throws ClientProtocolException
	 * @throws IOException
	 * @throws LoginException
	 */
	public int execute(HttpRequestBase req) throws ClientProtocolException,
			IOException, LoginException {

		int statusCode = -1;
		response = null;
		request = req;
		try{
			if (httpContext != null) {
				response = client.execute(request, httpContext);
			} else {
				response = client.execute(request);
			}
		}finally{
			if(response != null)
				statusCode = response.getStatusLine().getStatusCode();
			log.info(statusCode + " " + req.getMethod() + " " + req.getURI());
		}

		return statusCode;
	}
	
	/**
	 * Construct the functional URL for DAMS REST API
	 * @param path -
	 * 			system, nextId, index, client etc.
	 * @param object
	 * @param function -
	 * 			triplestores, filestores, predicates etc.
	 * @param format
	 * 			xml, html, json,  etc.
	 * @return
	 */
	public String getDAMSFunctionURL(String path, String function, String format){
		String[] parts = {path, function};
		return storageURL + toUrlPath(parts) + (format!=null&&format.length()>0?"?format="+format:"");
	}

	public String getIndexURL(String format){
		String[] parts = {"index"};
		return toDAMSURL(parts, format);
	}
	
	/**
	 * Construct REST URL for administration unit
	 * @param collection
	 * @param function
	 * @param format
	 * @return
	 */
	public String getUnitsURL(String unit, String function, String format){
		String[] parts = {"units", unit, function};
		return toDAMSURL(parts, format);
	}
	
	/**
	 * Construct REST URL for collections
	 * @param collection
	 * @param function
	 * @param format
	 * @return
	 */
	public String getCollectionsURL(String collection, String function, String format){
		String[] parts = {"collections", collection, function};
		return toDAMSURL(parts, format);
	}
	
	/**
	 * Construct REST URL for objects and components
	 * @param object
	 * @param compId
	 * @param function
	 * @param format
	 * @return
	 */
	public String getObjectsURL(String object, String compId, String function, String format){
		String[] parts = {"objects", object, compId, function};
		return toDAMSURL(parts, format);
	}
	
	/**
	 * Construct Files REST URL
	 * @param object
	 * @param compId
	 * @param fileName
	 * @param function
	 * @param format
	 * @return
	 */
	public String getFilesURL(String object, String compId, String fileName, String function, String format){
		String[] parts = {"files", object, compId, fileName, function};
		return toDAMSURL(parts, format);
	}
	
	/**
	 * Construct DAMS REST URL
	 * @param urlParts
	 * @param format
	 * @return
	 */
	public String toDAMSURL(String[] urlParts, String format){
		NameValuePair[] params = {new BasicNameValuePair("format", format), new BasicNameValuePair("ts",tripleStore), new BasicNameValuePair("fs", fileStore)};
		String paramsStr = concatParams(params);
		//System.out.println(storageURL + toUrlPath(urlParts) + (paramsStr.length()>0?"?":"") + paramsStr);
		return storageURL + toUrlPath(urlParts) + (paramsStr.length()>0?"?":"") + paramsStr;
	}
	
	/**
	 * Execute http request for JSON format
	 * @param request
	 * @return
	 * @throws Exception 
	 */
	public JSONObject getJSONResult(HttpRequestBase req) throws Exception{
		JSONObject resObj = null;
		InputStream in = null;
		Reader reader = null;
		int status = -1;
		try {
			status = execute(req);
		
			if(status == 200){
				in = response.getEntity().getContent();
				reader = new InputStreamReader(in);
				resObj = (JSONObject) JSONValue.parse(reader);
			}else{
				handleError("json");
			}
		}finally{
			req.releaseConnection();
			close(in);
			close(reader);
		}
		return resObj;
	}
	
	/**
	 * Execute http request for XML format
	 * @param request
	 * @return
	 * @throws Exception 
	 */
	public Document getXMLResult(HttpRequestBase req) throws Exception{
		Document resObj = null;
		InputStream in = null;
		SAXReader reader = null;
		int status = -1;
		try {
			status = execute(req);
			if(status == 200){
				reader = new SAXReader();
				in = response.getEntity().getContent();
				resObj = reader.read(in);
			}else{
				handleError("json");
			}
		}finally{
			req.releaseConnection();
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
	 * Concatenate parameters
	 * @param params
	 * @return
	 */
	public String concatParams(NameValuePair[] params) {
		String paramStr = "";
		for(int i=0; i<params.length; i++){
			String paramValue = params[i].getValue();
			if(paramValue != null && paramValue.length() > 0)
				paramStr += (paramStr.length()>0?"&":"") + params[i].getName() + "=" + paramValue;
		}
		return paramStr;
	}
	
	/**
	 * Convert object ID and the file name to DAMS REST URL path: objectId/[componentId/]/fileName
	 * @param objectId
	 * @return
	 */
	public String toUrlPath(String[] parts){
		String path = "";
		for(int i=0; i<parts.length; i++){
			if(parts[i] != null && parts[i].length() > 0){
				path += "/" + stripID(parts[i]);
			}
		}

		return path;
	}

	/**
	 * Retrieve the HTTP content from a URL provided.
	 * 
	 * @param url
	 * @return
	 * @throws Exception 
	 */
	public String getContentBodyAsString(String url) throws Exception {
		HttpGet get = new HttpGet(url);
		int status = -1;
		try {
			status = execute(get);
			if(status != 200)
				handleError(null);
		} finally {
			get.releaseConnection();
		}
		return EntityUtils.toString(response.getEntity());
	}

	/**
	 * Log response header and body information.
	 * @throws IOException 
	 * @throws IllegalStateException 
	 * @throws DocumentException 
	 * @throws LoginException 
	 * @throws Exception 
	 */
	public void handleError(String format) throws IllegalStateException, IOException, DocumentException, LoginException {
		int status = response.getStatusLine().getStatusCode();
		Header[] headers = response.getAllHeaders();
		for (int i = 0; i < headers.length; i++) {
			System.out.print(headers[i] + "; ");
		}

		String respContent = "";
		if ( !request.getMethod().equals("HEAD") ) {

			HttpEntity ent = response.getEntity();
			InputStream in = null;
			try{
				in = ent.getContent();
				String contentType = ent.getContentType().getValue();
				if(contentType.indexOf("xml") >= 0){
					try{
						SAXReader saxReader = new SAXReader();
						Document doc = saxReader.read(in);
						System.out.println(doc.asXML());
						Node node = doc.selectSingleNode(DOCUMENT_RESPONSE_ROOT_PATH + "/status");
						
						if(node != null)
							respContent += node.getText();
						node = doc.selectSingleNode(DOCUMENT_RESPONSE_ROOT_PATH + "/statusCode");
						if(node != null)
							respContent += " status code " + doc.selectSingleNode(DOCUMENT_RESPONSE_ROOT_PATH + "/statusCode").getText();
						node = doc.selectSingleNode(DOCUMENT_RESPONSE_ROOT_PATH + "/message");
						if(node != null)
							respContent += ". Error " + node.getText();
					}catch (Exception e){
						e.printStackTrace();
					}
				} else if(format.equals("json")){
					Reader reader = new InputStreamReader(in);
					JSONObject resultObj = (JSONObject) JSONValue.parse(reader);
					respContent += resultObj.get("status") + " status code " + resultObj.get("statusCode") + ". Error " + resultObj.get("message");
					System.out.println(resultObj.toString());
					reader.close();
				} else {

					byte[] buf = new byte[4096];
					StringBuilder strContent = new StringBuilder();
					int bRead = -1;

					while((bRead=in.read(buf)) > 0 && strContent.length() < MAX_SIZE)
						strContent.append((char)bRead);
					respContent = strContent.toString();
					System.out.println(respContent);
				}
			}finally{
				close(in);
			}
		}
		//200 - OK: Success, object/file exists
		//201 - Created: File/object created successfully
					
		String reqInfo = request.getMethod() + " " + request.getURI();
		log.info( reqInfo + ": " + respContent);
		//401 - unauthorized access
		if (status == 401) {  
			throw new LoginException(reqInfo + ": " + respContent);
			
		//403 - Forbidden: Deleting non-existing file, using POST to update or PUT to create
		} else if (status == 403) {  
			if(respContent.indexOf("not exists") > 0)
				throw new FileNotFoundException(reqInfo + ": " + respContent);
			else 
				throw new IOException(reqInfo + ": " + respContent);
			
		//404 - Not Found: Object/file does not exist 
		} else if (status == 404) { 
			throw new FileNotFoundException(reqInfo + ": " + respContent);
			
		//500 - Internal Error: Other errors
		} else if (status == 500) {  
			throw new IOException(reqInfo + ": " + respContent);
			
		//502 - Unavailable: Too many uploads 
		} else if (status == 502) { 
			throw new IOException(reqInfo + ": " + respContent);
			
		//Unknown status code
		} else { 
			throw new IOException(reqInfo + ": " + respContent);
		}
	}
	
	public String getResponseMessage() throws ParseException, IOException{
		return EntityUtils.toString(response.getEntity());
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
	 * Get SOLR URL base
	 * @return
	 */
	public String getSolrURLBase() {
		return solrURLBase;
	}

	/**
	 * Set SOLR URL base
	 * @param solrURLBase
	 */
	public void setSolrURLBase(String solrURLBase) {
		this.solrURLBase = solrURLBase;
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
	 * Create MultipartEntity
	 * @param srcFile
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static MultipartEntity toMultiPartEntity(String srcFile) throws UnsupportedEncodingException{
		File file = new File(srcFile);
		MultipartEntity ent = new MultipartEntity();
		String srcAbsPath = file.getAbsolutePath();
		String stagingAbsPath = new File(Constants.DAMS_STAGING).getAbsolutePath();
		int idx = srcAbsPath.indexOf(stagingAbsPath);
		if(idx == 0){
			ent.addPart("local", new StringBody(srcAbsPath.substring(idx+stagingAbsPath.length()).replace("\\", "/")));
		}else{
			String contentType = new FileDataSource(srcFile).getContentType();
			FileBody fileBody = new FileBody(file, contentType);
			ent.addPart("file", fileBody);
			// Add field dateCreated, sourceFileName, sourcePath etc.
			ent.addPart("sourcePath", new StringBody(file.getParent()));
			ent.addPart("sourceFileName", new StringBody(file.getName()));
			ent.addPart("dateCreated", new StringBody(damsDateFormat.format(file.lastModified())));
		}
			
		return ent;
	}
	
	/**
	 * Create MultipartEntity
	 * @param in
	 * @param contentType
	 * @return
	 */
	public static MultipartEntity toMultiPartEntity(InputStream in, String fileName){
		FileDataSource fileDataSource = new FileDataSource(fileName);
		InputStreamBody inputBody = new InputStreamBody(in, fileDataSource.getContentType(), fileName);
		MultipartEntity ent = new MultipartEntity();
		ent.addPart("file", inputBody);
		return ent;
	}
	
	/**
	 * Create MultipartEntity
	 * @param pros
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static MultipartEntity toMultiPartEntity(List<NameValuePair> pros) throws UnsupportedEncodingException{
		NameValuePair n = null;
		MultipartEntity ent = new MultipartEntity();
		String value = null;
		for(Iterator<NameValuePair> it=pros.iterator(); it.hasNext();){
			n = it.next();
			value = n.getValue();
			if(value != null)
				ent.addPart(n.getName(), new StringBody(value));
		}
		return ent;
	}
	
	/**
	 * Convert list of Statements to JSON String
	 * @param stmts
	 * @return
	 */
	public static String toJSONString(List<Statement> stmts){
		Statement stmt = null;
		JSONObject tmp = null;
		JSONArray tmpArr = new JSONArray();
		int tSize = stmts.size();
		for(int i=0; i<tSize; i++){
			stmt = stmts.get(i);
			tmp = new JSONObject();
			tmp.put("subject", stripID(stmt.getSubject().getURI()));
			tmp.put("predicate", stripID(stmt.getPredicate().getURI()));
			RDFNode node = stmt.getObject();
			if(node.isLiteral())
				tmp.put("object", node.asLiteral().getValue());
			else
				tmp.put("object", node.asResource().getId().toString());
			tmpArr.add(tmp);
		}
		return tmpArr.toJSONString();
	}
	
	/**
	 * Ark org, subject and filename of a full ark file name.
	 * @param fullArkFileName
	 * @return
	 */
	public static String[] toFileParts(String fullArkFileName)  {
		return fullArkFileName.split("-", 4);
	}
	
	/**
	 * Mimetype
	 * @param filename
	 * @return
	 */
	public static String getMimeType(String filename){
		FileDataSource fileDataSource = new FileDataSource(filename);
		//MimetypesFileTypeMap mimeTypes = new MimetypesFileTypeMap();
		String mimeType = fileDataSource.getContentType();
		return mimeType;
	}
	
	/**
	 * Append items found in SOLR
	 * @param items
	 * @param unit
	 * @param collection
	 * @param start
	 * @param rows
	 * @return
	 * @throws Exception
	 */
	public boolean appendSOLRItems(List<String> items, String unitTitle, String collectionTitle, int start, int rows) throws Exception{
		String solrQuery = toSolrQuery(unitTitle, collectionTitle);
		String solrParams = solrQuery + "&rows=" + rows + "&start=" + start + "&fl=id&wt=xml";
		
		Document doc = solrLookup(solrParams);
		int numFound = Integer.parseInt(doc.selectSingleNode("/response/result/@numFound").getStringValue());
		start = Integer.parseInt(doc.selectSingleNode("/response/result/@numFound").getStringValue());
		List<Node> idNodes = doc.selectNodes("/response/result/doc/str[@name='id']");
		for (Iterator<Node> it = idNodes.iterator(); it.hasNext();){
			Node idNode = it.next();
			items.add(idNode.getText());
		}
		return rows+start < numFound; 
	}
	
	/**
	 * SOLR lookup
	 * @param solrQuery
	 * @return
	 * @throws Exception
	 */
	public Document solrLookup(String solrQuery) throws Exception{
		String url = getSolrURL();
		url += (url.endsWith("/")?"":"/") + "select?" + solrQuery + "&wt=xml";
		HttpGet req= new HttpGet(url);
		return getXMLResult(req);
	}
	
	/**
	 * Retrieve the URL for the SOLR server
	 * @return
	 */
	private String getSolrURL(){
		if(solrURLBase == null)
			return Constants.SOLR_URL_BASE;
		else
			return solrURLBase;
	}
	
	public void close(){
		client.getConnectionManager().shutdown();
	}
	
	/**
	 * Construct SOLR query
	 * @param unit
	 * @param collection
	 * @return
	 * @throws UnsupportedEncodingException 
	 */
	public static String toSolrQuery(String unit, String collection) throws UnsupportedEncodingException{
		String solrParams = (unit==null?"":"unit:\""+unit + "\"");
		solrParams += (collection!=null&&solrParams.length()>0?" AND ":"") + (collection!=null?"collection:"+collection:"");
		if(solrParams.length() > 0)
			solrParams = solrParams.replace(" ", "+");
		
		solrParams = "q=" + URLEncoder.encode(solrParams, "UTF-8");
		return solrParams;
	}
	
	/**
	 * Retrieve the files that have the same source fileName and source path
	 * @param doc
	 * @param srcPath
	 * @param srcFileName
	 * @return
	 * @throws Exception 
	 */
	public static List<DamsURI> getFiles(Document doc, String srcPath, String srcFileName) throws Exception{
		List<DamsURI> fileURIs = new ArrayList<DamsURI>();
		List<Node> idNodes = doc.selectNodes(DOCUMENT_RESPONSE_ROOT_PATH + "/files/value[sourceFileName='" + srcFileName.replace("'", "&quot;") + "']");
		if(idNodes != null){
			String id = null;
			String object = null;
			String filePath = null;
			Node idNode = null;
			int nSize =  idNodes.size();
			for(int i=0; i<nSize; i++){
				idNode = idNodes.get(i);
				id = idNode.selectSingleNode("id").getText();
				object = idNode.selectSingleNode("object").getText();
				//Source path restriction
				if(srcPath != null && srcPath.length() > 0){
					filePath = idNode.selectSingleNode("sourcePath").getText();
					if(srcPath.equalsIgnoreCase(filePath)){
						fileURIs.add(DamsURI.toParts(id, object));
					}
				} else {
					fileURIs.add(DamsURI.toParts(id, object));
				}
			}
		}
		return fileURIs;
	}
	
	/**
	 * Static method to send mail
	 * @param from
	 * @param to
	 * @param subject
	 * @param content
	 * @param contenType
	 * @param smtp
	 * @throws MessagingException
	 */
	public static void sendMail(String from, String [] to, String subject, 
			String content, String contenType, String smtp) throws MessagingException{
		// Create mail session
		Properties props = new Properties();
		props.put("mail.smtp.host", smtp);
		Session session = Session.getInstance(props,null);

		// Create destination address
		InternetAddress dests[] = new InternetAddress[to.length];
		for(int i = 0; i < to.length; i++) {
			dests[i] = new InternetAddress(to[i]);	
		}

		// Default content type
		if(contenType == null || contenType.length() == 0)
			contenType = "text/plain";
		
		// Create message
		Message message = new MimeMessage(session);
		message.setFrom(new InternetAddress(from));
		message.setRecipients(Message.RecipientType.TO, dests);
		message.setSubject(subject);
		message.setContent(content, contenType);

		// Send the mail
		Transport.send(message);
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
				request.releaseConnection();
			}
		}

		@Override
		public int read() throws IOException {
			return in.read();
		}
	}

	/**
	 * PreemptiveAuthInterceptor class for PreemptiveAuth
	 * @author lsitu
	 *
	 */
    class PreemptiveAuthInterceptor implements HttpRequestInterceptor {
        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {

            AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

            // If no auth scheme available yet, try to initialize it preemptively
            if (authState.getAuthScheme() == null) {
                AuthScheme authScheme = (AuthScheme) context.getAttribute("preemptive-auth");
                CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (authScheme != null) {
                    Credentials creds = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()));
                    if (creds == null) {
                        throw new HttpException("No credentials for preemptive authentication");
                    }
                    authState.update(authScheme, creds);
                }
            }
        }
    }
}
