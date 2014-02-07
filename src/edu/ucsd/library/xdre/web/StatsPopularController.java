package edu.ucsd.library.xdre.web;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.statistic.analyzer.Statistics;
import edu.ucsd.library.xdre.statistic.beans.DAMSItem;
import edu.ucsd.library.xdre.statistic.report.StatsUsage;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;


 /**
 * Class StatsPopularController
 *
 * @author lsitu@ucsd.edu
 */
public class StatsPopularController implements Controller {

	public static final String[] APPS = {"pas", "cas"};
	private static Map<String, String> collectionMap = null;
	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		Map<String, Object> model = new HashMap<String, Object>();
		
		String subject = null;
		String subjects = "";
		String message = "";
		boolean isCas = false;
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs= null;
		List<DAMSItem> dlpObjects = new ArrayList<DAMSItem>();
		String templete = "statsPopular";
		
		if(request.isUserInRole(Constants.CURATOR_ROLE)){
			isCas = true;
		}
		try {
			
			con = Constants.DAMS_DATA_SOURCE.getConnection();
			ps = con.prepareStatement(StatsUsage.OBJECT_POPULARITY_QUERY);
			ps.setString(1, "pas");
			ps.setFetchSize(50);
			rs= ps.executeQuery();
			while(rs.next()){
				subject = rs.getString("subject");
				dlpObjects.add(new DAMSItem(subject, rs.getInt("num_access"), rs.getInt("num_view")));
				subjects += (subjects.length()>0?"+OR+":"") + subject;
			}
		} catch (SQLException e) {
			e.printStackTrace();
			message += "InternalError: " + e.getMessage();
		} catch (Exception e) {
			e.printStackTrace();
			message += "Error: " + e.getMessage();
		}finally{
			Statistics.close(rs);
			Statistics.close(ps);
			Statistics.close(con);
			rs = null;
			ps = null;
			con = null;
		}
		
		String data = "";
		String solrQuery = Constants.SOLR_URL_BASE + "/select?q=id:(" + subjects+ ")&fl=id+title_tesim+collections_tesim+collection_name_tesim+discover_access_group_ssim+*files_tesim+component_count_isi&rows=200&wt=json";
		DAMSClient damsClient = null;
		String clusterHost = "//" + Constants.CLUSTER_HOST_NAME + ".ucsd.edu";
		try {
			damsClient = DAMSClient.getInstance();
			data = damsClient.getContentBodyAsString(solrQuery);
			JSONObject json = (JSONObject) JSONValue.parse(data);

			JSONArray resultDoc = (JSONArray) ((JSONObject)json.get("response")).get("docs");
			
			Object[] dlpItems = dlpObjects.toArray();
			DAMSItem dlpObject = null;
			dlpObjects = new ArrayList<DAMSItem>();
			for(int i=0; i<dlpItems.length; i++){
				dlpObject = (DAMSItem)dlpItems[i];
				dlpObject.setClusterHost(clusterHost);
				updateData(dlpObject, resultDoc, i);

				dlpObjects.add(dlpObject);
				if(dlpObjects.size() == 100)
					break;
			}
		} catch (Exception e) {
			e.printStackTrace();
			message += e.getMessage() + "\n";
		}finally{
			if(damsClient != null){
				damsClient.close();
				damsClient = null;
			}
		}
		
		model.put("isCas", isCas);
		model.put("items", dlpObjects);
		model.put("message", message);
		model.put("clusterHost", clusterHost);
		
