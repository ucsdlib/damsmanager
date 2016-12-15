package edu.ucsd.library.xdre.statistic.beans;
/**
 * 
 * Class StatsCollectionItem, fields for collection item hits, file downloads statistics.
 * @author lsitu@ucsd.edu
 */
public class StatsCollectionItem {
	protected String collectionId = null; // collection ark
	protected String period = null;       // stats time period list
	protected int numOfViews = 0;       // number of views for each time period


	public StatsCollectionItem (String collectionId, String period, int numOfViews) {
		this.collectionId = collectionId;
		this.period = period;
		this.numOfViews = numOfViews;
	}

	public String getPeriod() {
		return period;
	}

	public void setPeriod(String period) {
		this.period = period;
	}

	public String getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(String collectionId) {
		this.collectionId = collectionId;
	}

	public int getNumOfViews() {
		return numOfViews;
	}

	public void setNumOfViews(int numOfViews) {
		this.numOfViews = numOfViews;
	}
}
