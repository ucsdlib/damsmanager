package edu.ucsd.library.xdre.statistic.analyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;

import edu.ucsd.library.xdre.utils.Constants;

/**
 * Class LogAnalyzer
 *
 * @author lsitu@ucsd.edu
 */
public class LogAnalyzer{
	public static String SPEC_COLL_URL = "//library.ucsd.edu/speccoll/";

	private static Logger log = Logger.getLogger(LogAnalyzer.class);
	private Calendar calendar = null;
	private DAMStatistic pasStats = null;
	//private DAMStatistic casStats = null;
	private boolean update = false;
	
	public LogAnalyzer () throws Exception{
		this(new HashMap<String, String>());
	}
	
	public LogAnalyzer (Map<String, String> collsMap) throws Exception{
		pasStats = new DAMStatistic("pas");
		//casStats = new DAMStatistic("cas");
		pasStats.setCollsMap(collsMap);
		//casStats.setCollsMap(collsMap);
	}
	
	public void analyze(File logFile) throws IOException{

		String line = null;
		String uri = null;
		InputStream in = null;
		InputStream gzipIn = null;
		Reader reader = null;
		
		BufferedReader bReader = null;
		//int idx = -1;
		calendar = Calendar.getInstance();
		calendar.set(Calendar.HOUR, 0);
		calendar.set(Calendar.MINUTE, 0);
		calendar.set(Calendar.SECOND, 0);
		try {
			String fileName = logFile.getName();
			String[] dateArr = fileName.substring(fileName.indexOf('.')+1, fileName.lastIndexOf('.')).split("-");
			calendar.set(Calendar.YEAR, Integer.parseInt(dateArr[0]));
			calendar.set(Calendar.MONTH, Integer.parseInt(dateArr[1])-1);
			calendar.set(Calendar.DATE, Integer.parseInt(dateArr[2]));
			log.info("Processing log file " + fileName + " for record " + pasStats.formatDate(calendar.getTime()));

			if (logFile.getName().endsWith(".gz")){
				in = new FileInputStream(logFile);
				gzipIn = new GZIPInputStream(in);
				reader = new InputStreamReader(gzipIn);
			}else{
				in = new FileInputStream(logFile);
				reader = new InputStreamReader(in);
			}
			
			bReader = new BufferedReader(reader);
			int spIdx = -1;
			while((line=bReader.readLine()) != null){
				spIdx = line.indexOf("GET /dc");
				if(spIdx > 0 && line.indexOf(Constants.CLUSTER_HOST_NAME + " ") > 0 && !excludeFromStats(line)) {
					// ignore spiders/search engines access
					if (isBotAccess(line, spIdx) && line.indexOf(SPEC_COLL_URL) < 0)
						continue;

					uri = getUri(line);
					if(uri != null){
						//idx = uri.indexOf("&user=");
						String[] uriParts = (uri.length()>1?uri.substring(1):uri).split("/");
						
						// ignore varieties of formatted metadata views like /dc/object/bb55641580.rdf
						if(uriParts.length == 3 && uriParts[2].length() > 10 && uriParts[2].charAt(10) == '.')
							continue;

						if(uriParts.length>1 && uriParts[1].equals("object")){
							String httpStatus = "";
							//Object access: /dc/object/oid/_cid_fid
							int uidIdx = uri.indexOf("access=curator");
							int sidx = line.indexOf("\" ") + 2;
							if (sidx > 0)
								httpStatus = line.substring(sidx, sidx + 3);

							if (uidIdx > 0 && uriParts.length >= 4 && uriParts[3].startsWith("_"))
								// file access from curator
								pasStats.addObject(uri, true);
							else if (uidIdx > 0 && httpStatus.startsWith("3"))
								// view from curator with redirect
								pasStats.addObject(uri, true);
							else if (!httpStatus.startsWith("3")) {
								// access with no redirect
								if (line.indexOf(SPEC_COLL_URL, spIdx) > 0) {
									// access from MSCL exhibits: http://library.ucsd.edu/speccoll/
									addMsclStats(uri);
								} else {
									pasStats.addObject(uri, false);
								}
							}
						}else{
							//Home Page: /dc
							//Search: /dc/search?utf8=%E2%9C%93&q=wagner
							//Facet Browse: /dc/search?utf8=%E2%9C%93&f%5Bcollection_sim%5D%5B%5D=Dr.+Seuss+Political+Cartoons&q=some+people
							//Collections Browser: /dc/search?utf8=%E2%9C%93 ?????????????????
							//Collections page: /dc/collections
							//DLP Collections page: /dc/dlp/collections
							//RCI collections page: /dc/rci/collections
							//Collections access: /dc/dams_collections/bb2936476d?counter=1
							pasStats.addAccess(uri);
							
						}
							
					}
				}
			}
		}finally{
			if(in != null){
				try {
					in.close();
				} catch (IOException e) {}
				in = null;
			}
			if(gzipIn != null){
				try {
					gzipIn.close();
				} catch (IOException e) {}
				gzipIn = null;
			}
			if(bReader != null){
				try {
					bReader.close();
				} catch (IOException e) {}
				bReader = null;
			}
			if(reader != null){
				try {
					reader.close();
				} catch (IOException e) {}
				reader = null;
			}
		}
	}
	
	private void addMsclStats(String uri) {
		if (uri.indexOf("/_2.jpg") > 0) {
			// count as object view
			uri = uri.replace("/_2.jpg", "");
		}
		pasStats.addObject(uri, false); 
	}
	
	public static String getUri(String line){
		int idx = line.indexOf('"');
		int nIdx = -1;
		if(idx > 0){
			String uri = null;
			nIdx=line.indexOf('"', idx + 1);
			while(nIdx>0 && line.charAt(nIdx-1)=='\\')
				nIdx=line.indexOf('"', nIdx + 1);
				
			if(nIdx>0){
				uri = line.substring(idx+1, nIdx);
				idx = uri.indexOf(" ");
				if(idx < (nIdx=uri.lastIndexOf(" ")))
					uri = uri.substring(idx+1, nIdx);
			}else
				uri = line.substring(line.indexOf(" ", idx+1) + 1);

			// exclude unexpected url
			if (uri.indexOf("?") < 0 && uri.indexOf("&") > 0) {
				log.info("Invalid request url: " + line);
				return null;
			}
			return 	uri;
		}else{ 
			log.info("Invalid request: " + line);
			return null;
		}
	}
	
	public void export(Connection con) throws SQLException {
		pasStats.setCalendar(calendar);
		//casStats.setCalendar(calendar);
		pasStats.setUpdate(update);
		//casStats.setUpdate(update);
		pasStats.export(con);
		//casStats.export(con);
	}
	
	private boolean excludeFromStats(String value) {
		String[] excludePatterns = {"/ucsd.ico", "/fonts/", "/assets/", "/get_data/", "/users/", "/images/"};
		for (String excludePattern : excludePatterns) {
			if (value.contains(excludePattern))
				return true;
		}
		return false;
	}

	private boolean isBotAccess(String value, int fromIndex) {
		String[] botPatterns = {"\"-\"", " SortSiteCmd/", "archive.org_bot"};
		for (String botPattern : botPatterns) {
			if (value.indexOf(botPattern, fromIndex) > 0)
				return true;
		}
		return false;
	}

	public void print() {
		pasStats.print();
		//casStats.print();
	} 
	
	public boolean isUpdate() {
		return update;
	}

	public void setUpdate(boolean update) {
		this.update = update;
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
