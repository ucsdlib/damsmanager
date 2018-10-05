package edu.ucsd.library.xdre.statistic.analyzer;

import java.net.URL;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import edu.ucsd.library.xdre.utils.Constants;

/**
 * Abstract class Statistic
 *
 * @author lsitu@ucsd.edu
 */
public abstract class Statistics {
	
	public static final String DATE_FORMAT = "yyyy-MM-dd";
	public static final String MONTH_FORMAT = "yyyy-MM";
	public static String WEB_STATS_INSERT = "INSERT INTO WEB_STATS(ID, STAT_DATE, NUM_ACCESS, APP_NAME) VALUES (?,?,?,?)";
	public static String STATS_DLP_INSERT = "INSERT INTO STATS_DLP(STAT_ID, NUM_SEARCH, NUM_BROWSE, NUM_COLPAGE, NUM_HOMEPAGE) VALUES (?,?,?,?,?)";
	public static String STATS_DLP_OBJECT_ACCESS_INSERT = "INSERT INTO STATS_DLP_OBJECT_ACCESS(STAT_ID, IS_PRIVATE, UNIT_ID, COL_ID, OBJECT_ID, NUM_ACCESS, NUM_VIEW, CLIENT_IP) VALUES (?,?,?,?,?,?,?,?)";
	public static String STATS_FILE_DOWNLOAD_INSERT = "INSERT INTO STATS_FILE_DOWNLOAD(STAT_ID, IS_PRIVATE, UNIT_ID, COL_ID, OBJECT_ID, COMP_ID, FILE_ID, NUM_VIEW, CLIENT_IP) VALUES (?,?,?,?,?,?,?,?,?)";
	public static String STATS_DLP_COLLECTION_ACCESS_INSERT = "INSERT INTO STATS_DLP_COL_ACCESS(STAT_ID, COLLECTION_ID, NUM_ACCESS) VALUES (?,?,?)";
	public static String STATS_DLC_KEYWORDS_INSERT = "INSERT INTO STATS_DLC_KEYWORDS(STAT_ID, KEYWORD, NUM_ACCESS, TYPE) VALUES (?,?,?,?)";
	public static String COLLECTION_STATS_INSERT = "INSERT INTO STATS_DLC_QUAN(ID, STAT_DATE, COLLECTION_ID, COLLECTION_TITLE, NUM_OBJECTS, SIZE_BYTES) VALUES (?,?,?,?,?,?)";
	public static String WEB_STATS_DELETE_RECORD = "DELETE FROM WEB_STATS WHERE STAT_DATE=to_date(?,'" + DATE_FORMAT + "') AND APP_NAME=?";
	public static String WEB_STATS_RECORD_EXIST = "SELECT COUNT(*) FROM WEB_STATS WHERE STAT_DATE=to_date(?,'" + DATE_FORMAT + "')";
	public static String COLLECTION_STATS_RECORD_EXIST = "SELECT * FROM STATS_DLC_QUAN WHERE to_char(STAT_DATE, '" + MONTH_FORMAT + "')=? AND COLLECTION_ID=?";
	public static String COLLECTION_STATS_DELETE_RECORD = "DELETE FROM STATS_DLC_QUAN WHERE to_char(STAT_DATE, '" + MONTH_FORMAT + "')=? AND COLLECTION_ID=?";
	private static Logger log = Logger.getLogger(Statistics.class);
	
	// object caching
	private static HashMap<String,Document> cacheContent = new HashMap<String,Document>();
	private static LinkedList<String> cacheAccess = new LinkedList<String>();
	private static int cacheSize = 1000; // max objects cached, 0 = disabled

	protected int numAccess =0;
	protected String appName = null;
	protected Calendar calendar = null;
	protected SimpleDateFormat dateFormat = null;
	protected SimpleDateFormat monthFormat = null;
	protected boolean update = false;
	
	public Statistics(String appName){
		this.appName = appName;
		dateFormat = new SimpleDateFormat(DATE_FORMAT);
		monthFormat = new SimpleDateFormat(MONTH_FORMAT);
	}
	public abstract void export(Connection con) throws Exception;
	public abstract void print();
	
