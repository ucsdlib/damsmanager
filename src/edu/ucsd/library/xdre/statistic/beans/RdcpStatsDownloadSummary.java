package edu.ucsd.library.xdre.statistic.beans;

import java.util.List;

/**
 * 
 * Class RdcpStatsDownloadSummary, fields for items statistics.
 * @author lsitu@ucsd.edu
 */
public class RdcpStatsDownloadSummary extends RdcpStatsItemSummary {
	protected String componentId = null;    // component id
	protected String fileId = null;         // file id
	protected String componentTitle = null; // component title

	public RdcpStatsDownloadSummary (String collectionTitle, String objectTitle, String componentTitle,
			String subjectId, String componentId, String fileId, List<String> periods, List<Integer> numOfViews) {
		super(collectionTitle, objectTitle,subjectId, periods, numOfViews);
		this.componentTitle = componentTitle;
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
