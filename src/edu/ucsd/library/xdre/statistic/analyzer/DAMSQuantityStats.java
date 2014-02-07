package edu.ucsd.library.xdre.statistic.analyzer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.collection.StatsCollectionQuantityHandler;
import edu.ucsd.library.xdre.statistic.beans.DAMSCollectionStats;
import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * Class CollectionQuanStats calculate the collection stats for monthly report.
 * 
 * @author lsitu@ucsd.edu
 */
public class DAMSQuantityStats {
	
	private static Logger log = Logger.getLogger(DAMSQuantityStats.class);
	public static final String SIO_COLLECTION = "bb06151725";
	public static final String ETD_COLLECTION = "bb0956474h";
	public static final String VRC_COLLECTION = "bb2253416m";
	public static final String STAR_COLLECTION = "bb8738126n";
	public static final String LAMBERT_COLLECTION = "bb0785910t";

	private DAMSClient damsClient = null;
	private Map<String, String> collectionsMap = null;
	private List<DAMSCollectionStats> collectionStatsList = null;
	private Calendar calendar = null;
	private SimpleDateFormat dbFormat = null;
	private StringBuilder failedItems = new StringBuilder();
	
	public DAMSQuantityStats(DAMSClient damsClient) throws Exception{
		this.damsClient = damsClient;
		dbFormat = new SimpleDateFormat(Statistics.DATE_FORMAT);
		collectionStatsList = new ArrayList<DAMSCollectionStats>();
		collectionsMap = DAMSClient.reverseMap(damsClient.listCollections());
		calendar = Calendar.getInstance();
		calendar.set(Calendar.DATE, -1);
	}

	public void doStatistics(Connection con) throws Exception{
		String colTitle = null;
		String colId = null;
		StatsCollectionQuantityHandler colHandler = null;
		String period = dbFormat.format(calendar.getTime());
		DAMSCollectionStats colStats = null;
		ResultSet rs = null;
		boolean exists = false;
		int idx = -1;
		synchronized(log){
			for(Iterator<String> it=collectionsMap.keySet().iterator(); it.hasNext();){
				colId = it.next();
				colTitle = (String)collectionsMap.get(colId);
				idx = colId.lastIndexOf("/");
				
				if(idx >= 0)
					colId = colId.substring(idx+1);
				try{
					String[] params = {Statistics.getDatabaseMonthFormater().format(calendar.getTime()), colId};
					rs = getQueryResult(con, Statistics.COLLECTION_STATS_RECORD_EXIST, params);
					exists = rs.next();
				}finally{
					Statistics.close(rs);
					rs = null;
				}
				
				if(!exists){
					try{
						System.out.println("Process collection: " + colTitle + "(" + colId + ") ... ");
						colHandler = new StatsCollectionQuantityHandler(damsClient, colId);

						if(!colHandler.execute()){
							failedItems.append(colTitle + "(" + colId + "): " + colHandler.getExeInfo() + "\n");
							log.error("Failed to process collection "  + colTitle + "(" + colId + "): " + colHandler.getExeInfo());
						}
						int objsCount = colHandler.getObjectsCount();
						if( objsCount > 0){
							colStats = new DAMSCollectionStats(period, colId, colTitle, objsCount, ((StatsCollectionQuantityHandler)colHandler).getDiskSize());
							collectionStatsList.add(colStats);
							System.out.println(period + " " + colTitle + ": " + objsCount + " objects; total size: " + ((StatsCollectionQuantityHandler)colHandler).getDiskSize() + " bytes.");
						}else{
							log.info("No records found in collection "  + colTitle + "(" + colId + ").");
						}
						
					}finally{
						if(colHandler != null){
							colHandler.release();
							colHandler = null;
						}
					}
				}else
					log.info(colTitle + "(" + colId + ") exists for month ended on " + Statistics.getDatabaseDateFormater().format(calendar.getTime()));
			}
		}
	}

	public Calendar getCalendar() {
		return calendar;
	}

	public void setCalendar(Calendar calendar) {
		this.calendar = calendar;
	}
	
	public String getFailedItems(){
		return failedItems.toString();
	}
	
	public void export(Connection con) throws SQLException{
		PreparedStatement ps = null;
		try{
			ps = con.prepareStatement(Statistics.COLLECTION_STATS_INSERT);
			for(Iterator<DAMSCollectionStats> it= collectionStatsList.iterator(); it.hasNext();){
				it.next().export(ps, WebStatistic.getNextId(con));
			}
		}finally{
			if(ps != null){
				try{
					ps.close();
				}catch(Exception e){e.printStackTrace();}
				ps = null;
			}
		}
	}
	
	public void print() throws SQLException{
		for(Iterator<DAMSCollectionStats> it=collectionStatsList.iterator(); it.hasNext();){
			it.next().toString();
		}
	}
	
	public static ResultSet getQueryResult(Connection con, String sql, String[] params) throws SQLException{
		PreparedStatement ps = null;
		ResultSet rs = null;
		try{
			ps = con.prepareStatement(sql);
			for(int i=0; i<params.length; i++){
				ps.setString(i+1, params[i]);
			}
			rs = ps.executeQuery();
		}catch(SQLException e){
			e.printStackTrace();
			if(rs != null){
				Statistics.close(rs);
				rs = null;
				ps = null;
			}
			throw e;
		}
		return rs;
	}
	
	public static long getItemsCount(DAMSClient damsClient, String collectionId) throws Exception{
		return damsClient.listAllRecords().size();
	}
}
