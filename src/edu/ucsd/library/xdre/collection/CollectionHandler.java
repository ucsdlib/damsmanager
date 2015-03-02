package edu.ucsd.library.xdre.collection;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.Normalizer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.servlet.http.HttpSession;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Source;
import javax.xml.transform.Templates;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.Node;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DomUtil;
import edu.ucsd.library.xdre.utils.ProcessHandler;
import edu.ucsd.library.xdre.utils.RequestOrganizer;
import edu.ucsd.library.xdre.utils.RightsAction;

/**
 * Class CollectionHandler, a class for procedure handling
 * 
 * @author lsitu@ucsd.edu
 */
public abstract class CollectionHandler implements ProcessHandler {
	public static final String INFO_MODEL_PREFIX = "info:fedora/afmodel:";
	public static final String HAS_FILE = "hasFile";
	public static SimpleDateFormat damsDateFormat = new SimpleDateFormat(DAMSClient.DAMS_DATE_FORMAT);
	
	private static Logger log = Logger.getLogger(CollectionHandler.class);
	protected static Random random = new Random();
	
	protected int submissionId = -1;
	protected String collectionId = null;
	protected List<String> items = null;

	protected int itemsDone = 0;
	protected int maxTry = 2;
	protected int userId = -1;

	protected boolean excludeEmbargoed = false;
	protected boolean exeResult = true;
	protected boolean multiThreading = false;
	protected boolean interrupted = false;

	protected HttpSession session = null;
	protected StringBuilder exeReport = null;
	protected FileWriter logWriter = null;

	protected String collectionData = null;
	protected DAMSClient damsClient = null;

	protected String collectionTitle = null;
	protected Map<String, String> collectionsMap = null;
	protected Map<String, String> unitsMap = null;
	protected int itemsCount = 0; //Total number of items in the collection/batch
	protected List<String> solrFailed = new ArrayList<String>();
	
	/**
	 * Empty constructor
	 */
	public CollectionHandler() {}
	
	public CollectionHandler(DAMSClient damsClient) throws Exception{
		this(damsClient, null);
	}

	/**
	 * Constructor
	 * @param damsClient
	 * @param collectionId
	 * @throws Exception
	 */
	public CollectionHandler(DAMSClient damsClient, String collectionId) throws Exception{
		this.damsClient = damsClient;
		this.collectionId = collectionId;
		init();
	}

	/**
	 * Constructor
	 * @param damsClient
	 * @param collectionId
	 * @param session
	 * @param userId
	 * @throws Exception
	 */
	public CollectionHandler(DAMSClient damsClient, String collectionId, HttpSession session,
			int userId) throws Exception {
		this(damsClient, collectionId);
		this.session = session;
		this.userId = userId;
	}

	/**
	 * Object initiation for a collection 
	 * @throws Exception
	 */
	protected void init() throws Exception {
		exeReport = new StringBuilder();
		collectionsMap = new HashMap<String, String>();
		unitsMap = damsClient.listUnits();
		Map<String, String> colls = damsClient.listCollections();
		Entry<String, String> ent = null;
		
		colls.putAll(unitsMap);
		for (Iterator<Entry<String, String>> it=colls.entrySet().iterator(); it.hasNext();){
			ent = (Entry<String, String>) it.next();
			String colId = ent.getValue();
			String colTitle = ent.getKey();
			collectionsMap.put(colId, colTitle);
		}

		if (collectionId != null && collectionId.length() > 0) {
			items = listItems(collectionId);
			//collectionData = damsClient.getMetadata(collectionId, null);
			itemsCount = items.size();
			collectionTitle = collectionsMap.get(collectionId);
		}
	}

	/**
	 * Retrieve embargoed objects in the collection
	 * @param collectionId
	 * @return
	 * @throws Exception 
	 */
	public List<String> getEmbargoedItems(String collectionId) throws Exception{
		List<String> embargoes = new ArrayList<String>();
		List<RightsAction> embargoList = listEmbargoes(collectionId, null);
		for(Iterator<RightsAction> it=embargoList.iterator(); it.hasNext();){
			embargoes.add(it.next().getOid());
		}
		return embargoes;
	}
	
	/**
	 * Retrieve embargoed objects in a group like collection, unit, etc.
	 * @param collectionId
	 * @param group - Unit, Collection etc.
	 * @return
	 * @throws Exception 
	 */
	public List<RightsAction> listEmbargoes(String id, String group) throws Exception{
		if(group != null && group.equalsIgnoreCase("unit")){
			return damsClient.getUnitEmbargoeds(id);
		}else
			return damsClient.getCollectionEmbargoeds(id);
	}
	