		return new ModelAndView(templete, "model", model);
    }
	
	private void updateData(DAMSItem dlpObject, JSONArray resultDoc, int idx){
		if(resultDoc.size() > idx){
			if(processObject(dlpObject, (JSONObject)resultDoc.get(idx)))
				return;
		}

		for(Iterator<JSONObject> it=resultDoc.iterator(); it.hasNext();){
			if(processObject(dlpObject, it.next())){
				return;
			}
		}
	}
	
	private boolean processObject(DAMSItem dlpObject, JSONObject jsonObj){
		Object obj = null;
		String subject = dlpObject.getSubject();
		if(subject.endsWith((String) jsonObj.get("id"))){
			String title = "";
			obj = jsonObj.get("title_tesim");
			if(obj instanceof JSONArray){
				for(Iterator<String> it=((JSONArray)obj).iterator(); it.hasNext();){
					title += (title.length()>0?": ":"") + it.next();
				}
			}else {
				title += (title.length()>0?": ":"") + obj;
			}
			dlpObject.setTitle(title);
			obj = jsonObj.get("collections_tesim");
			String value = null;
			if(obj instanceof JSONArray){
				for(Iterator<String> it1=((JSONArray)obj).iterator(); it1.hasNext();){
					value = it1.next();
					break;
				}
				dlpObject.setCollection(value);
			}
			obj = jsonObj.get("collection_name_tesim");
			if(obj instanceof JSONArray){
				for(Iterator<String> it1=((JSONArray)obj).iterator(); it1.hasNext();){
					value = it1.next();
					break;
				}
				dlpObject.setCollectionTitle(value);
			}
			
			obj = jsonObj.get("discover_access_group_ssim");
			if(obj != null)
				value = ((JSONArray)obj).indexOf("public")>=0?"public":((JSONArray)obj).indexOf("local")>=0?"local":"curator";
			else
				value = "curator";
			dlpObject.setStateAndView(value);
			
			long comCount = 0;
			Object coms = jsonObj.get("component_count_isi");
			if( coms != null){
				comCount = ((Long)coms).longValue();
				if(comCount > 0) {
					int i = 1;
					do{
						String comFileName = "component_" + i + "_files_tesim";
						obj = jsonObj.get(comFileName);
						if(obj == null)
							obj = new JSONArray ();
					}while( ((JSONArray)obj).toJSONString().indexOf("\"image-thumbnail\"") < 0 && ++i < comCount );
						
				}else
					obj = jsonObj.get("files_tesim");
			}else
				obj = jsonObj.get("files_tesim");
			String icon = getIcon(subject, (JSONArray)obj);
			if(icon != null){
				icon = (comCount>0?"_"+comCount:"") + "_" + icon;
			}
			dlpObject.setIcon( icon );
			return true;
		}
		return false;
	}
	
	public static String getIcon(String subject, JSONArray files){
		String icon = null;
		String fJson = null;
		JSONObject file = null;
		String fid = null;
		String use = null;
		
		if(files != null){
			for (int i=0; i<files.size(); i++){
				fJson = (String)files.get(i);
				if(fJson.indexOf("\"image-thumbnail\"") > 0){
					file = (JSONObject)JSONValue.parse(fJson);
					
					use = (String)file.get("use");
					fid = (String)file.get("id");
					if(use.equals("image-thumbnail")){
						return fid;
					}
				}
			}
		}
		return icon;
	}
	
	public static boolean isAudio(String fileName){
		return fileName.endsWith(".mp3") || fileName.endsWith(".wav");
	}
	
	public static synchronized Map<String, String> getCollectionMap(){
		if(collectionMap == null || collectionMap.size() == 0){
			collectionMap = new HashMap<String, String>();
			String colQuery = Constants.SOLR_URL_BASE + "/select?q=has_model_ssim:\"info:fedora/afmodel:DamsProvenanceCollection\"+OR+has_model_ssim:\"info:fedora/afmodel:DamsProvenanceCollectionPart\"+OR+has_model_ssim:\"info:fedora/afmodel:DamsAssembledCollection\"+OR+has_model_ssim:\"info:fedora/afmodel:DamsCollection\"&fl=id+title_tesim&rows=500&wt=json";
			
			JSONObject jsonObj = null;
			InputStream in = null;
			Reader reader = null;
			URL url = null;
			try {
				url = new URL(colQuery);
				in = url.openStream();
				reader = new InputStreamReader(in);
				jsonObj = (JSONObject) JSONValue.parse(reader);
				JSONArray jsonArr = (JSONArray) ((JSONObject)jsonObj.get("response")).get("docs");
				for(Iterator<JSONObject> it=jsonArr.iterator(); it.hasNext();){
					jsonObj = it.next();
					collectionMap.put(getValue(jsonObj, "id"), getValue(jsonObj, "title_tesim"));
				}
			} catch (MalformedURLException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}finally{
				if(in != null){
					try {
						in.close();
					} catch (IOException e) {}
				}
				if(reader != null){
					try {
						reader.close();
					} catch (IOException e) {}
				}
			}
		}
		return collectionMap;
	}
	
	private static String getValue(JSONObject jsonObj, String key){
		String value = null;
		Object obj = jsonObj.get(key);
		if(obj instanceof JSONArray){
			for(Iterator<String> it1=((JSONArray)obj).iterator(); it1.hasNext();){
				value = (String)it1.next();
				break;
			}
		}else{
			value = (String)obj;
		}
		return value;
	}
}