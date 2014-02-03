package edu.ucsd.library.xdre.statistic.report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.statistic.beans.DAMSKeywordsSummary;
import edu.ucsd.library.xdre.statistic.beans.StatSummary;

public class DAMSKeywordsUsage extends StatsUsage {
	private static Logger log = Logger.getLogger(DAMSKeywordsUsage.class);
	public static final int KEYWORDS_PER_PAGE = 20;
	
	private String type = null; //keyword, phrase
	private String keyword = null;
	private int pageNum = 0;
	private int total = 0;
	
	public DAMSKeywordsUsage(String appName, Date start, Date end, Connection con, String type, int pageNum) {
		super(appName, start, end, con);
		this.type = type;
		this.pageNum = pageNum;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public int getPageNum() {
		return pageNum;
	}

	public void setPageNum(int pageNum) {
		this.pageNum = pageNum;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public Map<String, Object> getGraphData() throws Exception {
		return null;
	}

	@Override
	public Collection<StatSummary> getStatSummary() throws Exception {
		
		List<StatSummary> dlpSum = new ArrayList<StatSummary>();
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		//Keywords/Phrases usage.
		int startRownum = (pageNum-1)*KEYWORDS_PER_PAGE+1;
		String period = null;
		StatSummary statSum = null;
		String query = null;
		if(keyword != null && keyword.length() > 0){
			if(appName != null && appName.equalsIgnoreCase("pas"))
				query = PAS_KEYWORDS_LIKE_QUERY.replace("LIKE_KEYWORDS", StringEscapeUtils.escapeJava(keyword).replace("\\\"", "\"").toLowerCase());
			else
				query = DLC_KEYWORDS_LIKE_QUERY.replace("LIKE_KEYWORDS", StringEscapeUtils.escapeJava(keyword).replace("\\\"", "\"").toLowerCase());

		}else{
			if(appName != null && appName.equalsIgnoreCase("pas"))
				query = PAS_KEYWORDS_QUERY;
			else
				query = DLC_KEYWORDS_QUERY;
		}
		
		try{
			ps = con.prepareStatement(query);
			ps.setString(1, type);
			ps.setString(2, dbFormat.format(start));
			ps.setString(3, dbFormat.format(end));
			ps.setInt(4, pageNum*KEYWORDS_PER_PAGE+1);
			rs = ps.executeQuery();
			while(rs.next()){
				if(--startRownum <= 0){
					statSum = new DAMSKeywordsSummary(null, rs.getInt("numaccess"), StringEscapeUtils.unescapeJava(rs.getString("KEYWORD")), type);
					dlpSum.add(statSum);
				}
			}
		}finally{
			if(rs != null){
				try{
					rs.close();
					rs = null;
				}catch(SQLException e){}
			}
			if(ps != null){
				try{
					ps.close();
					ps = null;
				}catch(SQLException e){}
			}
		}
		
		int returnSize = dlpSum.size();
		if(pageNum >1 ||  returnSize > pageNum*KEYWORDS_PER_PAGE)
			total = countTotal();
		else
			total =(pageNum-1)*KEYWORDS_PER_PAGE + returnSize;
		return dlpSum;
	}

	public int getTotal() {
		return total;
	}
	
	public int countTotal() throws SQLException{
		int count = 0;
		PreparedStatement ps = null;
		ResultSet rs = null;
		String query = null;
		if(keyword != null && keyword.length() > 0){
			if(appName != null && appName.equalsIgnoreCase("pas"))
				query = PAS_KEYWORDS_LIKE_COUNT_QUERY.replace("LIKE_KEYWORDS", StringEscapeUtils.escapeJava(keyword).replace("\\\"", "\"").toLowerCase());
			else
				query = DLC_KEYWORDS_LIKE_COUNT_QUERY.replace("LIKE_KEYWORDS", StringEscapeUtils.escapeJava(keyword).replace("\\\"", "\"").toLowerCase());
		}else{
			if(appName != null && appName.equalsIgnoreCase("pas"))
				query = PAS_KEYWORDS_COUNT_QUERY;
			else
				query = DLC_KEYWORDS_COUNT_QUERY;
		}
		try{
			ps = con.prepareStatement(query);
			ps.setString(1, type);
			ps.setString(2, dbFormat.format(start));
			ps.setString(3, dbFormat.format(end));
			rs = ps.executeQuery();
			if(rs.next()){
				count = rs.getInt(1);
			}
		}finally{
			if(rs != null){
				try{
					rs.close();
					rs = null;
				}catch(SQLException e){}
			}
			if(ps != null){
				try{
					ps.close();
					ps = null;
				}catch(SQLException e){}
			}
		}
		return count;
	}
}
