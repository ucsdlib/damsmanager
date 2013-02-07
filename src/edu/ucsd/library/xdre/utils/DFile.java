package edu.ucsd.library.xdre.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.simple.JSONObject;

/**
 * Class to construct a DAMS File object
 * @author lsitu
 *
 */
public class DFile {
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
		value += "id\":\"" + id +"\", ";
		value += "object\":\"" + object +"\", ";
		value += "use\":\"" + use +"\", "; 
		value += "sourceFileName\":\"" + sourceFileName +"\", "; 
		value += "sourcePath\":\"" + sourcePath +"\", "; 
		value += "dateCreated\":\"" + dateCreated +"\", "; 
		value += "size\":\"" + size +"\", "; 
		value += "formatName\":\"" + formatName +"\", ";
		value += "formatVersion\":\"" + formatVersion +"\", "; 
		value += "mimeType\":\"" + mimeType +"\", "; 
		value += "quality\":\"" + quality +"\", "; 
		value += "crc32checksum\":\"" + crc32checksum +"\", "; 
		value += "md5checksum\":\"" + md5checksum +"\", ";
		value += "sha1checksum\":\"" + sha1checksum +"\", "; 
		value += "sha256checksum\":\"" + sha256checksum +"\", "; 
		value += "sha512checksum\":\"" + sha512checksum +"\", "; 
		value += "preservationLevel\":\"" + preservationLevel +"\", "; 
		value += "objectCategory\":\"" + objectCategory +"\", "; 
		value += "compositionLevel\":\"" + compositionLevel +"\" ";
		value += "}";
		return value;
		
	}
	
	/**
	 * Update the properties
	 * @param dFile
	 */
	public void updateValues(DFile dFile){
		if(dFile.id != null)
			id = dFile.id;
		if(dFile.object != null)
			object = dFile.object;
		if(dFile.use != null)
			use = dFile.use;
		if(dFile.sourceFileName != null)
			sourceFileName = dFile.sourceFileName;
		if(dFile.sourcePath != null)
			sourcePath = dFile.sourcePath;
		if(dFile.dateCreated != null)
			dateCreated = dFile.dateCreated;
		if(dFile.size != null)
			size = dFile.size;
		if(dFile.formatName != null)
			formatName = dFile.formatName;
		if(dFile.formatVersion != null)
			formatVersion = dFile.formatVersion;
		if(dFile.mimeType != null)
			mimeType = dFile.mimeType;
		if(dFile.quality != null)
			quality = dFile.quality;
		if(dFile.crc32checksum != null)
			crc32checksum = dFile.crc32checksum;
		if(dFile.md5checksum != null)
			md5checksum = dFile.md5checksum;
		if(dFile.sha1checksum != null)
			sha1checksum = dFile.sha1checksum;
		if(dFile.sha256checksum != null)
			sha256checksum = dFile.sha256checksum;
		if(dFile.sha512checksum != null)
			sha512checksum = dFile.sha512checksum;
		if(dFile.compositionLevel != null)
			compositionLevel = dFile.compositionLevel;
		if(dFile.objectCategory != null)
			objectCategory = dFile.objectCategory;
		if(dFile.preservationLevel != null)
			preservationLevel = dFile.preservationLevel;
		if(dFile.status != null)
			status = dFile.status;
		
	}
	
	/**
	 * Convert properties to NameValue pairs
	 * @return
	 */
	public List<NameValuePair> toNameValuePairs(){
		List<NameValuePair> props = new ArrayList<NameValuePair>();
		props.add(new BasicNameValuePair("id", id));
		props.add(new BasicNameValuePair("object", object));
		props.add(new BasicNameValuePair("use", use));
		props.add(new BasicNameValuePair("sourceFileName", sourceFileName));
		props.add(new BasicNameValuePair("sourcePath", sourcePath));
		props.add(new BasicNameValuePair("dateCreated", dateCreated));
		props.add(new BasicNameValuePair("size", size));
		props.add(new BasicNameValuePair("formatName", formatName));
		props.add(new BasicNameValuePair("formatVersion", formatVersion));
		props.add(new BasicNameValuePair("mimeType", mimeType));
		props.add(new BasicNameValuePair("quality", quality));
		props.add(new BasicNameValuePair("crc32checksum", crc32checksum));
		props.add(new BasicNameValuePair("md5checksum", md5checksum));
		props.add(new BasicNameValuePair("sha1checksum", sha1checksum));
		props.add(new BasicNameValuePair("sha256checksum", sha256checksum));
		props.add(new BasicNameValuePair("sha512checksum", sha512checksum));
		props.add(new BasicNameValuePair("compositionLevel", compositionLevel));
		props.add(new BasicNameValuePair("objectCategory", objectCategory));
		props.add(new BasicNameValuePair("preservationLevel", preservationLevel));
		props.add(new BasicNameValuePair("status", status));
		return props;
	}

	/**
	 * Construct a DFile object
	 * @param jsonObject
	 * @return
	 */
	public static DFile toDFile(JSONObject jsonObject){
		DFile dFile = new DFile(
				(String)jsonObject.get("id"), 
				(String)jsonObject.get("object"), 
				(String)jsonObject.get("use"), 
				(String)jsonObject.get("sourceFileName"), 
				(String)jsonObject.get("sourcePath"), 
				(String)jsonObject.get("dateCreated"), 
				(String)jsonObject.get("size"), 
				(String)jsonObject.get("formatName"),
				(String)jsonObject.get("formatVersion"), 
				(String)jsonObject.get("mimeType"), 
				(String)jsonObject.get("quality"), 
				(String)jsonObject.get("crc32checksum"), 
				(String)jsonObject.get("md5checksum"),
				(String)jsonObject.get("sha1checksum"), 
				(String)jsonObject.get("sha256checksum"), 
				(String)jsonObject.get("sha512checksum"), 
				(String)jsonObject.get("preservationLevel"), 
				(String)jsonObject.get("objectCategory"), 
				(String)jsonObject.get("compositionLevel")
				);
		dFile.setStatus((String)jsonObject.get("status"));
		return dFile;
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
					getNodeText(node, "//dams:id"), 
					getNodeText(node, "//dams:object"), 
					getNodeText(node, "//dams:use"), 
					getNodeText(node, "//dams:sourceFileName"), 
					getNodeText(node, "//dams:sourcePath"), 
					getNodeText(node, "//dams:dateCreated"), 
					getNodeText(node, "//dams:size"), 
					getNodeText(node, "//dams:formatName"),
					getNodeText(node, "//dams:formatVersion"), 
					getNodeText(node, "//dams:mimeType"), 
					getNodeText(node, "//dams:quality"), 
					getNodeText(node, "//dams:crc32checksum"), 
					getNodeText(node, "//dams:md5checksum"),
					getNodeText(node, "//dams:sha1checksum"), 
					getNodeText(node, "//dams:sha256checksum"), 
					getNodeText(node, "//dams:sha512checksum"), 
					getNodeText(node, "//dams:preservationLevel"), 
					getNodeText(node, "//dams:objectCategory"), 
					getNodeText(node, "//dams:compositionLevel")
					);
			dFile.setStatus(getNodeText(node, "status"));
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
