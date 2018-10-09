package edu.ucsd.library.xdre.statistic.beans;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;

/**
 * Class FileDownloadCounter counting for file downloads.
 * @author lsitu
 */
public class FileDownloadCounter {
    private String oid = null;
    private String cid = null;
    private String fid = null;
    private String clientIp = null;
    private int view = 0;

    public FileDownloadCounter(String oid, String cid, String fid, String clientIp) {
        this.oid = oid;
        this.cid = cid;
        this.fid = fid;
        this.clientIp = clientIp;
    }

    public void increaseCounter(){
        view++;
    }
    
    public int getView() {
        return view;
    }

    public String getClientIp() {
        return clientIp;
    }

    public String getOid() {
        return oid;
    }

    public void setOid(String oid) {
        this.oid = oid;
    }

    public String getCid() {
        return cid;
    }

    public void setCid(String cid) {
        this.cid = cid;
    }

    public String getFid() {
        return fid;
    }

    public void setFid(String fid) {
        this.fid = fid;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public void setView(int view) {
        this.view = view;
    }

    /**
     * Persist the statistics for file downloads
     * @param ps
     * @return
     * @throws SQLException
     */
    public int export(PreparedStatement ps) throws SQLException{
        ps.setString(6, cid);
        ps.setString(7, fid);
        ps.setInt(8, view);
        ps.setString(9, clientIp);
        return ps.executeUpdate();
    }
    
    public String toString(){
        return clientIp + " /" + oid + (StringUtils.isBlank(cid) ? "" : "/" + cid) +  "/" + fid + " " + view;
    }
}