	/**
	 * Retrieve the file extension
	 * @param subjectId
	 * @return
	 */
	public String getFileExtension(String subjectId, String compId, String fileId){
		//XXX
		return null;
	}

	/**
	 * Populate the RDF XML
	 */
	public void populateSubject(String rdfXml, int operation) throws Exception{
		//XXX
	}
	
	public String getDDOMReference(String subject) {
		String tsParam = "";
		String tripleStore = damsClient.getTripleStore();
		if (tripleStore != null)
			tsParam = "&ds=" + tripleStore;
		return "<a href='" + "/xdre/ddom/getSubject.jsp?subject=" + subject
				+ tsParam + "' target='_blank'> subject " + subject + "</a>";
	}

	public static String getDDOMReference(String subject, String ddomDSName) {
		return "<a href='" + "/xdre/ddom/getSubject.jsp?subject=" + subject
				+ "&ds=" + ddomDSName + "' target='_blank'> subject " + subject
				+ "</a>";
	}

	public int getUserId() {
		return userId;
	}

	public void setUserId(int userId) {
		this.userId = userId;
	}

	public void setSession(HttpSession session) {
		this.session = session;
	}

	public void setStatus(String message) {
		if (session != null) {
			try {
				session.setAttribute("status", message.replace("\n", "<br />"));
			} catch (IllegalStateException e) {
				// e.printStackTrace();
			}
		}
	}
	
	public void logError(String message){
		exeResult = false;
		log.error(message);
		logMessage(message);
	}
	
	public void logMessage(String message){
		setStatus(message);
		log("log", message);
	}

	public void log(String key, String message) {
		if (session != null) {
			if ("log".equalsIgnoreCase(key)) {

				RequestOrganizer.addLogMessage(session,
						message.replace("\n", "<br />") + "<br />");
				synchronized (session) {
					// Write to the log file
					if (logWriter == null) {
						String logFileName = (Constants.TMP_FILE_DIR==null||Constants.TMP_FILE_DIR.length()==0?"":Constants.TMP_FILE_DIR+"/")
								+ "damslog-" + submissionId + ".txt";
						try {
							logWriter = new FileWriter(logFileName, true);
							logWriter.write("\n"
									+ "-------------------------------- "
									+ (new Date()).toString()
									+ " --------------------------------"
									+ "\n");

						} catch (IOException e) {
							e.printStackTrace();
							close(logWriter);
							logWriter = null;
						}
					}
					if (logWriter != null) {
						try {
							logWriter.write(message + "\n");
							logWriter.flush();
						} catch (IOException e) {
							e.printStackTrace();
							close(logWriter);
							logWriter = null;
						}
					}
				}
			} else if ("result".equalsIgnoreCase(key)) {
				RequestOrganizer.addResultMessage(session,
						message.replace("\n", "<br />") + "<br />");
			}
		}
	}

	public static String getFileContent(String fileName) throws IOException {
		StringBuilder strBuilder = new StringBuilder();
		Reader reader = new FileReader(fileName);
		// BufferedReader br = new BufferedReader(reader);
		char[] buffer = new char[1024 * 1000];
		int bytesReads = 0;
		while ((bytesReads = reader.read(buffer)) > 0) {
			strBuilder.append(buffer, 0, bytesReads);
		}
		reader.close();
		return strBuilder.toString();
	}
	
	public static Element createSubject(String subjectId, boolean useArk) {
		Element rdfSubjectElem = DomUtil.createElement(null,
				StringEscapeUtils.escapeXml("rdf:Description"), null, null);
		if (subjectId != null) {
			String attrLabel = "rdf:about";
			Attribute attr = DocumentHelper.createAttribute(rdfSubjectElem,
					StringEscapeUtils.escapeXml(attrLabel),
					StringEscapeUtils.escapeXml(subjectId));
			rdfSubjectElem.add(attr);
		}

		appendNameSpaceAttributes(rdfSubjectElem, useArk);
		return rdfSubjectElem;
	}

