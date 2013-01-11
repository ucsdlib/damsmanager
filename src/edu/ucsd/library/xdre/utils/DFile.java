package edu.ucsd.library.xdre.utils;

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
	private String sourceFilename = null;
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
	public DFile(String id, String object, String use, String sourceFilename, String sourcePath, String dateCreated, 
			String size, String formatName, String formatVersion, String mimeType, String quality, String crc32checksum, 
			String md5checksum, String sha1checksum){
		this.id = id;
		this.object = object;
		this.use = use;
		this.sourceFilename = sourceFilename;
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

	public String getSourceFilename() {
		return sourceFilename;
	}

	public void setSourceFilename(String sourceFilename) {
		this.sourceFilename = sourceFilename;
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

	public static DFile toDFile(JSONObject jsonObject){
		DFile dFile = new DFile((String)jsonObject.get("id"), (String)jsonObject.get("object"), (String)jsonObject.get("use"), (String)jsonObject.get("sourceFileName"), (String)jsonObject.get("sourcePath"), (String)jsonObject.get("dateCreated"), 
				(String)jsonObject.get("size"), (String)jsonObject.get("formatName"),(String)jsonObject.get("formatVersion"), (String)jsonObject.get("mimeType"), (String)jsonObject.get("quality"), (String)jsonObject.get("crc32checksum"), 
						(String)jsonObject.get("md5checksum"), (String)jsonObject.get("sha1checksum"), (String)jsonObject.get("sha256checksum"), (String)jsonObject.get("sha512checksum"), 
								(String)jsonObject.get("preservationLevel"), (String)jsonObject.get("objectCategory"), (String)jsonObject.get("compositionLevel"));
		dFile.setStatus((String)jsonObject.get("status"));
		return dFile;
	}
}
