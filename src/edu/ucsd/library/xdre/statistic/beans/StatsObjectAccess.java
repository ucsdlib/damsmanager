package edu.ucsd.library.xdre.statistic.beans;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * Class ObjectAccess counting on objects for dams statistics
 * @author lsitu
 */
public class StatsObjectAccess {
    private static Logger log = Logger.getLogger(StatsObjectAccess.class);

    private String subjectId = null;
    private Map<String, ObjectCounter> objectCounters = new TreeMap<>();
    private Map<String, FileDownloadCounter> fileDownloads = new TreeMap<>();

    public StatsObjectAccess(String subjectId){
        this.subjectId = subjectId;
    }

    /**
     * Utility function to count on object access and views
     * @param file
     * @param clientIp
     */
    public void increaseObjectAccess(String file, String clientIp) {
        ObjectCounter counter = objectCounters.get(clientIp);
        if (counter == null) {
            counter = new ObjectCounter(clientIp);
            objectCounters.put(clientIp, counter);
        }
        counter.increaseCounter(file);
    }

    /**
     * Utility function to count on file downloads
     * @param file
     * @param clientIp
     */
    public void increaseFileDownloads(String file, String clientIp) {
        String cid = "";
        String fid = "";
        String[] tokens = file.split("_");
        if (tokens.length == 2) {
            fid = tokens[1];
        } else if (tokens.length == 3) {
            cid = tokens[1];
            fid = tokens[2];
        } else {
            log.warn("Invalid file url: /" + subjectId + "/" + file );
        }

        String fileKey = file + "::" + clientIp; // the composite key for file downloads with client IP 
        FileDownloadCounter counter = fileDownloads.get(fileKey);
        if (counter == null) {
            counter = new FileDownloadCounter(cid, fid, clientIp);
            fileDownloads.put(fileKey, counter);
        }
        counter.increaseCounter();
    }

    public String getSubjectId() {
        return subjectId;
    }

    public Map<String, ObjectCounter> getObjectCounters(){
        return objectCounters;
    }

    public Map<String, FileDownloadCounter> getFileDownloads(){
        return fileDownloads;
    }

    public int export(PreparedStatement ps) throws SQLException {
        int updatedCount = 0;
        for (ObjectCounter objectCounter : objectCounters.values() ) {
            updatedCount += objectCounter.export(ps);
        }
        return updatedCount;
    }

    public ObjectCounter getCounter(String key) {
        return objectCounters.get(key);
    }

    public FileDownloadCounter getFileDownloadCounter(String key) {
        return fileDownloads.get(key);
    }
}
