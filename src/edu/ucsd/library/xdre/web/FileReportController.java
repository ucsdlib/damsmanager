package edu.ucsd.library.xdre.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * FileReportController - utility class to create files report for a collections.
 * @author lsitu
 *
 */
public class FileReportController implements Controller {
	private static Logger log = Logger.getLogger(FileReportController.class);

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String ds = request.getParameter("ts");
		String collectionId =  request.getParameter("category");
		String message = request.getParameter("message");


		HttpSession session = request.getSession();	
		DAMSClient damsClient = null;
		Map dataMap = new HashMap();
		try{
			if(ds == null)
				ds = Constants.DEFAULT_TRIPLESTORE;

			
			damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
			damsClient.setTripleStore(ds);

			Map<String, String> collectionMap = damsClient.listCollections();

			List<String> tsSrcs = damsClient.listTripleStores();

			message = (!StringUtils.isBlank(message) ||
					StringUtils.isBlank(collectionId)) ? message : (String)session.getAttribute("message");
			request.getSession().removeAttribute("message");

			dataMap.put("categories", collectionMap);
			dataMap.put("category", collectionId);
			dataMap.put("message", message);
			dataMap.put("triplestore", ds);
			dataMap.put("triplestores", tsSrcs);
		
		} catch (Exception e) {
			e.printStackTrace();
			message += e.getMessage();
		}finally{
			if(damsClient != null)
				damsClient.close();
		}
		return new ModelAndView("fileReport", "model", dataMap);
	}
}
