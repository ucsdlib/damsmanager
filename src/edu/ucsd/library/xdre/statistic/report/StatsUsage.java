package edu.ucsd.library.xdre.statistic.report;

import java.sql.Connection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Map;

import edu.ucsd.library.xdre.statistic.analyzer.Statistics;
import edu.ucsd.library.xdre.statistic.beans.StatSummary;

/**
 * 
 * Class AppUsage - Base class for generating the application usage reports.
 *
 * @author lsitu@ucsd.edu
 */
public abstract class StatsUsage {
	public static final String YEARLY_FORMAT = "yyyy";
	public static final String MONTHLY_FORMAT = "yyyy/MM";
	public static final String DAILY_FORMAT = "yyyy/MM/dd";
	public static final String APP_USAGE_QUERY = "SELECT TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM') AS period, SUM(s.NUM_ACCESS) as num_usage, SUM(d.NUM_SEARCH) as num_search, SUM(d.NUM_BROWSE) as num_browse, SUM(d.NUM_HOMEPAGE) as num_homepage, SUM(d.NUM_COLPAGE) as num_colpage FROM WEB_STATS s, STATS_DLP d WHERE s.ID=d.STAT_ID AND s.APP_NAME=? AND s.STAT_DATE>=to_date(?, '" + Statistics.DATE_FORMAT + "') AND s.STAT_DATE<=to_date(?, '" + Statistics.DATE_FORMAT + "') GROUP BY TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM') ORDER BY period"; 
	public static final String OBJECT_USAGE_QUERY = "SELECT TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM') AS period, COUNT(DISTINCT a.OBJECT_ID) as num_object, SUM(a.NUM_ACCESS) as num_access, SUM(a.NUM_VIEW) as num_view FROM WEB_STATS s, STATS_DLP_OBJECT_ACCESS a WHERE s.ID=a.STAT_ID AND s.APP_NAME=? AND s.STAT_DATE>=to_date(?, '" + Statistics.DATE_FORMAT + "') AND s.STAT_DATE<=to_date(?, '" + Statistics.DATE_FORMAT + "') GROUP BY TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM') ORDER BY period"; 
	public static final String DISTINCT_PAS_OBJECT_USAGE_QUERY = "SELECT TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM') AS period, COUNT(DISTINCT a.OBJECT_ID) as num_object FROM WEB_STATS s, STATS_DLP_OBJECT_ACCESS a WHERE s.ID=a.STAT_ID AND s.APP_NAME='pas' AND s.STAT_DATE>=to_date(?, '" + Statistics.DATE_FORMAT + "') AND s.STAT_DATE<=to_date(?, '" + Statistics.DATE_FORMAT + "') AND a.NUM_VIEW>0 GROUP BY TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM') ORDER BY period"; 
	public static final String DISTINCT_OBJECT_USAGE_QUERY = "SELECT TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM') AS period, COUNT(DISTINCT a.OBJECT_ID) as num_object FROM WEB_STATS s, STATS_DLP_OBJECT_ACCESS a WHERE s.ID=a.STAT_ID AND (s.APP_NAME='pas' OR s.APP_NAME='cas') AND s.STAT_DATE>=to_date(?, '" + Statistics.DATE_FORMAT + "') AND s.STAT_DATE<=to_date(?, '" + Statistics.DATE_FORMAT + "') AND a.NUM_VIEW>0 GROUP BY TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM') ORDER BY period"; 
	public static final String OBJECT_POPULARITY_QUERY = "SELECT * FROM (SELECT a.OBJECT_ID as subject, SUM(a.NUM_ACCESS) as num_access, SUM(a.NUM_VIEW) as num_view FROM WEB_STATS s, STATS_DLP_OBJECT_ACCESS a WHERE s.ID=a.STAT_ID AND s.APP_NAME=? GROUP BY a.OBJECT_ID ORDER BY num_view DESC, num_access DESC) as result Limit 150"; 
	public static final String PAS_USAGE_QUERY = "SELECT TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM') AS period, SUM(s.NUM_ACCESS) as num_access, SUM(d.NUM_SEARCH) as num_search, SUM(d.NUM_BROWSE) as num_browse, SUM(d.NUM_COLPAGE) as num_colpage, SUM(d.NUM_HOMEPAGE) as num_homepage FROM WEB_STATS s, STATS_DLP d WHERE s.ID=d.STAT_ID AND s.APP_NAME='pas' AND s.STAT_DATE>=to_date(?, '" + Statistics.DATE_FORMAT + "') AND s.STAT_DATE<=to_date(?, '" + Statistics.DATE_FORMAT + "') GROUP BY TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM') ORDER BY period"; 
	public static final String DLP_USAGE_QUERY = "SELECT TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM') AS period, SUM(s.NUM_ACCESS) as num_access, SUM(d.NUM_SEARCH) as num_search, SUM(d.NUM_BROWSE) as num_browse, SUM(d.NUM_COLPAGE) as num_colpage, SUM(d.NUM_HOMEPAGE) as num_homepage FROM WEB_STATS s, STATS_DLP d WHERE s.ID=d.STAT_ID AND (s.APP_NAME='pas' OR s.APP_NAME='cas') AND s.STAT_DATE>=to_date(?, '" + Statistics.DATE_FORMAT + "') AND s.STAT_DATE<=to_date(?, '" + Statistics.DATE_FORMAT + "') GROUP BY TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM') ORDER BY period"; 
	public static final String PAS_OBJECT_USAGE_QUERY = "SELECT TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM') AS period, COUNT(DISTINCT a.OBJECT_ID) as num_object, SUM(a.NUM_ACCESS) as num_access, SUM(a.NUM_VIEW) as num_view FROM WEB_STATS s, STATS_DLP_OBJECT_ACCESS a WHERE s.ID=a.STAT_ID AND s.APP_NAME='pas' AND s.STAT_DATE>=to_date(?, '" + Statistics.DATE_FORMAT + "') AND s.STAT_DATE<=to_date(?, '" + Statistics.DATE_FORMAT + "') GROUP BY TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM') ORDER BY period"; 
	public static final String DLP_OBJECT_USAGE_QUERY = "SELECT TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM') AS period, COUNT(DISTINCT a.OBJECT_ID) as num_object, SUM(a.NUM_ACCESS) as num_access, SUM(a.NUM_VIEW) as num_view FROM WEB_STATS s, STATS_DLP_OBJECT_ACCESS a WHERE s.ID=a.STAT_ID AND (s.APP_NAME='pas' OR s.APP_NAME='cas') AND s.STAT_DATE>=to_date(?, '" + Statistics.DATE_FORMAT + "') AND s.STAT_DATE<=to_date(?, '" + Statistics.DATE_FORMAT + "') GROUP BY TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM') ORDER BY period"; 
	public static final String DLP_COLLECTION_QUANTITY_QUERY = "SELECT TO_CHAR(STAT_DATE, 'PERIOD_PARAM') as period, COUNT(COLLECTION_ID) as num_collections, SUM(NUM_OBJECTS ) as num_items, SUM(SIZE_BYTES) as disk_size FROM STATS_DLC_QUAN WHERE COLLECTION_ID<>'bb8738126n' AND STAT_DATE>=to_date(?,'" + Statistics.DATE_FORMAT + "') AND STAT_DATE<=to_date(?, '" + Statistics.DATE_FORMAT + "') AND COLLECTION_ID IN ( COLLECTION_FILTER ) GROUP BY TO_CHAR(STAT_DATE, 'PERIOD_PARAM') ORDER BY period ";
	public static final String DLP_COLLECTION_RECORD_QUERY = "SELECT * FROM STATS_DLC_QUAN WHERE COLLECTION_ID=? ORDER BY STAT_DATE DESC";	
	public static final String PAS_KEYWORDS_QUERY = "SELECT * FROM (SELECT k.KEYWORD, SUM(k.NUM_ACCESS) as numaccess FROM WEB_STATS s, STATS_DLC_KEYWORDS k WHERE k.TYPE=? AND s.ID=k.STAT_ID AND s.APP_NAME='pas' AND s.STAT_DATE>=to_date(?, '" + Statistics.DATE_FORMAT + "') AND s.STAT_DATE<=to_date(?, '" + Statistics.DATE_FORMAT + "') GROUP BY (k.KEYWORD) ORDER BY numaccess DESC, k.KEYWORD ASC) as result LIMIT ?";
	public static final String DLC_KEYWORDS_QUERY = "SELECT * FROM (SELECT k.KEYWORD, SUM(k.NUM_ACCESS) as numaccess FROM WEB_STATS s, STATS_DLC_KEYWORDS k WHERE k.TYPE=? AND s.ID=k.STAT_ID AND (s.APP_NAME='pas' OR s.APP_NAME='cas') AND s.STAT_DATE>=to_date(?, '" + Statistics.DATE_FORMAT + "') AND s.STAT_DATE<=to_date(?, '" + Statistics.DATE_FORMAT + "') GROUP BY (k.KEYWORD) ORDER BY numaccess DESC, k.KEYWORD ASC) as result LIMIT ?";
	public static final String PAS_KEYWORDS_LIKE_QUERY = "SELECT * FROM (SELECT k.KEYWORD, SUM(k.NUM_ACCESS) as numaccess FROM WEB_STATS s, STATS_DLC_KEYWORDS k WHERE k.TYPE=? AND (LOWER(KEYWORD) LIKE 'LIKE_KEYWORDS%' OR LOWER(k.KEYWORD) LIKE '% LIKE_KEYWORDS%' OR LOWER(k.KEYWORD) LIKE '%-LIKE_KEYWORDS%' OR LOWER(k.KEYWORD) LIKE '%/LIKE_KEYWORDS%') AND s.ID=k.STAT_ID AND s.APP_NAME='pas' AND s.STAT_DATE>=to_date(?, '" + Statistics.DATE_FORMAT + "') AND s.STAT_DATE<=to_date(?, '" + Statistics.DATE_FORMAT + "') GROUP BY (k.KEYWORD) ORDER BY numaccess DESC, k.KEYWORD ASC) as result LIMIT ?";
	public static final String DLC_KEYWORDS_LIKE_QUERY = "SELECT * FROM (SELECT k.KEYWORD, SUM(k.NUM_ACCESS) as numaccess FROM WEB_STATS s, STATS_DLC_KEYWORDS k WHERE k.TYPE=? AND (LOWER(KEYWORD) LIKE 'LIKE_KEYWORDS%' OR LOWER(k.KEYWORD) LIKE '% LIKE_KEYWORDS%' OR LOWER(k.KEYWORD) LIKE '%-LIKE_KEYWORDS%' OR LOWER(k.KEYWORD) LIKE '%/LIKE_KEYWORDS%') AND s.ID=k.STAT_ID AND (s.APP_NAME='pas' OR s.APP_NAME='cas') AND s.STAT_DATE>=to_date(?, '" + Statistics.DATE_FORMAT + "') AND s.STAT_DATE<=to_date(?, '" + Statistics.DATE_FORMAT + "') GROUP BY (k.KEYWORD) ORDER BY numaccess DESC, k.KEYWORD ASC) as result LIMIT ?";
	public static final String PAS_KEYWORDS_COUNT_QUERY = "SELECT COUNT(*) FROM (SELECT k.KEYWORD, SUM(k.NUM_ACCESS) as numaccess FROM WEB_STATS s, STATS_DLC_KEYWORDS k WHERE k.TYPE=? AND s.ID=k.STAT_ID AND s.APP_NAME='pas' AND s.STAT_DATE>=to_date(?, '" + Statistics.DATE_FORMAT + "') AND s.STAT_DATE<=to_date(?, '" + Statistics.DATE_FORMAT + "') GROUP BY (k.KEYWORD)) as result";	
	public static final String DLC_KEYWORDS_COUNT_QUERY = "SELECT COUNT(*) FROM (SELECT k.KEYWORD, SUM(k.NUM_ACCESS) as numaccess FROM WEB_STATS s, STATS_DLC_KEYWORDS k WHERE k.TYPE=? AND s.ID=k.STAT_ID AND (s.APP_NAME='pas' OR s.APP_NAME='cas') AND s.STAT_DATE>=to_date(?, '" + Statistics.DATE_FORMAT + "') AND s.STAT_DATE<=to_date(?, '" + Statistics.DATE_FORMAT + "') GROUP BY (k.KEYWORD)) as result";	
	public static final String PAS_KEYWORDS_LIKE_COUNT_QUERY = "SELECT COUNT(*) FROM (SELECT k.KEYWORD, SUM(k.NUM_ACCESS) as numaccess FROM WEB_STATS s, STATS_DLC_KEYWORDS k WHERE k.TYPE=? AND s.ID=k.STAT_ID AND s.APP_NAME='pas' AND (LOWER(k.KEYWORD) LIKE 'LIKE_KEYWORDS%' OR LOWER(k.KEYWORD) LIKE '% LIKE_KEYWORDS%' OR LOWER(k.KEYWORD) LIKE '%-LIKE_KEYWORDS%' OR LOWER(k.KEYWORD) LIKE '%/LIKE_KEYWORDS%') AND s.STAT_DATE>=to_date(?, '" + Statistics.DATE_FORMAT + "') AND s.STAT_DATE<=to_date(?, '" + Statistics.DATE_FORMAT + "') GROUP BY (k.KEYWORD)) as result";		
	public static final String DLC_KEYWORDS_LIKE_COUNT_QUERY = "SELECT COUNT(*) FROM (SELECT k.KEYWORD, SUM(k.NUM_ACCESS) as numaccess FROM WEB_STATS s, STATS_DLC_KEYWORDS k WHERE k.TYPE=? AND s.ID=k.STAT_ID AND (s.APP_NAME='pas' OR s.APP_NAME='cas') AND (LOWER(k.KEYWORD) LIKE 'LIKE_KEYWORDS%' OR LOWER(k.KEYWORD) LIKE '% LIKE_KEYWORDS%' OR LOWER(k.KEYWORD) LIKE '%-LIKE_KEYWORDS%' OR LOWER(k.KEYWORD) LIKE '%/LIKE_KEYWORDS%') AND s.STAT_DATE>=to_date(?, '" + Statistics.DATE_FORMAT + "') AND s.STAT_DATE<=to_date(?, '" + Statistics.DATE_FORMAT + "') GROUP BY (k.KEYWORD)) as result";		
	public static final String PAS_COLLECTIONS_ACCESS_QUERY = "SELECT * FROM (SELECT TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM') AS period, c.COLLECTION_ID as colId, SUM(c.NUM_ACCESS) as num_access FROM WEB_STATS s, STATS_DLP_COL_ACCESS c WHERE s.ID=c.STAT_ID AND s.APP_NAME='pas' AND s.STAT_DATE>=to_date(?, '" + Statistics.DATE_FORMAT + "') AND s.STAT_DATE<=to_date(?, '" + Statistics.DATE_FORMAT + "') GROUP BY TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM'), c.COLLECTION_ID) as result ORDER BY result.period";	
	public static final String DLP_COLLECTIONS_ACCESS_QUERY = "SELECT * FROM (SELECT TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM') AS period, c.COLLECTION_ID as colId, SUM(c.NUM_ACCESS) as num_access FROM WEB_STATS s, STATS_DLP_COL_ACCESS c WHERE s.ID=c.STAT_ID AND (s.APP_NAME='pas' OR s.APP_NAME='cas') AND s.STAT_DATE>=to_date(?, '" + Statistics.DATE_FORMAT + "') AND s.STAT_DATE<=to_date(?, '" + Statistics.DATE_FORMAT + "') GROUP BY TO_CHAR(s.STAT_DATE, 'PERIOD_PARAM'), c.COLLECTION_ID) as result ORDER BY result.period";	
	
