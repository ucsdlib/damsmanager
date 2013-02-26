package edu.ucsd.library.xdre.web;

import java.net.URLEncoder;
import java.security.Principal;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.json.simple.JSONObject;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * EMUController - EMU utility for CSV metadata export.
 * @author lsitu
 *
 */
public class SolrDumpController implements Controller {
	private static Logger log = Logger.getLogger(SolrDumpController.class);
	//bb48472930 aal, bb97964875 sshl, bb9659622z mscl, bb58031438 Visual Resources, bb6110104w UCSD Libraries, bb78509612 UCSD History, bb19122517 Melanesian Archive
	private static String COLLECTIONS_EXCLUDED = "bb48472930, bb97964875, bb9659622z, bb58031438, bb6110104w, bb78509612, bb19122517";
	public static String SOLR_ITEMS_COUNT_QUERY = "q=&rows=0&qt=dismax&f.Facet_Date.facet.limit=0&ds=jdbc/dams4&wt=xml&fq=category:COLLECTIONID";

	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Principal userPrincipal = request.getUserPrincipal();
		boolean loginRequired = true;
		if(userPrincipal != null){
			if(request.isUserInRole(Constants.CURATOR_ROLE)){
				loginRequired = false;
			}
		}else{
			String ip = request.getRemoteAddr();
			String ips = Constants.IPS_ALLOWED;
			if(ips.indexOf(ip) >= 0){
				loginRequired = false;
			}
			System.out.println("XDRE Manager access allowed from " + ip + ". Operation DLP SOLR Dump Utility - " + request.getRequestURI());
		}

		if(!loginRequired){
			Map data = new HashMap();
			String message = request.getParameter("message");
			String ds = request.getParameter("ts");
			if(message == null)
				message = "";

			if(ds==null || ds.length()==0)
				ds = Constants.DEFAULT_TRIPLESTORE;

			Map<String, String> collMap = null;
			Map<String,JSONObject> colTitleMap = null;
			DAMSClient damsClient = null;
			try{
				damsClient = new DAMSClient();
				collMap = damsClient.listCollections();
				JSONObject colObj = null;
				String title = null;
				for(Iterator<String> it=collMap.keySet().iterator(); it.hasNext();){
					colObj = new JSONObject();
					title = it.next();
					colObj.put("subject", collMap.get(title));
					colObj.put("title", collMap.get("title"));
					colObj.put("count", "0");
					colTitleMap.put((String) colObj.get("title"), colObj);
				}
			}catch (Exception e){
				e.printStackTrace();
				message += "Internal error: " + e.getMessage();
			}finally{
				if(damsClient != null)
					damsClient.close();
			}
			data.put("triplestore", ds);
			data.put("triplestores", damsClient.listFileStores());
			data.put("collections", colTitleMap.values());
			data.put("message", message);
			return new ModelAndView("solrDump", "model", data);
		}else{
			String queryString = "";
			Map pMap = request.getParameterMap();
			for(Iterator<Entry> it=pMap.entrySet().iterator(); it.hasNext();){
				Entry en = (Entry)it.next();
				String key = (String)en.getKey();
				queryString += (queryString.length()>0?"&":"") + key + "=" + request.getParameter(key);
			}
			response.sendRedirect("loginPas.do?loginPage=" + URLEncoder.encode("solrDump.do?" + queryString, "UTF-8"));
		}
		return null;
	}
}
