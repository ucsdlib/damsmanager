package edu.ucsd.library.xdre.ingest;

import java.io.File;
import java.util.Iterator;
import java.util.List;

import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * Perform ingesting task to DAMS.
 * 
 * @author Longshou Situ
 */
public class DAMSUploadTaskHandler{
	
	public static final int bufferSize = 5096;
	public static String filePaths = "";

	private String collectionId = null;
	private String unitId = null;
	private String subjectId = null;
	private String compId = null;
	private String fileId = null;
	private String use = null;
	private String sourceFile = null;

  	private String arkOrg = null;
  	
  	private DAMSClient damsClient = null;
	
	public DAMSUploadTaskHandler(String compId, String fileId, String sourceFile, String collectionId, String use, DAMSClient damsClient){
		this.sourceFile = sourceFile;
		this.collectionId = collectionId;
		this.damsClient = damsClient;
		this.compId = compId;
		this.fileId = fileId;
	}
	
	public DAMSUploadTaskHandler(String contentId, String sourceFile, String collectionId, DAMSClient damsClient){
		this(contentId, sourceFile, collectionId, null, damsClient);
	}
	
	public DAMSUploadTaskHandler(String contentId, String sourceFile, String collectionId, String use, DAMSClient damsClient){
		this.sourceFile = sourceFile;
		this.collectionId = collectionId;
		this.damsClient = damsClient;
		String[] fileParts = contentId.split("-");
		compId = fileParts.length == 1 || fileParts[0].equals("0")?"":fileParts[0];
		String fName = new File(sourceFile).getName();
		int idx = fName.indexOf(".");
		fileId = (fileParts.length == 1?fileParts[0]:fileParts[1]) + (idx>0?fName.substring(idx):"");
	}
	
	/**
	 * Process the request, execute the procedure which has not being executed successful 
	 * @return
	 * @throws Exception 
	 */
	public boolean execute() throws Exception{	
		if(subjectId == null || subjectId.length() < 10)
			throw new Exception("Invalid Subject ID: " + subjectId);
		return uploadFile();
	}
	
	/**
	 * Upload the file to DAMS
	 * @return
	 * @throws Exception
	 */
	public boolean uploadFile() throws Exception {
		return damsClient.createFile(subjectId, compId, fileId, sourceFile, use);
	}
	
	public String getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(String collectionId) {
		this.collectionId = collectionId;
	}

	public String getSubjectId() {
		return subjectId;
	}

	public void setSubjectId(String subjectId) {
		this.subjectId = subjectId;
	}

	public static String getFilePaths() {
		return filePaths;
	}

	public static void setFilePaths(String filePaths) {
		DAMSUploadTaskHandler.filePaths = filePaths;
	}

	public String getCompId() {
		return compId;
	}

	public void setCompId(String compId) {
		this.compId = compId;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public String getSourceFile() {
		return sourceFile;
	}
	
	public void setSourceFile(String sourceFile) {
		this.sourceFile = sourceFile;
	}

	public String getUse() {
		return use;
	}

	public void setUse(String use) {
		this.use = use;
	}

	public String getArkOrg() {
		return arkOrg;
	}

	public void setArkOrg(String arkOrg) {
		this.arkOrg = arkOrg;
	}

	public String getUnitId() {
		return unitId;
	}

	public void setUnitId(String unitId) {
		this.unitId = unitId;
	}
}
