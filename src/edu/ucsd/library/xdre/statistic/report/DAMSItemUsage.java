package edu.ucsd.library.xdre.statistic.report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		String statsQuery = DLP_OBJECT_USAGE_QUERY;
		if(appName.equalsIgnoreCase("pas"))
			statsQuery = PAS_OBJECT_USAGE_QUERY;
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
		
		try{
			statsQuery = DISTINCT_OBJECT_USAGE_QUERY;
			if(appName.equalsIgnoreCase("pas"))
				statsQuery = DISTINCT_PAS_OBJECT_USAGE_QUERY;
			ps = con.prepareStatement(statsQuery.replace("PERIOD_PARAM", statsFormat));
			ps.setString(1, dbFormat.format(start));
			ps.setString(2, dbFormat.format(end));
			rs= ps.executeQuery();
			while (rs.next()){				
				tmpVal = rs.getString("num_object");
				objectViewList.add(tmpVal);
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
