package edu.ucsd.library.xdre.web;

import java.io.IOException;
import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.mail.MessagingException;
import javax.mail.internet.AddressException;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.sql.DataSource;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.shared.Mail;
import edu.ucsd.library.util.sql.EmployeeInfo;
import edu.ucsd.library.xdre.collection.CollectionHandler;
import edu.ucsd.library.xdre.collection.DerivativeHandler;
import edu.ucsd.library.xdre.collection.FileIngestionHandler;
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
		String collectionId = request.getParameter("category");
		String activeButton = request.getParameter("activeButton");
		boolean dataConvert = request.getParameter("dataConvert") != null;
		boolean isIngest = request.getParameter("ingest") != null;
		boolean isDevUpload = request.getParameter("devUpload") != null;
		boolean isBSJhoveReport = request.getParameter("bsJhoveReport") != null;
		boolean isSolrDump = request.getParameter("solrDump") != null;
		String fileStore = request.getParameter("fs");
		if(activeButton == null || activeButton.length() == 0)
			activeButton = "validateButton";
		HttpSession session = request.getSession();
		session.setAttribute("category", collectionId);
		
		String ds = request.getParameter("ts");
		if((ds == null || (ds=ds.trim()).length() == 0))
			throw new ServletException("No triplestore data source provided...");
		if(fileStore == null || (fileStore=fileStore.trim()).length() == 0)
			fileStore = null;
			
		String forwardTo = "/controlPanel.do?ts=" + ds + (fileStore!=null?"&fs=" + fileStore:"");
		if(dataConvert)
			forwardTo = "/pathMapping.do?ts=" + ds + (fileStore!=null?"&fs=" + fileStore:"");
		else if(isIngest){
			String repo = request.getParameter("repository");
			forwardTo = "/ingest.do?ts=" + ds + (fileStore!=null?"&fs=" + fileStore:"") + (repo!=null?"&repo=" + repo:"");
		}else if(isDevUpload)
			forwardTo = "/devUpload.do?" + (fileStore!=null?"&fs=" + fileStore:"");
		else if(isSolrDump)
			forwardTo = "/solrDump.do?" + (fileStore!=null?"&fs=" + fileStore:"");
		forwardTo += "&activeButton=" + activeButton; 

		String user = "";
		String email = "";
		if(( !(isBSJhoveReport || isDevUpload) && request.getParameter("rdfImport") == null && request.getParameter("dataConvert") == null )&& 
				(collectionId == null || (collectionId=collectionId.trim()).length() == 0)){
			message = "Please choose a collection ...";
		}else{
			String servletId = request.getParameter("progressId");
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
				user = getUserName(request);
				email = getUserEmail(request);
				message = handleProcesses(collectionId, null, request);
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
			if(user != null){
				if(sender.indexOf(user) == 0)
					email = null;
				else{
					if((email == null || email.length() == 0)){				
						email = user + "@ucsd.edu";
					}
				}
			}
			if(email == null || email.equals("lsitu@ucsd.edu"))
				Mail.sendMail(sender, new String[] {"lsitu@ucsd.edu"}, "XDER Manager Invocation Result - " + Constants.CLUSTER_HOST_NAME.replace("http://", "").replace(".ucsd.edu/", ""), message, "text/html", "smtp.ucsd.edu");
			else
				Mail.sendMail(sender, new String[] {"lsitu@ucsd.edu", email}, "XDER Manager Invocation Result - " + Constants.CLUSTER_HOST_NAME.replace("http://", "").replace(".ucsd.edu/", ""), message, "text/html", "smtp.ucsd.edu");
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
	public static String handleProcesses(
			String collectionId, String uploadData,
			HttpServletRequest request) throws Exception
	{
		
		String result = "";
		String message = "";
		String returnMessage = "";
		DAMSClient damsClient = null;
		
		HttpSession session = request.getSession();
		boolean[] operations = new boolean[20];
		operations[0] = request.getParameter("validateFileCount") != null;
		operations[1] = request.getParameter("validateChecksums") != null;
		operations[2] = request.getParameter("rdfImport") != null;
		operations[3] = request.getParameter("createDerivatives") != null;
		operations[4] = request.getParameter("uploadRDF") != null;
		operations[5] = request.getParameter("cacheThumbnails") != null || request.getAttribute("cacheThumbnails") != null;
		operations[6] = request.getParameter("createMETSFiles") != null;
		operations[7] = request.getParameter("luceneIndex") != null || request.getParameter("solrDump") != null;
		operations[8] = request.getParameter("sendToCDL") != null;
		operations[9] = request.getParameter("dataConvert") != null;
		operations[10] = request.getParameter("ingest") != null;
		operations[11] = request.getParameter("srbSyn") != null;
		operations[12] = request.getParameter("tsSyn") != null;
		operations[13] = request.getParameter("createJson") != null;
		operations[14] = request.getParameter("cacheJson") != null;
		operations[15] = request.getParameter("devUpload") != null;
		operations[16] = request.getParameter("jsonDiffUpdate") != null;
		operations[17] = request.getParameter("validateManifest") != null;
		operations[18] = request.getParameter("exportRdf") != null;
		operations[19] = request.getParameter("jhoveReport") != null;

		int submissionId = (int)System.currentTimeMillis();
		String logLink = (Constants.CLUSTER_HOST_NAME.indexOf(":8080/")<0?Constants.CLUSTER_HOST_NAME.replaceFirst("http://", "https://"):Constants.CLUSTER_HOST_NAME) + "/damsmanager/downloadLog.do?submissionId=" + submissionId;
		
		String ds = request.getParameter("ts");
		String dsDest = null;
		if((ds == null || (ds=ds.trim()).length() == 0) && !(operations[15] || operations[16]))
			throw new ServletException("No triplestore data source provided...");
		else if (operations[12]){
			dsDest = request.getParameter("dsDest");
			if (dsDest == null)
				throw new ServletException("No destination triplestore data source provided...");
			else if(ds.equals(dsDest) || !dsDest.startsWith("ts/"))
				throw new ServletException("Can't sync triplestore from " + ds + " to destination " + dsDest + ".");
		}
		
		String fileStore = request.getParameter("fs");
		damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
		damsClient.setTripleStore(ds);

		int totalFiles = 0;
		Date ckDate = null;
		if(operations[0]){
			String fileCount = request.getParameter("numOfFiles");
			if(fileCount != null && fileCount.length() > 0){
				//if(fileCount == null && fileCount.length() == 0)
				//message += "Please enter the number of files for validation.<br>";
			//else{
				try{
					totalFiles = Integer.parseInt(fileCount.trim());
				}catch(NumberFormatException e){
					message += "Please enter a valid integer:" + fileCount + ".<br>";
				}
			}
		}
		if(operations[1]){
			String checksumDate = request.getParameter("checksumDate");
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
			if(request.getParameter("tsRepopulateOnly") != null)
				tsOperation = "tsRepopulateOnly";
			else if(request.getParameter("tsRepopulation") != null)
				tsOperation = "tsRepopulation";
			else if(request.getParameter("samePredicatesReplacement") != null)
				tsOperation = "samePredicatesReplacement";
			else if(request.getParameter("tsRenew") != null){
				tsOperation = "tsRenew";
				if(collectionId == null || collectionId.length() == 0)
					message += "Please select a Collection to start a new round of triplestore population. \n";				
			 }
				
		   if(tsOperation == null)
			   tsOperation = "tsNew";
		   metadataOperationId = MetadataImportController.getOperationId(tsOperation);
		   rdfFileOption = request.getParameter("fileToIngest");
		   if(rdfFileOption.equalsIgnoreCase("rdfUrl")){
			   rdfXml = request.getParameter("rdfUrl");
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
			 jsonFileOption = request.getParameter("fileToUpdate");
			 if(jsonFileOption.equalsIgnoreCase("jsonUrl")){
				jsonString = request.getParameter("jsonUrl");
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
	    	   jsonToUpdate = (JSONObject)JSONValue.parse(uploadData);
			 
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
			String filesArr = request.getParameter("files");
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
	 String userIdAttr = (String) request.getSession().getAttribute("employeeId");
	 System.out.println("DAMS Manager User: " + request.getRemoteUser() + "; IP: " + request.getRemoteAddr());
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
			 System.out.println("FileStore " + fileStore + " selected for " + getUserName(request) + " to process operation " + i);
			 String opMessage = "Preparing procedure ";
			 RequestOrganizer.setProgressPercentage(session, 0);
			 message = "";
			
			 /*if(i == 0){
				 session.setAttribute("status", opMessage + "File Count Validation for FileStore " + fileStore + " ...");
				 handler = new FileCountHandler(damsClient, collectionId, totalFiles);
			 }else if (i == 1){
				   session.setAttribute("status", opMessage + "Checksum Validation for FileStore " + fileStore + " ...");
				   handler = new ChecksumHandler(damsClient, collectionId, ckDate);
			 }else if (i == 2){	
				  session.setAttribute("status", opMessage + "Populating tripleStore ...");
				  String fileType = request.getParameter("fileType");
				  if(fileType.equalsIgnoreCase("RDF")){
					  if(metadataOperationId == Constants.METADAT_RENEW)
						  handler = new RDFLoadingHandler(tsUtils, collectionId, rdfXml, metadataOperationId);
					  else{
						  handler = new RDFLoadingHandler(tsUtils, rdfXml, metadataOperationId);
						  handler.setCollectionId(collectionId);
					  }
				  } else
					  throw new ServiceException("Unsupported file format: " + fileType);
				  
			 }else*/ if (i == 3){
				 session.setAttribute("status", opMessage + "Derivatives Creation ...");
				 boolean derReplace = request.getParameter("derReplace")==null?false:true;
				 
				 String reqSize = request.getParameter("size");
				 String[] sizes = null;
				 if(reqSize != null && reqSize.length() > 0)
					 sizes = reqSize.split(",");
				 handler = new DerivativeHandler(damsClient, collectionId, sizes, derReplace);

			 }/*else if (i == 4){	
				 session.setAttribute("status", opMessage + "RDF XML File Creation &amp; File Store Upload ...");
				 String rdfXmlDataType = request.getParameter("rdfXmlDataType");
				 boolean rdfXmlReplace = request.getParameter("rdfXmlReplace") != null;
	             
				 handler = new MetaDataStreamUploadHandler(damsClient, collectionId, "rdf", rdfXmlReplace);
			 } else if (i == 6){	
				   session.setAttribute("status", opMessage + "METS File Creation &amp; File Store Upload ...");
				   boolean metsReplace = request.getParameter("metsReplace") != null;
				   handler = new MetaDataStreamUploadHandler(damsClient, collectionId, "mets", metsReplace);
			 } else if (i == 7) {
				 session.setAttribute("status", opMessage + "SOLR Index ...");
				 boolean replaceIndex = request.getParameter("indexReplace") != null;
				 if(collectionId.indexOf(",") > 0){
					 if(collectionId.equals("bb8738126n"))
						 throw new Exception("Unsupported SOLR update mixing with STAR collection for batch updates.");
					 CollectionHandler reportHandler = null;
					 String collIDs = collectionId;
					 String[] collArr = collectionId.split(",");
					 List<String> items = new ArrayList<String>();
					 System.out.println("SOLR indexing collections: " + collectionId);
					 String collNames = "";
					 for(int j=0; j<collArr.length; j++){
						 if(collArr[j] != null && (collArr[j]=collArr[j].trim()).length()>0){
							 collectionId = collArr[j];
							 try{
								 reportHandler = new CollectionReportHandler(damsClient, collectionId, false);
								 items.addAll(reportHandler.getItems());
								 collNames += reportHandler.getCollectionTitle() + "(" + reportHandler.getFilesCount() + "), ";
								 if(j>0 && j%5==0)
									 collNames += "\n";
							 }finally{
								 if(reportHandler != null){
									 reportHandler.releaseResource();
									 reportHandler = null;
								 } 
							 }
						 }
					 }
					 handler = new SolrIndexHandler( damsClient, items, userId, replaceIndex);
					 handler.setCollectionTitle(collNames.substring(0, collNames.lastIndexOf(",")));
					 handler.setCollectionId(collIDs);
				 }else
					 handler = new SolrIndexHandler(damsClient, collectionId, userId, replaceIndex);

				// if(dsSearchName == null || (dsSearchName=dsSearchName.trim()).length() == 0)
				//		dsSearchName = SolrIndexHelper.getDSName();
					
				//  ((SolrIndexHandler)handler).setDsSearchName(dsSearchName);
			 }else if (i == 8){	
				    //session.setAttribute("status", opMessage + "CDL Sending ...");
				    int operationType = 0;
				 		boolean resend = request.getParameter("cdlResend") != null;
				 		if(resend){
				 			operationType = 1;
				 		}else{
				 			resend = request.getParameter("cdlResendMets") != null;
				 			if(resend)
				 				operationType = 2;
				 		}
		            //handler = new CdlIngestHandler(tsUtils, collectionId, userId, operationType);
	    
			 		String feeder = request.getParameter("feeder");
			 		session.setAttribute("status", opMessage + "CDL " + feeder.toUpperCase() + " METS feeding ...");
		 			boolean includeEmbargoed = (request.getParameter("includeEmbargoed")!=null);
			 		if(feeder.equals("merritt")){
			 			String account = request.getParameter("account");
			 			String password = request.getParameter("password");
			 			//String accessGroupId = request.getParameter("accessGroup");
			 			handler = new CdlIngestHandler(damsClient, collectionId, userId, operationType, feeder, account, password);
			 		}else
			 			handler = new CdlIngestHandler(damsClient, collectionId, userId, operationType);
			 		if(!includeEmbargoed)
			 			handler.excludeEmbargoedObjects();
			 }else if (i == 9){	
				    session.setAttribute("status", opMessage + "Metadata Converting and populating ...");
				    String tsOperation = request.getParameter("sipOption");
				    
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
				    
					String repo = request.getParameter("repo");
				    String arkSetting = request.getParameter("arkSetting").trim();
				 	String filePath = request.getParameter("filePath").trim();
				 	String fileFilter = request.getParameter("fileFilter").trim();
				 	String preferedOrder = request.getParameter("preferedOrder");
				 	//String fileSuffixes = request.getParameter("suffixes").trim();
				 	String fileSuffixes = request.getParameter("fileSuffixes");
			 		if(fileSuffixes != null && fileSuffixes.length() > 0)
			 			fileSuffixes = fileSuffixes.trim();
			 		
				 	String coDelimiter = "p";
				 	if(arkSetting.equals("1")){
				 		if(preferedOrder == null || preferedOrder.equalsIgnoreCase("cofDelimiter")){
				 			coDelimiter = request.getParameter("cofDelimiter").trim();
				 		}else if (preferedOrder.equals("suffix"))
				 			coDelimiter = request.getParameter("coDelimiter").trim();
				 		else
				 			coDelimiter = null;
				 	}else{
				 		if(arkSetting.equals("5")){
					 		coDelimiter = request.getParameter("coDelimiter").trim();
				 		}
				 	}
				 		
				 	String[] fileOrderSuffixes = null;
				 	if(fileSuffixes != null && fileSuffixes.length() > 0)
				 		fileOrderSuffixes = fileSuffixes.split(",");
				 	int uploadOption = Constants.JETL_UPLOAD_ALL;
				 	boolean uploadFileOnly = request.getParameter("masterFile") != null;
				 	if(uploadFileOnly)
				 		uploadOption = Constants.JETL_UPLOAD_FILE_ONLY;
				 	if((filePath.startsWith("/") || filePath.startsWith("\\")) && (Constants.DAMS_STAGING.endsWith("/") 
				 			|| Constants.DAMS_STAGING.endsWith("\\")))
				 		filePath = filePath.substring(1);

				 	session.setAttribute("category", collectionId);
				 	session.setAttribute("repo", repo);
				 	session.setAttribute("arkSetting", arkSetting);
				 	session.setAttribute("filePath", filePath);
				 	session.setAttribute("fileFilter", fileFilter);
				 	session.setAttribute("preferedOrder", preferedOrder);
				 	
				 	List<String> fileList = new ArrayList<String>();
				 	fileList.add(Constants.DAMS_STAGING + filePath);

		            handler = new FileIngestionHandler(damsClient, fileList, Integer.parseInt(arkSetting), collectionId, fileFilter, coDelimiter);
		            ((FileIngestionHandler)handler).setFileOrderSuffixes(fileOrderSuffixes);
		            ((FileIngestionHandler)handler).setHttpServletRequest(request);
		            ((FileIngestionHandler)handler).setUploadOption(uploadOption);
		            ((FileIngestionHandler)handler).setPreferedOrder(preferedOrder);
		            ((FileIngestionHandler)handler).setRepository(repo);
	    
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
				 String manifestOption = request.getParameter("manifestOptions");
				 boolean validateManifest = true;
				 boolean writeManifest = manifestOption != null && manifestOption.equals("write");
				 if(writeManifest){
					 validateManifest = false;
					 session.setAttribute("status", opMessage + "Manifest Writing ...");
				 }else
					 session.setAttribute("status", opMessage + "Manifest Valification ...");
			     handler = new LocalStoreManifestHandler(tsUtils, collectionId, validateManifest, writeManifest);
			 } else if (i == 18){
				 String exFormat = request.getParameter("exportFormat");
				 String xslSource = request.getParameter("xsl");
				 if(xslSource == null || (xslSource=xslSource.trim()).length() == 0){
					 xslSource = "/pub/data1/import/apps/glossary/xsl/dams/convertToCSV.xsl";
					 if(!new File(xslSource).exists())
						 xslSource = Constants.CLUSTER_HOST_NAME + "glossary/xsl/dams/convertToCSV.xsl";
				 }
				 session.setAttribute("status", opMessage + (exFormat.equalsIgnoreCase("csv")?"CSV":exFormat.equalsIgnoreCase("ntriples")?"NTriples":"RDF") + " Metadata Export ...");
				 File outputFile = new File(Constants.TMP_FILE_DIR, "export-" + collectionId + "-" +System.currentTimeMillis() + "-rdf.xml");
				 boolean translated = request.getParameter("translated")!= null;
			     String nsInput = request.getParameter("nsInput");
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
			 }else if (i == 19){
				 session.setAttribute("status", opMessage + "Jhove report ...");
				 boolean bytestreamFilesOnly = request.getParameter("bsJhoveReport") != null;
				 boolean formatUpdate = request.getParameter("bsJhoveUpdate") != null;
				 handler = new JhoveReportHandler(collectionId, bytestreamFilesOnly);
				 
				 //xxx 
				 //bytestream only support
				 if(bytestreamFilesOnly){
					 ((LocalStoreJhoveReportHandler)handler).setUpdateFormat(formatUpdate);
					 if(collectionId==null || collectionId.length()==0){
						 BindingIterator bit = null;
						 String sparql = TripleStoreConstants.LOOKUP_BYTESTREAM_FILE_SPARCQL;
						 List<String> items = new ArrayList<String>();
						 try{
							 bit = tsUtils.twinqlSelect(sparql);
							 while(bit.hasNext()){
								 items.add(TripleStoreUtils.getSubjectIdFromUrl(bit.nextBinding().get("subject")));
							 }
						 }finally{
							 if(bit != null){
								 bit.close();
								 bit = null;
							 }
						 }
						 handler.setItems(items);
					 }	 
				 }
			 }*/else 	
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
					handler.release();
					String collectionName = handler.getCollectionId();
					if(collectionName != null && collectionName.length() >0 && logLink.indexOf("&category=")<0)
						logLink += "&category=" + collectionName.replace(" ", "");
					handler.setExeResult(successful);
					exeInfo += handler.getExeInfo();
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
		  result += exeInfo + "<br />";
		  returnMessage += "<br />" + message;
	  }
	 }
	}catch (Exception e) {
		e.printStackTrace();
		returnMessage += e.getMessage();
	}
	}else
		returnMessage = message;
	
	String logMessage = "For details information, please download " + "<a href=\"" + logLink + "\">log</a>" + ".";
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
	
	public static String getUserEmail(HttpServletRequest request){
		DataSource dsSourceAuth;
		EmployeeInfo emp;
		String email = "";
		try {
			Context initCtx = new InitialContext();
			dsSourceAuth = (DataSource)initCtx.lookup("java:comp/env/jdbc/authzt");
			emp = EmployeeInfo.lookup( dsSourceAuth, request.getRemoteUser() );
			email = emp.getEmail();
		} catch (NamingException e) {
			e.printStackTrace();
		}
		return email;
	}
	
	public static String getUserName(HttpServletRequest request){
		DataSource dsSourceAuth;
		EmployeeInfo emp;
		String userName = (String) request.getAttribute("user");
		if(userName == null){
			try {
				Context initCtx = new InitialContext();
				dsSourceAuth = (DataSource)initCtx.lookup("java:comp/env/jdbc/authzt");
				emp = EmployeeInfo.lookup( dsSourceAuth, request.getRemoteUser() );
				userName = emp.getUsername();
				request.setAttribute("user", userName);
			} catch (NamingException e) {
				e.printStackTrace();
			}
		}
		return userName;
	}
}