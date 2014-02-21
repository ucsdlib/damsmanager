package edu.ucsd.library.xdre.statistic.analyzer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
	private Map<String, String> unitsMap = null;
	private Map<String, Set<String>> unitsRecordsMap = null;
	private List<DAMSCollectionStats> collectionStatsList = null;
	private Set<String> collectionsTodo = null;
	private Calendar calendar = null;
	private SimpleDateFormat dbFormat = null;
	private StringBuilder failedItems = new StringBuilder();
	private boolean update = false;
	
	public DAMSQuantityStats(DAMSClient damsClient) throws Exception{
		this.damsClient = damsClient;
		dbFormat = new SimpleDateFormat(Statistics.DATE_FORMAT);
		collectionStatsList = new ArrayList<DAMSCollectionStats>();
		collectionsMap = DAMSClient.reverseMap(damsClient.listCollections());
		unitsRecordsMap = new HashMap<String, Set<String>>();
		unitsMap = DAMSClient.reverseMap(damsClient.listUnits());
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
			if(collectionsTodo == null || collectionsTodo.size() == 0){
				collectionsTodo = collectionsMap.keySet();
				for(Iterator<String> it=unitsMap.keySet().iterator(); it.hasNext();){
					String unitId = it.next();
					Set<String> uRecords = new HashSet<String>();
					uRecords.addAll(damsClient.listUnitObjects(unitId));
					unitsRecordsMap.put(unitId, uRecords);
				}
			}
			for(Iterator<String> it=collectionsTodo.iterator(); it.hasNext();){
				colId = it.next();
				for(Iterator<Set<String>> uit=unitsRecordsMap.values().iterator(); uit.hasNext();){
					Set<String> uRecords = uit.next();
					uRecords.remove(colId);
				}
				colTitle = (String)collectionsMap.get(colId);
				idx = colId.lastIndexOf("/");
				
				if(idx >= 0)
					colId = colId.substring(idx+1);
				try{
					String[] params = {Statistics.getDatabaseMonthFormater().format(calendar.getTime()), colId};
					rs = getQueryResult(con, Statistics.COLLECTION_STATS_RECORD_EXIST, params);
					exists = rs.next();
				}finally{
					if(rs != null){
						Statistics.close(rs.getStatement());	
						Statistics.close(rs);
						rs = null;
					}
				}
				
				colHandler = new StatsCollectionQuantityHandler(damsClient, colId);
				if(unitsRecordsMap.size() > 0){				
					for(Iterator<Set<String>> uit=unitsRecordsMap.values().iterator(); uit.hasNext();){
						Set<String> uRecords = uit.next();
						uRecords.removeAll(colHandler.getItems());
					}
				}
				if(!exists || update){
					try{
						System.out.println("Process collection: " + colTitle + "(" + colId + ") ... ");
						
						if(update && exists){
							String[] params = {Statistics.getDatabaseMonthFormater().format(calendar.getTime()), colId};
							int returnValue = executeUpdate(con, Statistics.COLLECTION_STATS_DELETE_RECORD, params);
							log.info("Deleted collection quantity statistics for " + colId + " month " + Statistics.getDatabaseMonthFormater().format(calendar.getTime()) + ". Return value: " + returnValue);
						}
						
						if(!colHandler.execute()){
							failedItems.append(colTitle + "(" + colId + "): " + colHandler.getExeInfo() + "\n");
							log.error("Failed to process collection "  + colTitle + "(" + colId + "): " + colHandler.getExeInfo());
						}else{
							int objsCount = colHandler.getObjectsCount();
							if( objsCount > 0){
								colStats = new DAMSCollectionStats(period, colId, colTitle, objsCount, colHandler.getDiskSize());
								collectionStatsList.add(colStats);
								System.out.println(period + " " + colTitle + ": " + objsCount + " objects; total size: " + colHandler.getDiskSize() + " bytes.");
							}else{
								log.info("No records found in collection "  + colTitle + "(" + colId + ").");
							}
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
			
			for(Iterator<String> uit=unitsRecordsMap.keySet().iterator(); uit.hasNext();){
				colId = uit.next();
				colTitle = unitsMap.get(colId);
				Set<String> uRecords = unitsRecordsMap.get(colId);

				idx = colId.lastIndexOf("/");			
				if(idx >= 0)
					colId = colId.substring(idx+1);
				try{
					String[] params = {Statistics.getDatabaseMonthFormater().format(calendar.getTime()), colId};
					rs = getQueryResult(con, Statistics.COLLECTION_STATS_RECORD_EXIST, params);
					exists = rs.next();
				}finally{
					if(rs != null){
						Statistics.close(rs.getStatement());	
						Statistics.close(rs);
						rs = null;
					}
				}
				
				if(!exists || update){
					if(update && exists){
						String[] params = {Statistics.getDatabaseMonthFormater().format(calendar.getTime()), colId};
						int returnValue = executeUpdate(con, Statistics.COLLECTION_STATS_DELETE_RECORD, params);
						log.info("Deleted collection quantity statistics for " + colId + " month " + Statistics.getDatabaseMonthFormater().format(calendar.getTime()) + ". Return value: " + returnValue);
					}
					if(uRecords.size() > 0){
						try{
							List<String> uItems = new ArrayList<String>();
							for(Iterator<String> it = uRecords.iterator(); it.hasNext();){
								String oid = it.next();
								uItems.add(oid);
								//System.out.println("Orphan: " + oid);
							}
							colHandler = new StatsCollectionQuantityHandler(damsClient, null);
							colHandler.setItems(uItems);
							colHandler.setCollectionId(colId);
							colHandler.setCollectionTitle(colTitle);
							if(!colHandler.execute()){
								failedItems.append(colTitle + "(" + colId + "): " + colHandler.getExeInfo() + "\n");
								log.error("Failed to process collection "  + colTitle + "(" + colId + "): " + colHandler.getExeInfo());
							}else{
								int objsCount = colHandler.getObjectsCount();
								if( objsCount > 0){
									colStats = new DAMSCollectionStats(period, colId, colTitle, objsCount, colHandler.getDiskSize());
									collectionStatsList.add(colStats);
									System.out.println(period + " " + colTitle + ": " + objsCount + " objects; total size: " + colHandler.getDiskSize() + " bytes.");
								}else{
									log.info("No records found in collection "  + colTitle + "(" + colId + ").");
								}
							}
						}finally{
							if(colHandler != null){
								colHandler.release();
								colHandler = null;
							}
						}
					}
				}
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
	
	public boolean isUpdate() {
		return update;
	}

	public void setUpdate(boolean update) {
		this.update = update;
	}

	public Set<String> getCollectionsTodo() {
		return collectionsTodo;
	}

	public void setCollectionsTodo(Set<String> collectionsTodo) {
		this.collectionsTodo = collectionsTodo;
	}

	public void export(Connection con) throws SQLException{
		PreparedStatement ps = null;
		try{
			ps = con.prepareStatement(Statistics.COLLECTION_STATS_INSERT);
			for(Iterator<DAMSCollectionStats> it= collectionStatsList.iterator(); it.hasNext();){
				it.next().export(ps, WebStatistic.getNextId(con));
			}
		}finally{
			Statistics.close(ps);
			ps = null;
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
			Statistics.close(rs);
			Statistics.close(ps);
			rs = null;
			ps = null;
			throw e;
		}
		return rs;
	}
	
	public static int executeUpdate(Connection con, String sql, String[] params) throws SQLException{
		PreparedStatement ps = null;
		int returnValue = -1;
		try{
			ps = con.prepareStatement(sql);
			for(int i=0; i<params.length; i++){
				ps.setString(i+1, params[i]);
			}
			returnValue = ps.executeUpdate();
			
		}finally{
			Statistics.close(ps);
			ps = null;
		}
		return returnValue;
	}
	public static long getItemsCount(DAMSClient damsClient, String collectionId) throws Exception{
		return damsClient.listAllRecords().size();
	}
}
