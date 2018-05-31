package edu.ucsd.library.xdre.statistic.beans;

import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.apache.commons.lang3.StringUtils;

/**
 * Class ObjectCounter counting for object access /views.
 * @author lsitu
 */
public class ObjectCounter {
    private String clientIp = null;
    private int access = 0;
    private int view = 0;

    public ObjectCounter(String clientIp){
        this.clientIp = clientIp;
    }

    public void increaseCounter(String file){
        access++;
        if(StringUtils.isBlank(file)){
            view++;
        }
    }

    public int getAccess() {
        return access;
    }

    public void setAccess(int access) {
        this.access = access;
    }

    public int getView() {
        return view;
    }

    public void setView(int view) {
        this.view = view;
    }

    public String getClientIp() {
        return clientIp;
    }

    /**
     * Persist statistics for object access /views
     * @param ps
     * @return
     * @throws SQLException
     */
    public int export(PreparedStatement ps) throws SQLException{
        ps.setInt(6, access);
        ps.setInt(7, view);
        ps.setString(8, clientIp);
        return ps.executeUpdate();
    }
    
    public String toString(){
        return clientIp + " " + view + " " + access;
    }
}
