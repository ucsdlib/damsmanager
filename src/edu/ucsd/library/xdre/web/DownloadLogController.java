package edu.ucsd.library.xdre.web;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.utils.Constants;


 /**
 * Class DirectoryController, the Model of the Staging Area directory picker for JETL
 *
 * @author lsitu@ucsd.edu
 */
public class DownloadLogController implements Controller {

	
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
			if(ips.indexOf(ip) >= 0){
				loginRequired = false;
				System.out.println("XDRE Manager access allowed from " + ip + ". Operation download - " + request.getRequestURI());
			}
		}

		if(!loginRequired){
		
		String fileName = request.getParameter("file");
		String message = "";
		OutputStream out= response.getOutputStream();
		String downloadName = "";
		if(fileName == null){
			String logType = request.getParameter("log");
			String submissionId = request.getParameter("submissionId");
			String category = request.getParameter("category");
			downloadName = (category==null?submissionId:category);
			if(logType != null && logType.equalsIgnoreCase("ingest")){
				response.setHeader("Content-Disposition", "attachment; filename=ingest.log");
				fileName = (Constants.TMP_FILE_DIR==null||Constants.TMP_FILE_DIR.length()==0?"":Constants.TMP_FILE_DIR+"/") + "ingest-" + category + ".log";
				downloadName = "ingest-" + downloadName  + ".log";
			}else{
				response.setHeader("Content-Disposition", "attachment; filename=damslog.txt");
				fileName = (Constants.TMP_FILE_DIR==null||Constants.TMP_FILE_DIR.length()==0?"":Constants.TMP_FILE_DIR+"/") + "damslog-" + submissionId + ".txt";
				downloadName = "damslog-" + downloadName  + ".txt";
			}
		}else{
			downloadName = fileName;
			fileName = Constants.TMP_FILE_DIR + "/" + fileName;
		}
		
		File outFile = new File(fileName);
		
		String contentType= "text/plain; charset=UTF-8";
		if(!outFile.exists() || !outFile.canRead()){
			response.setContentType(contentType);
			message = "File " + outFile.getAbsolutePath() + " doesn't exist.";
			out.write(message.getBytes());
		}else{

			contentType = request.getSession().getServletContext().getMimeType(outFile.getName());
			if (contentType==null) {
				contentType = "application/x-download";
			}else if (contentType.startsWith("text")) {
				contentType += ";charset=UTF-8";
			} 
			response.setContentType(contentType);
			long length = outFile.length();
			long lastModified = outFile.lastModified();
			String eTag = fileName + "_" + length + "_" + lastModified;
			response.reset();
			response.setHeader("Content-Disposition", "attachment;filename=" + downloadName);
			response.setHeader("ETag", eTag);
			response.setDateHeader("Last-Modified", lastModified);
			
			InputStream in = null;
			int bRead = -1;
			byte[] buf = new byte[5120];
			try{
				in = new FileInputStream(outFile);
				while((bRead=in.read(buf))> 0){
					out.write(buf, 0, bRead);
					out.flush();
				}
			}finally{
				if(in != null)
					in.close();
			}
		}
		out.flush();
		out.close();
		}else{
			String queryString = "";
			Map pMap = request.getParameterMap();
			for(Iterator it=pMap.entrySet().iterator(); it.hasNext();){
				Entry en = (Entry)it.next();
				String key = (String)en.getKey();
				queryString += (queryString.length()>0?"&":"") + key + "=" + request.getParameter(key);
			}
			response.sendRedirect("loginPas.do?loginPage=" + URLEncoder.encode("downloadLog.do?" + queryString, "UTF-8"));
		}
		return null;
	}
 }