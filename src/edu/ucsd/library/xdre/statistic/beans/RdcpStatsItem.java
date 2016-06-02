package edu.ucsd.library.xdre.statistic.beans;

/**
 * 
 * Class RdcpStatsItem, fields for items statistics.
 * @author lsitu@ucsd.edu
 */
public class RdcpStatsItem {
	protected String period = null;    // stats time period
	protected String subjectId = null; // item ark
	protected int numOfViews = 0;      // Number of times of the unique items were viewed.

	public RdcpStatsItem(String period, String subjectId, int numOfViews) {
		this.period = period;
		this.subjectId = subjectId;
		this.numOfViews = numOfViews;
	}

	public String getPeriod() {
		return period;
	}

	public void setPeriod(String period) {
		this.period = period;
	}

	public String getSubjectId() {
		return subjectId;
	}

	public void setSubjectId(String subjectId) {
		this.subjectId = subjectId;
	}

	public int getNumOfViews() {
		return numOfViews;
	}

	public void setNumOfViews(int numOfViews) {
		this.numOfViews = numOfViews;
	}

}
