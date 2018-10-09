package edu.ucsd.library.xdre.statistic.beans;

import java.sql.PreparedStatement;

/**
 * Class DAMStatisticCounter counting accesses for dams.
 *
 * @author lsitu@ucsd.edu
 */
public class WebStatsCounter {
    protected int numAccess = 0;
    protected String clientIp = null;

    public WebStatsCounter(String clientIp) {
        this.clientIp = clientIp;
    }

    public void addAccess() {
        numAccess++;
    }

    public int getNumAccess() {
        return numAccess;
    }

    public void setNumAccess(int numAccess) {
        this.numAccess = numAccess;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public int export(PreparedStatement ps) throws Exception{
        //WEB_STATS insert
        ps.setInt(3, numAccess);
        ps.setString(5, clientIp);
        return ps.executeUpdate();
    }

    public String toString(){
        return clientIp + " " + numAccess;
    }
}
