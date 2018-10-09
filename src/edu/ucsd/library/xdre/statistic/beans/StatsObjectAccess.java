package edu.ucsd.library.xdre.statistic.beans;

import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;

/**
 * Class ObjectAccess counting on objects for dams statistics
 * @author lsitu
 */
public class StatsObjectAccess {
    private static Logger log = Logger.getLogger(StatsObjectAccess.class);

    private Map<String, ObjectCounter> objectCounters = new TreeMap<>();
    private Map<String, FileDownloadCounter> fileDownloads = new TreeMap<>();

    /**
     * Utility function to count on object access and views
     * @param file
     * @param clientIp
     */
    public void increaseObjectAccess(String subjectId, String file, String clientIp) {
        ObjectCounter counter = objectCounters.get(subjectId);
        if (counter == null) {
            counter = new ObjectCounter(clientIp);
            objectCounters.put(subjectId, counter);
        }
        counter.increaseCounter(file);
    }

    /**
     * Utility function to count on file downloads
     * @param file
     * @param clientIp
     */
    public void increaseFileDownloads(String subjectId, String file, String clientIp) {
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

        String fileKey = file + "::" + subjectId; // the composite key for file downloads with client IP 
        FileDownloadCounter counter = fileDownloads.get(fileKey);
        if (counter == null) {
            counter = new FileDownloadCounter(subjectId, cid, fid, clientIp);
            fileDownloads.put(fileKey, counter);
        }
        counter.increaseCounter();
    }

    public Map<String, ObjectCounter> getObjectCounters(){
        return objectCounters;
    }

    public Map<String, FileDownloadCounter> getFileDownloads(){
        return fileDownloads;
    }

    public ObjectCounter getCounter(String key) {
        return objectCounters.get(key);
    }

    public FileDownloadCounter getFileDownloadCounter(String key) {
        return fileDownloads.get(key);
    }

    public String toString() {
        StringBuilder builder = new StringBuilder();
        for (String sid : objectCounters.keySet()) {
            builder.append(sid + " => " + objectCounters.get(sid) + "\n");
        }

        for (String sid : fileDownloads.keySet()) {
            builder.append(sid + " => " + fileDownloads.get(sid) + "\n");
        }
        return builder.toString();
    }
}
