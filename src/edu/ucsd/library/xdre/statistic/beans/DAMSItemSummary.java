package edu.ucsd.library.xdre.statistic.beans;

/**
 * 
 * Class DLPItemSummary, fields for unique items statistics.
 * @author lsitu@ucsd.edu
 */
public class DAMSItemSummary extends StatSummary{
	private int numOfViews = 0; //Number of times of the unique items were viewed.
	private int numOfItemAccessed = 0; //Number of items were accessed.
	private int numOfItemViewed = 0; //Number of items that were viewed.
	
	public DAMSItemSummary(String period, int numOfAccess, int numOfViews, int numOfItemAccessed){
		super(period, numOfAccess);
		this.numOfViews = numOfViews;
		this.numOfItemAccessed = numOfItemAccessed;
	}


	public void setNumOfViews(int numOfViews) {
		this.numOfViews = numOfViews;
	}

	public int getNumOfViews() {
		return numOfViews;
	}
	
	public int getNumOfItemAccessed() {
		return numOfItemAccessed;
	}

	public void setNumOfItemAccessed(int numOfItemAccessed) {
		this.numOfItemAccessed = numOfItemAccessed;
	}

	public int getNumOfItemViewed() {
		return numOfItemViewed;
	}

	public void setNumOfItemViewed(int numOfItemViewed) {
		this.numOfItemViewed = numOfItemViewed;
	}
	
}
