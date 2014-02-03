package edu.ucsd.library.xdre.statistic.beans;

/**
 *
 * Class DLCStatusItem
 * @author lsitu@ucsd.edu
 */
public class DAMStatusItem{
	private String subject = null;
	private String icon = "1-2a.jpg";
	private String title = null;
	private String localId = null;
	private String processId = null;
	private String processName = null;
	private String processArk = null;
	private String processedDate = null;
	private String processedBy = null;
	private String initialDate = null;
	private String notes = null;
	
	public DAMStatusItem(String subject, String localId, String title){
		this(subject, localId, title, null, null, null, null, null, null, null);
	}
	
	public DAMStatusItem(String subject, String localId, String title, String processId, String processArk, String processName, String processedDate, String processedBy, String initialDate, String notes){
		this.subject = subject;
		this.localId = localId;
		this.title = title;
		this.processId = processId;
		this.processArk = processArk;
		this.processName = processName;
		this.processedDate = processedDate;
		this.processedBy = processedBy;
		this.initialDate = initialDate;
		this.notes = notes;
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

	public String getLocalId() {
		return localId;
	}

	public void setLocalId(String localId) {
		this.localId = localId;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getIcon() {
		return icon;
	}

	public void setIcon(String icon) {
		this.icon = icon;
	}

	public String toString(){
		return subject + "\t" + localId + "\t" + title + "\t" + processId + "\t" + processName + "\t" + processedBy + "\n";
	}
}
