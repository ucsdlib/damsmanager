package edu.ucsd.library.xdre.web;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.annotation.PostConstruct;
import javax.mail.internet.AddressException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileItemFactory;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.methods.HttpGet;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Node;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.json.simple.JSONArray;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.collection.BatchExportHandler;
import edu.ucsd.library.xdre.collection.ChecksumsHandler;
import edu.ucsd.library.xdre.collection.CollectionHandler;
import edu.ucsd.library.xdre.collection.CollectionReleaseHandler;
import edu.ucsd.library.xdre.collection.DerivativeHandler;
import edu.ucsd.library.xdre.collection.FileCountValidaionHandler;
import edu.ucsd.library.xdre.collection.FileIngestionHandler;
import edu.ucsd.library.xdre.collection.FileReportHandler;
import edu.ucsd.library.xdre.collection.FileUploadHandler;
import edu.ucsd.library.xdre.collection.FilestoreSerializationHandler;
import edu.ucsd.library.xdre.collection.JhoveReportHandler;
import edu.ucsd.library.xdre.collection.MetadataExportHandler;
import edu.ucsd.library.xdre.collection.MetadataImportHandler;
import edu.ucsd.library.xdre.collection.SOLRIndexHandler;
import edu.ucsd.library.xdre.imports.RDFDAMS4ImportTsHandler;
import edu.ucsd.library.xdre.model.DAMSCollection;
import edu.ucsd.library.xdre.tab.BatchEditExcelSource;
import edu.ucsd.library.xdre.tab.ExcelSource;
import edu.ucsd.library.xdre.tab.FilesChecker;
import edu.ucsd.library.xdre.tab.InputStreamEditRecord;
import edu.ucsd.library.xdre.tab.InputStreamRecord;
import edu.ucsd.library.xdre.tab.RDFExcelConvertor;
import edu.ucsd.library.xdre.tab.Record;
import edu.ucsd.library.xdre.tab.RecordSource;
import edu.ucsd.library.xdre.tab.SubjectExcelSource;
import edu.ucsd.library.xdre.tab.SubjectMatching;
import edu.ucsd.library.xdre.tab.SubjectTabularRecord;
import edu.ucsd.library.xdre.tab.TabularRecord;
import edu.ucsd.library.xdre.tab.TabularRecordBasic;
import edu.ucsd.library.xdre.tab.XsltSource;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.FileUtils;
import edu.ucsd.library.xdre.utils.RequestOrganizer;


 /**
 * Class CollectionOperationController handles the operations for collection development 
 *
 * @author lsitu@ucsd.edu
 */
public class CollectionOperationController implements Controller {
	private static Logger log = Logger.getLogger(CollectionOperationController.class);
	@PostConstruct
	public void afterPropertiesSet(){
		
	}
	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String message = "";
		
		Map<String, String[]> paramsMap = null;
		if(ServletFileUpload.isMultipartContent(request)){
			paramsMap = new HashMap<String, String[]>();
			paramsMap.putAll(request.getParameterMap());
			FileItemFactory factory = new DiskFileItemFactory();

	        //Create a new file upload handler
			ServletFileUpload upload = new ServletFileUpload(factory);
	        
			List items = null;
			InputStream in = null;
			ByteArrayOutputStream out = null;
			byte[] buf = new byte[4096];
			List<String> dataItems = new ArrayList<String>();
			List<String> fileNames = new ArrayList<String>();
			try {
				items = upload.parseRequest(request);

			    Iterator<FileItem> iter = items.iterator();
			    while (iter.hasNext()) {
				    FileItem item = (FileItem) iter.next();
		
				    out = new ByteArrayOutputStream();
				    if (item.isFormField()){
				    	paramsMap.put(item.getFieldName(), new String[]{item.getString()});
				    }else{
				    	fileNames.add(item.getName());
				    	in = item.getInputStream();
				    	
				    	int bytesRead = -1;
						while((bytesRead=in.read(buf)) > 0){
							out.write(buf, 0, bytesRead);
						}
						dataItems.add(out.toString()) ;
				    }
			    }		    
			} catch (FileUploadException e) {
				throw new ServletException(e.getMessage());
			}finally{
				if(in != null){
					in.close();
					in = null;
				}
				if(out != null){
					out.close();
					out = null;
				}
			}
			
			if(dataItems.size() > 0){
				String[] a = new String[dataItems.size()];
				paramsMap.put("data", dataItems.toArray(a));
				paramsMap.put("fileName", fileNames.toArray(new String[dataItems.size()]));
			}
		}else
			paramsMap = request.getParameterMap();
		
		String collectionId = getParameter(paramsMap, "category");
		String activeButton = getParameter(paramsMap, "activeButton");
		boolean dataConvert = getParameter(paramsMap, "dataConvert") != null;
		boolean isIngest = getParameter(paramsMap, "ingest") != null;
		boolean isDevUpload = getParameter(paramsMap, "devUpload") != null;
		boolean isBSJhoveReport = getParameter(paramsMap, "bsJhoveReport") != null;
		boolean isSolrDump = getParameter(paramsMap, "solrDump") != null || getParameter(paramsMap, "solrRecordsDump") != null;
		boolean isSerialization = getParameter(paramsMap, "serialize") != null;
		boolean isMarcModsImport = getParameter(paramsMap, "marcModsImport") != null;
		boolean isExcelImport = getParameter(paramsMap, "excelImport") != null;
		boolean isSubjectImport = getParameter(paramsMap, "subjectImport") != null;
		boolean isCollectionImport = getParameter(paramsMap, "collectionImport") != null;
		boolean isBatchEdit = getParameter(paramsMap, "batchEdit") != null;
		boolean isCollectionRelease = getParameter(paramsMap, "collectionRelease") != null;
		boolean isFileUpload = getParameter(paramsMap, "fileUpload") != null;
		boolean isFileReport = getParameter(paramsMap, "fileReport") != null;
		boolean isBatchExport = getParameter(paramsMap, "batchExport") != null;
		if(activeButton == null || activeButton.length() == 0)
			activeButton = "validateButton";
		HttpSession session = request.getSession();
		session.setAttribute("category", collectionId);
		session.setAttribute("user", request.getRemoteUser());
		
		String ds = getParameter(paramsMap, "ts");
		if(ds == null || ds.length() == 0)
			ds = Constants.DEFAULT_TRIPLESTORE;
		
		String forwardTo = "/controlPanel.do?ts=" + ds ;
		if(dataConvert)
			forwardTo = "/pathMapping.do?ts=" + ds;
		else if(isIngest){
			String unit = getParameter(paramsMap, "unit");
			forwardTo = "/ingest.do?ts=" + ds + (unit!=null?"&unit=" + unit:"");
		}else if(isDevUpload)
			forwardTo = "/devUpload.do?";
		else if(isSolrDump)
			forwardTo = "/solrDump.do" + (StringUtils.isBlank(collectionId) ? "" : "#colsTab");
		else if(isSerialization)
			forwardTo = "/serialize.do?";
		else if(isMarcModsImport)
			forwardTo = "/marcModsImport.do?";
		else if(isExcelImport)
			forwardTo = "/excelImport.do?";
		else if(isCollectionImport)
			forwardTo = "/collectionImport.do?";
		else if(isBatchEdit)
		    forwardTo = "/batchEdit.do?";
		else if(isSubjectImport)
			forwardTo = "/subjectImport.do?";
		else if(isCollectionRelease)
			forwardTo = "/collectionRelease.do?";
		else if(isFileUpload)
			forwardTo = "/fileUpload.do?";
		else if(isFileReport)
			forwardTo = "/fileReport.do?";
		else if (isBatchExport)
			forwardTo = "/batchExport.do?";

		if(( !(isBatchExport || isBatchEdit || isSolrDump || isBSJhoveReport || isDevUpload || isFileUpload || isFileReport || isSubjectImport)
				&& getParameter(paramsMap, "rdfImport") == null && getParameter(paramsMap, "externalImport") == null
				&& getParameter(paramsMap, "dataConvert") == null ) && getParameter(paramsMap, "marcModsImport") == null
				&& getParameter(paramsMap, "excelImport") == null && getParameter(paramsMap, "collectionImport") == null 
				&& (collectionId == null || (collectionId=collectionId.trim()).length() == 0)){
			message = "Please choose a collection ...";
		}else{
			String servletId = getParameter(paramsMap, "progressId");
			boolean vRequest = false; 
			try{
				vRequest = RequestOrganizer.setReferenceServlet(session, servletId, Thread.currentThread());
			}catch (Exception e){
				message = e.getMessage();
			}
			if(!vRequest){
				if(isSolrDump || isCollectionRelease || isFileUpload || isFileReport)
					session.setAttribute("message", message);
				else {
					forwardTo += "&activeButton=" + activeButton;
					forwardTo += "&message=" + message;
				}

				forwordPage(request, response, response.encodeURL(forwardTo));
				return null;
			}
			
			session.setAttribute("status", "Processing request ...");
			try {
				message = handleProcesses(paramsMap, request.getSession());
			} catch (Exception e) {
				e.printStackTrace();
				//throw new ServletException(e.getMessage());
				message += "<br />Internal Error: " + e.getMessage();
			}
		}
		System.out.println("XDRE Manager execution for " + request.getRemoteUser() + " from IP " + request.getRemoteAddr() + ": ");
		System.out.println(message.replace("<br />", "\n"));
		
		try{
			int count = 0;
			String result = (String) session.getAttribute("result");
			while(result != null && result.length() > 0 && count++ < 10){
				try {
					Thread.sleep(500);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				result = (String) session.getAttribute("result");
			}
			RequestOrganizer.clearSession(session);
		}catch(IllegalStateException e){
			//e.printStackTrace();
		}
		
		if(isSolrDump || isMarcModsImport || isExcelImport || isCollectionImport || isSubjectImport || isCollectionRelease || isFileUpload || isFileReport) {
			session.setAttribute("message", message.replace("\n", "<br />"));
			if(collectionId != null && (isMarcModsImport || isExcelImport || isCollectionRelease || isFileReport))
				forwardTo += "category=" + collectionId;
		}else{
			forwardTo += "&activeButton=" + activeButton;
			if(collectionId != null)
				forwardTo += "&category=" + collectionId;
			
			forwardTo += "&message=" + URLEncoder.encode(message.replace("\n", "<br />"), "UTF-8");
		}
		System.out.println(forwardTo);
		//String forwardToUrl = "/controlPanel.do?category=" + collectionId + "&message=" + message + "&activeButton=" + activeButton;
		forwordPage(request, response, response.encodeURL(forwardTo));
		return null;
	}	
	