	public static void appendNameSpaceAttributes(Element elem, boolean useArk) {
		if (!useArk) {
			elem.addNamespace("mods", "http://www.loc.gov/mods/v3");
			elem.addNamespace("mix", "http://www.loc.gov/mix/v20");
			elem.addNamespace("pre", "http://www.loc.gov/standards/premis/v1");
			elem.addNamespace("rts",
					"http://cosimo.stanford.edu/sdr/metsrights/");;
			elem.addNamespace("rdf",
					"http://www.w3.org/1999/02/22-rdf-syntax-ns#");
			elem.addNamespace("file", "http://libraries.ucsd.edu/dams/");
			elem.addNamespace("xdre", "http://libraries.ucsd.edu/dams/");
		} else {
			elem.addNamespace("rdf",
					"http://www.w3.org/1999/02/22-rdf-syntax-ns#");
		}
	}

	public String leadingPhrase(String literal) {
		String tmp = null;
		if (literal == null)
			tmp = "Null";
		else if (literal.length() == 0)
			tmp = "";
		else {
			tmp = literal.substring(0, 1).toUpperCase() + literal.substring(1);
		}
		return tmp;
	}

	public long getFilesCount() {
		return items.size();
	}

	public void clearSession() {
		RequestOrganizer.clearSession(session);
	}

	public void release() throws Exception {
		close(logWriter);	
		logWriter = null;
	}

	public boolean getExeResult() {
		return exeResult;
	}

	public void setExeResult(boolean exeResult) {
		this.exeResult = exeResult;
	}

	public void setProgressPercentage(long percent) {
		if (session != null) {
			try {
				session.setAttribute(
						RequestOrganizer.PROGRESS_PERCENTAGE_ATTRIBUTE,
						Long.toString(percent));
			} catch (IllegalStateException e) {
				e.printStackTrace();
			}
		}
	}

	public List<String> getItems() {
		return items;
	}

	public void setItems(List<String> items) {
		this.items = items;
		itemsCount = items.size();
	}

	public String getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(String collectionId) {
		this.collectionId = collectionId;
	}

	public void outputXML(org.w3c.dom.Document doc, OutputStream out)
			throws IOException, TransformerException {
		// Serialize the document
		DOMSource domSource = new DOMSource(doc);
		StreamResult streamResult = new StreamResult(out);
		TransformerFactory tf = TransformerFactory.newInstance();
		Transformer serializer = tf.newTransformer();
		serializer.setOutputProperty(OutputKeys.INDENT, "yes");
		serializer.transform(domSource, streamResult);
	}

	public String generateMets(String subjectId) throws Exception {

		String response = damsClient.getMetadata(subjectId, "mets");
		if (!validateFile(response, subjectId, "mets:mets")) {
			throw new Exception("Failed to retrieve METS for subject "
					+ subjectId + ".");
		}
		return response;
	}

	public boolean validateFile(String fileContent, String subjectId,
			String element) {
		if (fileContent.indexOf(element) > 0
				&& fileContent.indexOf(subjectId) > 0)
			return true;
		return false;
	}

	public String loadRdf(String fileName, int itemsCount, int operation)
			throws DocumentException, MalformedURLException {
		File rdfFile = new File(fileName);
		return loadRdf(rdfFile, itemsCount, operation);
	}

	public String loadRdf(File rdfFile, int itemsCount, int operation)
			throws DocumentException, MalformedURLException {
		//XXX
		return null;
	}

	public static void transformCSV(String outputFile, String rdfXml,
			String xslFile, String category) throws TransformerException,
			MalformedURLException, IOException {
		Source xslSrc = null;
		Reader readerRdfSrc = null;
		InputStream inXslSrc = null;
		Reader readerXslSrc = null;
		FileOutputStream outputStream = null;
		try {
			readerRdfSrc = new FileReader(rdfXml);
			outputStream = new FileOutputStream(outputFile);
			Source xmlSource = new StreamSource(readerRdfSrc);
			if (xslFile.startsWith("http://") || xslFile.startsWith("https://")) {
				inXslSrc = new URL(xslFile).openStream();
				readerXslSrc = new InputStreamReader(inXslSrc);
			} else
				readerXslSrc = new FileReader(xslFile);

			xslSrc = new StreamSource(readerXslSrc);
			StreamResult result = new StreamResult(outputStream);

			TransformerFactory transFact = TransformerFactory.newInstance();
			Templates xslTemp = transFact.newTemplates(xslSrc);
			Transformer trans = xslTemp.newTransformer();
			trans.setOutputProperty("encoding", "UTF8");
			trans.setOutputProperty(OutputKeys.INDENT, "yes");
			trans.setParameter("category", category);
			trans.transform(xmlSource, result);
		} finally {
			close(readerRdfSrc);
			close(inXslSrc);
			close(readerXslSrc);
			close(outputStream);
		}
	}

