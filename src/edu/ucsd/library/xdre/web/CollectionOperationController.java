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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
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
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.collection.ChecksumsHandler;
import edu.ucsd.library.xdre.collection.CollectionHandler;
import edu.ucsd.library.xdre.collection.DerivativeHandler;
import edu.ucsd.library.xdre.collection.FileCountValidaionHandler;
import edu.ucsd.library.xdre.collection.FileIngestionHandler;
import edu.ucsd.library.xdre.collection.FilestoreSerializationHandler;
import edu.ucsd.library.xdre.collection.JhoveReportHandler;
import edu.ucsd.library.xdre.collection.MetadataExportHandler;
import edu.ucsd.library.xdre.collection.MetadataImportHandler;
import edu.ucsd.library.xdre.collection.SOLRIndexHandler;
import edu.ucsd.library.xdre.imports.RDFDAMS4ImportTsHandler;
import edu.ucsd.library.xdre.tab.ExcelSource;
import edu.ucsd.library.xdre.tab.ModsRecord;
import edu.ucsd.library.xdre.tab.Record;
import edu.ucsd.library.xdre.tab.RecordSource;
import edu.ucsd.library.xdre.tab.TabularRecord;
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
		String fileStore = getParameter(paramsMap, "fs");
		if(activeButton == null || activeButton.length() == 0)
			activeButton = "validateButton";
		HttpSession session = request.getSession();
		session.setAttribute("category", collectionId);
		
		String ds = getParameter(paramsMap, "ts");
		if(ds == null || ds.length() == 0)
			ds = Constants.DEFAULT_TRIPLESTORE;
		
		if(fileStore == null || (fileStore=fileStore.trim()).length() == 0)
			fileStore = null;
			
		String forwardTo = "/controlPanel.do?ts=" + ds + (fileStore!=null?"&fs=" + fileStore:"");
		if(dataConvert)
			forwardTo = "/pathMapping.do?ts=" + ds + (fileStore!=null?"&fs=" + fileStore:"");
		else if(isIngest){
			String unit = getParameter(paramsMap, "unit");
			forwardTo = "/ingest.do?ts=" + ds + (fileStore!=null?"&fs=" + fileStore:"") + (unit!=null?"&unit=" + unit:"");
		}else if(isDevUpload)
			forwardTo = "/devUpload.do?" + (fileStore!=null?"&fs=" + fileStore:"");
		else if(isSolrDump)
			forwardTo = "/solrDump.do" + (StringUtils.isBlank(collectionId) ? "" : "#colsTab");
		else if(isSerialization)
			forwardTo = "/serialize.do?" + (fileStore!=null?"&fs=" + fileStore:"");
		else if(isMarcModsImport)
			forwardTo = "/marcModsImport.do?";

		String[] emails = null;
		String user = request.getRemoteUser();
		if(( !(getParameter(paramsMap, "solrRecordsDump") != null || isBSJhoveReport || isDevUpload)
				&& getParameter(paramsMap, "rdfImport") == null && getParameter(paramsMap, "externalImport") == null 
				&& getParameter(paramsMap, "dataConvert") == null ) && getParameter(paramsMap, "marcModsImport") == null && 
				(collectionId == null || (collectionId=collectionId.trim()).length() == 0)){
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
				if(isSolrDump)
					session.setAttribute("message", message);
				else {
					forwardTo += "&activeButton=" + activeButton;
					forwardTo += "&message=" + message;
				}

				forwordPage(request, response, response.encodeURL(forwardTo));
				return null;
			}
			
			session.setAttribute("status", "Processing request ...");
			DAMSClient damsClient = null;
			try {
				//user = getUserName(request);
				//email = getUserEmail(request);
				damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
				JSONArray mailArr = (JSONArray)damsClient.getUserInfo(user).get("mail");
				if(mailArr != null && mailArr.size() > 0){
					emails = new String[mailArr.size()];
					mailArr.toArray(emails);
				}
				message = handleProcesses(paramsMap, request.getSession());
			} catch (Exception e) {
				e.printStackTrace();
				//throw new ServletException(e.getMessage());
				message += "<br />Internal Error: " + e.getMessage();
			}finally{
				if(damsClient != null)
					damsClient.close();
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
		//send email
		try {
			String sender = Constants.MAILSENDER_DAMSSUPPORT;
			if(emails == null && user != null){
				emails = new String[1];
				emails[0] = user + "@ucsd.edu";
			}
			if(emails == null)
				DAMSClient.sendMail(sender, new String[] {"lsitu@ucsd.edu"}, "DAMS Manager Invocation Result - " + Constants.CLUSTER_HOST_NAME.replace("http://", "").replace(".ucsd.edu/", ""), message, "text/html", "smtp.ucsd.edu");
			else
				DAMSClient.sendMail(sender, emails, "DAMS Manager Invocation Result - " + Constants.CLUSTER_HOST_NAME.replace("http://", "").replace(".ucsd.edu/", ""), message, "text/html", "smtp.ucsd.edu");
		} catch (AddressException e) {
			e.printStackTrace();
		} catch (MessagingException e) {
			e.printStackTrace();
		}
		
		if(isSolrDump || isMarcModsImport) {
			session.setAttribute("message", message.replace("\n", "<br />"));
			if(collectionId != null && isMarcModsImport)
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
		
		boolean[] operations = new boolean[20];
		operations[0] = getParameter(paramsMap, "validateFileCount") != null;
		operations[1] = getParameter(paramsMap, "validateChecksums") != null;
		operations[2] = getParameter(paramsMap, "rdfImport") != null;
		operations[3] = getParameter(paramsMap, "createDerivatives") != null;
		operations[4] = getParameter(paramsMap, "uploadRDF") != null;
		operations[5] = getParameter(paramsMap, "externalImport") != null;
		operations[6] = getParameter(paramsMap, "marcModsImport") != null;
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
		operations[15] = getParameter(paramsMap, "devUpload") != null;
		operations[16] = getParameter(paramsMap, "jsonDiffUpdate") != null;
		operations[17] = getParameter(paramsMap, "validateManifest") != null;
		operations[18] = getParameter(paramsMap, "metadataExport") != null;
		operations[19] = getParameter(paramsMap, "jhoveReport") != null;

		int submissionId = (int)System.currentTimeMillis();
		String logLink = "https://" + (Constants.CLUSTER_HOST_NAME.indexOf("localhost")>=0?":8443":Constants.CLUSTER_HOST_NAME.indexOf("lib-ingest")>=0?Constants.CLUSTER_HOST_NAME+".ucsd.edu:8443":Constants.CLUSTER_HOST_NAME+".ucsd.edu") + "/damsmanager/downloadLog.do?submissionId=" + submissionId;
		
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
		
		String fileStore = getParameter(paramsMap, "fs");
		damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
		damsClient.setTripleStore(ds);
		damsClient.setFileStore(fileStore);

		/*Date ckDate = null;
		if(operations[1]){
			String checksumDate = getParameter(paramsMap, "checksumDate");
			if(checksumDate == null || (checksumDate = checksumDate.trim()).length() == 0)
				message += "Please enter a date for checksum validation ...<br>";
			try{
				ckDate = (new SimpleDateFormat("MM-dd-yyyy")).parse(checksumDate);
			}catch (ParseException e){
				message += "Please enter a Date in valid format(mm/dd/yyyy): " + checksumDate + ".<br>";
			}
		}*/

		
		JSONArray filesJsonArr = null;
		if(operations[15]){
			String filesArr = getParameter(paramsMap, "files");
			if(filesArr == null || (filesArr = filesArr.trim()).length() == 0)
				message += "Missing perameter files.<br>";
			try{
				filesJsonArr = (JSONArray)JSONValue.parse(filesArr);
			}catch (Exception e){
				e.printStackTrace();
				message += "Invalid files parameter array: " + filesArr + " - " + e.getMessage() + "<br>";
			}
		}
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
				 session.setAttribute("status", opMessage + "File Count Validation for FileStore " + fileStore + " ...");
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
				   session.setAttribute("status", opMessage + "Checksum Validation for FileStore " + fileStore + " ...");
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
				 String[] sizes = null;
				 if(reqSize != null && reqSize.length() > 0)
					 sizes = reqSize.split(",");
				 handler = new DerivativeHandler(damsClient, collectionId, sizes, derReplace);

			 }/*else if (i == 4){	
				 session.setAttribute("status", opMessage + "RDF XML File Creation &amp; File Store Upload ...");
				 String rdfXmlDataType = getParameter(paramsMap, "rdfXmlDataType");
				 boolean rdfXmlReplace = getParameter(paramsMap, "rdfXmlReplace") != null;
	             
				 handler = new MetaDataStreamUploadHandler(damsClient, collectionId, "rdf", rdfXmlReplace);
			 }*/else if (i == 5){	
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
										  File convertedFile = new File(tmpDir.getAbsolutePath(), id + ".rdf.xml");
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
				  session.setAttribute("status", opMessage + "Import from MARC/MODS source ...");
				  String unit = getParameter(paramsMap, "unit");
				  String source = getParameter(paramsMap, "source");
				  String bibNumber = getParameter(paramsMap, "bibInput");
				  String modsXml = getParameter(paramsMap, "modsInput");
				  String copyrightStatus =getParameter(paramsMap, "copyrightStatus");
				  String copyrightJurisdiction = getParameter(paramsMap, "countryCode");
				  String copyrightOwner = getParameter(paramsMap, "copyrightOwner");
				  String program = getParameter(paramsMap, "program");
				  String access = getParameter(paramsMap, "accessOverride");
				  String endDate = getParameter(paramsMap, "licenseEndDate");
				  String[] dataPaths = getParameter(paramsMap, "dataPath").split(";");
				  String[] filesPaths = getParameter(paramsMap, "filesPath").split(";");
				  String importOption = getParameter(paramsMap, "importOption");
				  List<String> ingestFiles = new ArrayList<String>();
				  for(int j=0; j<filesPaths.length; j++){
					  if((filesPaths[j]=filesPaths[j].trim()).length() > 0)
						  ingestFiles.add(new File(Constants.DAMS_STAGING + "/" + filesPaths[j]).getAbsolutePath());
				  }
				  
				  List<File> dataFiles = new ArrayList<File>();
				  for(int j=0; j<dataPaths.length; j++){
					  String dataPath = dataPaths[j];
					  if(dataPath != null && (dataPath=dataPath.trim()).length() > 0){
						  File file = new File(Constants.DAMS_STAGING + "/" + dataPath);
						  CollectionHandler.listFiles(dataFiles, file);
					  }
				  }
				  
				  List<Object> sources = new ArrayList<Object>();
				  if (source.equalsIgnoreCase("bib")) {
					  String[] bibs = bibNumber.split(",");
					  for  (int j=0; j<bibs.length; j++) {
						  if(bibs[j] != null && (bibs[j]=bibs[j].trim()).length() > 0)
							  sources.add(bibs[j]);
					  }
				  } else {
					  sources.addAll(dataFiles);
					  dataFiles.clear();
				  }
 
				  // Handling pre-processing request
				  Element rdfPreview = null;
				  StringBuilder errorMessage = new StringBuilder();
				  StringBuilder duplicatRecords = new StringBuilder();
				  List<String> ids = new ArrayList<String>();
				  boolean preprocessing = importOption.equalsIgnoreCase("pre-processing");
				  boolean ingestWithFiles = importOption.equalsIgnoreCase("metadataAndFiles");
				  
				  if (preprocessing) {
					  Document doc = new DocumentFactory().createDocument();
					  rdfPreview = TabularRecord.createRdfRoot (doc);
				  }
				  
				  if (source.equalsIgnoreCase("bib") || source.equalsIgnoreCase("mods")) {
					  // Initiate handler for logging
					  handler = new MetadataImportHandler(damsClient, null);
					  handler.setSubmissionId(submissionId);
		 			  handler.setSession(session);
		 			  handler.setUserId(userId);
		 			  
					  for  (int j=0; j<sources.size(); j++) {
						  InputStream in = null;
						  String sourceID = null;
						  String[] collections = {collectionId};
						  
						  Object srcRecord =  sources.get(j);
						  sourceID = (srcRecord instanceof File ? ((File)srcRecord).getName() : srcRecord.toString());
						  if (preprocessing)
							  handler.logMessage("Pre-processing record " + sourceID + " ... ");
						  else
							  handler.logMessage("Processing record " + sourceID + " ... ");
						  
							  
						  if (source.equalsIgnoreCase("bib")) {
							  
							  String url = Constants.DAMS_STORAGE_URL.substring(0, Constants.DAMS_STORAGE_URL.indexOf("/dams/"))
									  + "/jollyroger/get?type=bib&mods=true&ns=true&value=" + sourceID;

							  handler.logMessage("Getting MarcXML for Roger record " + sourceID + " from URL: " + url);
							  HttpGet req = new HttpGet(url);
							  Document doc = damsClient.getXMLResult(req);
							  modsXml = doc.asXML();
							  in = new ByteArrayInputStream (modsXml.getBytes("UTF-8"));
						  } else {
							  // METS/MODS XML from staging area
							  File srcFile = (File)sources.get(j);
							  in = new FileInputStream(srcFile);
						  }
	
						  try {
							  File xsl = new File(session.getServletContext().getRealPath("files/mets2dams.xsl"));
							  ModsRecord record = new ModsRecord(xsl, in, sourceID.replaceAll("\\..*",""), collections, unit, 
										copyrightStatus, copyrightJurisdiction, copyrightOwner,
										program, access, endDate);
							  
							  // Add master file(s) for the bib/Roger record: a PDF or a TIFF, or a PDF + ZIP
							  List<File> filesToIngest = null;
							  if (source.equalsIgnoreCase("bib") && ingestWithFiles) {
								  filesToIngest = getRogerFiles ((String)srcRecord, ingestFiles);
								  // Processing the master file(s) with error report. 
								  if (filesToIngest.size() == 0) {
									  errorMessage.append("Roger record " + srcRecord
											  + " has no master file(s) for \"Ingest metadata and files\" option.\n");
								  } else if (filesToIngest.size() > 2 
										  || (filesToIngest.size() == 2 && !filesToIngest.get(1).getName().endsWith(".zip"))) {
									  errorMessage.append("Unexpected file(s) for Roger record " + srcRecord + ": ");
									  for (File file : filesToIngest) {
										  errorMessage.append((filesToIngest.indexOf(file) > 0 ? ", " : "") + file.getName());
									  }
									  errorMessage.append(".\n");
								  } else {
									  // Handle the use property for the file(s)
									  Map<String, String> fileUseMap = getFileUse(filesToIngest);
									  
									  record.addFiles(0, filesToIngest, fileUseMap);
								  }
							  }

							  String id = record.recordID();
							  
							  if(ids.indexOf(id) < 0) {
								  ids.add(id);
							  } else {
								  successful = false;
								  duplicatRecords.append(id + ", ");
								  handler.logError("Found duplicated record with ID " + id + ".");
							  }
							  
							  if (errorMessage.length() == 0 && duplicatRecords.length() == 0) {
								  if (preprocessing) {
									 // Pre-processing with rdf preview
									  rdfPreview.add(record.toRDFXML().selectSingleNode("//dams:Object").detach()); 
								  } else {
									  // Write the converted rdf/xml to file system
									  File tmpDir = new File (Constants.TMP_FILE_DIR + File.separatorChar + "converted");
									  if(!tmpDir.exists())
										  tmpDir.mkdir();
									  File convertedFile = new File(tmpDir.getAbsolutePath(), id + ".rdf.xml");
									  try{
										  writeXml(convertedFile, record.toRDFXML().asXML());
									  } finally {										  
										  convertedFile.deleteOnExit();
										  dataFiles.add(convertedFile);
									  }
								  }
							  }
						  } finally {
							  CollectionHandler.close(in);
						  }
					  }					  
					  handler.release();
					  handler = null;
					  
					  if (errorMessage.length() == 0 && duplicatRecords.length() == 0) {
						  if (preprocessing) {
							  File destFile = new File(Constants.TMP_FILE_DIR, "preview-" + submissionId + "-rdf.xml");
							  writeXml(destFile, rdfPreview.getDocument().asXML());
							  successful = true;
							  message = "\nThe converted RDF/XML is ready for <a href=\"" + logLink
									  + "&file=" + destFile.getName() + "\">download</a>.\n";
						  }else{
							  // Ingest the converted RDF/XML files
							  handler = new RDFDAMS4ImportTsHandler(damsClient, dataFiles.toArray(new File[dataFiles.size()]), importOption);
							  ((RDFDAMS4ImportTsHandler)handler).setFilesPaths(ingestFiles.toArray(new String[ingestFiles.size()]));
						  }
					  } else {
						  successful = false;
						  message = "\nPre-processing issues found:";
						  if (duplicatRecords.length() > 0)
							  message += "\nDuplicated records: " + duplicatRecords.substring(0, duplicatRecords.length() - 2).toString();
						  if (errorMessage.length() > 0)
							  message += "\nOther Errors: \n" + errorMessage.toString();
					  }
				  } else {
					  successful = false;
					  message += "\nUnknown source type: " + source;
				  }
			 } else if (i == 7) {
				 session.setAttribute("status", opMessage + "SOLR Index ...");
				 boolean update = getParameter(paramsMap, "indexReplace") != null;
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
			 }/* else if (i == 15){	
				 session.setAttribute("status", opMessage + "Moving files from dev to LocalStore ...");
				 //localStore = getLocalFileStore();
				 List<String> files = new ArrayList<String>();
				 Iterator it= filesJsonArr.iterator(); 
				 while(it.hasNext()){
					 files.add(URLDecoder.decode((String)it.next(), "UTF-8"));
				 }
				 handler = new DevUploadHandler(files);
			 } else if (i == 16){	
				 session.setAttribute("status", opMessage + "Single Item DIFF Updating ...");				    
				 dHandler = new JSONDiffUpdateHandler(subjectId, jsonToUpdate, tsUtils);
				 //((JSONDiffUpdateHandler)dHandler).setSession(session);
			 } else if (i == 17){
				 String manifestOption = getParameter(paramsMap, "manifestOptions");
				 boolean validateManifest = true;
				 boolean writeManifest = manifestOption != null && manifestOption.equals("write");
				 if(writeManifest){
					 validateManifest = false;
					 session.setAttribute("status", opMessage + "Manifest Writing ...");
				 }else
					 session.setAttribute("status", opMessage + "Manifest Valification ...");
			     handler = new LocalStoreManifestHandler(tsUtils, collectionId, validateManifest, writeManifest);
			 }*/ else if (i == 18){
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

			 }else 	
		          throw new ServletException("Unhandle operation index: " + i);
			 
		   	if(handler != null){
	 			try {
	 				handler.setSubmissionId(submissionId);
	 				handler.setDamsClient(damsClient);
	 				handler.setSession(session);
	 				handler.setUserId(userId);
	 				if(handler.getCollectionId() == null && (collectionId != null && collectionId.length()>0))
	 					handler.setCollectionId(collectionId);

	 				successful = handler.execute();
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
	
	String logMessage = "For details, please download " + "<a href=\"" + logLink + "\">log</a>" + ".";
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
	returnMessage +=  "\n" + logMessage;
	RequestOrganizer.addResultMessage(session, returnMessage.replace("\n", "<br />") + "<br />");
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
		OutputStream out = null;

		try {
			out = new FileOutputStream(destFile);
			out.write(xml.getBytes("UTF-8"));
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
				  fileUseMap.put(fileName, "image-master");
			} else if (fileName.endsWith(".pdf")) {
				  fileUseMap.put(fileName, "document-service");
			} else if (fileName.endsWith(".zip")) {
				  fileUseMap.put(fileName, "document-source");
			}
		}
		return fileUseMap;
	}
}
