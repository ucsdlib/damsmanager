package edu.ucsd.library.xdre.web;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;



/**
 * SolrDumpController - utility class to update solr in batch of collections.
 * @author lsitu
 *
 */
public class CollectionsViewController implements Controller {
	private static Logger log = Logger.getLogger(CollectionsViewController.class);

	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

			Map data = new HashMap();
			String message = request.getParameter("message");
			String ds = request.getParameter("ts");
			String view = request.getParameter("view");
			
			if(message == null)
				message = "";
			if(view == null || (view=view.trim()).length() == 0){
				String servletPath = request.getServletPath();
				view = servletPath.substring(servletPath.lastIndexOf("/")+1);
				int idx = view.lastIndexOf(".");
				if(idx > 0)
					view = view.substring(0, idx);
				log.info("Accessing view: " + view + " from " + servletPath + ".");
			}

			Map<String, String> collMap = null;
			Map<String,JSONObject> colTitleMap = null;
			DAMSClient damsClient = null;
			try{
				if(ds == null)
					ds = Constants.DEFAULT_TRIPLESTORE;
				
				damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
				List<String> tsSrcs = damsClient.listTripleStores();
				collMap = damsClient.listCollections();
				JSONObject colObj = null;
				String title = null;

				colTitleMap = new TreeMap<String, JSONObject>();
				for(Iterator<String> it=collMap.keySet().iterator(); it.hasNext();){
					colObj = new JSONObject();
					title = it.next();
					colObj.put("subject", collMap.get(title));
					colObj.put("title", title.replace("Collection]", "]").replace("CollectionPart]", "Part]"));
					colObj.put("count", "0");
					colTitleMap.put((String) colObj.get("title"), colObj);
				}
				
				data.put("triplestore", ds);
				data.put("triplestores", tsSrcs);
				data.put("collections", colTitleMap.values());
				data.put("message", message);
			}catch (Exception e){
				e.printStackTrace();
				message += "Internal error: " + e.getMessage();
			}finally{
				if(damsClient != null)
					damsClient.close();
			}

			return new ModelAndView(view, "model", data);
	}
}
