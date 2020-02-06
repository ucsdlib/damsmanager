package edu.ucsd.library.xdre.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;


 /**
 * Class CollectionReleaseController, the model/controller for collection release view
 *
 * @author lsitu@ucsd.edu
 */
public class CollectionReleaseController implements Controller {

	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response)
			throws Exception {
		String ds = request.getParameter("ts");
		String collectionId =  request.getParameter("category");
		String collectionToMerge =  request.getParameter("collectionToMerge");
		String collOption =  request.getParameter("option");
		String message = request.getParameter("message");


		HttpSession session = request.getSession();	
		session.setAttribute("user", request.getRemoteUser());

		DAMSClient damsClient = null;
		Map dataMap = new HashMap();
		try{
			if(ds == null)
				ds = Constants.DEFAULT_TRIPLESTORE;

			
			damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
			damsClient.setTripleStore(ds);

			Map<String, String> collectionToMergeMap = damsClient.listCollections();
			Map<String, String> collectionMap = null;
			if (StringUtils.isNotBlank(collOption) && collOption.equalsIgnoreCase("all")) {
				collectionMap = collectionToMergeMap;
				collOption = "";
			} else {
				collectionMap = getNonPublicCollections(collectionToMergeMap, damsClient);
				collOption = "All";
			}

			List<String> tsSrcs = damsClient.listTripleStores();

			message = (!StringUtils.isBlank(message) ||
					StringUtils.isBlank(collectionId)) ? message : (String)session.getAttribute("message");
			request.getSession().removeAttribute("message");

			dataMap.put("categories", collectionMap);
			dataMap.put("category", collectionId);
			dataMap.put("message", message);
			dataMap.put("triplestore", ds);
			dataMap.put("triplestores", tsSrcs);
			dataMap.put("collectionsToMerge", collectionToMergeMap);
			dataMap.put("collectionToMerge", collectionToMerge);
			dataMap.put("collOption", collOption);
		
		} catch (Exception e) {
			e.printStackTrace();
			message += e.getMessage();
		}finally{
			if(damsClient != null)
				damsClient.close();
		}
		return new ModelAndView("collectionRelease", "model", dataMap);
	}
	
	private <K, V> Map<K, V> getNonPublicCollections(Map<K, V> collections, DAMSClient damsClient)
			throws Exception{
		Map<K, V> collMap = cloneMap(collections);
		Map<V, K> revertedMap = revertMap(collections);
		String sparql = "SELECT ?subject WHERE {?subject <" + Constants.NS_PREFIX_MAP.get("dams") + "visibility> '\"public\"'}";
		List<Map<String, String>> results = damsClient.sparqlLookup(sparql);
		for (Map<String, String> result : results) {
			String subject = result.get("subject");
			if (collMap.containsValue(subject)) {
				collMap.remove(revertedMap.get(subject));
			}
		}
		return collMap;
	}
	
	public static <K,V> Map<V, K> revertMap(Map<K, V> toRevert) {
	    Map<V, K> reverted = new HashMap<V, K>();
	    for(K k: toRevert.keySet()){
	        reverted.put(toRevert.get(k), k);
	    }
	    return reverted;
	}
	
	private <K,V> Map<K, V> cloneMap(Map<K, V> toClone) {
	    Map<K, V> reverted = new TreeMap<K, V>();
	    for(K k: toClone.keySet()){
	        reverted.put(k, toClone.get(k));
	    }
	    return reverted;
	}
 }
