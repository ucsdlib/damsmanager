package edu.ucsd.library.xdre.statistic.beans;

import java.util.List;

/**
 * 
 * Class StatsCollectionItemSummary, fields for collection item hits, file downloads statistics.
 * @author lsitu@ucsd.edu
 */
public class StatsCollectionItemSummary implements Comparable<StatsCollectionItemSummary> {
	protected String collectionId = null;    // collection ark
	protected String collectionTitle = null; // collection title
	protected List<String> periods = null;   // stats time period list
	protected List<Integer> numOfViews = null;// number of views for each time period


	public StatsCollectionItemSummary (String collectionTitle, String collectionId,
			List<String> periods, List<Integer> numOfViews) {
		this.collectionId = collectionId;
		this.collectionTitle = collectionTitle;
		this.periods = periods;
		this.numOfViews = numOfViews;
	}

	public List<String> getPeriods() {
		return periods;
	}

	public void setPeriod(List<String> periods) {
		this.periods = periods;
	}

	public String getCollectionId() {
		return collectionId;
	}

	public void setCollectionId(String collectionId) {
		this.collectionId = collectionId;
	}

	public List<Integer> getNumOfViews() {
		return numOfViews;
	}

	public void setNumOfViews(List<Integer> numOfViews) {
		this.numOfViews = numOfViews;
	}

	public String getCollectionTitle() {
		return collectionTitle;
	}

	public void setCollectionTitle(String collectionTitle) {
		this.collectionTitle = collectionTitle;
	}
	public int compareTo(StatsCollectionItemSummary o) {
		return collectionTitle.compareToIgnoreCase(o.collectionTitle);
	}
}
