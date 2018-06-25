package edu.ucsd.library.xdre.statistic.analyzer;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;

import edu.ucsd.library.xdre.statistic.beans.FileDownloadCounter;
import edu.ucsd.library.xdre.statistic.beans.StatsObjectAccess;
import edu.ucsd.library.xdre.statistic.beans.ObjectCounter;
import edu.ucsd.library.xdre.utils.Constants;

/**
 * Class DAMStatistic
 *
 * @author lsitu@ucsd.edu
 */
public class DAMStatistic extends Statistics{
	private static Logger log = Logger.getLogger(DAMStatistic.class);
	
	public static String NEW_FORM_URL = "/new";
	public static String EDIT_FORM_URL = "/edit";
	
	protected int numSearch =0;
	protected int numBrowse =0;
	protected int numColPage =0;
	protected int numHomePage =0;
	protected Map<String, StatsObjectAccess> itemsMap = null;
	protected Map<String, StatsObjectAccess> itemsMapPrivate = null;
	protected Map<String, Integer> keywordsMap = null;
	protected Map<String, Integer> phrasesMap = null;
	protected Map<String, Integer> collsAccessMap = null;
	protected Map<String, String> collsMap = null;
	protected List<String> derivativeList = null;
	
	public DAMStatistic(String appName){
		super(appName);
		itemsMap = new TreeMap<String, StatsObjectAccess>();
		itemsMapPrivate = new TreeMap<String, StatsObjectAccess>();
		keywordsMap = new HashMap<String, Integer>();
		phrasesMap = new HashMap<String, Integer>();
		collsAccessMap = new HashMap<String, Integer>();
		derivativeList = getDerivativesList();
		log.info("DAMS Statistics derivativs: " + Arrays.toString(derivativeList.toArray()));
	}
	
	public Map<String, String> getCollsMap() {
		return collsMap;
	}

	public void setCollsMap(Map<String, String> collsMap) {
		this.collsMap = collsMap;
	}

	public Map<String, Integer> getKeywordsMap() {
		return keywordsMap;
	}

	/**
	 * Get the items counters for public access
	 * @return
	 */
	public Map<String, StatsObjectAccess> getItemsMap() {
		return itemsMap;
	}

	/**
	 * Get the items counters for private access
	 * @return
	 */
	public Map<String, StatsObjectAccess> getPrivateItemsMap() {
		return itemsMapPrivate;
	}

	public void addAccess(String uri) throws UnsupportedEncodingException{
		
		int idx = uri.indexOf("?");
		String uriPart = null;
		String paramsPart = null;
		if(idx > 0){
			uriPart = uri.substring(0, idx);
			if(idx+1 < uri.length())
				paramsPart = uri.substring(idx + 1);
		}else
			uriPart = uri;
		String[] parts = uriPart.substring(1).split("/");
		if(parts.length >=1 && parts.length <=4){
			numAccess++;
			if ( parts.length == 1 ){
				// home page access.
				numHomePage++;
			}else if (uriPart.indexOf("/collections") > 0) {
				// Collection home page: /dc/collections, /dc/dlp/collections, /dc/rci/collections
				numColPage++;
			}else if (parts[1].equalsIgnoreCase("search")) {
				
				if(parts.length == 2){
					try{
					
					// search & browse
					String id = null;
					int page = -1;
					List<String[]> searchParams = new ArrayList<String[]>();
					List<String[]> browseParams = new ArrayList<String[]>();
					boolean isColAccess = false;
					boolean isBrowse = false;
					if(paramsPart != null){
						String[] params = paramsPart.split("&");
						
						String[] tokens = null;
						for (int i=0; i<params.length; i++){
							if(params[i] != null){
								params[i] = URLDecoder.decode(params[i].trim(), "UTF-8");
								tokens = params[i].split("=");
								tokens[0] = tokens[0].trim();
								//System.out.println(tokens[0] + " => " + tokens[1]);
								if( tokens[0].equals("q") ){
									searchParams.add(tokens);
								}else if( tokens[0].startsWith("f[") || tokens[0].startsWith("f%5B")){
									browseParams.add(tokens);
									if( tokens[0].indexOf("collection_sim") > 0 )
										isColAccess = true;
								}else if (tokens[0].equals("id")){
									id = tokens[1].trim();
								}else if (tokens[0].equals("page") && tokens.length > 1){
									try{
										page = Integer.parseInt(tokens[1].trim());
									}catch(NumberFormatException e){}
								}
							}
						}
					}
					if(isColAccess && id != null){
						//Collection access
						isBrowse = true;
						numBrowse++;
						//increaseCollectionAccess(id, collsAccessMap, collsMap);
					}else if(browseParams.size() > 0 || page > 0){
						//Facet browse
						numBrowse++;
						isBrowse = true;
					}else{
						//Search
						numSearch++;
					}
					
					if(searchParams.size() > 0 && !isBrowse){
						String[] tokens = searchParams.get(0);
						String keywords = tokens.length==2?tokens[1]:null;
						if(keywords != null && keywords.length() > 0){
							try {
								keywords = URLDecoder.decode(keywords, "UTF-8");
								parseKeywords(keywords, keywordsMap, phrasesMap);
							} catch (UnsupportedEncodingException e) {
								log.info("Unsupported UTF-8 encoding for keywords: '" + keywords + "' in " + uri );
								e.printStackTrace();
							} catch (Exception e) {
								log.info("Invalid URL encoding for keywords: '" + keywords + "' in " + uri );
								e.printStackTrace();
							}
						}
					}
					}catch (IllegalArgumentException e){
						log.warn("Invalid encoding: " + uri, e);
					}
				}else if (parts.length >= 3 && parts[2].equals("facet")){
					// Facet browse: /dc/search/facet/subject_topic_sim?facet.sort=index
					numBrowse++;
				}else
					log.info("DAMS stats unknown search uri: " + uri);
				
			}else if ((parts[1].endsWith("collections") || parts[1].endsWith("collection")) && parts.length > 2 && parts[2] != null) {
				// Collections: /dc/dams_collections/bbxxxxxxxx?counter=1 or /dc/collection/bbxxxxxxxx?counter=1
				increaseCollectionAccess(parts[2], collsAccessMap, collsMap);
			}else {
				// other pages? skip for now.
				log.info("DAMS stats access skip uri: " + uri);
			}
		}else
			log.info("DAMS stats invalid uri: " + uri);
	}
	
