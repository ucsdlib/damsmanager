package edu.ucsd.library.xdre.statistic.analyzer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;

/**
 * Class DAMStatistic
 *
 * @author lsitu@ucsd.edu
 */
public class DAMStatistic extends Statistics{
	private static Logger log = Logger.getLogger(DAMStatistic.class);
	
	protected int numSearch =0;
	protected int numBrowse =0;
	protected int numColPage =0;
	protected int numHomePage =0;
	protected Map<String, ObjectCounter> itemsMap = null;
	protected Map<String, Integer> keywordsMap = null;
	protected Map<String, Integer> phrasesMap = null;
	protected Map<String, Integer> collsAccessMap = null;
	protected Map<String, String> collsMap = null;
	
	public DAMStatistic(String appName){
		super(appName);
		itemsMap = new TreeMap<String, ObjectCounter>();
		keywordsMap = new HashMap<String, Integer>();
		phrasesMap = new HashMap<String, Integer>();
		collsAccessMap = new HashMap<String, Integer>();
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

	public void addAccess(String uri) throws UnsupportedEncodingException{
		numAccess++;
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
			if ( parts.length == 1 ){
				// home page access.
				numHomePage++;
			}else if (uriPart.indexOf("/collections") > 0) {
				// Collection home page: /dc/collections, /dc/dlp/collections, /dc/rci/collections
				numColPage++;
			}else if (parts[1].equalsIgnoreCase("search")) {
				
				if(parts.length == 2){
					
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
								}else if( tokens[0].startsWith("f[") || tokens[0].startsWith("f%5B") ){
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
						increaseCollectionAccess(id, collsAccessMap, collsMap);
					}else if(browseParams.size() > 0 || page > 0){
						//Facet browse
						numBrowse++;
						isBrowse = true;
					}else
						//Search
						numSearch++;
					
					if(searchParams.size() > 0 && isBrowse){
						String keywords = searchParams.get(0)[1];
						if(keywords != null && keywords.length() > 0)
						try {
							keywords = URLDecoder.decode(keywords, "UTF-8");
							parseKeywords(keywords, keywordsMap, phrasesMap);
						} catch (UnsupportedEncodingException e) {
							System.out.println("Unsupported UTF-8 encoding for keywords: '" + keywords + "' in " + uri );
							e.printStackTrace();
						} catch (Exception e) {
							System.out.println("Invalid URL encoding for keywords: '" + keywords + "' in " + uri );
							e.printStackTrace();
						}
					}
				}else if (parts.length >= 3 && parts[2].equals("facet")){
					// Facet browse: /dc/search/facet/subject_topic_sim?facet.sort=index
					numBrowse++;
				}else
					System.out.println("DAMS stats unknown search uri: " + uri);
				
			}else if (parts[1].equalsIgnoreCase("dams_collections") && parts.length > 2 && parts[2] != null) {
				// Collections: /dc/dams_collections/bbxxxxxxxx?counter=1
				increaseCollectionAccess(parts[2], collsAccessMap, collsMap);
			}else {
				// other pages? skip for now.
				System.out.println("DAMS stats access skip uri: " + uri);
			}
		}else
			System.out.println("DAMS stats invalid uri: " + uri);
	}
	
	public void addObject(String uri){
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
		int len = parts.length;
		for(int i=1;i<len;i++){
			// /dc/object/oid/cid/_fid
			if(parts.length >= 3 && parts.length <=5 && parts[2] !=null)
				subjectId = parts[2];
			else
				System.out.println("DAMS stats unknown uri: " + uri);
			
			if(parts.length == 4 && parts[3] != null && parts[3].startsWith("_"))
				fileName = parts[3];
			else if(parts.length == 5 && parts[4] != null)
				fileName = parts[3] + "/" + parts[4];
		}
		
		if(subjectId == null || subjectId.length()!= 10){
			System.out.println("Invalid subject " + subjectId + ": " + uri);
			return;
		}
		ObjectCounter objCounter = itemsMap.get(subjectId);
		if(objCounter == null){
			objCounter = new ObjectCounter(subjectId);
			itemsMap.put(subjectId, objCounter);
		}
		objCounter.increaseCounter(fileName);
	}
	
	public String getParamValue(String param){
		String[] pair = param.split("=");
		if(pair.length > 1 && pair[1] != null)
			return pair[1].trim();
		else
			return "";
	}
	
	public void export(Connection con) throws SQLException{
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
				if(ps != null){
					ps.close();
					ps = null;
				}
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
			if(ps != null){
				ps.close();
				ps = null;
			}
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
			if(ps != null){
				ps.close();
				ps = null;
			}
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
			if(ps != null){
				ps.close();
				ps = null;
			}
		}
		
		//STATS_DLP_OBJECT_ACCESS_INSERT insert
		try{
			ObjectCounter objCounter = null;
			String key = null;
			ps = con.prepareStatement(STATS_DLP_OBJECT_ACCESS_INSERT);
			Iterator<String> it = itemsMap.keySet().iterator();
			while(it.hasNext()){
				key = (String) it.next();
				objCounter = itemsMap.get(key);
				ps.setInt(1, nextId);
				returnValue = objCounter.export(ps);
				ps.clearParameters();
			}
		}finally{
			if(ps != null){
				ps.close();
				ps = null;
			}
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
			if(ps != null){
				ps.close();
				ps = null;
			}
		}
		log.info("Inserted " + appName + " statistics record for " + dateFormat.format(calendar.getTime()));
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
				
			count = collsAccessMap.get(colid);
			if(count == null)
				collsAccessMap.put(colid, 1);
			else
				collsAccessMap.put(colid, ++count);
		}
	}
	

	class ObjectCounter {
		private String subjectId = null;
		private int access = 0;
		private int view = 0;
		public ObjectCounter(String subjectId){
			this.subjectId = subjectId;
		}
		public void increaseCounter(String file){
			access++;
			if(file != null && file.length()>0 && !(isThumbnail(file) || isIcon(file)))
				view++;
		}
		
		public int getAccess() {
			return access;
		}

		public int getView() {
			return view;
		}

		public boolean isThumbnail(String file){
			if(file.indexOf("_4.") >=0 || file.indexOf("_5.") >= 0)
				return true;
			else
				return false;
		}
		
		public boolean isIcon(String file){
			if(file.indexOf("icon") >0 || file.indexOf("icon") > 0)
				return true;
			else
				return false;
		}
		
		public int export(PreparedStatement ps) throws SQLException{
			ps.setString(2, subjectId);
			ps.setInt(3, access);
			ps.setInt(4, view);
			return ps.executeUpdate();
		}
		
		public String toString(){
			return "	" + subjectId + " " + view + " " + access;
		}
	}
	
	public static void main(String[] args) throws Exception{
		//String uri = "/apps/public/images/bullet.gif?time=1349809347851&id=&hash=search&q=&fq=Facet_Collection:%22mscl_HermanBacaPhotos%22+OR+Facet_Collection:%22mscl_HermanBacaPosters%22+OR+Facet_CollectionTitle:%22Herman+Baca+Papers%22+OR+Facet_Collection:%22Herman+Baca+Posters%22&sort=titlesort%20asc&fq=NOT+category:(bb36527497+OR+bb1093000r+OR+bb23558110+OR+component)+AND+NOT+attrib:(%22xdre%20updateFlag%20delete%22+OR+%22xdre%20suppress%20true%22)&qt=dismax&rows=20&facet.limit=250&facet.sort=true HTTP/1.1";
		String line = null;
		String statsFile = "C:\\tmp\\httpd.2012-10-09";
		Reader fr = null;
		BufferedReader bf = null;
		DAMStatistic damsStats = null;
		Map<String, String> collsMap = new HashMap<String, String>();
		try {
			fr = new FileReader(statsFile);
			bf = new BufferedReader(fr);
			damsStats =  new DAMStatistic("pas");
			damsStats.setCollsMap(collsMap);
			while((line=bf.readLine()) != null){
				if(line.indexOf(" libraries ") > 0 && line.indexOf("bullet.gif?") > 0 && line.indexOf("/apps/public")>=0){
					//System.out.println(line);
					damsStats.addAccess(LogAnalyzer.getUri(line));
				}
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		damsStats.print();
	}
}
