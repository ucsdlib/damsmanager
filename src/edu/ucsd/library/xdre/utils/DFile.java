package edu.ucsd.library.xdre.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.simple.JSONObject;

import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Statement;

/**
 * Class to construct a DAMS File object
 * @author lsitu
 *
 */
public class DFile {
	public static final String ID = "id";
	public static final String OBJECT = "object";
	public static final String USE = "use";
	public static final String SOURCE_FILE_NAME = "sourceFileName";
	public static final String SOURCE_PATH = "sourcePath";
	public static final String DATE_CREATED = "dateCreated";
	public static final String SIZE = "size";
	public static final String FORMAT_NAME = "formatName";
	public static final String FORMAT_VERSION = "formatVersion";
	public static final String MIME_TYPE = "mimeType";
	public static final String CRC32CHECKSUM = "crc32checksum";
	public static final String MD5CHECKSUM = "md5checksum";
	public static final String SHA1CHECKSUM = "sha1checksum";
	public static final String SHA256CHECKSUM = "sha256checksum";
	public static final String SHA512CHECKSUM = "sha512checksum";
	public static final String PRESERVATION_LEVEL = "preservationLevel";
	public static final String OBJECT_CATEGORY = "objectCategory";
	public static final String COMPOSITION_LEVEL = "compositionLevel";
	public static final String QUALITY = "quality";
	public static final String DURATION = "duration";
	public static final String STATUS = "status";
	
	private String id = null; //ID property
	private String object = null; //ID property
	private String use = null;
	private String sourceFileName = null;
	private String sourcePath = null;
	private String dateCreated = null;
	private String size = "0";
	private String formatName = null;
	private String formatVersion = null;
	private String mimeType = null;
	private String crc32checksum = null;
	private String md5checksum = null;
	private String sha1checksum = null;
	private String sha256checksum = null;
	private String sha512checksum = null;
	private String preservationLevel = "full";
	private String objectCategory  = "file";
	private String compositionLevel = "0";
	private String quality = null;
	private String duration = null;
	private String status = null;

	/**
	 * Constructor
	 * @param id
	 * @param use
	 * @param sourceFilename
	 * @param sourcePath
	 * @param dateCreated
	 * @param size
	 * @param formatName
	 * @param formatVersion
	 * @param mimeType
	 * @param crc32Checksum
	 * @param md5Checksum
	 * @param sha1Checksum
	 */
	public DFile(String id, String object, String use, String sourceFileName, String sourcePath, String dateCreated, 
			String size, String formatName, String formatVersion, String mimeType, String quality, String crc32checksum, 
			String md5checksum, String sha1checksum){
		this.id = id;
		this.object = object;
		this.use = use;
		this.sourceFileName = sourceFileName;
		this.sourcePath = sourcePath;
		this.dateCreated = dateCreated;
		this.size = size;
		this.formatName = formatName;
		this.formatVersion = formatVersion;
		this.mimeType = mimeType;
		this.quality = quality;
		this.crc32checksum = crc32checksum;
		this.md5checksum = md5checksum;
		this.sha1checksum = sha1checksum;
	}
	
	/**
	 * Constructor
	 * @param id
	 * @param use
	 * @param sourceFilename
	 * @param sourcePath
	 * @param dateCreated
	 * @param size
	 * @param formatName
	 * @param formatVersion
	 * @param mimeType
	 * @param crc32Checksum
	 * @param md5Checksum
	 * @param sha1Checksum
	 * @param sha256Checksum
	 * @param sha512Checksum
	 * @param preservationLevel
	 * @param objectCategory
	 * @param compositionLevel
	 */
	public DFile(String id, String object, String use, String sourceFilename, String sourcePath, String dateCreated, 
			String size, String formatName, String formatVersion, String mimeType, String quality, String crc32checksum, 
			String md5checksum, String sha1checksum, String sha256checksum, String sha512checksum, 
			String preservationLevel, String objectCategory, String compositionLevel) {

		this(id, object, use, sourceFilename, sourcePath, dateCreated, size, formatName, formatVersion, mimeType, quality, 
				crc32checksum, md5checksum, sha1checksum);
		this.sha256checksum = sha256checksum;
		this.sha512checksum = sha512checksum;
		this.preservationLevel = preservationLevel;
		this.objectCategory = objectCategory;
		this.compositionLevel = compositionLevel;
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}

	public String getUse() {
		return use;
	}

	public void setUse(String use) {
		this.use = use;
	}

	public String getPreservationLevel() {
		return preservationLevel;
	}

	public void setPreservationLevel(String preservationLevel) {
		this.preservationLevel = preservationLevel;
	}

	public String getObjectCategory() {
		return objectCategory;
	}

