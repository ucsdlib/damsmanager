package edu.ucsd.library.xdre.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.shared.Mail;
import edu.ucsd.library.xdre.exports.CSVMetadataExportHandler;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;


 /**
 * Class CSVExportController - export metadata in CSV fromat.
 *
 * @author lsitu@ucsd.edu
 */
public class CSVExportController implements Controller {
	private static Logger log = Logger.getLogger(CSVExportController.class);
	private static Map<String, String> COLLECTION_REQUESTS = new HashMap<String, String>();
	private static Random random = new Random();
	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Principal userPrincipal = request.getUserPrincipal();
		boolean loginRequired = true;
		if(userPrincipal != null){
			loginRequired = false;
			if(request.isUserInRole(Constants.CURATOR_ROLE))
				loginRequired = false;
		}else{
			String ip = request.getRemoteAddr();	
			String ips = Constants.IPS_ALLOWED;
			if(ips.indexOf(ip) >= 0)
				loginRequired = false;
			log.info("XDRE Manager access allowed from " + ip + " at " + new Date() + ". Operation EMU Utility CSV export - " + request.getRequestURI() + "?" + request.getQueryString());
		}

		if(!loginRequired){
			String subject = request.getParameter("subject");
			String user = request.getParameter("user");
			String xsl = request.getParameter("xsl");
			String nsInput = request.getParameter("nsInput");
			String ds = request.getParameter("ds");
			boolean collection = request.getParameter("collection")!=null;
			boolean translated = true;//request.getParameter("translated")!= null;
			boolean componentsIncluded = true;//request.getParameter("components")!= null;
			String message = "";
	
			
			boolean subjectNeeded = (subject == null || (subject=subject.trim()).length()==0);
			if(subjectNeeded){
				message += "Subject is required for CSV metadata export.";
			}
	
			String email = null;
			CSVMetadataExportHandler handler = null;
			DAMSClient damsClient = null;
			OutputStream out = null;
			if(message == null || message.length() == 0){
				ds = Constants.DEFAULT_TRIPLESTORE;
				
				damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
				damsClient.setTripleStore(ds);
				if(xsl == null || (xsl=xsl.trim()).length() == 0){
					xsl = Constants.CLUSTER_SHARE_PATH + "/apps/glossary/xsl/dams/convertToCSV.xsl";
					if(!new File(xsl).exists())
						xsl = Constants.CLUSTER_HOST_NAME + "/glossary/xsl/dams/convertToCSV.xsl";
				}else if(xsl.indexOf('/') <= 0 || xsl.indexOf('\\') <= 0){	
					if(!new File(Constants.CLUSTER_SHARE_PATH + "/apps/glossary/xsl/dams/" + xsl).exists())
						xsl = Constants.CLUSTER_HOST_NAME + "glossary/xsl/dams/" + xsl;
					else
						xsl = Constants.CLUSTER_SHARE_PATH + "/apps/glossary/xsl/dams/" + xsl;
				}
					
				
				if(user == null || (user=user.trim()).length() == 0){
					user =CollectionOperationController.getUserName(request);
					email = CollectionOperationController.getUserEmail(request);
				}
				
				String requestStr = request.getRemoteAddr()+ "-" + request.getQueryString();
				if(addRequest(requestStr)){
					int ran = random.nextInt();
					File outputFile = new File(Constants.TMP_FILE_DIR, "export-" + request.getSession().getId() + (ran>=0?"-":"") + ran + "-rdf.xml");
					try{
						response.setContentType("application/x-download");
						List<String> items = new ArrayList<String>();
						if(subject != null && (subject=subject.trim()).length()>0){
							String[] itemsArr = subject.split(",");
							for(int i=0; i<itemsArr.length; i++){
								subject = itemsArr[i];
								if(subject != null && (subject=subject.trim()).length() > 0){
									items.add(subject);
								}
							}
						}

					    List<String> nsInputs = new ArrayList<String>();
					    if(nsInput != null && (nsInput=nsInput.trim()).length() > 0){
					    	String[] nsInputArr = nsInput.split(",");
					    	for(int j=0; j<nsInputArr.length; j++){
					    		if(nsInputArr[j]!= null && (nsInputArr[j]=nsInputArr[j].trim()).length()>0)
					    			nsInputs.add(nsInputArr[j]);
					    	}
					    }
					    
					    if(collection)
					    	handler = new CSVMetadataExportHandler(damsClient, subject, outputFile, xsl);
					    else{
					    	handler = new CSVMetadataExportHandler(damsClient, outputFile, xsl);
					    	handler.setItems(items);
					    }
						handler.setNamespaces(nsInputs);
						handler.setSession(request.getSession());
						handler.setTranslated(translated);
						handler.setComponentsIncluded(componentsIncluded);
						handler.execute();
						message = handler.getExeInfo().replace("/damsmanager/", "/xdre/damsmanager/") + "\n";
					}catch(Exception e){
						message += "Error: " + e.getMessage() + "\n" + message;
						System.out.println("Error rdf " + outputFile.getName() + ": " + e.getMessage() + "\n" + message);
						e.printStackTrace();
					}finally{
						removeRequest(requestStr);
						if(handler != null){
							handler.releaseResource();
						}
					}
					
					if(handler != null){
						
						File csvFile = handler.getCSVFile();
						String fileName = csvFile.getName();
						long length = csvFile.length();
						long lastModified = csvFile.lastModified();
						String eTag = fileName + "_" + length + "_" + lastModified;
						response.setHeader("Content-Disposition", "attachment; filename=" + subject.substring(0,10)+".csv");
						response.setHeader("ETag", eTag);
						response.setDateHeader("Last-Modified", lastModified);
						try{
							out = response.getOutputStream();
							if(out != null){
								InputStream in = null;
								byte[] buf = new byte[5120];
								int bRead = -1;
								try{
									in = new FileInputStream(csvFile);
									while((bRead=in.read(buf)) > 0){
										out.write(buf, 0, bRead);
										out.flush();
									}
								}catch(Exception e){
									//message += "CSV transformation error: " + e.getMessage() + "\n" + message;
									//e.printStackTrace();
								}finally{
									if(in != null)
										in.close();
									if(out != null)
										out.close();
								}
							}
						}catch(IOException io){}
					}else{
						response.setContentType("text/html; charset=UTF-8");
						response.getWriter().write("<html><body><div style=\"font-weight:bold;font-size:18;color:#444;text-align:center;margin-top:50px;\">" + message + "</div></body></html>");
					}
					
					try{
						//send email
						String sender = "dams-support@ucsd.edu";
						
						if(user != null){
							if(sender.indexOf(user) == 0)
								email = null;
							else{
								if((email == null || email.length() == 0)){				
									email = user + "@ucsd.edu";
								}
							}
						}
						if(email == null){
							//Mail.sendMail(sender, new String[] {sender}, "XDRE Manager CSV Export - " + Constants.CLUSTER_HOST_NAME.replace("http://", "").replace(".ucsd.edu/", ""), message, "text/html", "smtp.ucsd.edu");
							log.info(" Unknown EMU report for collection " + subject + " prepared at " + new Date() + ": " + message + "." );
						}else
							Mail.sendMail(sender, new String[] {sender, email}, "XDRE Manager CSV Export - " + Constants.CLUSTER_HOST_NAME.replace("http://", "").replace(".ucsd.edu/", ""), message, "text/html", "smtp.ucsd.edu");
						
					}catch (Exception e) {
						e.printStackTrace();
					}finally{
						log.info("EMU report for " + email + ": " + message + "." );
					}
					
				}else{
					message = "Thank you very much for using the DAMS Export Metadata Utility (EMU). <br />Your request is being processed. You will be notified via email (at " + user + "@ucsd.edu) once it's ready.";
					response.sendRedirect("emu.do?message=" + URLEncoder.encode(message, "UTF-8"));
				}
			}else{
				response.sendRedirect("emu.do?message=" + URLEncoder.encode(message, "UTF-8"));
			}
		}else{
			String queryString = "";
			Map pMap = request.getParameterMap();
			for(Iterator it=pMap.entrySet().iterator(); it.hasNext();){
				Entry en = (Entry)it.next();
				String key = (String)en.getKey();
				queryString += (queryString.length()>0?"&":"") + key + "=" + request.getParameter(key);
			}
			response.sendRedirect("loginPas.do?loginPage=" + URLEncoder.encode("csvExport.do?" + queryString, "UTF-8"));
		}
		return null;
	}
	
	public static synchronized boolean addRequest(String request){
		return (COLLECTION_REQUESTS.put(request, "")==null);
	}
	
	public static synchronized void removeRequest(String request){
		COLLECTION_REQUESTS.remove(request);
	}
 }