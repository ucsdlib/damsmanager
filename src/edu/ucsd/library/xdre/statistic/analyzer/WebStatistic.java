package edu.ucsd.library.xdre.statistic.analyzer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * Class WebStatistic
 *
 * @author lsitu@ucsd.edu
 */
public class WebStatistic extends Statistics{

	private static Logger log = Logger.getLogger(WebStatistic.class);
	
	public WebStatistic(String appName){
		super(appName);
	}
	
	public void addAccess(String uri){
		numAccess++;
	}
	
	public String getParamValue(String param){
		String[] pair = param.split("=");
		if(pair.length > 1 && pair[1] != null)
			return pair[1].trim();
		else
			return "";
	}
	
	public void export(Connection con) throws SQLException{
		int nextId = getNextId(con);
		int returnValue = -1;
		PreparedStatement ps = null;
		
		//Update record for the calendar date
		if(update){
			try{
				ps = con.prepareStatement(WEB_STATS_DELETE_RECORD);
				ps.setString(1, dateFormat.format(calendar.getTime()));
				ps.setString(2, appName);
				returnValue = ps.executeUpdate();
				log.info("Deleted " + appName + " statistics record for date " + dateFormat.format(calendar.getTime()));
			}finally{
				Statistics.close(ps);
				ps = null;
			}
		}
		//WEB_STATS insert
		try{
			ps = con.prepareStatement(WEB_STATS_INSERT);
			ps.setInt(1, nextId);
			ps.setDate(2, java.sql.Date.valueOf(dateFormat.format(calendar.getTime())));
			ps.setInt(3, numAccess);
			ps.setString(4, appName);
			returnValue = ps.executeUpdate();
		}finally{
			Statistics.close(ps);
			ps = null;
		}
		log.info("Inserted " + appName + " statistics record for " + dateFormat.format(calendar.getTime()));
	}
	
	public void print(){
		System.out.println(appName + " - ");
		System.out.println("	Access: " + numAccess);
	}
}
