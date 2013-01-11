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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DomUtil;
import edu.ucsd.library.xdre.utils.ProcessHandler;
import edu.ucsd.library.xdre.utils.RequestOrganizer;

/**
 * Class CollectionHandler, a class for procedure handling
 * 
 * @author lsitu@ucsd.edu
 */
public abstract class CollectionHandler implements ProcessHandler {
	private static Logger log = Logger.getLogger(CollectionHandler.class);

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
	protected Map<String, String> reposMap = null;
	protected int itemsCount = 0; //Total number of items in the collection/batch

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
		reposMap = damsClient.listRepositories();
		Map<String, String> colls = damsClient.listCollections();
		Entry<String, String> ent = null;
		
		colls.putAll(reposMap);
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
	 * Retrieve the embargoed objects in the collection
	 * @param collectionId
	 * @return
	 */
	public List<String> getEmbargoedItems(String collectionId){
		//XXX
		return null;
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

	public void excludeEmbargoedObjects() {
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
	 * List items in a collection/repository 
	 * @param collectionId
	 * @return
	 * @throws Exception
	 */
	public List<String> listItems(String categoryId) throws Exception{
		if(reposMap.containsValue(categoryId))
			return damsClient.listRepoObjects(categoryId);
		else
			return damsClient.listObjects(categoryId);
	}
	
	/**
	 * List all items in DAMS
	 * @return
	 * @throws Exception 
	 */
	public List<String> listAllItems() throws Exception{
		String repoId = null;
		List<String> items = new ArrayList<String>();
		for(Iterator<String> it=reposMap.values().iterator();it.hasNext();){
			repoId = it.next();
			items.addAll(damsClient.listRepoObjects(repoId));
		}
		return items; 
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