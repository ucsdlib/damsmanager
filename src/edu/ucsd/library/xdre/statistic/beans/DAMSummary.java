package edu.ucsd.library.xdre.statistic.beans;

/**
 * 
 * Class DLPStatsSummary, fields for statistics summary.
 * @author lsitu@ucsd.edu
 */
public class DAMSummary extends StatSummary{
	//Usages 	Queries 	Peeks 	Views 	Items Used 	Items Total 	Collections 	Size(mb)
	private int numOfQueries = 0;
	private DAMSItemSummary itemSummary = null;
	private int numOfCollections = 0;
	private int numOfItems = 0; //Number of unique items.
	//Total size in megabytes
	private long totalSize = 0;
	
	public DAMSummary(String period, int numOfUsage, int numOfQueries){
		super(period, numOfUsage);
		this.numOfQueries = numOfQueries;
	}
	
	public DAMSummary(String period, int numOfAccess, int numOfQueries, DAMSItemSummary itemSummary){
		this(period, numOfAccess, numOfQueries);
		this.itemSummary = itemSummary;
	}

	public DAMSItemSummary getItemSummary() {
		return itemSummary;
	}

	public void setItemSummary(DAMSItemSummary itemSummary) {
		this.itemSummary = itemSummary;
	}

	public int getNumOfQueries() {
		return numOfQueries;
	}

	public void setNumOfQueries(int numOfQueries) {
		this.numOfQueries = numOfQueries;
	}

	public int getNumOfCollections() {
		return numOfCollections;
	}

	public void setNumOfCollections(int numOfCollections) {
		this.numOfCollections = numOfCollections;
	}

	public int getNumOfItems() {
		return numOfItems;
	}

	public void setNumOfItems(int numOfItems) {
		this.numOfItems = numOfItems;
	}

	public long getTotalSize() {
		return totalSize;
	}

	public void setTotalSize(long totalSize) {
		this.totalSize = totalSize;
	}
}