	private void forwordPage(HttpServletRequest request, HttpServletResponse response, String forwardToUrl) throws ServletException, IOException{
		try{
			response.sendRedirect(request.getContextPath() + forwardToUrl);
		}catch(Exception e){
			e.printStackTrace();
			try{
				String logLink = "https://" + (Constants.CLUSTER_HOST_NAME.indexOf("localhost")>=0?":8443":Constants.CLUSTER_HOST_NAME.indexOf("gimili")>=0?Constants.CLUSTER_HOST_NAME+ "ucsd.edu:8443":Constants.CLUSTER_HOST_NAME+".ucsd.edu") + "/damsmanager/downloadLog.do?sessionId=" + request.getSession().getId() + "\">log</a>";
				response.sendRedirect(request.getContextPath() + forwardToUrl.substring(forwardToUrl.indexOf("&message=")) + "Execution finished. For details, please view " + logLink);
			}catch(Exception e1){
				e.printStackTrace();
				response.sendError(HttpServletResponse.SC_TEMPORARY_REDIRECT, e.getMessage());
			}
		}
	}
	
	/*
	 * Method to handler the major requests
	 */
	public static String handleProcesses( Map<String, String[]> paramsMap, HttpSession session) throws Exception
	{
		
		String message = "";
		String returnMessage = "";
		DAMSClient damsClient = null;
		String collectionId = getParameter(paramsMap, "category");
		
		boolean[] operations = new boolean[23];
		operations[0] = getParameter(paramsMap, "validateFileCount") != null;
		operations[1] = getParameter(paramsMap, "validateChecksums") != null;
		operations[2] = getParameter(paramsMap, "rdfImport") != null;
		operations[3] = getParameter(paramsMap, "createDerivatives") != null;
		operations[4] = getParameter(paramsMap, "collectionRelease") != null;
		operations[5] = getParameter(paramsMap, "externalImport") != null;
		operations[6] = getParameter(paramsMap, "marcModsImport") != null
				|| getParameter(paramsMap, "excelImport") != null
				|| getParameter(paramsMap, "collectionImport") != null
				|| getParameter(paramsMap, "batchEdit") != null;
		operations[7] = getParameter(paramsMap, "luceneIndex") != null
				|| getParameter(paramsMap, "solrDump") != null
				|| getParameter(paramsMap, "solrRecordsDump") != null;
		operations[8] = getParameter(paramsMap, "sendToCDL") != null;
		operations[9] = getParameter(paramsMap, "dataConvert") != null;
		operations[10] = getParameter(paramsMap, "ingest") != null;
		operations[11] = getParameter(paramsMap, "serialize") != null;
		operations[12] = getParameter(paramsMap, "tsSyn") != null;
		operations[13] = getParameter(paramsMap, "createJson") != null;
		operations[14] = getParameter(paramsMap, "cacheJson") != null;
		operations[15] = getParameter(paramsMap, "fileUpload") != null;
		operations[16] = getParameter(paramsMap, "jsonDiffUpdate") != null;
		operations[17] = getParameter(paramsMap, "validateManifest") != null;
		operations[18] = getParameter(paramsMap, "metadataExport") != null;
		operations[19] = getParameter(paramsMap, "jhoveReport") != null;
		operations[20] = getParameter(paramsMap, "fileReport") != null;
		operations[21] = getParameter(paramsMap, "subjectImport") != null;
		operations[22] = getParameter(paramsMap, "batchExport") != null;

		int submissionId = (int)System.currentTimeMillis();
		session.setAttribute("submissionId", submissionId);
		String logLink = "https://" + (Constants.CLUSTER_HOST_NAME.indexOf("localhost")>=0?"localhost:8443" :
			Constants.CLUSTER_HOST_NAME.indexOf("lib-ingest")>=0?Constants.CLUSTER_HOST_NAME+".ucsd.edu:8443" :
				Constants.CLUSTER_HOST_NAME+".ucsd.edu") + "/damsmanager/downloadLog.do?submissionId=" + submissionId;
		String dataLink = "";
		
		String ds = getParameter(paramsMap, "ts");
		String dsDest = null;
		if((ds == null || (ds=ds.trim()).length() == 0) && !(operations[15] || operations[16]))
			ds = Constants.DEFAULT_TRIPLESTORE;
		else if (operations[12]){
			dsDest = getParameter(paramsMap, "dsDest");
			if (dsDest == null)
				throw new ServletException("No destination triplestore data source provided...");
			else if(ds.equals(dsDest) || !dsDest.startsWith("ts/"))
				throw new ServletException("Can't sync triplestore from " + ds + " to destination " + dsDest + ".");
		}
		

		String user = (String)session.getAttribute("user");
		damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
		damsClient.setTripleStore(ds);
		damsClient.setUser(user);

		File matchedSubjectsFile = null;
		File logFile = null;
		File arkReportFile = null;
		String[] emails = null;
		JSONArray mailArr = (JSONArray)damsClient.getUserInfo(user).get("mail");
		if(mailArr != null && mailArr.size() > 0){
			emails = new String[mailArr.size()];
			mailArr.toArray(emails);
		}
		
		String clientVersion = session.getServletContext().getInitParameter("src-version");
		String clientTool = "Custom";

		if(message.length() == 0){
		 int userId = -1;
		 String userIdAttr = (String) session.getAttribute("employeeId");
		 if(userIdAttr != null && userIdAttr.length() > 0){
			 try{
			     userId = Integer.parseInt(userIdAttr);
			 }catch(NumberFormatException e){
				 userId = -1;
			 }
		 }
		 
		 CollectionHandler handler = null;
		 OutputStream fileOut = null;
		 	 
	 try {
	 
	 boolean successful = true;
	 for(int i=0; i<operations.length; i++){
		 handler = null;
		 String exeInfo = "";
				
		 if(operations[i]){
			 String opMessage = "Preparing procedure ";
			 RequestOrganizer.setProgressPercentage(session, 0);
			 message = "";
			
			 if(i == 0){
				 session.setAttribute("status", opMessage + "File Count Validation ...");
				 boolean ingestFile = getParameter(paramsMap, "ingestFile") != null;
				 boolean dams4FileRename = getParameter(paramsMap, "dams4FileRename") != null;
				 handler = new FileCountValidaionHandler(damsClient, collectionId);
				 ((FileCountValidaionHandler)handler).setDams4FileRename(dams4FileRename);
				 if(ingestFile){
					 String[] filesPaths = getParameter(paramsMap, "filesLocation").split(";");
					  List<String> ingestFiles = new ArrayList<String>();
					  for(int j=0; j<filesPaths.length; j++)
						  ingestFiles.add(new File(Constants.DAMS_STAGING + "/" + filesPaths[j]).getAbsolutePath());
					 ((FileCountValidaionHandler)handler).setIngestFile(ingestFile);
					 ((FileCountValidaionHandler)handler).setFilesPaths(ingestFiles.toArray(new String[ingestFiles.size()]));
				 }
			 }else if (i == 1){
				   session.setAttribute("status", opMessage + "Checksum Validation ...");
				   handler = new ChecksumsHandler(damsClient, collectionId, null);
			 }else if (i == 2){	
				  session.setAttribute("status", opMessage + "Importing metadata ...");
				  String dataFormat = getParameter(paramsMap, "dataFormat");
				  String importMode = getParameter(paramsMap, "importMode");
				  handler = new MetadataImportHandler(damsClient, collectionId, getParameter(paramsMap, "data"), dataFormat, importMode);
			 }else if (i == 3){
				 session.setAttribute("status", opMessage + "Derivatives Creation ...");
				 boolean derReplace = getParameter(paramsMap, "derReplace")==null?false:true;
				 
				 String reqSize = getParameter(paramsMap, "size");
				 String frameNo = getParameter(paramsMap, "frameNo").trim();
				 String[] sizes = null;
				 if(reqSize != null && reqSize.length() > 0)
					 sizes = reqSize.split(",");
				 handler = new DerivativeHandler(damsClient, collectionId, sizes, derReplace);
				 if(StringUtils.isNotBlank(frameNo))
					 ((DerivativeHandler)handler).setFrameNo(frameNo);

			 }else if (i == 4){	
				 session.setAttribute("status", opMessage + " release collection " + collectionId + " ...");
				 String releaseState = getParameter(paramsMap, "releaseState");
				 String releaseOption = getParameter(paramsMap, "releaseOption");
				 String collectionToMerge = getParameter(paramsMap, "collectionToMerge");
				 
				 log.info("Collection release:  category =>" + collectionId + ", releaseState => " + releaseState + ", releaseOption => " + releaseOption + ", collectionToMerge => " + collectionToMerge);

				 handler = new CollectionReleaseHandler(damsClient, collectionId, releaseState, releaseOption);			 
				 ((CollectionReleaseHandler)handler).setCollectionToMerge(collectionToMerge);
			 }else if (i == 5){	
				  session.setAttribute("status", opMessage + "Importing objects ...");
				  String[] dataPaths = getParameter(paramsMap, "dataPath").split(";");
				  String[] filesPaths = getParameter(paramsMap, "filesPath").split(";");
				  String importOption = getParameter(paramsMap, "importOption");
				  boolean replace = getParameter(paramsMap, "externalImportReplace") != null;
				  List<File> dFiles = new ArrayList<File>();
				  for(int j=0; j<dataPaths.length; j++){
					  String dataPath = dataPaths[j];
					  if(dataPath != null && (dataPath=dataPath.trim()).length() > 0){
						  File file = new File(Constants.DAMS_STAGING + "/" + dataPath);
						  CollectionHandler.listFiles(dFiles, file);
					  }
				  }
				  
				  List<String> ingestFiles = new ArrayList<String>();
				  for(int j=0; j<filesPaths.length; j++){
					  if((filesPaths[j]=filesPaths[j].trim()).length() > 0)
						  ingestFiles.add(new File(Constants.DAMS_STAGING + "/" + filesPaths[j]).getAbsolutePath());
				  }
				  
				  String[] excelExts = {"xls", "xlsx"};
				  List<File> excelFiles = FileUtils.filterFiles(dFiles, excelExts);
				  
				  if (excelFiles.size() > 0) {
					  // Remove the Excel source that need conversion from the file list
					  dFiles.removeAll(excelFiles);
					  
					  // Pre-processing
					  boolean preprocessing = importOption.equalsIgnoreCase("pre-processing");
					  Element rdfPreview = null;
					  StringBuilder errorMessage = new StringBuilder();
					  StringBuilder duplicatRecords = new StringBuilder();
					  List<String> ids = new ArrayList<String>();
					  if (preprocessing) {
						  Document doc = new DocumentFactory().createDocument();
						  rdfPreview = TabularRecord.createRdfRoot (doc);
					  }
					  handler = new MetadataImportHandler(damsClient, null);
					  handler.setSubmissionId(submissionId);
		 			  handler.setSession(session);
		 			  handler.setUserId(userId);

					  // Directory to hold the converted rdf/xml
					  File tmpDir = new File (Constants.TMP_FILE_DIR + File.separatorChar + "converted");
					  if(!tmpDir.exists())
						  tmpDir.mkdir();
					  
					  // Convert Excel source files to DAMS4 rdf/xml
					  int filesCount = 0;
					  for (File f : excelFiles) {
						  filesCount++;
						  RecordSource src = new ExcelSource(f);

						  for (Record rec = null; (rec = src.nextRecord()) != null;) {
							  String id = rec.recordID();
							  handler.logMessage("Pre-processing record with ID " + id + " ... ");
							  
							  if(ids.indexOf(id) < 0) {
								  ids.add(id);
							  } else {
								  duplicatRecords.append(id + ", ");
								  handler.logError("Found duplicated record with ID " + id + ".");
							  }
							  
							  try {
								  
								  Document doc = rec.toRDFXML();
								  if (duplicatRecords.length() == 0 && errorMessage.length() == 0) {
									  if (preprocessing) {
										  // preview when there are no error reported
										  rdfPreview.add(rec.toRDFXML().selectSingleNode("//dams:Object").detach()); 
									  } else {
										  File convertedFile = new File(tmpDir.getAbsolutePath(), id.replaceAll("[\\//:.*]+","") + ".rdf.xml");
										  try{
											  writeXml(convertedFile, doc.asXML());
										  } finally {										  
											  convertedFile.deleteOnExit();
											  if(dFiles.indexOf(convertedFile) < 0) {
												  dFiles.add(convertedFile);
												  handler.logMessage("Added converted RDF/XML file " + convertedFile.getAbsolutePath());
											  }
										  }
									  }
								  }
							  } catch(Exception e) {
								  log.warn("Excel Input Stream error", e);
								  errorMessage.append("-" + e.getMessage() + "\n");
								  handler.logMessage(e.getMessage() + "\n");
							  }
				          }
						  handler.setProgressPercentage(filesCount * 100/excelFiles.size());
					  }
					  
					  if (errorMessage.length() == 0 && duplicatRecords.length() == 0) {
							 
						  if (preprocessing) {
							  File destFile = new File(Constants.TMP_FILE_DIR, "preview-" + submissionId + "-rdf.xml");
							  writeXml(destFile, rdfPreview.getDocument().asXML());

							  successful = true;
							  message = "\nPre-processing passed. ";
							  message += "\nThe converted RDF/XML is ready for <a href=\"" + logLink + "&file=" + destFile.getName() + "\">download</a>.";
							  //handler.logMessage(message);
							  handler.release();
							  handler = null;
						  } else {
							  handler.release();
							  // Initiate the ingest task for Excel AND/OR RDF/XML files
							  handler = new RDFDAMS4ImportTsHandler(damsClient, dFiles.toArray(new File[dFiles.size()]), importOption);
							  ((RDFDAMS4ImportTsHandler)handler).setFilesPaths(ingestFiles.toArray(new String[ingestFiles.size()]));
							  ((RDFDAMS4ImportTsHandler)handler).setReplace(replace);
						  }
					  } else {
						  successful = false;
						  message = "\nPre-processing issues found:";
						  if (duplicatRecords.length() > 0)
							  message += "\nDuplicated records: " + duplicatRecords.substring(0, duplicatRecords.length() - 2).toString();
						  if (errorMessage.length() > 0)
							  message += "\nOther Errors: \n" + errorMessage.toString();
						  //handler.logMessage(message);
						  handler.release();
						  handler = null;
					  }
				  } else {
					  // Ingest for RDF/XML files
					  handler = new RDFDAMS4ImportTsHandler(damsClient, dFiles.toArray(new File[dFiles.size()]), importOption);
					  ((RDFDAMS4ImportTsHandler)handler).setFilesPaths(ingestFiles.toArray(new String[ingestFiles.size()]));
					  ((RDFDAMS4ImportTsHandler)handler).setReplace(replace);
				  }
			 } else if (i == 6){	
				  session.setAttribute("status", opMessage + "Importing from Standard Input Stream source ...");
				  log.info(opMessage + "Importing from Standard Input Stream source ...");

				  String unit = getParameter(paramsMap, "unit");
				  String source = getParameter(paramsMap, "source");
				  String bibNumber = getParameter(paramsMap, "bibInput");
				  String modsXml = getParameter(paramsMap, "modsInput");
				  String copyrightStatus =getParameter(paramsMap, "copyrightStatus");
				  String copyrightJurisdiction = getParameter(paramsMap, "countryCode");
				  String copyrightOwner = getParameter(paramsMap, "copyrightOwner");
				  String program = getParameter(paramsMap, "program");
				  String access = getParameter(paramsMap, "accessOverride");
				  String beginDate = getParameter(paramsMap, "licenseBeginDate");
				  String endDate = getParameter(paramsMap, "licenseEndDate");
				  String[] dataPaths = getParameter(paramsMap, "dataPath").split(";");
				  String filePath = getParameter(paramsMap, "filesPath");
				  String[] filesPaths = filePath == null ? new String[0] : filePath.split(";");
				  String importOption = getParameter(paramsMap, "importOption");
				  String preingestOption = getParameter(paramsMap, "preingestOption");
				  String filesCheckPath = getParameter(paramsMap, "filesCheckPath");
				  String[] filesCheckPaths = filesCheckPath == null ? new String[0] : filesCheckPath.split(";");

				  boolean excelImport = getParameter(paramsMap, "excelImport") != null;
				  boolean batchEdit = getParameter(paramsMap, "batchEdit") != null;
				  boolean collectionImport = getParameter(paramsMap, "collectionImport") != null;
				  boolean preprocessing = importOption == null;
				  boolean filesCheck = preingestOption != null && preingestOption.startsWith("file-");
				  boolean watermarking = getParameter(paramsMap, "watermarking") != null;

				  List<String> ingestFiles = new ArrayList<String>();
				  if (preprocessing)
					  filesPaths = filesCheckPaths;  

				  for(int j=0; j<filesPaths.length; j++){
					  if((filesPaths[j]=filesPaths[j].trim()).length() > 0)
						  ingestFiles.add(new File(Constants.DAMS_STAGING + "/" + filesPaths[j]).getAbsolutePath());
					  System.out.println(j + " files path: " + filesPaths[j] + "=>" + new File(Constants.DAMS_STAGING + "/" + filesPaths[j]).getAbsolutePath());
				  }

				  List<File> dataFiles = new ArrayList<File>();
				  for(int j=0; j<dataPaths.length; j++){
					  String dataPath = dataPaths[j];
					  if(dataPath != null && (dataPath=dataPath.trim()).length() > 0){
						  File file = new File(Constants.DAMS_STAGING + "/" + dataPath);
						  CollectionHandler.listFiles(dataFiles, file);
					  }
				  }
				  
				  // initiate the source metadata
				  List<Object> sources = new ArrayList<Object>();
				  if (source != null && source.equalsIgnoreCase("bib")) {
					  String[] bibs = bibNumber.split(",");
					  for  (int j=0; j<bibs.length; j++) {
						  if(bibs[j] != null && (bibs[j]=bibs[j].trim()).length() > 0)
							  sources.add(bibs[j]);
					  }
				  } else {
					  List<String> filters = new ArrayList<>();
					  if (batchEdit || excelImport || collectionImport) {
						  // Excel Input Stream
						  source = "excel";
						  filters.add("xls");
						  filters.add("xlsx");
					  } else {
						  // MARC/MODS source
						  filters.add("xml");
					  }

					  dataFiles = FileUtils.filterFiles(dataFiles, filters.toArray(new String[filters.size()]));
					  sources.addAll(dataFiles);
					  dataFiles.clear();
				  }

				  // Handling pre-processing request
				  Element rdfPreview = null;
				  StringBuilder duplicatRecords = new StringBuilder();
				  List<String> ids = new ArrayList<String>();
				  boolean ingestWithFiles = ingestFiles.size() > 0;
				  
				  if (preprocessing) {
					  Document doc = new DocumentFactory().createDocument();
					  rdfPreview = TabularRecord.createRdfRoot (doc);
				  }
				  
				  int recordsCount = 0;
				  boolean preSuccessful = true;
				  boolean hasWarnings = false;
				  StringBuilder proMessage = new StringBuilder();
				  List<String> srcFileNames = new ArrayList<String>();

				  if (source != null && (source.equalsIgnoreCase("bib") || source.equalsIgnoreCase("mods")
						  || source.equalsIgnoreCase("excel")) || batchEdit) {
					  // Initiate the logging handler 
					  handler = new MetadataImportHandler(damsClient, null);
					  handler.setSubmissionId(submissionId);
		 			  handler.setSession(session);
		 			  handler.setUserId(userId);
		 			  
		 			  Map<String, String> collections = new HashMap<String, String>();
		 			  if (StringUtils.isNotBlank(collectionId)) {
		 				  String collType = damsClient.getCollectionType(collectionId);
		 				 collections.put(collectionId, collType);
		 			  }
		 			  
					  for  (int j=0; j<sources.size(); j++) {
						  InputStream in = null;
						  String sourceID = null;
						  
						  Object srcRecord =  sources.get(j);
						  sourceID = (srcRecord instanceof File ? ((File)srcRecord).getName() : srcRecord.toString());
						  if (preprocessing)
							  handler.setStatus("Pre-processing record " + sourceID + " ... ");
						  else
							  handler.setStatus("Processing record " + sourceID + " ... ");
						  
						  RecordSource recordSource = null;
						  InputStreamRecord record = null;
						  
						  try {
							  if (batchEdit || source.equalsIgnoreCase("excel")) {
								  clientTool = "Excel";

								  // initiate ignored fields for objects and collections
								  List<String> ignoredFields = new ArrayList<>();
								  if (collectionImport) {
								      ignoredFields.addAll(Arrays.asList(ExcelSource.IGNORED_FIELDS_FOR_COLLECTIONS));
									  // Fields not required for collection import tool
									  ignoredFields.addAll(Arrays.asList(ExcelSource.RIGHTS_VALIDATION_FIELDS));

									  if (StringUtils.isNotBlank(Constants.BATCH_ADDITIONAL_FIELDS)) {
										  String[] additionalFields = Constants.BATCH_ADDITIONAL_FIELDS.trim().split(",");
										  ignoredFields.addAll(Arrays.asList(additionalFields));
									  }
								  } else
									  ignoredFields = Arrays.asList(ExcelSource.IGNORED_FIELDS_FOR_OBJECTS);

								  if (batchEdit) {
									  recordSource = new BatchEditExcelSource((File)srcRecord, ignoredFields);
								  } else {
    								  // Handling Excel Input Stream records
    								  recordSource = new ExcelSource((File)srcRecord, ignoredFields);
    								  ((ExcelSource)recordSource).setWatermarking(watermarking);
								  }

								  // Report for Excel column name validation
								  List<String> invalidColumns = ((ExcelSource)recordSource).getInvalidColumns();

								  if (invalidColumns != null && invalidColumns.size() > 0) {
									  successful = false;
									  preSuccessful = false;

									  proMessage.append("Excel source " + sourceID + " - failed - " + CollectionHandler.damsDateFormat.format(new Date()) + ": \n");

									  if (invalidColumns != null && invalidColumns.size() > 0) {
										  // Report invalid columns
										  proMessage.append( "* Found the following invalid column name" + (invalidColumns.size() > 1 ? "s" : "") + ": " );
										  for (int k=0; k<invalidColumns.size(); k++) {
											  proMessage.append(invalidColumns.get(k));
											  if (k == invalidColumns.size() - 1)
												  proMessage.append("\n");
											  else
												  proMessage.append("; ");
										  }
									  }
								  }
							  } else {
								  // Handling AT/Roger records
								  try {
									  if (source.equalsIgnoreCase("bib")) {
										  
										  clientTool = "MARC";
										  String url = Constants.DAMS_STORAGE_URL.substring(0, Constants.DAMS_STORAGE_URL.indexOf("/dams/"))
												   + "/jollyroger/get?type=bib&mods=true&ns=true&value=" + sourceID;
			
										  log.info("Getting MARC XML for Roger record " + sourceID + " from URL: " + url);
										  HttpGet req = new HttpGet(url);
										  Document doc = damsClient.getXMLResult(req);
										  modsXml = doc.asXML();
										  in = new ByteArrayInputStream (modsXml.getBytes("UTF-8"));
									  } else {
										  // METS/MODS XML from staging area
										  clientTool = "AT";
										  File srcFile = (File)sources.get(j);
										  in = new FileInputStream(srcFile);
									  }
								  
									  File xsl = new File(session.getServletContext().getRealPath("files/mets2dams.xsl"));
									  recordSource = new XsltSource( xsl, sourceID.replaceAll("\\..*",""), in );
								  } finally {
									  CollectionHandler.close(in);
									  in = null;
								  }
							  }
						  } catch (Exception e) {
							  e.printStackTrace();
							  successful = false;
							  preSuccessful = false;
							  String error = e.getMessage() != null ? e.getMessage() : e.getCause() != null ? e.getCause().getMessage() : e.getClass().getName();
							  handler.setStatus(error);
							  log.error("Error metadata source " + sourceID + ": " + error);
							  proMessage.append(sourceID + " - failed - " + CollectionHandler.damsDateFormat.format(new Date()) + " - " + error);
						  }

						  String id = "";
						  String info = "";
						  if (recordSource != null && preSuccessful) {
							  String[] copyrightOwners = null;
							  if (StringUtils.isNotBlank(copyrightOwner))
								  copyrightOwners = copyrightOwner.split("\\|");

							  for (Record rec = null; (rec = recordSource.nextRecord()) != null;) {

								  recordsCount++;
								  String objTitle = "";
								  id = rec.recordID();  
								  StringBuilder errorMessage = new StringBuilder(); 
								  StringBuilder warningMessage = new StringBuilder(); 
								  try {
									  if (collectionImport) {
										  String collType = getParameter(paramsMap, "collType");
										  String visibility = getParameter(paramsMap, "visibility");
										  String parentCollection = getParameter(paramsMap, "parentCollection");
							 			  Map<String, String> parentCollections = new HashMap<String, String>();
							 			  if (StringUtils.isNotBlank(parentCollection)) {
							 				  String parentType = damsClient.getCollectionType(parentCollection);
							 				 parentCollections.put(parentCollection, parentType);
							 			  }

										  id = collectionId;
										  if (!preprocessing && StringUtils.isBlank(id)) {
											  collectionId = id = RDFDAMS4ImportTsHandler.toDamsUrl(damsClient.mintArk(Constants.DEFAULT_ARK_NAME));
										  }
							 			  record = new InputStreamRecord(rec, id, collType, parentCollections, unit, visibility);

							 			 String collTitle = record.toRDFXML().selectSingleNode("//dams:title/mads:Title/mads:authoritativeLabel").getStringValue();
							 			 String existed = checkRecord (collTitle, damsClient);
							 			 if (StringUtils.isNotBlank(existed) && !existed.endsWith(id)) {
							 				String existingCollection = "http://" + Constants.CLUSTER_HOST_NAME + (Constants.CLUSTER_HOST_NAME.startsWith("localhost") ? "" : ".ucsd.edu/dc") 
							 						+ "/collection/" + existed.substring(existed.lastIndexOf("/") + 1);
							 				errorMessage.append("\n* Collection with title \"" + collTitle +  "\" exists for " + existingCollection + "! Please use another title.");
							 			 }

							 			 if (recordsCount == 2) {
											  preSuccessful = false;
											  proMessage.append("\n* Found more than one record in the Excel file. Only one records allowed for collection Input Stream!");
											  break;
										  }
								  } else if (batchEdit) {
									  record = new InputStreamEditRecord(rec, damsClient);
								  } else
									  
										  record = new InputStreamRecord (rec, collections, unit, copyrightStatus, copyrightJurisdiction, 
											  copyrightOwners, program, access, beginDate, endDate );
									  
									  objTitle = getTitle(record.toRDFXML());
									  info = "Pre-processing record with ID " + id + " ... ";
									  handler.setStatus(info);
									  log.info(info);
									  
									  if(ids.indexOf(id) < 0) {
										  ids.add(id);
									  } else {
										  duplicatRecords.append(rec + ", ");
										  String error = "Duplicated record with ID " + id;
										  handler.setStatus(error);
										  log.error(info);
										  errorMessage.append("\n* " + error);
									  }
									  
									  // Add master file(s) for the bib/Roger record: a PDF or a TIFF, or a PDF + ZIP
									  List<File> filesToIngest = null;
									  if (source.equalsIgnoreCase("bib") && ingestWithFiles) {
										  filesToIngest = getRogerFiles ((String)srcRecord, ingestFiles);
										  // Processing the master file(s) with error report. 
										  if (filesToIngest.size() == 0) {
											  errorMessage.append("\n* Roger record " + srcRecord
													  + " has no master file(s) for \"Ingest metadata and files\" option.");
										  } else if (filesToIngest.size() > 2 
												  || (filesToIngest.size() == 2 && !filesToIngest.get(1).getName().endsWith(".zip"))) {
											  errorMessage.append("\n* Unexpected file(s) for Roger record " + srcRecord + ": ");
											  for (File file : filesToIngest) {
												  errorMessage.append((filesToIngest.indexOf(file) > 0 ? ", " : "") + file.getName());
											  }
										  } else {
											  // Handle the use property for the file(s)
											  Map<String, String> fileUseMap = getFileUse(filesToIngest);
											  
											  record.addFiles(0, filesToIngest, fileUseMap);
										  }
									  } else if (source.equalsIgnoreCase("excel")) {
										  // Invalid control values
										  List<Map<String, String>> invalidValues = ((ExcelSource)recordSource).getInvalidValues();
										  appendErrorMessage(id, "Invalid control value(s)", invalidValues, errorMessage);

										  // Values with control characters
										  List<Map<String, String>> controlCharValues = ((ExcelSource)recordSource).getControlCharValues();
										  appendErrorMessage(id, "Value with control character(s) ignored", controlCharValues, warningMessage);
									  }
								  } catch (Exception e) {
									  e.printStackTrace();
									  info = "Error: " + e.getMessage();
									  handler.setStatus(info);
									  log.warn(info);
									  errorMessage.append("\n* " + e.getMessage());
								  }
							  
								  objTitle = StringUtils.isEmpty(objTitle) ? "[Object]" : objTitle;
								  if (errorMessage.length() == 0) {
									  String status = warningMessage.length() > 0 ? "warning" : "successful";

									  info = objTitle + " - " + id + " - " + " " + status + " - " + CollectionHandler.damsDateFormat.format(new Date());
									  if (warningMessage.length() > 0) {
										  info +=  " - " + warningMessage.toString();
										  hasWarnings = true;
									  } else
										  info += "\n";

									  log.info(info);

									  if (preprocessing) {

										  proMessage.append("\n" + info);

										  // Pre-processing with rdf preview
										  if(collectionImport) {
											  rdfPreview = record.toRDFXML().getRootElement(); 
										  } else
											  rdfPreview.add(record.toRDFXML().selectSingleNode("//dams:Object").detach()); 
									  } else {
										  // Pre-processing succeeded with warnings for ingest request. Add control character warnings to report.
										  if (warningMessage.length() > 0)
											  handler.addWarning(info);

										  // Write the converted rdf/xml to file system
										  File tmpDir = new File (Constants.TMP_FILE_DIR + File.separatorChar + "converted");
										  if(!tmpDir.exists())
											  tmpDir.mkdir();
										  File convertedFile = new File(tmpDir.getAbsolutePath(), id.replaceAll("[\\//:.*]+","") + ".rdf.xml");
										  try{
											  Document rdf = record.toRDFXML();
											  if (collectionImport) {
												  record.ingestCollectionImage(ingestFiles.toArray(new String[ingestFiles.size()]));
											  } else {
												  List<Node> srcFiles = rdf.selectNodes("//dams:Object//dams:File/dams:sourceFileName");
												  for (Node srcFile : srcFiles ) {
													  	srcFileNames.add(srcFile.getText());
												  }
											  }
											  writeXml(convertedFile, rdf.asXML());
										  } finally {										  
											  convertedFile.deleteOnExit();
											  dataFiles.add(convertedFile);
										  }
									  }
								  } else {
									  // Pre-processing failed
									  preSuccessful = false;
									  info = objTitle + " - " + id + " - " + " failed - " 
											  + CollectionHandler.damsDateFormat.format(new Date()) + " - " + errorMessage.toString();
									  if (warningMessage.length() > 0)
										  info += warningMessage.toString();

									  proMessage.append("\n\n" + info);
									  log.error(info);
								  }
								  
								  handler.setProgressPercentage(j * 100/sources.size()); 
							  }
						  }
					  }

					  if (preSuccessful) {

						  if (preprocessing) {
							  // pre-processing only, no ingest

							  if (filesCheck) {
								  // pre-processing for files check only
								  proMessage = new StringBuilder();
								  List<Node> srcFiles = rdfPreview.getDocument().selectNodes("//dams:Object//dams:File/dams:sourceFileName");

								  for (Node srcFile : srcFiles ) {
									  	srcFileNames.add(srcFile.getText());
								  }
								  
								  String selectedPaths = "";
								  for(int j=0; j<filesPaths.length; j++) {
									  selectedPaths += StringUtils.isNotBlank(filesPaths[j]) ? filesPaths[j] + "; " : "";
									  selectedPaths = selectedPaths.length() > 0 ? selectedPaths.substring(0, selectedPaths.length() - 2) : "";
								  }

								  FilesChecker filesChecker = new FilesChecker(srcFileNames, ingestFiles.toArray(new String[ingestFiles.size()]));
								  if (preingestOption.equalsIgnoreCase("file-match")) {
									  
									  List<String> matchedFiles = filesChecker.getMatchedFiles();
									  List<String> missingFiles = filesChecker.getMissingFiles();
									  Map<String, File> extraFiles = filesChecker.getExtraFiles();
									  message = "\nPre-ingest validation result for files match: \n";
									  
									  boolean matched = filesChecker.filesMatch();

									  // report files that are matched in selected source file location
									  proMessage.append("\n" + (matchedFiles.size() == 0 ? "No" : "There are " + matchedFiles.size()) + " matched files found in the metadata.\n");


									  // report files that are in the metadata but missing from the selected source file location
									  proMessage.append("\n" + (matched ? "No" : "The following " + missingFiles.size()) + " files are found in the metadata but missing from the selected file location"); 
									  proMessage.append(" '" + selectedPaths + "'" + (matched ? "." : ": ") + "\n");
									  for (String missingFile : missingFiles) {
										  proMessage.append("* " + missingFile + "\n");
									  }

									  // report files that are found in the selected source file location but not in the metadata
									  proMessage.append("\n" + (extraFiles.size() == 0 ? "No" : "The following " + extraFiles.size()) + " files are found in the selected file location");
									  proMessage.append(" '" + selectedPaths + "' but not in the metadata" + (matched ? "." : ":") + "\n");
									  for (String extraFile : extraFiles.keySet()) {
										  proMessage.append("* " + extraFile + " => " + extraFiles.get(extraFile).getParent() + "\n");
									  }

									  message += proMessage.toString();
									  handler.logMessage(message);
								  } else {
									  // pre-processing for files validation only  
									  if (StringUtils.isNotBlank(Constants.EXIFTOOL_COMMAND))
										  filesChecker.setCommand(Constants.EXIFTOOL_COMMAND);

									  boolean valid = filesChecker.filesValidate();
									  
									  List<String> matchedFiles = filesChecker.getMatchedFiles();
									  Map<File, String> invalidFiles = filesChecker.getInvalidFiles();
									  Map<File, String> unknownFiles = filesChecker.getUnknownFiles();
									  message = "\nPre-ingest validation result for files validation: \n";

									  // report files that are valid in selected source file location
									  int validFilesCount = matchedFiles.size() - invalidFiles.size();
									  String validMessage = "There are total " + validFilesCount + " matched files are valid";
									  if (validFilesCount <= 0)
										  validMessage = "No files are valid";
									  else if (validFilesCount != matchedFiles.size())
										  validMessage = "There are " + validFilesCount + " of " + matchedFiles.size() + " total matched files are valid";
									  validMessage += " in the selected location " + " '" + selectedPaths + "'.";
									  proMessage.append(validMessage + "\n");

									  // report files that are invalid
									  int invalidFilesCount = invalidFiles.size();
									  proMessage.append("\n" + (invalidFilesCount <= 0 ? "No" : "The following " + invalidFilesCount) + " invalid files are found" + (invalidFilesCount <= 0 ? "." : ": ") + "\n"); 
									  for (File invalidFile : invalidFiles.keySet()) {
										  proMessage.append("* " + invalidFile.getName() + " => " + invalidFile.getParent() + " - " + invalidFiles.get(invalidFile) + "\n");
									  }

									  // report files that are not supported by ExifTool
									  int unknownFilesCount = unknownFiles.size();
									  if (unknownFilesCount > 0) {
										  proMessage.append("\nThe following files are unknown file type: \n"); 
										  for (File unknownFile : unknownFiles.keySet()) {
											  proMessage.append("* " + unknownFile.getName() + " => " + unknownFile.getParent() + "\n");
										  }
									  }

									  message += proMessage.toString();
									  handler.logMessage(message);
								  }
								  
							  } else {
								  // Write the converted RDF/xml for preview
								  File destFile = new File(Constants.TMP_FILE_DIR, "preview-" + submissionId + "-rdf.xml");
								  writeXml(destFile, rdfPreview.getDocument().asXML());
								  if (preingestOption != null && preingestOption.equalsIgnoreCase("pre-processing-csv")) {
									  // convert to Excel/csv format
									  try(InputStream xsl2jsonInput = CILHarvestingTaskController.getDams42JsonXsl();) {
										  RDFExcelConvertor convertor = new RDFExcelConvertor(destFile.getAbsolutePath(), xsl2jsonInput);
										  String jsonString = convertor.convert2CSV();
										  destFile = new File(Constants.TMP_FILE_DIR, "preview-" + submissionId + ".csv");
										  write2File (destFile, jsonString);
										  dataLink = "\nThe converted source in Excel/CSV format is ready for <a href=\"" + logLink;
									  }
								  } else {
									  dataLink = "\nThe converted RDF/XML is ready for <a href=\"" + logLink;
								  }

								  dataLink += "&file=" + destFile.getName() + "\">download</a>.\n";

								  // Logging the result for pre-processing
								  message = "\nPre-processing " + (preSuccessful && hasWarnings ? "succeeded with warning(s)" : preSuccessful ? "successful" : "failed") + ": \n"
										  + (proMessage.length() == 0 ? "" : "\n " + proMessage.toString());
								  handler.logMessage(message);
							  }
						  } else {
							  // files existing check
							  if (srcFileNames.size() > 0 && !batchEdit) {
								  FilesChecker filesChecker = new FilesChecker(srcFileNames, ingestFiles.toArray(new String[ingestFiles.size()]));
								  filesChecker.filesMatch();
								  List<String> missingFiles = filesChecker.getMissingFiles();
								  if (missingFiles.size() > 0) {
									  successful = preSuccessful = false;
									  String selectedPaths = "";
									  for(int j=0; j<filesPaths.length; j++) {
										  selectedPaths += StringUtils.isNotBlank(filesPaths[j]) ? filesPaths[j] + "; " : "";
										  selectedPaths = selectedPaths.length() > 0 ? selectedPaths.substring(0, selectedPaths.length() - 2) : "";
									  }

									  // report files that are in the metadata but missing from the selected source file location
									  proMessage = new StringBuilder();
									  proMessage.append("\nThe following " + missingFiles.size() + " files are found in the metadata but missing from the selected file location"); 
									  proMessage.append(" '" + selectedPaths + "': " + "\n");
									  for (String missingFile : missingFiles) {
										  proMessage.append("* " + missingFile + "\n");
									  }								 
									  // Logging the result for failed pre-processing
									  message = "\nPre-processing failed" + ": " 
											  + (proMessage.length() == 0 ? "" : "\n " + proMessage.toString());
									  handler.logMessage(message);
								  }
							  }

							 if (preSuccessful) {
								  // handler clean up for pre-processing. Keep warning message for ingest report.
								  String warnings = handler.getWarnings();
								  handler.release();
								  handler = null;
	
								  // ingest objects with the converted RDF/XML
								  importOption = batchEdit ? "metadataOnly" : "metadataAndFiles";
	
								  handler = new RDFDAMS4ImportTsHandler(damsClient, dataFiles.toArray(new File[dataFiles.size()]), importOption);
								  ((RDFDAMS4ImportTsHandler)handler).setFilesPaths(ingestFiles.toArray(new String[ingestFiles.size()]));
								  ((RDFDAMS4ImportTsHandler)handler).setReplace(true);
								  ((RDFDAMS4ImportTsHandler)handler).setWatermarking(watermarking);
								  handler.setCollectionId(collectionId);
								  if (StringUtils.isNotBlank(warnings))
									  handler.addWarning(warnings);
							 }
						  }
					  } else {
						  successful = false;
						  // Logging the result for failed pre-processing
						  message = "\nPre-processing " + (preSuccessful?"successful":"failed") + ": \n"
								  + (proMessage.length() == 0 ? "" : "\n " + proMessage.toString());
						  handler.logMessage(message);
					  }
					  
					  // handler clean up
					  if (preprocessing || !preSuccessful) {
						  logFile = handler.getLogFile();
						  handler.release();
						  handler = null;
						  Thread.sleep(100);
					  }

				  } else {
					  successful = false;
					  message += "\nUnknown source type: " + source;
				  }
			 } else if (i == 7) {
				 session.setAttribute("status", opMessage + "SOLR Index ...");
				 boolean update = getParameter(paramsMap, "indexReplace") != null;

				 // set priority low for bulk indexing.
				 damsClient.setPriority(DAMSClient.PRIORITY_LOW);

				 if (getParameter(paramsMap, "solrRecordsDump") != null) {
					 // Handle single records submission
					 List<String> items = new ArrayList<String>();
					 String txtInput = getParameter(paramsMap, "textInput");
					 String fileInputValue = getParameter(paramsMap, "data");
					 if (txtInput != null && (txtInput = txtInput.trim()).length() > 0) {
						 String[] subjects = txtInput.split(",");
						 for (String subject : subjects) {
							 subject = subject.trim();
							 if (subject.length() > 0) {
								 items.add(subject);
							 }
						 }
					 }
					 
					 // Handle records submitted in file with csv format, in lines or mixed together
					 if (fileInputValue != null && (fileInputValue = fileInputValue.trim()).length() > 0) {
						 // Handle record with line input
						 String[] lines = fileInputValue.split("\n");
						 for (String line : lines) {
							 // Handle CSV encoding records and records delimited by comma, whitespace etc.
							 if (line != null && (line = line.trim().replace("\"", "")).length() > 0) {
								 String[] tokens = line.split(",");
								 for (String token : tokens) {
									 String[] records = token.split(" ");
									 for (String record : records) {
										 record = record.trim();
										 if (record.length() > 0) {
											 items.add(record);
										 }
									 }
								 }
							 }
						 }
					 }
					 
					 // for single items submitting from text input, set it to high priority .
					 if (StringUtils.isNotBlank(txtInput))
						 damsClient.setPriority(DAMSClient.PRIORITY_HIGH);

					 // Initiate SOLRIndexHandler to index the records
					 handler = new SOLRIndexHandler( damsClient, null, update );
					 handler.setItems(items);
					 handler.setCollectionTitle("SOLR Records");
				 } else {
					 // Handle solr update for collections
					 if(collectionId.indexOf(",") > 0){
						 String collIDs = collectionId;
						 String[] collArr = collectionId.split(",");
						 List<String> items = new ArrayList<String>();
						 String collNames = "";
						 for(int j=0; j<collArr.length; j++){
							 if(collArr[j] != null && (collArr[j]=collArr[j].trim()).length()>0){
								 collectionId = collArr[j];
								 if(collectionId.equalsIgnoreCase("all")){
									 items.addAll(damsClient.listAllRecords());
									 collNames += "All Records (" + items.size() + "), ";
								 }else{
									 try{
										 handler = new SOLRIndexHandler( damsClient, collectionId );
										 items.addAll(handler.getItems());
										 collNames += handler.getCollectionTitle() + "(" + handler.getFilesCount() + "), ";
										 if(j>0 && j%5==0)
											 collNames += "\n";
									 }finally{
										 if(handler != null){
											 handler.release();
											 handler = null;
										 } 
									 }
								 }
							 }
						 }
						 handler = new SOLRIndexHandler( damsClient, null, update );
						 handler.setItems(items);
						 handler.setCollectionTitle(collNames.substring(0, collNames.lastIndexOf(",")));
						 handler.setCollectionId(collIDs);
					 }else{
						 if(collectionId.equalsIgnoreCase("all")){
							 handler = new SOLRIndexHandler(damsClient, null, update);
							 handler.setItems(damsClient.listAllRecords());
						 }else
							 handler = new SOLRIndexHandler(damsClient, collectionId, update);
					 }
				 }
			 }/*else if (i == 8){	
				    //session.setAttribute("status", opMessage + "CDL Sending ...");
				    int operationType = 0;
				 		boolean resend = getParameter(paramsMap, "cdlResend") != null;
				 		if(resend){
				 			operationType = 1;
				 		}else{
				 			resend = getParameter(paramsMap, "cdlResendMets") != null;
				 			if(resend)
				 				operationType = 2;
				 		}
		            //handler = new CdlIngestHandler(tsUtils, collectionId, userId, operationType);
	    
			 		String feeder = getParameter(paramsMap, "feeder");
			 		session.setAttribute("status", opMessage + "CDL " + feeder.toUpperCase() + " METS feeding ...");
		 			boolean includeEmbargoed = (getParameter(paramsMap, "includeEmbargoed")!=null);
			 		if(feeder.equals("merritt")){
			 			String account = getParameter(paramsMap, "account");
			 			String password = getParameter(paramsMap, "password");
			 			//String accessGroupId = getParameter(paramsMap, "accessGroup");
			 			handler = new CdlIngestHandler(damsClient, collectionId, userId, operationType, feeder, account, password);
			 		}else
			 			handler = new CdlIngestHandler(damsClient, collectionId, userId, operationType);
			 		if(!includeEmbargoed)
			 			handler.excludeEmbargoedObjects();
			 }else if (i == 9){	
				    session.setAttribute("status", opMessage + "Metadata Converting and populating ...");
				    String tsOperation = getParameter(paramsMap, "sipOption");
				    
				    if(tsOperation == null || tsOperation.length() == 0)
				    	tsOperation = "tsNew";
				    
				    int operationType = MetadataImportController.getOperationId(tsOperation);
				    String srcFile = (String) session.getAttribute("source");
				    String srcFormat = (String) session.getAttribute("format");
				    String pathMap = (String) session.getAttribute("pathMap");
				    int sheetNo = 0;
				    if(session.getAttribute("sheetNo") != null)
				    	sheetNo = ((Integer)session.getAttribute("sheetNo")).intValue();
				    
			    	String rdfFileToWrite = Constants.TMP_FILE_DIR + "tmpRdf_" + session.getId() + ".xml";
			    	if("excel".equalsIgnoreCase(srcFormat)){
				    	handler = new ExcelConverter(damsClient, collectionId, srcFile, sheetNo, pathMap, operationType);
						ExcelConverter converter = (ExcelConverter)handler;
						converter.setUseArk(true);
						converter.setRdfFileToWrite(rdfFileToWrite);
				    }else
				    	throw new ServletException("Unsupported data format: " + srcFormat);
	    
			 }*/else if (i == 10){	
				    session.setAttribute("status", opMessage + "Stage Ingesting ...");
				    
					String unit = getParameter(paramsMap, "unit");
				    String arkSetting = getParameter(paramsMap, "arkSetting").trim();
				 	String filePath = getParameter(paramsMap, "filePath").trim();
				 	String fileFilter = getParameter(paramsMap, "fileFilter").trim();
				 	String preferedOrder = getParameter(paramsMap, "preferedOrder");
				 	String fileSuffixes = getParameter(paramsMap, "fileSuffixes");
				 	String fileUse = getParameter(paramsMap, "fileUse");
			 		if(fileSuffixes != null && fileSuffixes.length() > 0)
			 			fileSuffixes = fileSuffixes.trim();
			 		
				 	String coDelimiter = "p";
				 	if(arkSetting.equals("1")){
				 		if(preferedOrder == null || preferedOrder.equalsIgnoreCase("cofDelimiter")){
				 			coDelimiter = getParameter(paramsMap, "cofDelimiter").trim();
				 		}else if (preferedOrder.equals("suffix"))
				 			coDelimiter = getParameter(paramsMap, "coDelimiter").trim();
				 		else
				 			coDelimiter = null;
				 	}else{
				 		if(arkSetting.equals("5")){
					 		coDelimiter = getParameter(paramsMap, "coDelimiter").trim();
				 		}
				 	}
				 		
				 	String[] fileOrderSuffixes = null;
				 	if(fileSuffixes != null && fileSuffixes.length() > 0)
				 		fileOrderSuffixes = fileSuffixes.split(",");
				 	
				 	String[] fileUses = null;
				 	if(fileUse != null && (fileUse=fileUse.trim()).length() > 0){
				 		fileUses = fileUse.split(",");
				 		for(int j=0; j<fileUses.length; j++){
				 			if(fileUses[j] != null)
				 				fileUses[j] = fileUses[j].trim();
				 		}
				 	}

				 	session.setAttribute("category", collectionId);
				 	session.setAttribute("unit", unit);
				 	session.setAttribute("arkSetting", arkSetting);
				 	session.setAttribute("filePath", filePath);
				 	session.setAttribute("fileFilter", fileFilter);
				 	session.setAttribute("preferedOrder", preferedOrder);
				 	session.setAttribute("fileSuffixes", fileSuffixes);
				 	session.setAttribute("fileUse", fileUse);
				 	
				 	String[] dirArr = filePath.split(";");
				 	List<String> fileList = new ArrayList<String>();
				 	String dir = null;
				 	for (int j=0; j<dirArr.length; j++){
				 		dir = dirArr[j];
				 		if(dir != null && (dir=dir.trim()).length()>0){
						 	if((dir.startsWith("/") || dir.startsWith("\\")) && (Constants.DAMS_STAGING.endsWith("/") 
						 			|| Constants.DAMS_STAGING.endsWith("\\")))
						 		dir = dir.substring(1);
				 			fileList.add(Constants.DAMS_STAGING + dir);
				 		}
				 	}

		            handler = new FileIngestionHandler(damsClient, fileList, Integer.parseInt(arkSetting), collectionId, fileFilter, coDelimiter);
		            ((FileIngestionHandler)handler).setFileOrderSuffixes(fileOrderSuffixes);
		            ((FileIngestionHandler)handler).setPreferedOrder(preferedOrder);
		            ((FileIngestionHandler)handler).setUnit(unit);
		            ((FileIngestionHandler)handler).setFileUses(fileUses);
		    	    
			 } else if (i == 11) {
				 session.setAttribute("status", opMessage + "Serialize records as RDF/XML to filestore ...");
				 if(collectionId.indexOf(",") > 0){
					 String collIDs = collectionId;
					 String[] collArr = collectionId.split(",");
					 List<String> items = new ArrayList<String>();
					 String collNames = "";
					 for(int j=0; j<collArr.length; j++){
						 if(collArr[j] != null && (collArr[j]=collArr[j].trim()).length()>0){
							 collectionId = collArr[j];
							 if(collectionId.equalsIgnoreCase("all")){
								 items.addAll(damsClient.listAllRecords());
								 collNames += "All Records (" + items.size() + "), ";
							 }else{
								 try{
									 handler = new SOLRIndexHandler( damsClient, collectionId );
									 items.addAll(handler.getItems());
									 collNames += handler.getCollectionTitle() + "(" + handler.getFilesCount() + "), ";
									 if(j>0 && j%5==0)
										 collNames += "\n";
								 }finally{
									 if(handler != null){
										 handler.release();
										 handler = null;
									 } 
								 }
							 }
						 }
					 }
					 handler = new FilestoreSerializationHandler( damsClient, null );
					 handler.setItems(items);
					 handler.setCollectionTitle(collNames.substring(0, collNames.lastIndexOf(",")));
					 handler.setCollectionId(collIDs);
				 }else{
					 if(collectionId.equalsIgnoreCase("all")){
						 handler = new FilestoreSerializationHandler(damsClient, null);
						 handler.setItems(damsClient.listAllRecords());
					 }else
						 handler = new FilestoreSerializationHandler(damsClient, collectionId);
				 }
			 } else if (i == 15) {	
				 session.setAttribute("status", opMessage + "Uploading files from dams-staging to filestore ...");
				 Map<String, String> filesMap = new TreeMap<String, String>();
				 for(Iterator<String> it = paramsMap.keySet().iterator(); it.hasNext();){
					 String key = it.next();
					 if (key.startsWith("f-")) {
						 String file = paramsMap.get(key)[0];
						 String fileURI = paramsMap.get(key.replaceFirst("f-", "fid-"))[0];
						 
						 if(fileURI != null && fileURI.startsWith(Constants.DAMS_ARK_URL_BASE))
							 filesMap.put(file, fileURI.trim());
						 else
							 message += "Invalid fileURL for file " + file + " (" + fileURI + "). \n";
					 }
				 }
				 handler = new FileUploadHandler(damsClient, filesMap);
				 handler.setItems(Arrays.asList(filesMap.keySet().toArray(new String[filesMap.size()])));
			 } else if (i == 18){
				 boolean components = getParameter(paramsMap, "exComponents") == null;
				 String exFormat = getParameter(paramsMap, "exportFormat");
				 String xslSource = getParameter(paramsMap, "xsl");
				 if(xslSource == null || (xslSource=xslSource.trim()).length() == 0){
					 xslSource = "/pub/data1/import/apps/glossary/xsl/dams/convertToCSV.xsl";
					 if(!new File(xslSource).exists())
						 xslSource = Constants.CLUSTER_HOST_NAME + "glossary/xsl/dams/convertToCSV.xsl";
				 }
				 session.setAttribute("status", opMessage + (exFormat.equalsIgnoreCase("csv")?"CSV":exFormat.equalsIgnoreCase("N-TRIPLE")?"N-TRIPLE":"RDF XML ") + " Metadata Export ...");
				 File outputFile = new File(Constants.TMP_FILE_DIR, "export-" + DAMSClient.stripID(collectionId) + "-" +System.currentTimeMillis() + "-rdf.xml");
			     String nsInput = getParameter(paramsMap, "nsInput");
			     List<String> nsInputs = new ArrayList<String>();
			     boolean componentsIncluded = true;
			     if(nsInput != null && (nsInput=nsInput.trim()).length() > 0){
			    	 String[] nsInputArr = nsInput.split(",");
			    	 for(int j=0; j<nsInputArr.length; j++){
			    		 if(nsInputArr[j]!= null && (nsInputArr[j]=nsInputArr[j].trim()).length()>0)
			    			 nsInputs.add(nsInputArr[j]);
			    	 }
			     }
			     fileOut = new FileOutputStream(outputFile);
				 handler = new MetadataExportHandler(damsClient, collectionId, nsInputs, componentsIncluded, exFormat, fileOut);
				 ((MetadataExportHandler)handler).setFileUri(logLink + "&file=" + outputFile.getName());
				 ((MetadataExportHandler)handler).setComponents(components);
			    
			 }else if (i == 19){
				 session.setAttribute("status", opMessage + "Jhove report ...");
				 boolean bytestreamFilesOnly = getParameter(paramsMap, "bsJhoveReport") != null;
				 boolean update = getParameter(paramsMap, "bsJhoveUpdate") != null;
				 handler = new JhoveReportHandler(damsClient, collectionId, bytestreamFilesOnly);
				 if(update)
					 ((JhoveReportHandler)handler).setJhoveUpdate(getParameter(paramsMap, "jhoveUpdate"));

			 }else if (i == 20) {
				 session.setAttribute("status", opMessage + "File report ...");
				 handler = new FileReportHandler(damsClient, collectionId);

			 }else if (i == 21) {
				session.setAttribute("status", opMessage + "Subject Import ...");

				String[] dataPaths = getParameter(paramsMap, "dataPath").split(";");
				String importOption = getParameter(paramsMap, "importOption");
				String preingestOption = getParameter(paramsMap, "preingestOption");
				boolean preprocessing = importOption == null;
				boolean testMatching = preprocessing && preingestOption.startsWith("test-matching");

				Element rdfPreview = null;
				if (preprocessing) {
					Document doc = new DocumentFactory().createDocument();
					rdfPreview = TabularRecordBasic.createRdfRoot (doc);
				}

				boolean preSuccessful = true;
				StringBuilder proMessage = new StringBuilder();
				//çallowedFileds = new ArrayList<String>();

				// Initiate the logging handler 
				handler = new MetadataImportHandler(damsClient, null);
				handler.setSubmissionId(submissionId);
	 			handler.setSession(session);
	 			handler.setUserId(userId);
	 			
	 			Map<String, String> collections = new HashMap<String, String>();
	 			if (StringUtils.isNotBlank(collectionId)) {
	 				String collType = damsClient.getCollectionType(collectionId);
	 				 collections.put(collectionId, collType);
	 			}

				List<File> dataFiles = new ArrayList<File>();
				for(int j=0; j<dataPaths.length; j++){
					String dataPath = dataPaths[j];
					if(dataPath != null && (dataPath=dataPath.trim()).length() > 0){
						File file = new File(Constants.DAMS_STAGING + "/" + dataPath);
						CollectionHandler.listFiles(dataFiles, file);
					}
				}

				String[] fileFilters = {"xls", "xlsx"};
				List<File> sources = FileUtils.filterFiles(dataFiles, fileFilters);
				dataFiles.clear();

	 			for  (int j=0; j<sources.size(); j++) {
					String sourceID = null;
					
					Object srcRecord =  sources.get(j);
					sourceID = (srcRecord instanceof File ? ((File)srcRecord).getName() : srcRecord.toString());
					if (preprocessing)
						handler.setStatus("Pre-processing record " + sourceID + " ... ");
					else
						handler.setStatus("Processing record " + sourceID + " ... ");
					
					RecordSource recordSource = null;
					// Handling Excel Input Stream records
					recordSource = new SubjectExcelSource((File)srcRecord, Arrays.asList(SubjectTabularRecord.ALL_FIELDS_FOR_SUBJECTS));

					// Report for Excel column name validation
					List<String> invalidColumns = ((SubjectExcelSource)recordSource).getInvalidColumns();
					if (invalidColumns != null && invalidColumns.size() > 0) {
						successful = false;
						preSuccessful = false;
						proMessage.append("Excel source " + sourceID + " - failed - " + CollectionHandler.damsDateFormat.format(new Date()) + ": \n");
						if (invalidColumns != null && invalidColumns.size() > 0) {
							proMessage.append( "****Invalid column name" + (invalidColumns.size() > 1 ? "s" : "") + ": " );
							for (int k=0; k<invalidColumns.size(); k++) {
								proMessage.append(invalidColumns.get(k));
								if (k == invalidColumns.size() - 1)
									proMessage.append("\n");
								else
									proMessage.append("; ");
							}
						}
					}


					String id = "";
					String info = "";
					if (recordSource != null && preSuccessful) {
						for (Record rec = null; (rec = recordSource.nextRecord()) != null;) {

							Document rdf = rec.toRDFXML();
							String subjectType = RDFDAMS4ImportTsHandler.getModelLabel(rdf.selectSingleNode("/rdf:RDF/*"));
							if (ExcelSource.getControlValues().get(SubjectTabularRecord.SUBJECT_TYPE).contains("Subject:" + subjectType))
								subjectType = "Subject:" + subjectType;

							Node subjectLabelNode = rdf.selectSingleNode("/rdf:RDF/*/mads:authoritativeLabel");
							id = rdf.selectSingleNode("/rdf:RDF/*/@rdf:about").getStringValue();  
							String subjectLabel = subjectLabelNode==null ? "[Label]" : subjectLabelNode.getText();

							List<Map<String, String>> invalidControlValues = ((SubjectExcelSource)recordSource).getInvalidValues();
							String errorMessage = getInvalidControlValues(invalidControlValues);
							if (StringUtils.isNotBlank(errorMessage)) {
								preSuccessful = false;
								invalidControlValues.clear();
								info = subjectType + " - " + subjectLabel + " - " + " failed - " + CollectionHandler.damsDateFormat.format(new Date());
								info += "\n****Invalid Control Value: " + errorMessage;
							} else
								info = subjectType + " - " + subjectLabel + " - " + " successful - " + CollectionHandler.damsDateFormat.format(new Date());

							proMessage.append("\n\n" + info);
							log.info(info);
			
							if (preprocessing && !testMatching) {
								 // Pre-processing with rdf preview
								rdfPreview.add(rdf.selectSingleNode("/rdf:RDF/*").detach()); 
							} else {
								// Write the converted rdf/xml to file system
								File tmpDir = new File (Constants.TMP_FILE_DIR + File.separatorChar + "converted");
								if(!tmpDir.exists())
									tmpDir.mkdir();
								File convertedFile = new File(tmpDir.getAbsolutePath(), id.replaceAll("[\\//:.*]+","") + ".rdf.xml");
								try{
									writeXml(convertedFile, rdf.asXML());
								} finally {										
									convertedFile.deleteOnExit();
									dataFiles.add(convertedFile);
								}
							}

							handler.setProgressPercentage(j * 100/sources.size()); 
						}
					}
				}

				if (preSuccessful) {
					// handle pre-ingest test subject matching
					if (testMatching) {
						String matchedResult = "";
						List<String> subjectTypes = ExcelSource.getControlValues().get(SubjectTabularRecord.SUBJECT_TYPE);
						if (subjectTypes == null || subjectTypes.size() == 0) {
							matchedResult = "Error: Controll values for subject type are not intitiated propertly.";
						}

						SubjectMatching subjectMatching = new SubjectMatching(damsClient, dataFiles, subjectTypes);
						matchedResult = subjectMatching.getMatchSubjects();

						matchedSubjectsFile = new File(Constants.TMP_FILE_DIR, "matched_subjects-" + submissionId + ".csv");
						writeXml(matchedSubjectsFile, matchedResult);
						dataLink = "\nThe result of matching subjects is ready for <a href=\"" + logLink;
						dataLink += "&file=" + matchedSubjectsFile.getName() + "\">download</a>.\n";
					} else if (preprocessing) {
						// pre-processing only, no ingest, write the converted RDF/xml for preview
						File destFile = new File(Constants.TMP_FILE_DIR, "preview-" + submissionId + "-rdf.xml");
						writeXml(destFile, rdfPreview.getDocument().asXML());
						dataLink = "\nThe converted RDF/XML is ready for <a href=\"" + logLink;
						dataLink += "&file=" + destFile.getName() + "\">download</a>.\n";
					} else {
						// handler clean up for pre-processing
						handler.release();
						handler = null;

						// ingest objects with the converted RDF/XML
						importOption = "metadataAndFiles";
						handler = new RDFDAMS4ImportTsHandler(damsClient, dataFiles.toArray(new File[dataFiles.size()]), importOption);
					}
				}
				// logging and handler clean up
				if (preprocessing || !preSuccessful) {
					// Logging the result for pre-processing
					message = "\nPre-processing " + (preSuccessful?"successful":"failed") + ": \n"
							+ (proMessage.length() == 0 ? "" : "\n " + proMessage.toString());
					handler.logMessage(message);

					logFile = handler.getLogFile();
					handler.release();
					handler = null;
					Thread.sleep(100);
				}
			 } else if (i == 22) {
				session.setAttribute("status", opMessage + "Batch Export ...");
				List<String> items = new ArrayList<>();
				String exportFormat = getParameter(paramsMap, "exportFormat");
				String txtInput = getParameter(paramsMap, "textInput");
				String fileInputValue = getParameter(paramsMap, "data");
				boolean components = getParameter(paramsMap, "excludeComponents") == null;
				if (txtInput != null && (txtInput = txtInput.trim()).length() > 0) {
					appendArks(items, txtInput);
				}

				// Handle records submitted in file with csv format, in lines or mixed together
				if (fileInputValue != null && (fileInputValue = fileInputValue.trim()).length() > 0) {
					appendArksFromFileInput(items, fileInputValue);
				}

				File rdfFile = BatchExportHandler.getRdfFile("" + submissionId);
				fileOut = new FileOutputStream(rdfFile);
				handler = new BatchExportHandler(damsClient, collectionId, exportFormat, components, fileOut);
				((BatchExportHandler)handler).addItems(items);

				File destFile = rdfFile;
				if (exportFormat.equalsIgnoreCase("csv")) {
					destFile = BatchExportHandler.getCsvFile("" + submissionId);
				} else if (exportFormat.equalsIgnoreCase("excel")) {
					destFile = BatchExportHandler.getExcelFile("" + submissionId);
				}

				dataLink = "\nThe exported content is ready for <a href=\"" + logLink;
				dataLink += "&file=" + destFile.getName() + "\">download</a>.\n";
			} else
				throw new ServletException("Unhandle operation index: " + i);

			if (handler != null) {
	 			try {
	 				damsClient.setClientInfo(clientTool + (StringUtils.isNotBlank(clientVersion) ? " " + clientVersion : ""));
	 				handler.setSubmissionId(submissionId);
	 				handler.setDamsClient(damsClient);
	 				handler.setSession(session);
	 				handler.setUserId(userId);
	 				if(handler.getCollectionId() == null && (collectionId != null && collectionId.length()>0))
	 					handler.setCollectionId(collectionId);

	 				if (logFile == null)
	 					logFile = handler.getLogFile();
	 				if (arkReportFile == null)
	 					arkReportFile = handler.getArkReportFile();

	 				// Collection Import need to keep the origin parent record for updating collection links
	 				DAMSCollection oCollection = null;
	 				DAMSCollection collection = null;
	 				boolean collectionImport = getParameter(paramsMap, "collectionImport") != null;
	 				if (collectionImport && StringUtils.isNotBlank(collectionId) && damsClient.exists(collectionId, "", "")) {
	 					oCollection = DAMSCollection.getRecord(damsClient, collectionId);
	 				}

	 				successful = handler.execute();

	 				// update parent linking for Collection Import
	 				if (collectionImport && successful && StringUtils.isNotBlank(collectionId)) {
	 					collection = DAMSCollection.getRecord(damsClient, collectionId);
	 					InputStreamRecord.updateCollectionHierarchy (damsClient, oCollection, collection);
	 				}
	 			}catch (InterruptedException e) {
	 				successful = false;
	 				exeInfo += e.getMessage();
	 				e.printStackTrace();
				} catch (Exception e) {
					successful = false;
	 				exeInfo += "\n" + e.getMessage();
	 				e.printStackTrace();
				}finally{
					String collectionName = handler.getCollectionId();
					if(collectionName != null && collectionName.length() >0 && logLink.indexOf("&category=")<0)
						logLink += "&category=" + collectionName.replace(" ", "");
					handler.setExeResult(successful);
					exeInfo += handler.getExeInfo();
					handler.release();
					if(fileOut != null){
						CollectionHandler.close(fileOut);
						fileOut = null;
					}
				} 
		   	}
		 }else
			 continue;
      
	  message += exeInfo;
	  if(! successful){
		  String errors = "Execution failed:\n" + message + "\n";
		  returnMessage += errors;
		  break;
	  }else{
		  returnMessage += "\n" + message;
	  }
	 }
	}catch (Exception e) {
		e.printStackTrace();
		returnMessage += e.getMessage();
	}finally{
		if(damsClient != null)
			damsClient.close();
		if(fileOut != null){
			CollectionHandler.close(fileOut);
			fileOut = null;
		}
	}
	}else
		returnMessage = message;
	
	if(returnMessage.length() > 1000){
		returnMessage = returnMessage.substring(0, 1000);
		int idx = returnMessage.lastIndexOf("\n");
		if(idx > 0)
			returnMessage = returnMessage.substring(0, idx);
		else{
			idx = returnMessage.lastIndexOf("</a>");
			if(idx < returnMessage.lastIndexOf("<a "))
				returnMessage = returnMessage.substring(0, idx);
		}
		returnMessage = "\n" + returnMessage + "\n    ...     ";
	}
	returnMessage +=  "\n" + dataLink;
	RequestOrganizer.addResultMessage(session, returnMessage.replace("\n", "<br />") + "<br />");

	//send email
	try {
		String sender = Constants.MAILSENDER_DAMSSUPPORT;
		if(emails == null && user != null){
			emails = new String[1];
			emails[0] = user + "@ucsd.edu";
		}

		String arkReportAttachment = arkReportFile != null && arkReportFile.exists() ? arkReportFile.getAbsolutePath() : null;
		String logFileAttachment = logFile != null && logFile.exists() ? logFile.getAbsolutePath() : null;
		String matchedSubjectsAttachment = matchedSubjectsFile == null ? null : matchedSubjectsFile.getAbsolutePath();
		String[] attachments = {arkReportAttachment, logFileAttachment, matchedSubjectsAttachment};
		if(emails == null)
			DAMSClient.sendMail(sender, new String[] {sender}, "DAMS Manager Invocation Result - " + Constants.CLUSTER_HOST_NAME.replace("http://", "").replace(".ucsd.edu/", ""), returnMessage, "text/html", "smtp.ucsd.edu", attachments);
		else
			DAMSClient.sendMail(sender, emails, "DAMS Manager Invocation Result - " + Constants.CLUSTER_HOST_NAME.replace("http://", "").replace(".ucsd.edu/", ""), returnMessage, "text/html", "smtp.ucsd.edu", attachments);
	} catch (AddressException e) {
		e.printStackTrace();
	} catch (Exception e) {
		e.printStackTrace();
	}

	return returnMessage;
	}
	