	public void setObjectCategory(String objectCategory) {
		this.objectCategory = objectCategory;
	}

	public String getCompositionLevel() {
		return compositionLevel;
	}

	public void setCompositionLevel(String compositionLevel) {
		this.compositionLevel = compositionLevel;
	}

	public String getSize() {
		return size;
	}

	public void setSize(String size) {
		this.size = size;
	}

	public String getMimeType() {
		return mimeType;
	}

	public void setMimeType(String mimeType) {
		this.mimeType = mimeType;
	}

	public String getFormatName() {
		return formatName;
	}

	public void setFormatName(String formatName) {
		this.formatName = formatName;
	}

	public String getFormatVersion() {
		return formatVersion;
	}

	public void setFormatVersion(String formatVersion) {
		this.formatVersion = formatVersion;
	}

	public String getSourceFileName() {
		return sourceFileName;
	}

	public void setSourceFileName(String sourceFileName) {
		this.sourceFileName = sourceFileName;
	}

	public String getSourcePath() {
		return sourcePath;
	}

	public void setSourcePath(String sourcePath) {
		this.sourcePath = sourcePath;
	}

	public String getDateCreated() {
		return dateCreated;
	}

	public void setDateCreated(String dateCreated) {
		this.dateCreated = dateCreated;
	}

	public String getCrc32checksum() {
		return crc32checksum;
	}

	public void setCrc32checksum(String crc32checksum) {
		this.crc32checksum = crc32checksum;
	}

	public String getMd5checksum() {
		return md5checksum;
	}

	public void setMd5checksum(String md5checksum) {
		this.md5checksum = md5checksum;
	}

	public String getSha1checksum() {
		return sha1checksum;
	}

	public void setSha1checksum(String sha1checksum) {
		this.sha1checksum = sha1checksum;
	}

	public String getSha256checksum() {
		return sha256checksum;
	}

	public void setSha256checksum(String sha256checksum) {
		this.sha256checksum = sha256checksum;
	}

	public String getSha512checksum() {
		return sha512checksum;
	}

	public void setSha512checksum(String sha512checksum) {
		this.sha512checksum = sha512checksum;
	}

	public String getQuality() {
		return quality;
	}

	public void setQuality(String quality) {
		this.quality = quality;
	}

	public String getDuration() {
		return duration;
	}