	public void addObject(String uri, boolean isPrivateAccess, String clientIp){
		String subjectId = "";
		String fileName = "";
		//String[] parts = uri.replace("?", "&").split("&");
		String uriPart = null;
		String paramsPart = null;
		int idx = uri.indexOf("?");
		if(idx > 0){
			uriPart = uri.substring(0, idx);
			if(idx + 1 < uri.length())
				paramsPart = uri.substring(idx + 1);
		}else
			uriPart = uri;

		String[] parts = uriPart.substring(1).split("/");
		if(parts.length >= 3 && parts.length <=5 && parts[2] !=null)
			// /dc/object/oid/_cid_fid/download
			subjectId = parts[2];
		else{
			log.warn("DAMS stats unknown uri: " + uri);
			return;
		}
		
		if(StringUtils.isBlank(clientIp)) {
			log.warn("Invalid client IP " + clientIp + " in request " + uri + ".");
			return;
		}		

		if(parts.length >= 4 && parts[3] != null && parts[3].startsWith("_"))
			fileName = parts[3];
		
		if(subjectId == null || subjectId.length()!= 10){
			log.warn("Invalid subject " + subjectId + ": " + uri);
			return;
		}

		Map<String, StatsObjectAccess> iMap = itemsMap;
		if (isPrivateAccess)
			iMap = itemsMapPrivate;

		StatsObjectAccess objAccess = iMap.get(subjectId);
		if(objAccess == null){
			objAccess = new StatsObjectAccess(subjectId);
			iMap.put(subjectId, objAccess);
		}

		// differentiate the counts for file download and object access/hits
		int file_name_idx = fileName.lastIndexOf("_");
		String fileSubfix = file_name_idx >= 0 ? fileName.substring(file_name_idx) : "";

		if (uri.indexOf("/download") > 0) {
			objAccess.increaseFileDownloads(fileName, clientIp);
		} else if (StringUtils.isNotBlank(fileName) && derivativeList.indexOf(fileSubfix) < 0) {
			// count all source files as download
			objAccess.increaseFileDownloads(fileName, clientIp);
		} else {
			objAccess.increaseObjectAccess(fileName, clientIp);
		}
	}
	
	private List<String> getDerivativesList() {
		// DEFAULT_DERIVATIVES: 2,3,4,5,6,7
		List<String> derSufixes = new ArrayList<String>(Arrays.asList(Constants.DEFAULT_DERIVATIVES.split(",")));
		for (int i=0; i < derSufixes.size(); i++) {
			derSufixes.set(i, "_" + derSufixes.get(i) + ".jpg");
		}

		String[] dersAdditional = {"_2.mp3", "_2.mp4"};
		derSufixes.addAll(Arrays.asList(dersAdditional));
		return derSufixes;
	}