	public static String getParameter(Map<String, String[]> paramsMap, String paramName){
		String paramValue = null;
		String[] paramArr = paramsMap.get(paramName);
		if(paramArr != null && paramArr.length > 0){
			paramValue = paramArr[0];
		}
		return paramValue;
	}
	
	/**
	 * Serialize xml document to file
	 * @param destFile
	 * @param doc
	 * @throws IOException
	 */
	public static void writeXml (File destFile, Document doc) throws IOException {
		  OutputStreamWriter out = null;                                                                                                   
		  XMLWriter writer = null;
		  OutputFormat pretty = OutputFormat.createPrettyPrint();
		  try{			  
			  out = new FileWriter(destFile);
			  writer = new XMLWriter(out, pretty);
			  writer.write(doc);
		  } finally {
			  CollectionHandler.close(out);
			  if(writer != null){
				  try{
					  writer.close();
				  }catch (Exception e) {
					  e.printStackTrace();
				   }
				  writer = null;
			  }
		  }
	}
	
	/**
	 * Serialize xml string to file
	 * @param destFile
	 * @param xml
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	public static void writeXml (File destFile, String xml)
			throws UnsupportedEncodingException, IOException {
		write2File(destFile, xml);
	}

	/**
	 * write content to file
	 * @param destFile
	 * @param content
	 * @throws UnsupportedEncodingException
	 * @throws IOException
	 */
	public static void write2File (File destFile, String content)
			throws UnsupportedEncodingException, IOException {
		OutputStream out = null;

		try {
			out = new FileOutputStream(destFile);
			out.write(content.getBytes("UTF-8"));
		} finally {
			CollectionHandler.close(out);
			out = null;
		}
	}
	
