package edu.ucsd.library.xdre.statistic.beans;

/**
 * 
 * Class DLPObject
 * @author lsitu@ucsd.edu
 */
public class DAMSItem {
	private String subject = null;
	private String title = null;
	private String collection = null;
	private String collectionTitle = null;
	private String icon = null;
	private String stateAndView = null;
	private int numAccess = 0;
	private int numView = 0;
	private String clusterHost = null;

	public DAMSItem(String subject, int numAccess, int numView){
		this.subject = subject;
		this.numAccess = numAccess;
		this.numView = numView;
	}
	public int getNumAccess() {
		return numAccess;
	}
	public void setNumAccess(int numAccess) {
		this.numAccess = numAccess;
	}
	public int getNumView() {
		return numView;
	}
	public void setNumView(int numView) {
		this.numView = numView;
	}
	public String getSubject() {
		return subject;
	}
	public void setSubject(String subject) {
		this.subject = subject;
	}
	public String getCollection() {
		return collection;
	}
	public void setCollection(String collection) {
		this.collection = collection;
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
	public String getIcon() {
		return icon;
	}
	public void setIcon(String icon) {
		this.icon = icon;
	}
	public String getStateAndView() {
		return stateAndView;
	}
	public void setStateAndView(String stateAndView) {
		this.stateAndView = stateAndView;
	}
	public String getClusterHost() {
		return clusterHost;
	}
	public void setClusterHost(String clusterHost) {
		this.clusterHost = clusterHost;
	}
}
