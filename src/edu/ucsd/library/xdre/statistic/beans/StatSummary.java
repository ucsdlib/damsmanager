package edu.ucsd.library.xdre.statistic.beans;

/**
 * 
 * Class StatsSummary, fields for statistics summary.
 * @author lsitu@ucsd.edu
 */
public class StatSummary {
	protected String period = null;
	protected int numOfUsage = 0;
	protected String periodDisplay = null;
	
	public StatSummary(){}
	
	public StatSummary(String period, int numOfUsage){
		this.period = this.periodDisplay = period;
		this.numOfUsage = numOfUsage;
	}
	public int getNumOfUsage() {
		return numOfUsage;
	}
	public void setNumOfUsage(int numOfUsage) {
		this.numOfUsage = numOfUsage;
	}
	public String getPeriod() {
		return period;
	}
	public void setPeriod(String period) {
		this.period = period;
	}

	public String getPeriodDisplay() {
		return periodDisplay;
	}

	public void setPeriodDisplay(String periodDisplay) {
		this.periodDisplay = periodDisplay;
	}
}
