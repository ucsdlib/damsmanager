package edu.ucsd.library.xdre.web;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.collection.MetadataImportHandler;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * Controller to perform RDF metadata import.
 * @author lsitu
 *
 */
public class MetadataImportController implements Controller{

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String message = "";
		DAMSClient damsClient = null;	

		request.setCharacterEncoding("UTF-8");
		String ark = request.getParameter("ark");
		String dataFormat = request.getParameter("dataFormat");
		String data = request.getParameter("data");
		String importMode = request.getParameter("importMode");
		MetadataImportHandler handler = null;	

		boolean result = false;
		try{
			damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
			damsClient.setUser(request.getRemoteUser());

			handler = new MetadataImportHandler(damsClient, null, data.trim(), dataFormat, importMode);
			result = handler.execute();
			if(result) {
				String damsUrl = "http://" + Constants.CLUSTER_HOST_NAME + (Constants.CLUSTER_HOST_NAME.startsWith("localhost")?"":".ucsd.edu/dc") + "/object/"
						+ ark.substring(ark.lastIndexOf("/") + 1, ark.length());
				message = "Update successfully. " + "View item <a href=\"" + damsUrl +  "\" target=\"dc\">" + ark + "</a>.";
			} else {
				StringBuilder err = new StringBuilder();
				for (String error : handler.getErrors()) {
					err.append((err.length() > 0 ? "; \n": "") + error);
				}
				message = err.toString();
			}
		}catch (Exception e){
			e.printStackTrace();
			message = "Error: " + e.getMessage();
		}finally{
			if(damsClient != null)
				damsClient.close();
			if(handler != null) {
				handler.release();
				handler = null;
			}
		}

		request.getSession().setAttribute("message", message);
		if(result) {
			response.sendRedirect(request.getContextPath() + "/rdfImport.do");
		} else {
			request.getRequestDispatcher("rdfEdit.do").forward(request, response);
		}
		return null;
	}
}
