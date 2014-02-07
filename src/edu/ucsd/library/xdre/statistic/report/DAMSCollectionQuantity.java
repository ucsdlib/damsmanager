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

import edu.ucsd.library.xdre.statistic.beans.DAMSCollectionStats;
import edu.ucsd.library.xdre.statistic.beans.StatSummary;

public class DAMSCollectionQuantity extends StatsUsage {
	private static Logger log = Logger.getLogger(DAMSCollectionQuantity.class);
	
	public DAMSCollectionQuantity(String appName, Date start, Date end, Connection con) {
		super(appName, start, end, con);
	}

	@Override
	public Map<String, Object> getGraphData() throws Exception {
		return null;
	}

	@Override
	public Collection<StatSummary> getStatSummary() throws Exception {
		
		Map<String, StatSummary> dlpSum = new TreeMap<String, StatSummary>();
		
		PreparedStatement ps = null;
		ResultSet rs = null;
		
		//PAS/CAS usage.
		String period = null;
		StatSummary statSum = null;
		try{
			ps = con.prepareStatement(DLP_COLLECTION_QUANTITY_QUERY.replace("PERIOD_PARAM", statsFormat));
			ps.setString(1, dbFormat.format(start));
			ps.setString(2, dbFormat.format(end));
			rs = ps.executeQuery();
			while(rs.next()){
				period = rs.getString("period");
				statSum = new DAMSCollectionStats(period, null, null, rs.getLong("num_collections"), rs.getLong("disk_size"));
				dlpSum.put(statSum.getPeriod(), statSum);
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
