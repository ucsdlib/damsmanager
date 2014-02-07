package edu.ucsd.library.xdre.utils;

/**
 * Class to construct an Rights Action object
 * @author lsitu
 *
 */
public class RightsAction {
	private String id = null;
	private String oid = null;
	private String startDate = null;
	private String endDate = null;
	private String type = null;
	public RightsAction(String id, String startDate, String endDate, String type){
		this(id, startDate, endDate, type, null);
	}
	
	public RightsAction(String id, String startDate, String endDate, String type, String oid){
		this.id = id;
		this.startDate = startDate;
		this.endDate = endDate;
		this.oid = oid;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getStartDate() {
		return startDate;
	}

	public void setStartDate(String startDate) {
		this.startDate = startDate;
	}

	public String getEndDate() {
		return endDate;
	}

	public void setEndDate(String endDate) {
		this.endDate = endDate;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getOid() {
		return oid;
	}

	public void setOid(String oid) {
		this.oid = oid;
	}
}
