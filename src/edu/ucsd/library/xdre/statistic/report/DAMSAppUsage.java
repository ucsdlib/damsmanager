package edu.ucsd.library.xdre.statistic.report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.ucsd.library.xdre.statistic.analyzer.Statistics;
import edu.ucsd.library.xdre.statistic.beans.StatSummary;

/**
 * 
 * Class DAMSAppUsage generates the PAS/CAS usage reports.
 *
 * @author lsitu@ucsd.edu
 */
public class DAMSAppUsage extends StatsUsage{
	
	public DAMSAppUsage(String appName, Date start, Date end, Connection con){
		super(appName, start, end, con);
	}
	
	public Map<String, Object> getGraphData() throws Exception{
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		String periods = "";
		String graphData = "";
		List<String> periodsList = null;
		List<String> usageList = null;
		List<String> searchList = null;
		List<String> browseList = null;
		List<String> homePageList = null;
		List<String> colPageList = null;

		
		periodsList = new ArrayList<String>();
		usageList = new ArrayList<String>();
		searchList = new ArrayList<String>();
		browseList = new ArrayList<String>();
		homePageList = new ArrayList<String>();
		colPageList = new ArrayList<String>();
		String tmpVal = null;
		boolean isMarked = false;
		try{
			ps = con.prepareStatement(APP_USAGE_QUERY.replace("PERIOD_PARAM", statsFormat));
			ps.setString(1, appName);
			ps.setString(2, dbFormat.format(start));
			ps.setString(3, dbFormat.format(end));
			rs= ps.executeQuery();
			while (rs.next()){
				tmpVal = rs.getString("period");
				//Graph ploting
				periods += (periods.length()>0?", '":"'") + formatOutput(tmpVal, false) + "'";
				periodsList.add(formatOutput(tmpVal, true));
				
				//Graph ploting
				tmpVal = rs.getString("num_usage");
				int val = Integer.parseInt(tmpVal);
				if( !isMarked && rs.getRow()> 1 && val>0){
					if(statsFormat.equals(DAILY_FORMAT))
						graphData += (graphData.length()>0?", ":" ") + "{y: " + tmpVal + ", marker: { symbol: 'url(./images/" + appName +".gif)'}}";
					else
						graphData += (graphData.length()>0?", ":" ") + tmpVal;
					isMarked = true;
				}else
					graphData += (graphData.length()>0?", ":" ") + tmpVal;
				usageList.add(tmpVal);
				
				searchList.add(rs.getString("num_search"));
				browseList.add(rs.getString("num_browse"));
				homePageList.add(rs.getString("num_homepage"));
				colPageList.add(rs.getString("num_colpage"));
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
		int pSize = periodsList.size();
		if(pSize < 12){
			tmpVal = "0";
			Calendar cal = Calendar.getInstance();
			if(periodsList.size() > 0){
				cal.setTime(digitFormatMonthly.parse(periodsList.get(0)));
				cal.add(Calendar.DATE, 1);
			}

			
			for(int i=0; i<12-pSize; i++){
				graphData = tmpVal + (graphData.length()>0?", ":" ") + graphData;
				cal.add(Calendar.MONTH, -1);
				
				periods = "'" + outFormatMonthly.format(cal.getTime()) + (periods.length()>0?"', ":"'") + periods;
				periodsList.add(0, digitFormatMonthly.format(cal.getTime()));
				usageList.add(0, tmpVal);	
				searchList.add(0, tmpVal);
				browseList.add(0, tmpVal);
				homePageList.add(0, tmpVal);
				colPageList.add(0, tmpVal);
			}
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("periodsList", periodsList);
		map.put(appName + "UsageList", usageList);
		map.put(appName + "SearchList", searchList);
		map.put(appName + "BrowseList", browseList);
		map.put(appName + "HomePageList", homePageList);
		map.put(appName + "ColPageList", colPageList);
		map.put(appName + "Data", "[" + graphData + "]");
		map.put("periods", "[" + periods + "]");
		return map;
	}

	@Override
	public Collection<StatSummary> getStatSummary() throws Exception {
		return null;
	}
}
