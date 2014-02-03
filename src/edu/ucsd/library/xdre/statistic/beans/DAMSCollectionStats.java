package edu.ucsd.library.xdre.statistic.beans;

import java.sql.PreparedStatement;
import java.sql.SQLException;

/**
 *
 * Class CollectionStats
 * @author lsitu@ucsd.edu
 */
public class DAMSCollectionStats extends StatSummary{
	private String collectionId = null;
	private String collectionTitle = null;
	private long numOfItems = 0;
	private long diskSize = 0;
	
	public DAMSCollectionStats(String period, String collectionId, String collectionTitle, long numOfItems, long diskSize){
		this.period = period;
		this.collectionId = collectionId;
		this.collectionTitle = collectionTitle;
		this.numOfItems = numOfItems;
		this.diskSize = diskSize;
	}
	
	public String getCollectionId() {
		return collectionId;
	}
	public void setCollectionId(String collectionId) {
		this.collectionId = collectionId;
	}
	public String getCollectionTitle() {
		return collectionTitle;
	}
	public void setCollectionTitle(String collectionTitle) {
		this.collectionTitle = collectionTitle;
	}
	public long getDiskSize() {
		return diskSize;
	}
	public void setDiskSize(int diskSize) {
		this.diskSize = diskSize;
	}
	public long getNumOfItems() {
		return numOfItems;
	}
	public void setNumOfItems(int numOfItems) {
		this.numOfItems = numOfItems;
	}
	public String getPeriod() {
		return period;
	}
	public void setPeriod(String period) {
		this.period = period;
	}
	
	public int export(PreparedStatement ps, int id) throws SQLException{
		//SimpleDateFormat dbMonthFormat = new SimpleDateFormat(Statistics.MONTH_FORMAT);
		ps.clearParameters();
		ps.setInt(1, id);
		ps.setDate(2, java.sql.Date.valueOf(period));
		ps.setString(3, collectionId);
		ps.setString(4, collectionTitle);
		ps.setLong(5, numOfItems);
		ps.setLong(6, diskSize);
		return ps.executeUpdate();
	}
	
	public String toString(){
		return period + "\t" + collectionTitle + "\t" + collectionId + "\t" + numOfItems + "\t" + diskSize  + "\n";
	}
}