	public String getParamValue(String param){
		String[] pair = param.split("=");
		if(pair.length > 1 && pair[1] != null)
			return pair[1].trim();
		else
			return "";
	}
	
	public void export(Connection con) throws Exception{
		int nextId = getNextId(con);
		int returnValue = -1;
		PreparedStatement ps = null;
		
		//Update record for the calendar date
		if(update){
			try{
				ps = con.prepareStatement(WEB_STATS_DELETE_RECORD);
				ps.setString(1, dateFormat.format(calendar.getTime()));
				ps.setString(2, appName);
				returnValue = ps.executeUpdate();
				log.info("Deleted " + appName + " statistics record for date " + dateFormat.format(calendar.getTime()));
			}finally{
				Statistics.close(ps);
				ps = null;
			}
		} else {
			if(isRecordExist(con, calendar.getTime())){
				log.debug(appName + " statistics record for date " + dateFormat.format(calendar.getTime()) + " exists.");
				return;
			}
		}
		//WEB_STATS insert
		try{
			ps = con.prepareStatement(WEB_STATS_INSERT);
			ps.setInt(1, nextId);
			ps.setDate(2, java.sql.Date.valueOf(dateFormat.format(calendar.getTime())));
			ps.setInt(3, numAccess);
			ps.setString(4, appName);
			returnValue = ps.executeUpdate();
		}finally{
			Statistics.close(ps);
			ps = null;
		}
		
		//STATS_DLP insert
		try{
			ps = con.prepareStatement(STATS_DLP_INSERT);
			ps.setInt(1, nextId);
			ps.setInt(2, numSearch);
			ps.setInt(3, numBrowse);
			ps.setInt(4, numColPage);
			ps.setInt(5, numHomePage);
			returnValue = ps.executeUpdate();
		}finally{
			Statistics.close(ps);
			ps = null;
		}
		
		//STATS_DLP_COLLECTION_ACCESS_INSERT insert
		try{
			int numAccess = 0;
			String key = null;
			ps = con.prepareStatement(STATS_DLP_COLLECTION_ACCESS_INSERT);
			Iterator<String> it = collsAccessMap.keySet().iterator();
			while(it.hasNext()){
				key = (String) it.next();
				numAccess = collsAccessMap.get(key);
				ps.setInt(1, nextId);
				ps.setString(2, key);
				ps.setInt(3, numAccess);
				returnValue = ps.executeUpdate();
				ps.clearParameters();
			}
		}finally{
			Statistics.close(ps);
			ps = null;
		}
		
		//STATS_DLP_OBJECT_ACCESS_INSERT insert
		try{
			// eliminate the counts from curator access with redirects from public access
			for (String key : itemsMapPrivate.keySet()) {
				if (itemsMap.containsKey(key)) {
					StatsObjectAccess pubObjAccess = itemsMap.get(key);
					StatsObjectAccess privObjAccess = itemsMapPrivate.get(key);
					for (String ip : privObjAccess.getObjectCounters().keySet()) {
						ObjectCounter pubCounter = pubObjAccess.getCounter(ip);
						ObjectCounter privCounter = privObjAccess.getCounter(ip);
						if (pubCounter != null) {
							int pubAccess = pubCounter.getAccess() - privCounter.getAccess();
							pubCounter.setAccess(pubAccess > 0 ? pubAccess : 0);

							int pubViews = pubCounter.getView() - privCounter.getView();
							pubCounter.setView(pubViews > 0 ? pubViews : 0);
						}
					}
				}
			}
			ps = con.prepareStatement(STATS_DLP_OBJECT_ACCESS_INSERT);
			persistObjectAccessStats (nextId, ps, itemsMap, false);
			persistObjectAccessStats (nextId, ps, itemsMapPrivate, true);
		}finally{
			Statistics.close(ps);
			ps = null;
		}

		//STATS_FILE_DOWNLOAD_INSERT insert
		try{
			ps = con.prepareStatement(STATS_FILE_DOWNLOAD_INSERT);
			persistFileDownloadStats (nextId, ps, itemsMap, false);
			persistFileDownloadStats (nextId, ps, itemsMapPrivate, true);
		}finally{
			Statistics.close(ps);
			ps = null;
		}
		
		//Keywords/Phrases STATS_DLC_KEYWORDS_INSERT insert
		try{
			Integer counter = null;
			String key = null;
			ps = con.prepareStatement(STATS_DLC_KEYWORDS_INSERT);
			Iterator<String> it = keywordsMap.keySet().iterator();
			while(it.hasNext()){
				key = (String) it.next();
				counter = keywordsMap.get(key);
				ps.setInt(1, nextId);
				ps.setString(2, StringEscapeUtils.escapeJava(key).replace("\\\"", "\""));
				ps.setInt(3, counter.intValue());
				ps.setString(4, "keyword");
				returnValue = ps.executeUpdate();
				ps.clearParameters();
			}
			it = phrasesMap.keySet().iterator();
			while(it.hasNext()){
				key = (String) it.next();
				counter = phrasesMap.get(key);
				ps.setInt(1, nextId);
				ps.setString(2, StringEscapeUtils.escapeJava(key).replace("\\\"", "\""));
				ps.setInt(3, counter.intValue());
				ps.setString(4, "phrase");
				returnValue = ps.executeUpdate();
				ps.clearParameters();
			}
		}finally{
			Statistics.close(ps);
			ps = null;
		}
		log.info("Inserted " + appName + " statistics record for " + dateFormat.format(calendar.getTime()));
	}

