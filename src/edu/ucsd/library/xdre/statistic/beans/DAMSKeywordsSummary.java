package edu.ucsd.library.xdre.statistic.beans;

/**
 * 
 * Class DLCKeywordsSummary, fields for keywords/phrases statistics.
 * @author lsitu@ucsd.edu
 */
public class DAMSKeywordsSummary extends StatSummary{
	private String keyword = null; 
	private String type = null;
	
	public DAMSKeywordsSummary(String period, int numOfAccess, String keyword, String type){
		super(period, numOfAccess);
		this.keyword =keyword;
		this.type = type;
	}

	public String getKeyword() {
		return keyword;
	}

	public void setKeyword(String keyword) {
		this.keyword = keyword;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

}