	public void setDuration(String duration) {
		this.duration = duration;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public String getObject() {
		return object;
	}

	public void setObject(String object) {
		this.object = object;
	}
	
	public String toString(){
		String value = "{";
		value += "\"" + ID + "\":\"" + id +"\", ";
		value += "\"" + OBJECT + "\":\"" + object +"\", ";
		value += "\"" + USE + "\":\"" + use +"\", "; 
		value += "\"" + SOURCE_FILE_NAME + "\":\"" + sourceFileName +"\", "; 
		value += "\"" + SOURCE_PATH + "\":\"" + sourcePath +"\", "; 
		value += "\"" + DATE_CREATED + "\":\"" + dateCreated +"\", "; 
		value += "\"" + SIZE + "\":\"" + size +"\", "; 
		value += "\"" + FORMAT_NAME + "\":\"" + formatName +"\", ";
		value += "\"" + FORMAT_VERSION + "\":\"" + formatVersion +"\", "; 
		value += "\"" + MIME_TYPE + "\":\"" + mimeType +"\", "; 
		value += "\"" + QUALITY + "\":\"" + quality +"\", "; 
		value += "\"" + CRC32CHECKSUM + "\":\"" + crc32checksum +"\", "; 
		value += "\"" + MD5CHECKSUM + "\":\"" + md5checksum +"\", ";
		value += "\"" + SHA1CHECKSUM + "\":\"" + sha1checksum +"\", "; 
		value += "\"" + SHA256CHECKSUM + "\":\"" + sha256checksum +"\", "; 
		value += "\"" + SHA512CHECKSUM + "\":\"" + sha512checksum +"\", "; 
		value += "\"" + PRESERVATION_LEVEL + "\":\"" + preservationLevel +"\", "; 
		value += "\"" + OBJECT_CATEGORY + "\":\"" + objectCategory +"\", "; 
		value += "\"" + COMPOSITION_LEVEL + "\":\"" + compositionLevel +"\" ";
		value += "\"" + DURATION + "\":\"" + duration +"\", ";
		value += "\"" + STATUS + "\":\"" + status +"\"";
		value += "}";
		return value;
		
	}
	
	/**
	 * Update File properties
	 * @param props
	 */
	public void updateProperties(JSONObject props){
		String prop = null;
		String value = null;
		for(Iterator<String> it=props.keySet().iterator(); it.hasNext();){
			prop = it.next();
			value = (String)props.get(prop);
		if(prop.equals(ID))
			id = value;
		else if(prop.equals(OBJECT))
			object = value;
		else if(prop.equals(USE))
			use = value;
		else if(prop.equals(SOURCE_FILE_NAME))
			sourceFileName = value;
		else if(prop.equals(SOURCE_PATH))
			sourcePath = value;
		else if(prop.equals(DATE_CREATED))
			dateCreated = value;
		else if(prop.equals(SIZE))
			size = value;
		else if(prop.equals(FORMAT_NAME))
			formatName = value;
		else if(prop.equals(FORMAT_VERSION))
			formatVersion = value;
		else if(prop.equals(MIME_TYPE))
			mimeType = value;
		else if(prop.equals(QUALITY))
			quality = value;
		else if(prop.equals(CRC32CHECKSUM))
			crc32checksum = value;
		else if(prop.equals(MD5CHECKSUM))
			md5checksum = value;
		else if(prop.equals(SHA1CHECKSUM))
			sha1checksum = value;
		else if(prop.equals(SHA256CHECKSUM))
			sha256checksum = value;
		else if(prop.equals(SHA512CHECKSUM))
			sha512checksum = value;
		else if(prop.equals(COMPOSITION_LEVEL))
			compositionLevel = value;
		else if(prop.equals(OBJECT_CATEGORY))
			objectCategory = value;
		else if(prop.equals(PRESERVATION_LEVEL))
			preservationLevel = value;
		else if(prop.equals(DURATION))
			duration = value;
		else if(prop.equals(STATUS))
			status = value;
		}
	}
	
	/**
	 * Construct a key/value map for the properties.
	 * @return
	 */
	public Map<String, String> toProperties(){
		Map<String, String> props = new HashMap<String, String>();
		props.put(ID, id);
		props.put(OBJECT, object);
		props.put(USE, use);
		props.put(SOURCE_FILE_NAME, sourceFileName);
		props.put(SOURCE_PATH, sourcePath);
		props.put(DATE_CREATED, dateCreated);
		props.put(SIZE, size);
		props.put(FORMAT_NAME, formatName);
		props.put(FORMAT_VERSION, formatVersion);
		props.put(MIME_TYPE, mimeType);
		props.put(QUALITY, quality);
		props.put(CRC32CHECKSUM, crc32checksum);
		props.put(MD5CHECKSUM, md5checksum);
		props.put(SHA1CHECKSUM, sha1checksum);
		props.put(SHA256CHECKSUM, sha256checksum);
		props.put(SHA512CHECKSUM, sha512checksum);
		props.put(COMPOSITION_LEVEL, compositionLevel);
		props.put(OBJECT_CATEGORY, objectCategory);
		props.put(PRESERVATION_LEVEL, preservationLevel);
		props.put(DURATION, duration);
		props.put(STATUS, status);
		return props;
	}
	
	/**
	 * Convert properties to NameValue pairs
	 * @return
	 */
	public List<NameValuePair> toNameValuePairs(){
		List<NameValuePair> props = new ArrayList<NameValuePair>();
		props.add(new BasicNameValuePair(ID, id));
		props.add(new BasicNameValuePair(OBJECT, object));
		props.add(new BasicNameValuePair(USE, use));
		props.add(new BasicNameValuePair(SOURCE_FILE_NAME, sourceFileName));
		props.add(new BasicNameValuePair(SOURCE_PATH, sourcePath));
		props.add(new BasicNameValuePair(DATE_CREATED, dateCreated));
		props.add(new BasicNameValuePair(SIZE, size));
		props.add(new BasicNameValuePair(FORMAT_NAME, formatName));
		props.add(new BasicNameValuePair(FORMAT_VERSION, formatVersion));
		props.add(new BasicNameValuePair(MIME_TYPE, mimeType));
		props.add(new BasicNameValuePair(QUALITY, quality));
		props.add(new BasicNameValuePair(CRC32CHECKSUM, crc32checksum));
		props.add(new BasicNameValuePair(MD5CHECKSUM, md5checksum));
		props.add(new BasicNameValuePair(SHA1CHECKSUM, sha1checksum));
		props.add(new BasicNameValuePair(SHA256CHECKSUM, sha256checksum));
		props.add(new BasicNameValuePair(SHA512CHECKSUM, sha512checksum));
		props.add(new BasicNameValuePair(COMPOSITION_LEVEL, compositionLevel));
		props.add(new BasicNameValuePair(OBJECT_CATEGORY, objectCategory));
		props.add(new BasicNameValuePair(PRESERVATION_LEVEL, preservationLevel));
		props.add(new BasicNameValuePair(DURATION, duration));
		props.add(new BasicNameValuePair(STATUS, status));
		return props;
	}

	/**
	 * Construct a DFile object
	 * @param jsonObject
	 * @return
	 */
	public static DFile toDFile(JSONObject jsonObject){
		DFile dFile = new DFile(
				(String)jsonObject.get(ID), 
				(String)jsonObject.get(OBJECT), 
				(String)jsonObject.get(USE), 
				(String)jsonObject.get(SOURCE_FILE_NAME), 
				(String)jsonObject.get(SOURCE_PATH), 
				(String)jsonObject.get(DATE_CREATED), 
				(String)jsonObject.get(SIZE), 
				(String)jsonObject.get(FORMAT_NAME),
				(String)jsonObject.get(FORMAT_VERSION), 
				(String)jsonObject.get(MIME_TYPE), 
				(String)jsonObject.get(QUALITY), 
				(String)jsonObject.get(CRC32CHECKSUM), 
				(String)jsonObject.get(MD5CHECKSUM),
				(String)jsonObject.get(SHA1CHECKSUM), 
				(String)jsonObject.get(SHA256CHECKSUM), 
				(String)jsonObject.get(SHA512CHECKSUM), 
				(String)jsonObject.get(PRESERVATION_LEVEL), 
				(String)jsonObject.get(OBJECT_CATEGORY), 
				(String)jsonObject.get(COMPOSITION_LEVEL)
				);
		dFile.setDuration((String)jsonObject.get(DURATION));
		dFile.setStatus((String)jsonObject.get(STATUS));
		return dFile;
	}
	
	/**
	 * Construct a DFile object with a list of Statements
	 * @param jsonObject
	 * @return
	 */
	public static DFile toDFile(List<Statement> stmts){
		Literal value = null;
		String propName = null;
		Statement stmt = null;
		JSONObject jsonObj = new JSONObject();
		for(Iterator<Statement> it=stmts.iterator(); it.hasNext();){
			stmt = it.next();
			if(stmt.getObject().isLiteral()){
				value= stmt.getLiteral();
				propName = stmt.getPredicate().getLocalName();
				if(value != null)
					jsonObj.put(propName, value.getString());
				else
					jsonObj.put(propName, null);
			}
		}
		return toDFile(jsonObj);
	}
	
	/**
	 * Parse RDF XML String for a DFile object.
	 * @param rdfXml
	 * @return
	 * @throws DocumentException
	 */
	public static DFile toDFile(String rdfXml) throws DocumentException{
		DFile dFile = null;
		Document doc = null;
		InputStream in = null;
		SAXReader saxReader = new SAXReader();
		in = new ByteArrayInputStream(rdfXml.getBytes());
		try {
			doc = saxReader.read(in);
			Node node = doc.getRootElement();
			dFile = new DFile(
					getNodeText(node, "//dams:" + ID), 
					getNodeText(node, "//dams:" + OBJECT), 
					getNodeText(node, "//dams:" + USE), 
					getNodeText(node, "//dams:" + SOURCE_FILE_NAME), 
					getNodeText(node, "//dams:" + SOURCE_PATH), 
					getNodeText(node, "//dams:" + DATE_CREATED), 
					getNodeText(node, "//dams:" + SIZE), 
					getNodeText(node, "//dams:" + FORMAT_NAME),
					getNodeText(node, "//dams:" + FORMAT_VERSION), 
					getNodeText(node, "//dams:" + MIME_TYPE), 
					getNodeText(node, "//dams:" + QUALITY), 
					getNodeText(node, "//dams:" + CRC32CHECKSUM), 
					getNodeText(node, "//dams:" + MD5CHECKSUM),
					getNodeText(node, "//dams:" + SHA1CHECKSUM), 
					getNodeText(node, "//dams:" + SHA256CHECKSUM), 
					getNodeText(node, "//dams:" + SHA512CHECKSUM), 
					getNodeText(node, "//dams:" + PRESERVATION_LEVEL), 
					getNodeText(node, "//dams:" + OBJECT_CATEGORY), 
					getNodeText(node, "//dams:" + COMPOSITION_LEVEL)
					);
			dFile.setDuration(getNodeText(node, "//dams:" + DURATION));
			dFile.setStatus(getNodeText(node, STATUS));
		}finally{
			if(in != null){
				try {
					in.close();
				} catch (IOException e) {}
				in = null;
			}
		}
		return dFile;
	}
	
	public static String getNodeText(Node node, String nodeName){
		String value = null;
		Node n = node.selectSingleNode(nodeName);
		if(n != null){
			value = n.getText();
		}
		return value;
	}
}