	private int persistObjectAccessStats (int statsId, PreparedStatement ps, Map<String, StatsObjectAccess> itemsMap, boolean isPrivate) {
		int returnValue = 0;
		String key;
		Iterator<String> it = itemsMap.keySet().iterator();
		while(it.hasNext()){
			key = (String) it.next();
			StatsObjectAccess objCounter = itemsMap.get(key);
			try {
				String sid = objCounter.getSubjectId();
				Document doc = Statistics.cacheGet(sid);
				if (doc == null) {
					doc = Statistics.getRecordForStats(sid);
					Statistics.cacheAdd(sid, doc);
				}
				
				String unitId = getUnitCode(doc);;
				String colId = getCollection(doc);
				ps.setInt(1, statsId);
				ps.setBoolean(2, isPrivate);
				ps.setString(3, unitId);
				ps.setString(4, colId);
				ps.setString(5, sid);
				returnValue = objCounter.export(ps);
				ps.clearParameters();
			}catch(Exception ex) {
				ex.printStackTrace();
				log.info("Stats insert failed: " + objCounter.toString() );
			}
		}
		return returnValue;
	}

	private int persistFileDownloadStats (int statsId, PreparedStatement ps, Map<String, StatsObjectAccess> itemsMap, boolean isPrivate)
			throws Exception {
		int returnValue = 0;
		String key;
		Map<String, FileDownloadCounter> fileDownloads;
		Iterator<String> it = itemsMap.keySet().iterator();
		while(it.hasNext()){
			key = (String) it.next();
			StatsObjectAccess objectAccess = itemsMap.get(key);
			String sid = objectAccess.getSubjectId();
			Document doc = Statistics.cacheGet(sid);
			if (doc == null) {
				doc = Statistics.getRecordForStats(sid);
				Statistics.cacheAdd(sid, doc);
			}

			fileDownloads = itemsMap.get(key).getFileDownloads();
			for (FileDownloadCounter fCounter : fileDownloads.values()) {
				try {
					String unitId = getUnitCode(doc);;
					String colId = getCollection(doc);
					ps.setInt(1, statsId);
					ps.setBoolean(2, isPrivate);
					ps.setString(3, unitId);
					ps.setString(4, colId);
					ps.setString(5, sid);
					returnValue = fCounter.export(ps);
					ps.clearParameters();
				}catch(Exception ex) {
					ex.printStackTrace();
					log.info("Stats insert failed: " + fCounter.toString() );
				}
			}
		}
		return returnValue;
	}

	public String getUnitCode(Document doc) {
		if (doc != null) {
			Node node = doc.selectSingleNode("//doc/arr[@name='unit_code_tesim']/str");
			if (node != null) {
				String id = node.getStringValue();
				return id.substring(id.lastIndexOf("/") + 1);
			}
		}
		return null;
	}

	public String getCollection(Document doc) {
		if (doc != null) {
			Node node = doc.selectSingleNode("//doc/arr[@name='collections_tesim']/str");
			if (node != null) {
				return node.getText();
			}
		}
		return null;
	}
	
