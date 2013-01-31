package edu.ucsd.library.xdre.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.collection.CollectionHandler;
import edu.ucsd.library.xdre.collection.DerivativeHandler;
import edu.ucsd.library.xdre.collection.FileCountValidaionHandler;
import edu.ucsd.library.xdre.collection.FileIngestionHandler;
import edu.ucsd.library.xdre.collection.JhoveReportHandler;
import edu.ucsd.library.xdre.collection.SOLRIndexHandler;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.RequestOrganizer;


 /**
 * Class LocalStoreCollectionManagementController handles operations
 * with no user files submited to the server
 *
 * @author lsitu@ucsd.edu
 */
public class CollectionOperationController implements Controller {

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
			try {
				items = upload.parseRequest(request);

			    Iterator<FileItem> iter = items.iterator();
			    while (iter.hasNext()) {
				    FileItem item = (FileItem) iter.next();
		
				    out = new ByteArrayOutputStream();
				    if (item.isFormField()){
				    	paramsMap.put(item.getFieldName(), new String[]{item.getString()});
				    }else{
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
			}
		}else
			paramsMap = request.getParameterMap();
		
		String collectionId = getParameter(paramsMap, "category");
		String activeButton = getParameter(paramsMap, "activeButton");
		boolean dataConvert = getParameter(paramsMap, "dataConvert") != null;
		boolean isIngest = getParameter(paramsMap, "ingest") != null;
		boolean isDevUpload = getParameter(paramsMap, "devUpload") != null;
		boolean isBSJhoveReport = getParameter(paramsMap, "bsJhoveReport") != null;
		boolean isSolrDump = getParameter(paramsMap, "solrDump") != null;
		String fileStore = getParameter(paramsMap, "fs");
		if(activeButton == null || activeButton.length() == 0)
			activeButton = "validateButton";
		HttpSession session = request.getSession();
		session.setAttribute("category", collectionId);
		
		String ds = getParameter(paramsMap, "ts");
		if(ds == null || ds.length() == 0)
			throw new ServletException("No triplestore data source provided...");
		
		if(fileStore == null || (fileStore=fileStore.trim()).length() == 0)
			fileStore = null;
			
		String forwardTo = "/controlPanel.do?ts=" + ds + (fileStore!=null?"&fs=" + fileStore:"");
		if(dataConvert)
			forwardTo = "/pathMapping.do?ts=" + ds + (fileStore!=null?"&fs=" + fileStore:"");
		else if(isIngest){
			String repo = getParameter(paramsMap, "repo");
			forwardTo = "/ingest.do?ts=" + ds + (fileStore!=null?"&fs=" + fileStore:"") + (repo!=null?"&repo=" + repo:"");
		}else if(isDevUpload)
			forwardTo = "/devUpload.do?" + (fileStore!=null?"&fs=" + fileStore:"");
		else if(isSolrDump)
			forwardTo = "/solrDump.do?" + (fileStore!=null?"&fs=" + fileStore:"");
		forwardTo += "&activeButton=" + activeButton; 

		String[] emails = null;
		String user = request.getRemoteUser();
		if(( !(isBSJhoveReport || isDevUpload) && getParameter(paramsMap, "rdfImport") == null && getParameter(paramsMap, "dataConvert") == null )&& 
				(collectionId == null || (collectionId=collectionId.trim()).length() == 0)){
			message = "Please choose a collection ...";
		}else{
			String servletId = getParameter(paramsMap, "progressId");
			boolean vRequest = false; 
			try{
				vRequest = RequestOrganizer.setReferenceServlet(session, servletId, Thread.currentThread());
			}catch (Exception e){
					forwardTo += "&message=" + e.getMessage();
			}
			if(!vRequest){
				forwordPage(request, response, response.encodeURL(forwardTo));
				return null;
			}
			
			session.setAttribute("status", "Processing request ...");
			try {
				//user = getUserName(request);
				//email = getUserEmail(request);
				DAMSClient damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
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
		if(collectionId != null)
			forwardTo += "&category=" + collectionId;
		
		forwardTo += "&message=" + URLEncoder.encode(message, "UTF-8");
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
				String logLink = "<a href=\"" + Constants.CLUSTER_HOST_NAME.replace("http://", "https://").replace(":8080/", ":8443/") + "damsmanager/downloadLog.do?sessionId=" + request.getSession().getId() + "\">log</a>";
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
		operations[5] = getParameter(paramsMap, "cacheThumbnails") != null;
		operations[6] = getParameter(paramsMap, "createMETSFiles") != null;
		operations[7] = getParameter(paramsMap, "luceneIndex") != null || getParameter(paramsMap, "solrDump") != null;
		operations[8] = getParameter(paramsMap, "sendToCDL") != null;
		operations[9] = getParameter(paramsMap, "dataConvert") != null;
		operations[10] = getParameter(paramsMap, "ingest") != null;
		operations[11] = getParameter(paramsMap, "srbSyn") != null;
		operations[12] = getParameter(paramsMap, "tsSyn") != null;
		operations[13] = getParameter(paramsMap, "createJson") != null;
		operations[14] = getParameter(paramsMap, "cacheJson") != null;
		operations[15] = getParameter(paramsMap, "devUpload") != null;
		operations[16] = getParameter(paramsMap, "jsonDiffUpdate") != null;
		operations[17] = getParameter(paramsMap, "validateManifest") != null;
		operations[18] = getParameter(paramsMap, "exportRdf") != null;
		operations[19] = getParameter(paramsMap, "jhoveReport") != null;

		int submissionId = (int)System.currentTimeMillis();
		String logLink = (Constants.CLUSTER_HOST_NAME.indexOf(":8080/")>0?Constants.CLUSTER_HOST_NAME.replaceFirst("http://", "https://").replace(":8080/", ":8443/"):Constants.CLUSTER_HOST_NAME) + "/damsmanager/downloadLog.do?submissionId=" + submissionId;
		
		String ds = getParameter(paramsMap, "ts");
		String dsDest = null;
		if((ds == null || (ds=ds.trim()).length() == 0) && !(operations[15] || operations[16]))
			throw new ServletException("No triplestore data source provided...");
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

		Date ckDate = null;
		if(operations[1]){
			String checksumDate = getParameter(paramsMap, "checksumDate");
			if(checksumDate == null || (checksumDate = checksumDate.trim()).length() == 0)
				message += "Please enter a date for checksum validation ...<br>";
			try{
				ckDate = (new SimpleDateFormat("MM-dd-yyyy")).parse(checksumDate);
			}catch (ParseException e){
				message += "Please enter a Date in valid format(mm/dd/yyyy): " + checksumDate + ".<br>";
			}
		}
		
		String rdfXml = null;	
		int metadataOperationId = -1;
	    /*if(operations[2]){
			String rdfFileOption = null;
			String tsOperation = null;
			if(getParameter(paramsMap, "tsRepopulateOnly") != null)
				tsOperation = "tsRepopulateOnly";
			else if(getParameter(paramsMap, "tsRepopulation") != null)
				tsOperation = "tsRepopulation";
			else if(getParameter(paramsMap, "samePredicatesReplacement") != null)
				tsOperation = "samePredicatesReplacement";
			else if(getParameter(paramsMap, "tsRenew") != null){
				tsOperation = "tsRenew";
				if(collectionId == null || collectionId.length() == 0)
					message += "Please select a Collection to start a new round of triplestore population. \n";				
			 }
				
		   if(tsOperation == null)
			   tsOperation = "tsNew";
		   metadataOperationId = MetadataImportController.getOperationId(tsOperation);
		   rdfFileOption = getParameter(paramsMap, "fileToIngest");
		   if(rdfFileOption.equalsIgnoreCase("rdfUrl")){
			   rdfXml = getParameter(paramsMap, "rdfUrl");
			  if(rdfXml == null || (rdfXml=rdfXml.trim()).length() == 0)
				 message += "Please enter a URL for the RDF file.<br>";
			  else{
				  rdfXml = damsClient.getContentBodyAsString(rdfXml);
				  if(rdfXml.indexOf("<rdf:RDF ") < 0 ){
					   int infoSize = rdfXml.length()< 1024?rdfXml.length():1024;
					   String vString = rdfXml.substring(0, infoSize);
					   System.out.println("Invalid file format: <br />" + vString);
					   message += "Invalid file format: <br />" + vString;
					}
			  }
	       }else
	    	   rdfXml = uploadData;
	    }*/
	    
	    JSONObject jsonToUpdate = null;
	    String subjectId = null;
	    if(operations[16]){
	    	 String jsonString = null;
			 String jsonFileOption = null;
			 jsonFileOption = getParameter(paramsMap, "fileToUpdate");
			 if(jsonFileOption.equalsIgnoreCase("jsonUrl")){
				jsonString = getParameter(paramsMap, "jsonUrl");
			  if(jsonString == null || (jsonString=jsonString.trim()).length() == 0)
				 message += "Please enter a URL for the JSON file.<br>";
			  else{
				  jsonString = damsClient.getContentBodyAsString(jsonString);
				  try {
					  jsonToUpdate = (JSONObject)JSONValue.parse(jsonString);
					}catch (Exception e){
						message += "Invalid JSON format: <br />" + jsonString;
					}
			  }
	       }else
	    	   jsonToUpdate = (JSONObject)JSONValue.parse(getParameter(paramsMap, "data"));
			 
			 if(jsonToUpdate != null && jsonToUpdate.size() > 0){
				 subjectId = (String) jsonToUpdate.remove("arkId");
				 if(subjectId == null || (subjectId=subjectId.trim()).length() == 0)
					 message += "Missing subject: <br />" + jsonToUpdate;
				 else
					 subjectId = subjectId.replaceFirst("20775/", "");
				 ds = (String) jsonToUpdate.remove("ds");
				 if(ds == null || (ds=ds.trim()).length() == 0)
					 message += "Triplestore data source is missing: <br />" + jsonToUpdate;
			 }else
				 message += "No JSON data provided: <br />" + jsonToUpdate;
	    }

		
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
				 handler = new FileCountValidaionHandler(damsClient, collectionId);
			 }else /*if (i == 1){
				   session.setAttribute("status", opMessage + "Checksum Validation for FileStore " + fileStore + " ...");
				   handler = new ChecksumHandler(damsClient, collectionId, ckDate);
			 }else if (i == 2){	
				  session.setAttribute("status", opMessage + "Importing metadata ...");
				  String dataFormat = getParameter(paramsMap, "dataFormat");
				  String importMode = getParameter(paramsMap, "importMode");
				  handler = new MetadataImportHandler(damsClient, collectionId, getParameter(paramsMap, "data"), dataFormat, importMode);
			 }else */if (i == 3){
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
			 } else if (i == 6){	
				   session.setAttribute("status", opMessage + "METS File Creation &amp; File Store Upload ...");
				   boolean metsReplace = getParameter(paramsMap, "metsReplace") != null;
				   handler = new MetaDataStreamUploadHandler(damsClient, collectionId, "mets", metsReplace);
			 } */else if (i == 7) {
				 session.setAttribute("status", opMessage + "SOLR Index ...");
				 boolean update = getParameter(paramsMap, "indexReplace") != null;
				 if(collectionId.indexOf(",") > 0){
					 String collIDs = collectionId;
					 String[] collArr = collectionId.split(",");
					 List<String> items = new ArrayList<String>();
					 System.out.println("SOLR indexing collections: " + collectionId);
					 String collNames = "";
					 for(int j=0; j<collArr.length; j++){
						 if(collArr[j] != null && (collArr[j]=collArr[j].trim()).length()>0){
							 collectionId = collArr[j];
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
					 handler = new SOLRIndexHandler( damsClient, null, update );
					 handler.setItems(items);
					 handler.setCollectionTitle(collNames.substring(0, collNames.lastIndexOf(",")));
					 handler.setCollectionId(collIDs);
				 }else
					 handler = new SOLRIndexHandler(damsClient, collectionId, update);

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
				    
					String repo = getParameter(paramsMap, "repo");
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
				 	if((filePath.startsWith("/") || filePath.startsWith("\\")) && (Constants.DAMS_STAGING.endsWith("/") 
				 			|| Constants.DAMS_STAGING.endsWith("\\")))
				 		filePath = filePath.substring(1);
				 	
				 	String[] fileUses = null;
				 	if(fileUse != null && (fileUse=fileUse.trim()).length() > 0){
				 		fileUses = fileUse.split(",");
				 		for(int j=0; j<fileUses.length; j++){
				 			if(fileUses[j] != null)
				 				fileUses[j] = fileUses[j].trim();
				 		}
				 	}

				 	session.setAttribute("category", collectionId);
				 	session.setAttribute("repo", repo);
				 	session.setAttribute("arkSetting", arkSetting);
				 	session.setAttribute("filePath", filePath);
				 	session.setAttribute("fileFilter", fileFilter);
				 	session.setAttribute("preferedOrder", preferedOrder);
				 	session.setAttribute("fileSuffixes", fileSuffixes);
				 	session.setAttribute("fileUse", fileUse);
				 	
				 	List<String> fileList = new ArrayList<String>();
				 	fileList.add(Constants.DAMS_STAGING + filePath);

		            handler = new FileIngestionHandler(damsClient, fileList, Integer.parseInt(arkSetting), collectionId, fileFilter, coDelimiter);
		            ((FileIngestionHandler)handler).setFileOrderSuffixes(fileOrderSuffixes);
		            ((FileIngestionHandler)handler).setPreferedOrder(preferedOrder);
		            ((FileIngestionHandler)handler).setRepository(repo);
		            ((FileIngestionHandler)handler).setFileUses(fileUses);
	    
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
			 } else if (i == 18){
				 String exFormat = getParameter(paramsMap, "exportFormat");
				 String xslSource = getParameter(paramsMap, "xsl");
				 if(xslSource == null || (xslSource=xslSource.trim()).length() == 0){
					 xslSource = "/pub/data1/import/apps/glossary/xsl/dams/convertToCSV.xsl";
					 if(!new File(xslSource).exists())
						 xslSource = Constants.CLUSTER_HOST_NAME + "glossary/xsl/dams/convertToCSV.xsl";
				 }
				 session.setAttribute("status", opMessage + (exFormat.equalsIgnoreCase("csv")?"CSV":exFormat.equalsIgnoreCase("ntriples")?"NTriples":"RDF") + " Metadata Export ...");
				 File outputFile = new File(Constants.TMP_FILE_DIR, "export-" + collectionId + "-" +System.currentTimeMillis() + "-rdf.xml");
				 boolean translated = getParameter(paramsMap, "translated")!= null;
			     String nsInput = getParameter(paramsMap, "nsInput");
			     List<String> nsInputs = new ArrayList<String>();
			     boolean componentsIncluded = true;
			     if(nsInput != null && (nsInput=nsInput.trim()).length() > 0){
			    	 String[] nsInputArr = nsInput.split(",");
			    	 for(int j=0; j<nsInputArr.length; j++){
			    		 componentsIncluded = false;
			    		 if(nsInputArr[j]!= null && (nsInputArr[j]=nsInputArr[j].trim()).length()>0)
			    			 nsInputs.add(nsInputArr[j]);
			    	 }
			     }
			     
			     if(exFormat.equalsIgnoreCase("csv")){
			    	 translated = true;
			    	 handler = new CSVMetadataExportHandler(damsClient, collectionId, outputFile, xslSource);
					 ((CSVMetadataExportHandler)handler).setTranslated(translated);
					 ((CSVMetadataExportHandler)handler).setNamespaces(nsInputs);
					 ((CSVMetadataExportHandler)handler).setComponentsIncluded(componentsIncluded);
			     }else if(exFormat.equalsIgnoreCase("ntriples")){
			    	 translated = false;
			    	 outputFile = new File(Constants.TMP_FILE_DIR, "export-" + collectionId + "-" +System.currentTimeMillis() + "-ntriples.txt");
			    	 handler = new NTriplesMetadataExportHandler(damsClient, collectionId, outputFile);
					 ((NTriplesMetadataExportHandler)handler).setTranslated(translated);
					 ((NTriplesMetadataExportHandler)handler).setNamespaces(nsInputs);
					 ((NTriplesMetadataExportHandler)handler).setComponentsIncluded(componentsIncluded);
			     }else{
					 handler = new RDFMetadaExportHandler(damsClient, collectionId, outputFile);
					 ((RDFMetadaExportHandler)handler).setTranslated(translated);
					 ((RDFMetadaExportHandler)handler).setNamespaces(nsInputs);
					 ((RDFMetadaExportHandler)handler).setComponentsIncluded(componentsIncluded);
			     }
			 }*/else if (i == 19){
				 session.setAttribute("status", opMessage + "Jhove report ...");
				 boolean bytestreamFilesOnly = getParameter(paramsMap, "bsJhoveReport") != null;
				 boolean updateFormat = getParameter(paramsMap, "bsJhoveUpdate") != null;
				 handler = new JhoveReportHandler(damsClient, collectionId, updateFormat);
				 if(bytestreamFilesOnly && (collectionId == null || collectionId.length() == 0)){
					 // Report all bytestream format files in DAMS
					 List<String> items = handler.listAllItems();
					 handler.setItems(items);
				 }
				 ((JhoveReportHandler)handler).setBytestreamFormatOnly(bytestreamFilesOnly);

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
				} 
		   	}
		 }else
			 continue;
      
	  message += exeInfo.replace("\n", "<br />") + "<br />";
	  if(! successful){
		  String errors = "Execution failed:<br />" + message + "<br />";
		  returnMessage += errors;
		  break;
	  }else{
		  returnMessage += "<br />" + message;
	  }
	 }
	}catch (Exception e) {
		e.printStackTrace();
		returnMessage += e.getMessage();
	}
	}else
		returnMessage = message;
	
	String logMessage = "For details, please download " + "<a href=\"" + logLink + "\">log</a>" + ".";
	if(returnMessage.length() > 1000){
		returnMessage = returnMessage.substring(0, 1000);
		int idx = returnMessage.lastIndexOf("<br ");
		if(idx > 0)
			returnMessage = returnMessage.substring(0, idx);
		else{
			idx = returnMessage.lastIndexOf("</a>");
			if(idx < returnMessage.lastIndexOf("<a "))
				returnMessage = returnMessage.substring(0, idx);
		}
		returnMessage = "<br />" + returnMessage + "<br />    ...     ";
	}
	returnMessage +=  "<br />" + logMessage;
	RequestOrganizer.addResultMessage(session, "<br />" + logMessage);
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
}