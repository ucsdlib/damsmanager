package edu.ucsd.library.xdre.web;

import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import com.hp.hpl.jena.rdf.model.Statement;

import edu.ucsd.library.xdre.utils.Constants;

/**
 * EMUController - EMU utility for CSV metadata export.
 * @author lsitu
 *
 */
public class EMUController implements Controller {
	private static Logger log = Logger.getLogger(EMUController.class);
	public static String SOLR_ITEMS_COUNT_QUERY = "q=&rows=0&qt=dismax&f.Facet_Date.facet.limit=0&ds=jdbc/dams4&wt=xml&fq=category:COLLECTIONID";
	public static String[] DLCLIBRARIES = {"bb48472930", "bb06151725", "bb97964875", "bb9659622z"};//AAL["bb5086428r","bb2253416m","bb1229862s","bb3550355p"], SIO["bb06151725","bb15711429","bb13322220","bb20830477","bb6417546k","bb97964477"], SSHL["bb4847646x"], MSCL["bb78509612","bb8362683h","bb96598309","bb9762256h","bb0205841k","bb0956474h","bb1297990g","bb19122517","bb3994052b","bb5802933z"]

	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		Principal userPrincipal = request.getUserPrincipal();
		boolean loginRequired = true;
		boolean isProxy = true;
		if(userPrincipal != null){
			loginRequired = false;
			if(request.isUserInRole(Constants.CURATOR_ROLE)){
				loginRequired = false;
				isProxy = false;
			}
		}else{
			String ip = request.getRemoteAddr();
			String ips = Constants.IPS_ALLOWED;
			if(ips.indexOf(ip) >= 0){
				loginRequired = false;
			}
			System.out.println("XDRE Manager access allowed from " + ip + ". Operation EMU Utility - " + request.getRequestURI());
		}