	public void print(){
		System.out.println(appName + " - ");
		System.out.println("	Access: " + numAccess);
		System.out.println("	Search: " + numSearch);
		System.out.println("	Browse: " + numBrowse);
		System.out.println("	Collection Page: " + numColPage);
		
		String key = null;
		Iterator<String> it = itemsMap.keySet().iterator();
		while(it.hasNext()){
			key = (String) it.next();
			System.out.println(itemsMap.get(key));
		}
		
		if(keywordsMap != null){
			System.out.println("\nKeywords - ");
			it = keywordsMap.keySet().iterator();
			while(it.hasNext()){
				key = (String) it.next();
				System.out.println(key + " -> " + keywordsMap.get(key));
			}
		}
		if(phrasesMap != null){
			System.out.println("\nPhrases - ");
			it = phrasesMap.keySet().iterator();
			while(it.hasNext()){
				key = (String) it.next();
				System.out.println(key + " -> " + phrasesMap.get(key));
			}
		}
		if(collsAccessMap != null){
			System.out.println("\nCollections Access - ");
			it = collsAccessMap.keySet().iterator();
			while(it.hasNext()){
				key = (String) it.next();
				System.out.println(key + " -> " + collsAccessMap.get(key));
			}
		}
	}
	
	
	public static void parseKeywords(String keywordsStr, Map<String, Integer> keywordsMap, Map<String, Integer> phrasesMap) throws UnsupportedEncodingException{
		String tmp = "";
		int i = 0;
		keywordsStr = URLDecoder.decode(keywordsStr, "UTF-8");
		char[] str = keywordsStr.trim().toCharArray();
		int len = str.length;
		while(i < len){
			if(str[i]==' '){
				if(i+3 < len && str[i+1]=='O' && str[i+2]=='R' && str[i+3]==' '){
					if(tmp.length() > 0){
						addKeyword(tmp, keywordsMap, phrasesMap);
						tmp = "";
					}
					i += 2;
				}else if(i+4 < len && str[i+1]=='A' && str[i+2]=='N'  && str[i+3]=='D' && str[i+4]==' '){
					if(tmp.length() > 0){
						addKeyword(tmp, keywordsMap, phrasesMap);
						tmp = "";
					}
					i += 3;
				}else if(i+4 < len && str[i+1]=='N' && str[i+2]=='O'  && str[i+3]=='T' && str[i+4]==' '){
					if(tmp.length() > 0){
						addKeyword(tmp, keywordsMap, phrasesMap);
						tmp = "";
					}
					i += 3;
				}else if(i+1 < len && (str[i+1]=='\"' || (str[i-1]=='\"'&&(i>2 && str[i-2]!='\\')))){ //Phrase
					if(tmp.length() > 0){
						addKeyword(tmp, keywordsMap, phrasesMap);
						tmp = "";
					}
					//i += 1;
				}else
					tmp += str[i];
			}else{
				tmp += str[i];
			}
			
			i += 1;
		}

		if(tmp.length() > 0)
			addKeyword(tmp, keywordsMap, phrasesMap);
	}
	
	public static void addKeyword(String keyword, Map<String, Integer> keywordsMap, Map<String, Integer> phrasesMap){
		keyword = keyword.trim();
		if(keyword.length()>0){
			if(keyword.startsWith("\"") && keyword.endsWith("\"")){
				if(keyword.equals("\"\"") || keyword.equals("\""))
					keyword = "";
				else {
					keyword = keyword.substring(1, keyword.length()-1).trim();
					if(keyword.indexOf(" ") > 0)
						increaseWordCount(keyword, phrasesMap);
					else
						increaseWordCount(keyword, keywordsMap);
				}
			}else{
				increaseWordCount(keyword, keywordsMap);
			}
		}
	}
	
	public static synchronized void increaseWordCount(String keyword, Map<String, Integer> wordsMap){
			//keyword = RDFGenerator.toUnicode(keyword);
			Integer counter = wordsMap.get(keyword);
			if(counter == null){
				counter = new Integer(1);
			}else
				counter = new Integer(counter.intValue()+1);
			wordsMap.put(keyword, counter);
	}
	
	/**
	 * Count collections access
	 * @param uri
	 * @param collsAccessMap
	 * @param collsMap
	 */
	public static void increaseCollectionAccess(String colid, Map<String, Integer> collsAccessMap, Map<String, String> collsMap){
		Integer count = null;
		if(colid != null){
			colid = colid.trim().replaceAll("[;:,?'\" ]*", "");
			// Only counting the ark ids for now
			if(colid.length() == 10){
				count = collsAccessMap.get(colid);
				if(count == null)
					collsAccessMap.put(colid, 1);
				else
					collsAccessMap.put(colid, ++count);
			}
		}
	}
}
