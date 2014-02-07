package edu.ucsd.library.xdre.statistic.beans;

/**
 *
 * Class DLCollectionProcess
 * @author lsitu@ucsd.edu
 */
public class DAMSCollectionProcess{
	private String collectionId = null;
	private String collectionTitle = null;
	private String processId = null;
	private String processName = null;
	private String processArk = null;
	private int totalNumOfItems = 0;
	private int numOfItemsProcessed = 0;
	private int numOfItemsUpdated = 0;
	private String processedDate = null;
	private String updatedDate = null;
	private String processedBy = null;
	private String updatedBy = null;
	private String initialDate = null;
	
	public DAMSCollectionProcess(String processId, String processName, String collectionId, String collectionTitle, 
			int totalNumOfItems, int numOfItemsProcessed, String processedDate, String processedBy){
		this(processId, processName, collectionId, collectionTitle, totalNumOfItems, numOfItemsProcessed, processedDate, processedBy, processedDate, 0, "", "");
	}
	
	public DAMSCollectionProcess(String processId, String processName, String collectionId, String collectionTitle, 
			int totalNumOfItems, int numOfItemsProcessed, String processedDate, String processedBy, String initialDate){
		this(processId, processName, collectionId, collectionTitle, totalNumOfItems, numOfItemsProcessed, processedDate, processedBy, initialDate, 0, "", "");
	}
	
	public DAMSCollectionProcess(String processId, String processName, String collectionId, String collectionTitle, 
			int totalNumOfItems, int numOfItemsProcessed, String processedDate, String processedBy, String initialDate, int numOfItemsUpdated, String updatedDate, String updatedBy){
		this(processId, null, processName, collectionId, collectionTitle, totalNumOfItems, numOfItemsProcessed, processedDate, processedBy, initialDate, numOfItemsUpdated, updatedDate, updatedBy);
	}
	
	public DAMSCollectionProcess(String processId, String processArk, String processName, String collectionId, String collectionTitle, 
			int totalNumOfItems, int numOfItemsProcessed, String processedDate, String processedBy, String initialDate, int numOfItemsUpdated, String updatedDate, String updatedBy){
		this.processId = processId;
		this.processArk = processArk;
		this.processName = processName;
		this.collectionId = collectionId;
		this.collectionTitle = collectionTitle;
		this.totalNumOfItems = totalNumOfItems;
		this.numOfItemsProcessed = numOfItemsProcessed;
		this.processedDate = processedDate;
		this.processedBy = processedBy;
		this.initialDate = initialDate;
		this.numOfItemsUpdated = numOfItemsUpdated;
		this.updatedDate = updatedDate;
		this.updatedBy = updatedBy;
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
	
	public int getNumOfItemsProcessed() {
		return numOfItemsProcessed;
	}

	public void setNumOfItemsProcessed(int numOfItemsProcessed) {
		this.numOfItemsProcessed = numOfItemsProcessed;
	}

	public int getNumOfItemsUpdated() {
		return numOfItemsUpdated;
	}

	public void setNumOfItemsUpdated(int numOfItemsUpdated) {
		this.numOfItemsUpdated = numOfItemsUpdated;
	}

	public String getProcessedBy() {
		return processedBy;
	}

	public void setProcessedBy(String processedBy) {
		this.processedBy = processedBy;
	}

	public String getProcessedDate() {
		return processedDate;
	}

	public void setProcessedDate(String processedDate) {
		this.processedDate = processedDate;
	}

	public String getProcessId() {
		return processId;
	}

	public void setProcessId(String processId) {
		this.processId = processId;
	}

	public String getProcessName() {
		return processName;
	}

	public void setProcessName(String processName) {
		this.processName = processName;
	}

	public int getTotalNumOfItems() {
		return totalNumOfItems;
	}

	public void setTotalNumOfItems(int totalNumOfItems) {
		this.totalNumOfItems = totalNumOfItems;
	}

	public String getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(String updatedBy) {
		this.updatedBy = updatedBy;
	}

	public String getUpdatedDate() {
		return updatedDate;
	}

	public void setUpdatedDate(String updatedDate) {
		this.updatedDate = updatedDate;
	}

	public String getInitialDate() {
		return initialDate;
	}

	public void setInitialDate(String initialDate) {
		this.initialDate = initialDate;
	}

	public String getProcessArk() {
		return processArk;
	}

	public void setProcessArk(String processArk) {
		this.processArk = processArk;
	}

	public String toString(){
		return processId + "\t" + processName + "\t" + collectionTitle + "\t" + collectionId + "\t" + totalNumOfItems + "\t" + numOfItemsProcessed + "\t" + numOfItemsProcessed + "\t" + processedBy + "\n";
	}
}