	public String getAppName() {
		return appName;
	}
	public void setAppName(String appName) {
		this.appName = appName;
	}
	public Calendar getCalendar() {
		return calendar;
	}
	public void setCalendar(Calendar calendar) {
		this.calendar = calendar;
	}

	public int getNumAccess() {
        return numAccess;
    }

    public String formatDate(Date date){
		return dateFormat.format(date);
	}
	public boolean isUpdate() {
		return update;
	}
	public void setUpdate(boolean update) {
		this.update = update;
	}
	public static SimpleDateFormat getDatabaseDateFormater(){
		return new SimpleDateFormat(Statistics.DATE_FORMAT);
	}
	public static SimpleDateFormat getDatabaseMonthFormater(){
		return new SimpleDateFormat(Statistics.MONTH_FORMAT);
	}
	public static void close(Connection con){
		try {
			if(con != null && !con.isClosed()){
					con.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void close(Statement stmt){
		try {
			if(stmt != null){
					stmt.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public static void close(ResultSet rs){
		Statement stmt = null;
		try {
			if(rs != null){
				stmt = rs.getStatement();
				rs.close();
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		close(stmt);
	}
	
	public static int getNextId(Connection con) throws SQLException{
		int nextId = 0;
		ResultSet rs = null;
		Statement stmt = null;
		String sql = "SELECT nextval('WEB_STAT_SEQUENCE')";
		try{
			stmt = con.createStatement();
			rs = stmt.executeQuery(sql);
			if(rs.next())
				nextId = rs.getInt(1);
		}finally{
			if(rs != null){
				try {
					rs.close();
				} catch (SQLException e) {}
				rs = null;
			}
			if(stmt != null){
				try {
					stmt.close();
				} catch (SQLException e) {}
				stmt = null;
			}
		}
		
		return nextId;
	}
	
	public static boolean isRecordExist(Connection con, Date date) throws SQLException{
		PreparedStatement ps = null;
		ResultSet rs = null;
		SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);;
		try{
			ps = con.prepareStatement(DAMStatistic.WEB_STATS_RECORD_EXIST);
			ps.setString(1, dateFormat.format(date));
			rs = ps.executeQuery();
			if(rs.next() && rs.getInt(1) > 0)
				return true;
		}finally{
			Statistics.close(rs);
			Statistics.close(ps);
			rs = null;
			ps = null;
		}
		return false;
	}
	
	public static synchronized void cacheAdd( String objid, Document doc )
	{
		if ( cacheSize > 0 )
		{
			if ( !cacheContent.containsKey(objid) )
			{
				while ( cacheContent.size() >= cacheSize )
				{
					cacheContent.remove(cacheAccess.pop());
				}
			}
			cacheContent.put(objid,doc);
			cacheAccess.add(objid);
		}
	}

	public static synchronized Document cacheGet( String objid )
	{
		if ( cacheSize > 0)
		{
			cacheAccess.remove(objid);
			cacheAccess.add(objid);
			return cacheContent.get(objid);
		}
		else
		{
			return null;
		}
	}

	public static Document getRecordForStats(String id) throws Exception {
		URL url = new URL(Constants.SOLR_URL_BASE + "/select?q=id:" + id + "&fl=" + URLEncoder.encode("*title* OR *collection* OR *unit*", "UTF-8"));
		SAXReader reader = new SAXReader();
		return reader.read(url);
	}

	public static String getTextValue (Document doc, String xPath) {
		String val = "";
		if (doc != null) {
			Node node = doc.selectSingleNode(xPath);
			if (node != null)
				return node.getText();
		}
		return val;
	}

	public static String getTitleFromJson (String jsonVal) {
		if (StringUtils.isNotBlank(jsonVal)) {
			JSONObject obj = (JSONObject) JSONValue.parse(jsonVal);
			return (String)obj.get("name");
		}
		return "";
	}

	public static String escapeCsv(String value) {
		if ( StringUtils.isNotBlank(value) ) {
			if (value.indexOf(",") >= 0 || value.indexOf("\"") >= 0 
					|| value.indexOf(System.getProperty("line.separator")) >= 0)
				return "\"" + value.replace("\"", "\"\"") + "\"";
		}
		return value;
	}
}