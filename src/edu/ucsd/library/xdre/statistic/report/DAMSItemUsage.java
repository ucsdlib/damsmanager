package edu.ucsd.library.xdre.statistic.report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
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
public class DAMSItemUsage extends StatsUsage{
	
	public DAMSItemUsage(String appName, Date start, Date end, Connection con){
		super(appName, start, end, con);
	}
	
	public Map<String, Object> getGraphData() throws Exception{
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		String months = "";
		String graphData = "";
		boolean isMarker = false;
		List<String> monthsList = null;
		List<String> objectList = null;
		List<String> accessList = null;
		List<String> viewList = null;
		List<String> objectViewList = null;

		
		monthsList = new ArrayList<String>();
		objectList = new ArrayList<String>();
		accessList = new ArrayList<String>();
		viewList = new ArrayList<String>();
		objectViewList = new ArrayList<String>();
		//ps = con.prepareStatement(OBJECT_USAGE_QUERY);
		String tmpVal = null;
		String statsQuery = applyQueryIpFilter(DLP_OBJECT_USAGE_QUERY);
		if(appName.equalsIgnoreCase("pas"))
			statsQuery = applyQueryIpFilter(PAS_OBJECT_USAGE_QUERY);
		try{
			ps = con.prepareStatement(statsQuery.replace("PERIOD_PARAM", statsFormat));
			ps.setString(1, dbFormat.format(start));
			ps.setString(2, dbFormat.format(end));
			rs= ps.executeQuery();
			while (rs.next()){
				tmpVal = rs.getString("period");
				//Graph ploting
				months += (months.length()>0?", '":"'") + formatOutput(tmpVal, false) + "'";	
				monthsList.add(formatOutput(tmpVal, true));
				
				tmpVal = rs.getString("num_access");
				//Graph ploting
				int val = Integer.parseInt(tmpVal);
				if( !isMarker && rs.getRow()> 1 && val>0){
					//graphData += (graphData.length()>0?", ":" ") + "{y: " + tmpVal + ", marker: { symbol: 'url(./images/" + appName +".gif)'}}";
					isMarker = true;
					graphData += (graphData.length()>0?", ":" ") + tmpVal;
				}else
					graphData += (graphData.length()>0?", ":" ") + tmpVal;
				
				accessList.add(tmpVal);
				viewList.add(rs.getString("num_view"));
				objectList.add(rs.getString("num_object"));
			}
		}finally{
			Statistics.close(rs);
			Statistics.close(ps);
			rs = null;
			ps = null;
		}
		
		try{
			statsQuery = applyQueryIpFilter(DISTINCT_OBJECT_USAGE_QUERY);
			if(appName.equalsIgnoreCase("pas"))
				statsQuery = applyQueryIpFilter(DISTINCT_PAS_OBJECT_USAGE_QUERY);
			ps = con.prepareStatement(statsQuery.replace("PERIOD_PARAM", statsFormat));
			ps.setString(1, dbFormat.format(start));
			ps.setString(2, dbFormat.format(end));
			rs= ps.executeQuery();
			while (rs.next()){				
				tmpVal = rs.getString("num_object");
				objectViewList.add(tmpVal);
			}
		}finally{
			Statistics.close(rs);
			Statistics.close(ps);
			rs = null;
			ps = null;
		}
		
		int pSize = monthsList.size();
		if(pSize < 12){
			tmpVal = "0";
			Calendar cal = Calendar.getInstance();
			cal.setTime(digitFormatMonthly.parse(monthsList.get(0)));
			cal.add(Calendar.DATE, 1);
			
			for(int i=0; i<12-pSize; i++){
				graphData = tmpVal + (graphData.length()>0?", ":" ") + graphData;
				cal.add(Calendar.MONTH, -1);
				
				months = "'" + outFormatMonthly.format(cal.getTime()) + (months.length()>0?"', ":"'") + months;
				monthsList.add(0, digitFormatMonthly.format(cal.getTime()));
				accessList.add(0, tmpVal);	
				viewList.add(0, tmpVal);
				objectList.add(0, tmpVal);
				objectViewList.add(0, tmpVal);
			}
		}
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("periodsList", monthsList);
		map.put(appName + "ObjectList", objectList);
		map.put(appName + "ObjectViewList", objectViewList);
		map.put(appName + "AccessList", accessList);
		map.put(appName + "ViewList", viewList);
		map.put(appName + "Data", "[" + graphData + "]");
		map.put("periods", "[" + months + "]");
		return map;
	}

	@Override
	public Collection<StatSummary> getStatSummary() throws Exception {
		return null;
	}
}