		if(!loginRequired){
			Map data = new HashMap();
			Map<String,JSONObject> collectionMap = null;
			String message = request.getParameter("message");
			String xsl = request.getParameter("xsl");
			if(message == null)
				message = "";

			String libKey = null;
			String itemArk = null;
			String solrParams = "";
			JSONObject item = null;
			Map<String, Collection> ucsdLibraries = new HashMap<String,  Collection>();
			String solrCore = "dams4";
			try{
				Map<String,JSONObject> colMap = getCollectionsMap(Constants.SOLR_URL_BASE, solrCore);
				colMap.remove("bb6110104w");//ucsdLibraries
				for(int i=0;i<DLCLIBRARIES.length;i++){
					solrParams = "";
					if( i == 1 )
						item = colMap.get(DLCLIBRARIES[i]);
					else
						item = colMap.remove(DLCLIBRARIES[i]);
						
					collectionMap = new TreeMap<String,JSONObject>();
					Object hItems = item.get("hasItems");
					if(hItems instanceof JSONArray){
						JSONArray itemsArr = (JSONArray)hItems;
						for(int j=0; j<itemsArr.size(); j++){
							itemArk = (String)itemsArr.get(j);
							solrParams += (solrParams.length()>0?"+OR+":"") + itemArk;
						}
					}else{
						if(hItems != null){
							String subject = (String)hItems;
							solrParams = subject.startsWith("\"")?subject.substring(1, subject.length()-1):subject;
						}
					}
					
					switch(i){
						case 0:
							libKey = "al";
							itemArk = "bb3209061j";//denny bb3209061j
							if(solrParams.length()==0 || solrParams.indexOf(itemArk)<0)
								solrParams += (solrParams.length()>0?"+OR+":"") + itemArk;
							itemArk = "bb1229862s";//al_dmca bb1229862s
							if(solrParams.length()==0 || solrParams.indexOf(itemArk)<0)
								solrParams += "+OR+" + itemArk;
							break;
						case 1:
							libKey = "sio";
							itemArk = "bb06151725"; //SIO
							if(solrParams.length()==0 || solrParams.indexOf(itemArk)<0)
								solrParams += (solrParams.length()>0?"+OR+":"") + itemArk;
							itemArk = "bb66224024";//bb66224024 Historic Charts of the Pacific
							if(solrParams.length()==0 || solrParams.indexOf(itemArk)<0)
								solrParams += "+OR+" + itemArk;
							solrParams += "+OR+bb15711429+OR+bb13322220+OR+bb20830477+OR+bb6417546k+OR+bb97964477";//CEO collections
							break;
						case 2:
							libKey = "sshl";
							itemArk = "bb4847646x"; //SSHL
							if(solrParams.length()==0 || solrParams.indexOf(itemArk)<0)
								solrParams += (solrParams.length()>0?"+OR+":"") + itemArk;
							break;
						default:
							libKey = "mscl";
							for(Iterator it=colMap.keySet().iterator();it.hasNext();){
								itemArk = (String)it.next();
								if(solrParams.length()==0 || solrParams.indexOf(itemArk)<0)
									solrParams += (solrParams.length()>0?"+OR+":"") + itemArk;
							}
							break;
					}
					//Added all other collections to mscl for now
					initItemsCount(solrParams, colMap, collectionMap);
					ucsdLibraries.put(libKey, collectionMap.values());
				}
				
			}catch (Exception e){
				e.printStackTrace();
				message += "Internal error: " + e.getMessage();
			}
			data.put("xsl", xsl);
			data.put("isProxy", isProxy);
			data.put("message", message);
			data.put("libraries", ucsdLibraries);
			return new ModelAndView("emu", "model", data);
		}else{
			String queryString = "";
			Map pMap = request.getParameterMap();
			for(Iterator it=pMap.entrySet().iterator(); it.hasNext();){
				Entry en = (Entry)it.next();
				String key = (String)en.getKey();
				queryString += (queryString.length()>0?"&":"") + key + "=" + request.getParameter(key);
			}
			response.sendRedirect("loginPas.do?loginPage=" + URLEncoder.encode("emu.do?" + queryString, "UTF-8"));
		}
		return null;
	}
	
	
	private void initItemsCount(String colParams, Map arkMap, Map titleMap) throws MalformedURLException, DocumentException{
		String solrCore = "dams4";
		Document solrDoc = getSOLRResult(Constants.SOLR_URL_BASE, solrCore, SOLR_ITEMS_COUNT_QUERY.replace("COLLECTIONID",colParams));
		List<Node> facets = solrDoc.selectNodes("/response/lst[@name='facet_counts']/lst[@name='facet_fields']/lst[@name='Facet_Collection']/int");
		String colName = null;
		String tmpColName = null;
		String count = null;
		Node facetNode = null;
		JSONObject colObj = null;
		boolean itemFound = false;
		for(Iterator it=facets.iterator();it.hasNext();){
			facetNode = (Node)it.next();
			count = facetNode.getText();
			colName = facetNode.selectSingleNode("@name").getStringValue();
			itemFound = false;
			for(Iterator itm=arkMap.values().iterator();itm.hasNext();){
				colObj = (JSONObject)itm.next();
				tmpColName = (String)colObj.get("colName");
				if(tmpColName!=null && tmpColName.indexOf("+OR+")<0 && tmpColName.indexOf("(")<0 && colName.equals(tmpColName) && !tmpColName.equals("mscl_melanesianArchive")){
					colObj.put("count", count);
					titleMap.put(colObj.get("title"), colObj);
					itemFound = true;
					break;
				}	
			}
			if(itemFound){
				titleMap.put(colObj.get("title"), colObj);
				arkMap.remove(colObj.get("subject"));
			}
		}
	}
	
	public static Map<String, JSONObject> getCollectionsMap(String solrBase, String solrCore) throws MalformedURLException, DocumentException{

		String subject = null;
		String title = null;
		String hasItems = null;
		String colName = null;
		Node colNode = null;
		JSONObject colData = null;
		Map<String, JSONObject> collectionsMap = new HashMap<String, JSONObject>();
		String params = "q=category:bb36527497&rows=1000&f.Facet_Date.facet.limit=0&ds=jdbc/dams4&wt=xml&fl=attrib+OR+subject";
		Document colDoc =  getSOLRResult(solrBase, solrCore, params);
		for(Iterator it=colDoc.selectNodes("/response/result/doc").iterator();it.hasNext();){
			colData = new JSONObject();
			colNode = (Node)it.next();
			subject = colNode.selectSingleNode("str[@name='subject']").getText();
			title = getAttributeValue(colNode.selectSingleNode("arr[@name='attrib']/str[starts-with(.,'" + Constants.ARK_DAMS_TITLE + " xdre:title')]"));
			hasItems = getAttributeValue(colNode.selectSingleNode("arr[@name='attrib']/str[starts-with(.,'bb1502546x rdf:has_item')]"));
			colName = getAttributeValue(colNode.selectSingleNode("arr[@name='attrib']/str[starts-with(.,'bb26288486 xdre:collectionName')]"));

			title = (title!=null&&title.charAt(0)=='"'?title.substring(1,title.length()-1):title);
			colName = (colName!=null&&colName.charAt(0)=='"'?colName.substring(1,colName.length()-1):colName);
			colData.put("subject", subject);
			colData.put("title", title);
			colData.put("hasItems", (hasItems==null?null:JSONValue.parse(hasItems)));
			colData.put("colName", colName);
			collectionsMap.put(subject, colData);
		}
		return collectionsMap;
	}
	
	public static String getAttributeValue(Node node){
		if(node == null)
			return null;
		String val = node.getText();
		if(val != null){
			val = val.substring(val.indexOf("|||") + 3);
		}
		return val;
	}
	
	public static Document getSOLRResult(String solrBase, String solrCore, String params) throws DocumentException, MalformedURLException{
		URL url = new URL(solrBase + solrCore + "/select?" + params);
		SAXReader reader = new SAXReader();
		return reader.read(url);
	}
}