	public static String getIDValue(String uri) {
		if (uri.startsWith("<") && uri.endsWith(">"))
			uri = uri.substring(1, uri.length() - 1);
		return uri;
	}

	public static void copyFile(File src, File desc) throws IOException {
		byte[] buf = new byte[5120];
		InputStream in = null;
		OutputStream out = null;
		if (desc.exists())
			desc.delete();
		File descDir = desc.getParentFile();
		if (!descDir.exists()) {
			if (!descDir.mkdirs())
				throw new IOException("Failed to create path: "
						+ descDir.getAbsolutePath() + ".");
		}
		if (!desc.exists())
			if (!desc.createNewFile())
				throw new IOException("Unable to cerate file: "
						+ desc.getAbsolutePath());

		try {
			in = new FileInputStream(src);
			out = new FileOutputStream(desc);
			int len = -1;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
			}
		} finally {
			close(in);
			close(out);
		}
	}

	public void localCopyFile(File src, File desc) throws IOException {
		byte[] buf = new byte[5120];
		InputStream in = null;
		OutputStream out = null;
		if (desc.exists())
			desc.delete();
		File descDir = desc.getParentFile();
		if (!descDir.exists()) {
			if (!descDir.mkdirs())
				throw new IOException("Failed to create path: "
						+ descDir.getAbsolutePath() + ".");
		}
		if (!desc.exists())
			if (!desc.createNewFile())
				throw new IOException("Unable to cerate file: "
						+ desc.getAbsolutePath());
		long srcLength = src.length();
		long lenthCopied = 0;
		try {
			in = new FileInputStream(src);
			out = new FileOutputStream(desc);
			int len = -1;
			while ((len = in.read(buf)) > 0) {
				out.write(buf, 0, len);
				lenthCopied += len;
				if (lenthCopied % 100000000 == 0)
					setStatus("Copying file " + src.getName() + " to "
							+ desc.getPath() + ": "
							+ (lenthCopied * 100 / srcLength) + "% done ...");
			}
		} finally {
			close(in);
			close(out);
		}
	}

	public static boolean isArkFileName(String fileName) {
		return fileName != null && fileName.length() > 20
				&& fileName.startsWith("20775-") && fileName.charAt(18) == '-'
				&& fileName.lastIndexOf(".") > 19;
	}
	
	/**
	 * Ark org, subject and filename of a full ark file name.
	 * @param fullArkFileName
	 * @return

	public static String[] toFileParts(String fullArkFileName)  {
		return fullArkFileName.split("-", 4);
	}
	 */
	public int getSubmissionId() {
		return submissionId;
	}

	public void setSubmissionId(int submissionId) {
		this.submissionId = submissionId;
	}

	public DAMSClient getDamsClient() {
		return damsClient;
	}

	public void setDamsClient(DAMSClient damsClient) {
		this.damsClient = damsClient;
	}

	public String getCollectionTitle() {
		return collectionTitle;
	}
	
	public String getCollectionTitle(String colId) {
		return collectionsMap.get(colId);
	}

	public void setCollectionTitle(String collectionTitle) {
		this.collectionTitle = collectionTitle;
	}

	public boolean isMultiThreading() {
		return multiThreading;
	}

	public void setMultiThreading(boolean multiThreading) {
		this.multiThreading = multiThreading;
	}

	public boolean isExcludeEmbargoed() {
		return excludeEmbargoed;
	}

	public void excludeEmbargoedObjects() throws Exception {
		List<String> embargoItems = getEmbargoedItems(collectionId);
		int idx = -1;
		if (embargoItems.size() > 0) {
			for (Iterator it = embargoItems.iterator(); it.hasNext();) {
				idx = items.indexOf(it.next());
				if (idx >= 0)
					items.remove(idx);
			}
		}
		excludeEmbargoed = true;
	}
	
	/**
	 * Check for image file
	 * @param fileName
	 * @return
	 */
	public boolean isImage(String fileName, String use){
		fileName = fileName.toLowerCase();
		String mimeType = DAMSClient.getMimeType(fileName);
		if((use!=null && use.toLowerCase().startsWith("image")) || 
				mimeType.indexOf("image")>=0 || fileName.endsWith(".tif") || fileName.endsWith(".png"))
			return true;
		else
			return false;
	}
	
	/**
	 * Check for document file
	 * @param fileName
	 * @return
	 */
	public boolean isDocument(String fileName, String use){
		fileName = fileName.toLowerCase();
		String mimeType = DAMSClient.getMimeType(fileName);
		if( mimeType.toLowerCase().indexOf("pdf")>=0 || fileName.toLowerCase().endsWith(".pdf"))
			return true;
		else
			return false;
	}
	
	/**
	 * Update a record in SOLR 
	 * @param oid
	 * @return
	 * @throws Exception
	 */
	public boolean solrIndex(String oid) throws Exception{
		return damsClient.solrUpdate(oid);
	}
	
	/**
	 * Remove a record from SOLR
	 * @param oid
	 * @return
	 * @throws Exception
	 */
	public boolean solrDelete(String oid) throws Exception{
		return damsClient.solrDelete(oid);
	}
	
	/**
	 * Update SOLR with logging.
	 * @param oid
	 */
	protected boolean updateSOLR(String oid){
		String message = "";
		setStatus("SOLR update for record " + oid  + " ... " );
		boolean succeeded = false;
		try{
			succeeded = solrIndex(oid);
			if(!succeeded){
				solrFailed.add(oid);
				message = "SOLR update failed for object " + oid  + ".";
				setStatus(message); 
				log.error(message);
			}else{
				message = "SOLR update succeeded for object " + oid  + ". ";
				setStatus(message); 
				//logMessage(message);
				log.info(message);
			}
		} catch (Exception e) {
			e.printStackTrace();
			solrFailed.add(oid);
			message = "SOLR update failed for " + oid + ": " + e.getMessage();
			setStatus(message); 
			log.error(message);
		}
		return succeeded;
	}
	
	/**
	 * Generate error message for SOLR update. 
	 * @return
	 */
	protected String getSOLRReport(){
		StringBuilder builder = new StringBuilder();
		int iLen = solrFailed.size();
		if(iLen > 0){
			builder.append("SOLR index request failed for the following record" + (iLen>1?"s":"") + ": \n");
			for(int i=0; i<iLen; i++){
				builder.append(solrFailed.get(i) + ", \n");
			}
		}
		
		return builder.toString();
	}
	
	/**
	 * List all the files recursively.
	 * @param file
	 * @throws Exception 
	 */
	public static void listFile(Map<String, File> fMap, File file) throws Exception{
		List<File> files = new ArrayList<File> ();
		listFiles(files, file);
		String fName = null;
		for(int i=0; i<files.size(); i++){
			file = files.get(i);
			fName = file.getName();
			if(fMap.get(fName) != null){
				String message = "Duplicate source file name found: " + file.getAbsoluteFile() + "(" + fMap.get(fName).getAbsolutePath() + ").";
				// XXX ignore the files for validation, manifest, bagit, bag etc.
				if(file.getAbsolutePath().indexOf("/siogeocoll/Master_Files_Bag/") > 0 || fName.equals("CLMR_RCI_Apr2013.xls") || fName.equalsIgnoreCase("Thumbs.db") || fName.equals(".DS_Store") || ((fName.indexOf("validation") >=0 || fName.indexOf("manifest") >= 0 || fName.startsWith("bagit") || fName.startsWith("bag-info")) && fName.endsWith(".txt")))
					log.warn(message);
				else
					log.warn(message);
			}else if(file.getAbsolutePath().indexOf("/siogeocoll/Master_Files_Bag/") < 0)
				fMap.put(fName, file);
		}
	}
	
	/**
	 * List files
	 * @param files
	 * @param file
	 */
	public static void listFiles(List<File> files, File file){
		if(file.isDirectory()){
			File[] filesArr = file.listFiles();
			for(int i=0; i<filesArr.length; i++){
				listFiles(files, filesArr[i]);
			}
		}else{
			files.add(file);
		}
	}

	/**
	 * Close a resource.
	 * 
	 * @param resource
	 */
	public static void close(Closeable resource) {
		if (resource != null) {
			try {
				resource.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * List items in a collection/units 
	 * @param collectionId
	 * @return
	 * @throws Exception
	 */
	public List<String> listItems(String categoryId) throws Exception{
		if(unitsMap.containsValue(categoryId))
			return damsClient.listUnitObjects(categoryId);
		else
			return damsClient.listObjects(categoryId);
	}
	
	/**
	 * List all items in DAMS
	 * @return
	 * @throws Exception 
	 */
	public List<String> listAllItems() throws Exception{
		String unitId = null;
		List<String> items = new ArrayList<String>();
		for(Iterator<String> it=unitsMap.values().iterator();it.hasNext();){
			unitId = it.next();
			items.addAll(damsClient.listUnitObjects(unitId));
		}
		return items; 
	}
	
	/**
	 * Look up record from dams
	 * @param value
	 * @param modelName
	 * @return
	 * @throws Exception
	 */
	public static String lookupRecord(DAMSClient damsClient, String field, String value, String modelName) throws Exception{
		return lookupRecord(damsClient, field, value, modelName, null);
	}
	
	/**
	 * Look up copyrights record from dams
	 * @param value
	 * @param modelName
	 * @return
	 * @throws Exception
	 */
	public static String lookupRecord(DAMSClient damsClient, String field, String value, String modelName, Map<String, String> properties) throws Exception{
		List<String> recordIds = lookupRecords(damsClient, field, value, modelName, properties);
		if(recordIds.size() > 0)
			return recordIds.get(0);
		else
			return null;
	}
	
	/**
	 * Look up copyrights record from dams
	 * @param value
	 * @param modelName
	 * @return
	 * @throws Exception
	 */
	public static List<String> lookupRecords(DAMSClient damsClient, String field, String value, String modelName, Map<String, String> properties) throws Exception{
		List<String> recordIds = new ArrayList<String>();
		if(properties != null && properties.containsKey(field))
			properties.remove(field);
		
		// XXX No scheme_code_tesim lookup, need handle scheme_code_tesim to disable it for solr search???
		String scheme_code_tesim_key = "scheme_code_tesim";
		String scheme_code_tesim_value = properties.get(scheme_code_tesim_key);
		if(properties.containsKey(scheme_code_tesim_key)){
			properties.put(scheme_code_tesim_key, null);
		}
		String scheme_name_tesim_key = "scheme_name_tesim";
		String scheme_name_tesim_value = properties.get(scheme_name_tesim_key);
		if(properties.containsKey(scheme_name_tesim_key)){
			properties.put(scheme_name_tesim_key, null);
		}
		
		//String modelParam = "(\"" + INFO_MODEL_PREFIX + "Dams" + modelName + "\" OR \"" + INFO_MODEL_PREFIX + (modelName.startsWith("Mads")?"":"Mads") + modelName + "\")";
		String modelParam = INFO_MODEL_PREFIX + modelName;
		String propsParams = toSolrQuery(properties);
		String query = "q=" + URLEncoder.encode(field + ":\"" + StringEscapeUtils.escapeJava(value) + "\" AND has_model_ssim:" + modelParam, "UTF-8") + "&rows=1000" + (propsParams.length()>0?"&fq="+ URLEncoder.encode(propsParams, "UTF-8"):"");
		Document doc = damsClient.solrLookup(query);
		int numFound = Integer.parseInt(doc.selectSingleNode("/response/result/@numFound").getStringValue());
		if(numFound <= 0)
			return recordIds;
		else {
			Node record = null;
			Node propNode = null;
			boolean matched = false;
			List<Node> records = doc.selectNodes("/response/result/doc");
			if(properties == null || properties.size() == 0){
				// If no additional properties provided, just return the first record.
				for(Iterator<Node> it=records.iterator(); it.hasNext();){
					record = it.next();
					propNode = record.selectSingleNode("*[@name='" + field + "']/str");
					if(propNode.getText().equalsIgnoreCase(value)){
						recordIds.add(record.selectSingleNode("*[@name='id']").getText());
						//matched = true;
						//break;
					}
				}
			}else{
				String key = null;
				String propValue = null;

				// Matching all the properties to discover the record
				for(Iterator<Node> it=records.iterator(); it.hasNext();){
					record = it.next();
					propNode = record.selectSingleNode("*[@name='" + field + "']/str");
					if(propNode.getText().equalsIgnoreCase(value) || Normalizer.normalize(propNode.getText(), Normalizer.Form.NFD).equalsIgnoreCase(value)){
						matched = true;
						for(Iterator<String> pit=properties.keySet().iterator(); pit.hasNext();){
							key = pit.next();
							propValue = properties.get(key);
							
							if(key.equalsIgnoreCase(scheme_code_tesim_key)){
								propNode = record.selectSingleNode("*[@name='scheme_code_tesim']/str");
								if(propNode != null){
									String scheme_code = propNode.getText();
									if((scheme_code_tesim_value == null && scheme_code.length()>0) || !scheme_code.equalsIgnoreCase(scheme_code_tesim_value)){
										matched = false;
										break;
									}
								}else{
									//  XXX No scheme_code_tesim lookup, need second lookup for scheme_tesim???
									propNode = record.selectSingleNode("*[@name='scheme_tesim']/str");
									String authority_scheme_id = propNode==null?"":propNode.getText();
									String scheme_id = lookupRecord(damsClient, "code_tesim", scheme_code_tesim_value, "MadsScheme", new HashMap<String, String>());
									if(authority_scheme_id == null || scheme_id == null || !authority_scheme_id.endsWith(scheme_id)){
										matched = false;
										break;
									}
								}
							}else if(key.equalsIgnoreCase(scheme_name_tesim_key)){
								propNode = record.selectSingleNode("*[@name='scheme_name_tesim']/str");
								if(propNode != null){
									String scheme_name = propNode.getText();
									if((scheme_name_tesim_value == null && scheme_name.length()>0) || !scheme_name.equalsIgnoreCase(scheme_name_tesim_value)){
										matched = false;
										break;
									}
								}else{
									//  XXX No scheme_name_tesim lookup, need second lookup for scheme_tesim???
									propNode = record.selectSingleNode("*[@name='scheme_tesim']/str");
									String authority_scheme_id = propNode==null?"":propNode.getText();
									String scheme_id = lookupRecord(damsClient, "name_tesim", scheme_name_tesim_value, "MadsScheme", new HashMap<String, String>());
									if(authority_scheme_id == null || scheme_id == null || !authority_scheme_id.endsWith(scheme_id)){
										matched = false;
										break;
									}
								}
							}else{
								propNode = record.selectSingleNode("*[@name='"+key+"']/str");
								if(propValue == null || propValue.length() == 0){
									if(propNode != null && propNode.getText().length() > 0){
										matched = false;
										break;
									}
								}else{
									if(propNode == null || !(propValue.equalsIgnoreCase(propNode.getText()) || Normalizer.normalize(propNode.getText(), Normalizer.Form.NFD).equalsIgnoreCase(value))){
										matched = false;
										break;
									}	
								}
							}
						}
						if(matched)
							recordIds.add(record.selectSingleNode("*[@name='id']").getText());
							//break;
					}
				}
			}
			
			return recordIds;
		}
	}

	/**
	 * Perform actions to lookup records from the triplestore with SPARQL
	 * @param damsClient
	 * @param field
	 * @param value
	 * @param modelName
	 * @param properties
	 * @return
	 * @throws Exception
	 */
	public List<Map<String, String>> lookupRecordsFromTs(String field, String value, String modelName, Map<String, String> properties) throws Exception{
		List<String> variables = new ArrayList<String>();
		if(properties == null)
			properties = new HashMap<String, String>();
		
		properties.put(field, value);
		variables.add("soluction");
		return sparqlLookup(damsClient, modelName, properties, variables);
	}
	
	public static List<Map<String, String>> sparqlLookup(DAMSClient damsClient, String modelName, Map<String, String> properties, List<String> variables) throws Exception{
		String sparql = buildSparql(modelName, properties, variables);
		return damsClient.sparqlLookup(sparql);
	}
	
	public static String buildSparql(String modelName, Map<String, String> properties, List<String> variables){
		String sub = variables==null||variables.size()>0?variables.get(0):"soluction";
		String sparql = "SELECT";
		if(variables != null && variables.size() > 0){
			for (Iterator<String> it=variables.iterator(); it.hasNext();)
				sparql += " ?" + it.next();
		}else
			sparql += " ?" + sub; 
		sparql += " WHERE { ";
		
		Map<String, String> prefixMap = new HashMap<String, String>();
		
		if(modelName != null && modelName.length() > 0){
			boolean caseSensitive = true;
			String bnId = nextBlankNodeId();
			sparql += buildClause ("?"+sub, "rdf:type", "?"+bnId, prefixMap, caseSensitive);
			sparql += " . " + buildClause ("?"+bnId, "rdf:label", modelName, prefixMap, caseSensitive);		
		}
		
		// build clause for properties
		if(properties != null){
			for(Iterator<String> it=properties.keySet().iterator(); it.hasNext();){
				String pre = it.next();
				String obj = properties.get(pre);
				if(!sparql.endsWith(" WHERE ") && obj != null && obj.length() > 0)
					sparql += " . ";
				
				if(pre.indexOf("/") > 0 && obj != null){
					String[] pres = pre.split("/");
					String lastVariable = "?"+sub;
					String bnObj = null;
					for(int i=0; i<pres.length; i++){
						if(pres[i] != null && (pres[i]=pres[i].trim()).length() > 0){
							if(!(sparql.endsWith(" WHERE ") || sparql.endsWith(" . ")))
								sparql += " . ";
							
							if(i < pres.length - 1)
								bnObj = "?" + nextBlankNodeId();
							else
								bnObj = obj;
							
							sparql += buildClause (lastVariable, pres[i], bnObj, prefixMap, false);
							lastVariable = bnObj;
						}
					}
				}else
					//sparql += buildClause ("?"+sub, pre, obj, prefixMap);
					sparql += buildClause ("?"+sub, pre, obj, prefixMap, false);
			}
		}
		
		sparql += " }";
		
		// build SPARQL prefix
		String prefix = "";
		for(Iterator<String> it=prefixMap.keySet().iterator(); it.hasNext();){
			String nsPrefix = it.next();
			prefix += "PREFIX " + nsPrefix + ": " + "<" + prefixMap.get(nsPrefix) + "> ";
		}
		
		return prefix + "\n" + sparql;
	}
	
	public static String buildClause (String sub, String pre, String obj, Map<String, String> prefixMap, boolean caseSensitive){
		String subQuery = "";
		if(obj != null){
			if(caseSensitive || obj.length() == 0 || obj.startsWith("?") || obj.startsWith("<") && obj.endsWith(">"))
				subQuery = subjectPart(sub) + " " + predicatePart(pre, prefixMap) + " " + objectPart(obj);
			else
				subQuery = buildFilterClause (sub, pre, obj, prefixMap);
		}
		
		return subQuery;
	}
	
	public static String buildFilterClause (String sub, String pre, String obj, Map<String, String> prefixMap){
		String subQuery = "";
		if(obj != null){
			String bnVariable = "?" + nextBlankNodeId();
			subQuery += buildClause (sub, pre, bnVariable, prefixMap, true);
			subQuery += " . FILTER ( lcase(" + bnVariable + ") =  lcase('" + obj + "'))";
		}
		return subQuery;
	}
	
	
	public static String subjectPart(String symbol){
		return (symbol.startsWith("?")||symbol.startsWith("<")&&symbol.endsWith(">")?symbol:"<" + symbol +">");
	}

	public static String predicatePart(String symbol, Map<String, String> prefixMap){
		if (symbol.startsWith("?")||symbol.startsWith("<")&&symbol.endsWith(">"))
			return symbol;
		if(symbol.startsWith("http")){
			for(Iterator<String> it=Constants.NS_PREFIX_MAP.keySet().iterator(); it.hasNext();){
				String key = it.next();
				String value = Constants.NS_PREFIX_MAP.get(key);
				if(symbol.indexOf(value) == 0){
					prefixMap.put(key, value);
					symbol = symbol.replace(value, key + ":");
					return symbol;
				}
			}
		}else{
			String[] tokens = symbol.split(":");
			if(tokens.length == 2){
				String ns = Constants.NS_PREFIX_MAP.get(tokens[0]);
				if(ns != null){
					prefixMap.put(tokens[0], ns);
					return symbol;
				}
			}
		}
		return "<" + symbol + ">";
	}
	
	public static String objectPart(String symbol){
		return (symbol.startsWith("?")||symbol.startsWith("<")&&symbol.endsWith(">")?symbol:"'" + symbol +"'");
	}
	
	public static String nextBlankNodeId(){
		return "_" + (""+random.nextInt()).replace("-", "n");
	}
	
	private static String toSolrQuery(Map<String, String> properties) throws UnsupportedEncodingException{
		String solrParams = "";
		String key = null;
		String value = null;
		if(properties != null){
			for(Iterator<String> it=properties.keySet().iterator(); it.hasNext();){
				key = it.next();
				value = properties.get(key);
				if(value != null && value.length() > 0)
					solrParams += key+ ":\"" + StringEscapeUtils.escapeJava(value) + "\"" + " AND ";
			}
			
			int len = solrParams.length();
			if(len > 0)
				solrParams = solrParams.substring(0, len-5);
		}
		return solrParams;
	}

	/**
	 * Default implementation. Override when necessary.
	 */
	@Override
	public boolean execute() throws Exception {
		int iSize = items.size();
		String subjectId = null;
		for (int i = 0; i < iSize && !interrupted; i++) {
			subjectId = items.get(i);
			invokeTask(subjectId, i);
			try {
				Thread.sleep(10);
			} catch (InterruptedException ie) {
				interrupted = true;
				String message = "Error - Execution inerrupted after "
						+ subjectId + " processed.";
				log.info(message);
				log("log", message);
				setStatus(message);
			}
		}
		return exeResult;
	}

	/**
	 * Override for task invocation.
	 */
	@Override
	public void invokeTask(String subjectId, int idx) throws Exception {
	}
}