	private static List<File> getRogerFiles (String bib, List<String> paths) {
		List<File> fileList = new ArrayList<File>();
		// List the source files
		for(String filePath : paths){
			File file = new File (filePath);
			if(file.exists()){
				getRogerFile(fileList, bib, file);
			}
		}
		Collections.sort(fileList);
		return fileList;
	}
	
	private static void getRogerFile(List<File> files, String bib, File file) {
		if(file.isDirectory()){
			File[] filesArr = file.listFiles();
			for(int i=0; i<filesArr.length; i++){
				getRogerFile(files, bib, filesArr[i]);
			}
		}else{
			if (file.getName().startsWith(bib + ".")){
				files.add(file);
			}
		}
	}
	
	private static Map<String, String> getFileUse(List<File> files) {
		Map<String, String> fileUseMap = new HashMap<String, String>();
		for (File file : files) {
			String fileName = file.getName();
			if (fileName.endsWith(".tif")) {
				  fileUseMap.put(fileName, "image-source");
			} else if (fileName.endsWith(".pdf")) {
				  fileUseMap.put(fileName, "document-service");
			} else if (fileName.endsWith(".zip")) {
				  fileUseMap.put(fileName, "document-source");
			}
		}
		return fileUseMap;
	}
	
	private static String getTitle(Document doc) {
		String title = "";
		Node node = doc.selectSingleNode("//*[contains(name(), 'dams:Object') or contains(name(), 'Collection')]/dams:title/mads:Title/mads:authoritativeLabel");
		if (node != null) {
			title = node.getText();
		}
		return title;
	}

