package edu.ucsd.library.xdre.statistic.beans;

import java.util.List;

/**
 * 
 * Class RdcpStatsItemSummary, fields for items statistics.
 * @author lsitu@ucsd.edu
 */
public class RdcpStatsItemSummary {
	protected String subjectId = null;       // item ark
	protected String collectionTitle = null; // collection title
	protected String title = null;           // object title
	protected List<String> periods = null;   // stats time period list
	protected List<Integer> numOfViews = null;// number of views for each time period


	public RdcpStatsItemSummary (String collectionTitle, String objectTitle, 
			String subjectId, List<String> periods, List<Integer> numOfViews) {
		this.collectionTitle = collectionTitle;
		this.title = objectTitle;
		this.subjectId = subjectId;
		this.periods = periods;
		this.numOfViews = numOfViews;
	}

	public List<String> getPeriods() {
		return periods;
	}

	public void setPeriod(List<String> periods) {
		this.periods = periods;
	}

	public String getSubjectId() {
		return subjectId;
	}

	public void setSubjectId(String subjectId) {
		this.subjectId = subjectId;
	}

	public List<Integer> getNumOfViews() {
		return numOfViews;
	}

	public void setNumOfViews(List<Integer> numOfViews) {
		this.numOfViews = numOfViews;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getCollectionTitle() {
		return collectionTitle;
	}

	public void setCollectionTitle(String collectionTitle) {
		this.collectionTitle = collectionTitle;
	}

}
