package edu.ucsd.library.xdre.web;

import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Node;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * Controller for RDF Import forms.
 * @author lsitu
 *
 */
public class RdfImportFormController implements Controller{

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String message = "";
		DAMSClient damsClient = null;
		Map dataMap = new HashMap();		

		String title = "";
		String ark = request.getParameter("ark");
		String data = request.getParameter("data");
		Document doc = null;
		
		HttpSession session = request.getSession();
		if (StringUtils.isBlank(ark)) {
			message = (String)session.getAttribute("message");
			session.removeAttribute("message");
			dataMap.put("message", (message==null?"":message));
			return new ModelAndView("rdfImport", "model", dataMap);
		} else {
			String format = "RDF/XML";
			
			if (StringUtils.isBlank(data)) {
				try{
					damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
					doc = damsClient.getRecord(ark.trim());
					Node titleNode = doc.selectSingleNode("/rdf:RDF/*[local-name()='Object' or contains(local-name(), 'Collection')]/dams:title/mads:Title/mads:authoritativeLabel | /rdf:RDF/*/mads:authoritativeLabel");
					if (titleNode != null)
						title = titleNode.getText();
					else
						title = "Unknown Title";
	
					data = doc.asXML().trim();
					session.setAttribute("title", title);
				}catch (Exception e){
					e.printStackTrace();
					message = "Error: " + e.getMessage();
				}finally{
					if(damsClient != null)
						damsClient.close();
				}
			} else {
				data = data.trim();
				message = (String)session.getAttribute("message");
				title = (String)session.getAttribute("title");
				session.removeAttribute("message");
			}
			session.setAttribute("data", data);
	
			dataMap.put("rdf", data);
			dataMap.put("ark", ark);
			dataMap.put("title", title);
			dataMap.put("formats", getFormats());
			dataMap.put("format", format);
			dataMap.put("importMode", Constants.IMPORT_MODE_ALL);
			dataMap.put("message", message);
			return new ModelAndView("rdfEdit", "model", dataMap);

		}
	}

	public static Map<String, String> getFormats(){
		Map<String, String> formats = new TreeMap<>();
		formats.put("RDF XML", "RDF/XML");
		return formats;
	}
}