	private static String checkRecord (String collTitle, DAMSClient damsClient) throws Exception {
		Map<String, String> collMap = damsClient.listCollections();
		for (String key : collMap.keySet()){
			String title = key.substring(0, key.lastIndexOf(" [")).trim();
			if (title.equalsIgnoreCase(collTitle)) {
				return collMap.get(key);
			}
		}
		return null;
	}

	private static String getInvalidControlValues(List<Map<String, String>> invalidValues) {
		List<String> validColumns = Arrays.asList(SubjectTabularRecord.ALL_FIELDS_FOR_SUBJECTS);
		// process to retrieve control values errors for the record since it will parse the row for the next record
		StringBuilder cvErrors = new StringBuilder();
		for (int k = 0; k < invalidValues.size(); k++) {
			Map<String, String> m = invalidValues.get(k);
			int len = 0;
			for (String key : m.keySet()) {
				if (validColumns.contains(key)) {
					if (len++ > 0)
						cvErrors.append(" | ");
					cvErrors.append(key + " => " + m.get(key));
				}
			}
		}
		return cvErrors.toString();
	}

	/*
	 * Create report message for validation errors
	 * @param id String the id of the record
	 * @param messageTitle String the title of the record
	 * @param errors List of errors 
	 * @param errorBuilder StringBuilder the error report builder
	 */
	private static void appendErrorMessage(String id, String messageTitle, List<Map<String, String>> errors, StringBuilder errorBuilder) {
		if (errors != null && errors.size() > 0) {
			StringBuilder cvErrors = new StringBuilder();
			for (int k=0; k< errors.size(); k++) {
				Map<String, String> m = errors.get(k);
				if (m.containsKey(TabularRecord.OBJECT_ID) && m.get(TabularRecord.OBJECT_ID).equals(String.valueOf(id))) {
					cvErrors.append( "* Row index " + m.get("row") + " [");

					// don't count for the row number and the record id
					m.remove("row");
					m.remove(TabularRecord.OBJECT_ID);
					int l = 0;
					for (String key : m.keySet()) {
						if (l++ > 0)
							cvErrors.append(" | ");
						cvErrors.append(key + " => " + m.get(key));     
					}
					cvErrors.append("]\n");
				}
			}

			if (cvErrors.length() > 0) {
				errorBuilder.append( messageTitle + " - \n" + cvErrors.toString() );
			}
		}
	}

	/*
	 * Append arks in delimited values
	 * @param items
	 * @param inputValue
	 */
	private static void appendArks(List<String> items, String inputValue) {
		String[] tokens = inputValue.split("\\,");
		for (String token : tokens) {
			String[] records = token.split(" ");
			for (String record : records) {
				record = record.trim();
				if (record.length() > 0) {
					items.add(record);
				}
			}
		}
	}

	/*
	 * Append arks from file input
	 * @param items
	 * @param fileInputValue
	 */
	private static void appendArksFromFileInput(List<String> items, String fileInputValue) {
		// Handle record with line input
		String[] lines = fileInputValue.split("\n");
		for (String line : lines) {
			// Handle CSV encoding records and records delimited by comma, whitespace etc.
			if (line != null && (line = line.trim().replace("\"", "")).length() > 0) {
				appendArks(items, line);
			}
		}
	}
}