	protected SimpleDateFormat dbFormat = new SimpleDateFormat(Statistics.DATE_FORMAT);
	protected SimpleDateFormat dbMonthFormat = new SimpleDateFormat(Statistics.MONTH_FORMAT);
	protected SimpleDateFormat outFormatMonthly = new SimpleDateFormat("MMM yyyy");
	protected SimpleDateFormat digitFormatMonthly = new SimpleDateFormat("MM/yyyy");
	protected SimpleDateFormat outFormatYearly = new SimpleDateFormat("yyyy");
	protected SimpleDateFormat outFormatDaily = new SimpleDateFormat("MMM dd");
	protected SimpleDateFormat digitFormatDaily = new SimpleDateFormat("MM/dd");

	protected  String appName = null;
	protected  Date start = null;
	protected  Date end = null;
	protected  Connection con = null;
	protected  String statsFormat = MONTHLY_FORMAT;
	public StatsUsage(String appName, Date start, Date end, Connection con){
		this.appName = appName;
		this.start = start;
		this.end = end;
		this.con = con;
	}
	
	public abstract Map<String, Object> getGraphData() throws Exception;
	public abstract Collection<StatSummary> getStatSummary() throws Exception;
	
	protected String formatOutput(String timeVal, boolean digitFormat) throws ParseException{
		String outVal = "";
		SimpleDateFormat dFormat = new SimpleDateFormat(statsFormat);
		Date date = dFormat.parse(timeVal);
		if(statsFormat.equals(YEARLY_FORMAT)){
			outVal = outFormatYearly.format(date);
		}else if(statsFormat.equals(DAILY_FORMAT)){
			if(digitFormat)
				outVal = digitFormatDaily.format(date);
			else
				outVal = outFormatDaily.format(date);
		}else{
			if(digitFormat)
				outVal = digitFormatMonthly.format(date);
			else
				outVal = outFormatMonthly.format(date);
		}
		return outVal;
	}

	public String getAppName() {
		return appName;
	}

	public void setAppName(String appName) {
		this.appName = appName;
	}

	public Connection getCon() {
		return con;
	}

	public void setCon(Connection con) {
		this.con = con;
	}

	public Date getEnd() {
		return end;
	}

	public void setEnd(Date end) {
		this.end = end;
	}

	public Date getStart() {
		return start;
	}

	public void setStart(Date start) {
		this.start = start;
	}
	
	public void setStatsYearly(){
		statsFormat = YEARLY_FORMAT;
	}
	
	public void setStatsMonthly(){
		statsFormat = MONTHLY_FORMAT;
	}
	
	public void setStatsDaily(){
		statsFormat = DAILY_FORMAT;
	}
}
