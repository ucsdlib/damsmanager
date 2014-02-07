package edu.ucsd.library.xdre.statistic.report;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.statistic.analyzer.Statistics;
import edu.ucsd.library.xdre.statistic.beans.DAMSItemSummary;
import edu.ucsd.library.xdre.statistic.beans.DAMSummary;
import edu.ucsd.library.xdre.statistic.beans.StatSummary;

/**
 * Class DLPUsage - Statistic summary for DLC.
 *
 * @author lsitu@ucsd.edu
 */
public class DAMSUsage extends StatsUsage {
	private static Logger log = Logger.getLogger(DAMSUsage.class);
	private String collectionFilter = null;
	
	public DAMSUsage(String appName, Date start, Date end, Connection con) {
		super(appName, start, end, con);
	}

	@Override
	public Map<String, Object> getGraphData() throws Exception {
		return null;
	}

	public String getCollectionFilter() {
		return collectionFilter;
	}

	public void setCollectionFilter(String collectionFilter) {
		this.collectionFilter = collectionFilter;
	}

	@Override
	public Collection<StatSummary> getStatSummary() throws Exception {
		
		Map<String, StatSummary> dlpSum = new TreeMap<String, StatSummary>();
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		//PAS/CAS usage.
		String period = null;
		DAMSummary statSum = null;
		String usageQuery = DLP_USAGE_QUERY;
		try{
			if(appName != null && appName.equalsIgnoreCase("pas"))
				usageQuery = PAS_USAGE_QUERY;
			ps = con.prepareStatement(usageQuery.replace("PERIOD_PARAM", statsFormat));
			ps.setString(1, dbFormat.format(start));
			ps.setString(2, dbFormat.format(end));
			rs = ps.executeQuery();
			while(rs.next()){
				period = rs.getString("period");
				statSum = new DAMSummary(period, rs.getInt("num_access"), rs.getInt("num_search")+rs.getInt("num_browse"));
				statSum.setPeriodDisplay(formatOutput(period, true));
				dlpSum.put(statSum.getPeriod(), statSum);
			}
		}finally{
			Statistics.close(rs);
			Statistics.close(ps);
			rs = null;
			ps = null;
		}
		
		//DAMS unique items usage
		DAMSItemSummary itemSum = null;
		String statsQuery = DLP_OBJECT_USAGE_QUERY;
		if(appName.equalsIgnoreCase("pas"))
			statsQuery = PAS_OBJECT_USAGE_QUERY;

		try{
			ps = con.prepareStatement(statsQuery.replace("PERIOD_PARAM", statsFormat));
			ps.setString(1, dbFormat.format(start));
			ps.setString(2, dbFormat.format(end));
			rs = ps.executeQuery();
			while(rs.next()){
				period = rs.getString("period");
				itemSum = new DAMSItemSummary(period, rs.getInt("num_access"), rs.getInt("num_view"), rs.getInt("num_object"));
				statSum = (DAMSummary) dlpSum.get(period);
				if(statSum != null)
					statSum.setItemSummary(itemSum);
				else
					log.error("DLP Statistic Summary for period " + period + " doesn't exist.");
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
		
		//Number of unique items viewed
		try{
			statsQuery = DISTINCT_OBJECT_USAGE_QUERY;
			if(appName.equalsIgnoreCase("pas"))
				statsQuery = DISTINCT_PAS_OBJECT_USAGE_QUERY;
			ps = con.prepareStatement(statsQuery.replace("PERIOD_PARAM", statsFormat));
			ps.setString(1, dbFormat.format(start));
			ps.setString(2, dbFormat.format(end));
			rs = ps.executeQuery();
			while(rs.next()){
				period = rs.getString("period");
				statSum = (DAMSummary) dlpSum.get(period);
				if(statSum != null){
					itemSum = statSum.getItemSummary();
					if(itemSum != null)
						itemSum.setNumOfItemViewed(rs.getInt("num_object"));
					else
						log.error("DLP Statistic item Summary for period " + period + " doesn't exist.");
				}else
					log.error("DLP Statistic Summary for period " + period + " doesn't exist.");
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
		
		//Collection quantity number of collections and disk size
		try{
			statsQuery = DLP_COLLECTION_QUANTITY_QUERY.replace("PERIOD_PARAM", statsFormat);
			if(collectionFilter != null && collectionFilter.length() > 0)
				statsQuery = statsQuery.replace("COLLECTION_FILTER", collectionFilter);
			else
				statsQuery = statsQuery.replace("AND COLLECTION_ID IN ( COLLECTION_FILTER ) ", "");
			ps = con.prepareStatement(statsQuery);

			ps.setString(1, dbFormat.format(start));
			ps.setString(2, dbFormat.format(end));
			rs = ps.executeQuery();
			while(rs.next()){
				period = rs.getString("period");
				statSum = (DAMSummary) dlpSum.get(period);
				if(statSum != null){
					statSum.setTotalSize( rs.getLong("disk_size"));
					statSum.setNumOfCollections((int)rs.getLong("num_collections"));
					statSum.setNumOfItems( rs.getInt("num_items"));
				}else
					log.error("DLP Statistic Summary for period " + period + " doesn't exist.");
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


		return dlpSum.values();
	}
}
