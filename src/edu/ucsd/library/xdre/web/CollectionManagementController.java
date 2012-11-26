package edu.ucsd.library.xdre.web;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
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

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;


 /**
 * Class CollectionManagementController, the Model of the collection management
 * home page
 *
 * @author lsitu@ucsd.edu
 */
public class CollectionManagementController implements Controller {

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Map dataMap = null;
		String logOutDisplay = "none";
		boolean statusTagging = true;
		
		HttpSession session = request.getSession();

		if (request.getRemoteUser() != null) {
			logOutDisplay = "inline";
			session.setAttribute("employeeId", request.getRemoteUser());
		}

		String message = request.getParameter("message");
		String ds = request.getParameter("ds");
		String fs = request.getParameter("fileStore");
		if(message == null)
			message = "";
		String activeButton = request.getParameter("activeButton");
		if(activeButton == null || activeButton.length() == 0)
			activeButton = "validateButton";
		session.setAttribute("activeButton", activeButton);

		
		String category = null;
		DAMSClient damsClient = null;
		try {
			category = request.getParameter("category");
			session.setAttribute("category", category);

			damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
			List<String> fsSrcs = damsClient.listFileStores();
			
			dataMap = new HashMap();
			dataMap.put("logOut", logOutDisplay);
			dataMap.put("message", message);
			dataMap.put("activeButton", activeButton);
			dataMap.put("collections", damsClient.listCollections());
			dataMap.put("triplestore", ds);
			dataMap.put("triplestores", damsClient.listTripleStores());
			dataMap.put("statusTagging", statusTagging);
			dataMap.put("filestores", fsSrcs);
			dataMap.put("filestore", fs);
			dataMap.put("filestoreDefault", Constants.DEFAULT_FILESTORE);
			System.out.println("Catalina memory:" + Runtime.getRuntime().totalMemory() + "/" + Runtime.getRuntime().maxMemory());
		} catch (Exception e) {
			e.printStackTrace();
			String eMessage = e.getMessage();
			throw new ServletException(eMessage);
		}
		return new ModelAndView("controlPanel", "model", dataMap);
	}
	}