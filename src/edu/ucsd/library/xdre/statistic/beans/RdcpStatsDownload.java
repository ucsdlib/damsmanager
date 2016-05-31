package edu.ucsd.library.xdre.statistic.beans;

/**
 * 
 * Class RdcpStatsDownload, fields for file download statistics.
 * @author lsitu@ucsd.edu
 */
public class RdcpStatsDownload extends RdcpStatsItem {
	protected String componentId = null; // component id
	protected String fileId = null;      // file name
	protected String componentTitle = null; // component title

	public RdcpStatsDownload(String period, String subjectId, String componentId, String fileId, int numOfViews) {
		super(period, subjectId, numOfViews);
		this.componentId = componentId;
		this.fileId = fileId;
	}

	public String getComponentId() {
		return componentId;
	}

	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}

	public String getFileId() {
		return fileId;
	}

	public void setFileId(String fileId) {
		this.fileId = fileId;
	}

	public String getComponentTitle() {
		return componentTitle;
	}

	public void setComponentTitle(String componentTitle) {
		this.componentTitle = componentTitle;
	}

}
