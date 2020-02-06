package edu.ucsd.library.xdre.web;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.naming.InitialContext;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;


 /**
 * Class CollectionManagementController, the model of the collection/units management
 * home page
 *
 * @author lsitu@ucsd.edu
 */
public class FileUploadController implements Controller {

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String message = request.getParameter("message");
		String filesPath = request.getParameter("filesPath");

		if(message == null)
			message = "";
		if(filesPath == null)
			filesPath = "";

		Map dataMap = new HashMap();
		DAMSClient damsClient = null;
		try {
			
			damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
			request.getSession().setAttribute("user", request.getRemoteUser());

			List<String> ingestFiles = new ArrayList<String>();
			filesPath = filesPath.trim();
			
			if (filesPath.length() > 0) {
				String[] filesPaths = filesPath.split(";");
				File tmpFile = null;
				for(int j=0; j<filesPaths.length; j++) {
					tmpFile = new File(Constants.DAMS_STAGING + "/" + filesPaths[j]);
					File[] files = tmpFile.listFiles();
					for (File file : files) {
						if (file.isFile() && !file.isHidden()) {
							String filePath = file.getPath().replace('\\', '/');
							ingestFiles.add(filePath.substring(filePath.indexOf(filesPaths[j])));
						}
					}
				}
			}
			
			Collections.sort(ingestFiles);

			message = !StringUtils.isBlank(message) ? message : (String)request.getSession().getAttribute("message");
			request.getSession().removeAttribute("message");
			
			dataMap.put("message", message);
			dataMap.put("filesPath", filesPath);
			dataMap.put("files", ingestFiles);

		} catch (Exception e) {
			e.printStackTrace();
			String eMessage = e.getMessage();
			throw new ServletException(eMessage);
		}finally{
			if(damsClient != null)
				damsClient.close();
		}
		return new ModelAndView("fileUpload", "model", dataMap);
	}
}
