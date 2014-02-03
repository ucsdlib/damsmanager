package edu.ucsd.library.xdre.statistic.analyzer;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.log4j.Logger;

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
	public static String STATS_DLP_OBJECT_ACCESS_INSERT = "INSERT INTO STATS_DLP_OBJECT_ACCESS(STAT_ID, OBJECT_ID, NUM_ACCESS, NUM_VIEW) VALUES (?,?,?,?)";
	public static String STATS_DLP_COLLECTION_ACCESS_INSERT = "INSERT INTO STATS_DLP_COL_ACCESS(STAT_ID, COLLECTION_ID, NUM_ACCESS) VALUES (?,?,?)";
	public static String STATS_DLC_KEYWORDS_INSERT = "INSERT INTO STATS_DLC_KEYWORDS(STAT_ID, KEYWORD, NUM_ACCESS, TYPE) VALUES (?,?,?,?)";
	public static String COLLECTION_STATS_INSERT = "INSERT INTO STATS_DLC_QUAN(ID, STAT_DATE, COLLECTION_ID, COLLECTION_TITLE, NUM_OBJECTS, SIZE_BYTES) VALUES (?,?,?,?,?,?)";
	public static String WEB_STATS_DELETE_RECORD = "DELETE FROM WEB_STATS WHERE STAT_DATE=to_date(?,'" + DATE_FORMAT + "') AND APP_NAME=?";
	public static String WEB_STATS_RECORD_EXIST = "SELECT COUNT(*) FROM WEB_STATS WHERE STAT_DATE=to_date(?,'" + DATE_FORMAT + "')";
	public static String COLLECTION_STATS_RECORD_EXIST = "SELECT * FROM STATS_DLC_QUAN WHERE to_char(STAT_DATE, '" + MONTH_FORMAT + "')=? AND COLLECTION_ID=?";
	private static Logger log = Logger.getLogger(Statistics.class);
	
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
	public abstract void export(Connection con) throws SQLException;
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
}