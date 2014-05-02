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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import edu.ucsd.library.xdre.statistic.analyzer.Statistics;
import edu.ucsd.library.xdre.statistic.beans.DAMSCollectionAccess;
import edu.ucsd.library.xdre.statistic.beans.StatSummary;

/**
 * 
 * Class DAMSCollectionsUsage generates the PAS/CAS collections usage reports.
 *
 * @author lsitu@ucsd.edu
 */
public class DAMSCollectionsAccess extends StatsUsage{
	Map<String, String> collectionsMap = null;
	
	public DAMSCollectionsAccess(String appName, Date start, Date end, Connection con){
		super(appName, start, end, con);
		collectionsMap = new HashMap<String, String>();
	}
	
	public Map<String, Object> getGraphData() throws Exception{
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		String period = null;
		String colId = null;
		String colTitle = null;
		int numAccess = 0;

		DAMSCollectionAccess dlpColAccess = null;
		List<String> monthsList = new ArrayList<String>();
		Map<String, DAMSCollectionAccess> colStats = null;
		Map<String, Map<String, DAMSCollectionAccess>> colAccessTmpMap = new HashMap<String, Map<String, DAMSCollectionAccess>>();
		String statsQuery = DLP_COLLECTIONS_ACCESS_QUERY;
		if(appName.equalsIgnoreCase("pas"))
			statsQuery = PAS_COLLECTIONS_ACCESS_QUERY;
		
		if(statsFormat.equals(StatsUsage.MONTHLY_FORMAT)){
			SimpleDateFormat outFormat = new SimpleDateFormat(statsFormat);
			Calendar cal = Calendar.getInstance();
			cal.setTime(start);
			while(cal.getTime().before(end)){
				monthsList.add(outFormat.format(cal.getTime()));
				cal.add(Calendar.MONTH, 1);
			}
		}
		try{
			ps = con.prepareStatement(statsQuery.replace("PERIOD_PARAM", statsFormat));
			ps.setString(1, dbFormat.format(start));
			ps.setString(2, dbFormat.format(end));
			rs= ps.executeQuery();
			while (rs.next()){
				period = rs.getString("period");
				colId = rs.getString("colId");
				numAccess = rs.getInt("num_access");
				if(monthsList.indexOf(period)<0)
					monthsList.add(period);
				colTitle = collectionsMap.get(colId);
				if(colTitle == null){
					colTitle = colId;
					continue;
				}
				
				dlpColAccess = new DAMSCollectionAccess(period, colId, collectionsMap.get(colId), numAccess);
				
				if((colStats=colAccessTmpMap.get(colTitle)) == null){
					colStats = new TreeMap<String, DAMSCollectionAccess>();
					colAccessTmpMap.put(colTitle, colStats);
				}
				colStats.put(period, dlpColAccess);
			}
		}finally{
			Statistics.close(rs);
			Statistics.close(ps);
			rs = null;
			ps = null;
		}
		
		int idx = -1;
		int[] colStatsArr = null;
		Map<String, int[]> colStatsData = new TreeMap<String, int[]>();
		for(Iterator<String> it=colAccessTmpMap.keySet().iterator();it.hasNext();){
			colStatsArr = new int[monthsList.size()]; 
			colTitle = it.next();
			colStats = colAccessTmpMap.get(colTitle);
			for(Iterator<String> colIt=colStats.keySet().iterator();colIt.hasNext();){
				period = colIt.next();
				idx = monthsList.indexOf(period);
				colStatsArr[idx] = ((DAMSCollectionAccess)colStats.get(period)).getNumAccess();
			}
			colStatsData.put(colTitle, colStatsArr);
		}
		
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("periodsList", monthsList);
		map.put("colStatsData", colStatsData);
		map.put(appName + "Data", "[]");
		map.put("periods", "[]");
		return map;
	}

	@Override
	public Collection<StatSummary> getStatSummary() throws Exception {
		return null;
	}

	public Map<String, String> getCollectionsMap() {
		return collectionsMap;
	}

	public void setCollectiotMap(Map<String, String> CollectionsMap) {
		this.collectionsMap = CollectionsMap;
	}
	
	
}
