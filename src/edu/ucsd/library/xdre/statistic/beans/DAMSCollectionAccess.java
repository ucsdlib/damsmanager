package edu.ucsd.library.xdre.statistic.beans;

/**
 *
 * Class CollectionStats
 * @author lsitu@ucsd.edu
 */
public class DAMSCollectionAccess {
	private String period = null;
	private String collectionId = null;
	private String collectionTitle = null;
	private int numAccess = 0;
	
	public DAMSCollectionAccess(String period, String collectionId, String collectionTitle, int numAccess){
		this.period = period;
		this.collectionId = collectionId;
		this.collectionTitle = collectionTitle;
		this.numAccess = numAccess;
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

	public String getPeriod() {
		return period;
	}
	public void setPeriod(String period) {
		this.period = period;
	}
	public int getNumAccess() {
		return numAccess;
	}

	public void setNumAccess(int numAccess) {
		this.numAccess = numAccess;
	}

	public String toString(){
		return period + "\t" + collectionTitle + "\t" + collectionId + "\t" + numAccess;
	}
}
