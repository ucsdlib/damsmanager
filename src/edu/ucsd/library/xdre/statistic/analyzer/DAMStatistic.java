package edu.ucsd.library.xdre.statistic.analyzer;

import java.io.UnsupportedEncodingException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;

import edu.ucsd.library.xdre.statistic.beans.FileDownloadCounter;
import edu.ucsd.library.xdre.statistic.beans.ObjectCounter;
import edu.ucsd.library.xdre.statistic.beans.StatsDlcKeywordsCounter;
import edu.ucsd.library.xdre.statistic.beans.StatsDlpColAccessCounter;
import edu.ucsd.library.xdre.statistic.beans.StatsDlpCounter;
import edu.ucsd.library.xdre.statistic.beans.StatsObjectAccess;
import edu.ucsd.library.xdre.statistic.beans.WebStatsCounter;
import edu.ucsd.library.xdre.utils.Constants;

/**
 * Class DAMStatistic
 *
 * @author lsitu@ucsd.edu
 */
public class DAMStatistic extends Statistics{
    private static Logger log = Logger.getLogger(DAMStatistic.class);

    protected Map<String, WebStatsCounter> ipWebStatsMap = null;
    protected Map<String, StatsDlpCounter> ipStatsDlpMap = null;
    protected Map<String, StatsObjectAccess> ipItemsMap = null;
    protected Map<String, StatsObjectAccess> ipItemsMapPrivate = null;
    protected Map<String, StatsDlcKeywordsCounter> ipKeywordsMap = null;
    protected Map<String, StatsDlpColAccessCounter> ipColAccessMap = null;
    protected Map<String, String> colsMap = null;
    protected List<String> derivativeList = null;

    public DAMStatistic(String appName){
        super(appName);
        ipWebStatsMap = new HashMap<>();
        ipStatsDlpMap = new HashMap<>();
        ipItemsMap = new HashMap<>();
        ipItemsMapPrivate = new HashMap<>();
        ipKeywordsMap = new HashMap<>();
        ipColAccessMap = new HashMap<>();
        derivativeList = getDerivativesList();
        log.info("DAMS Statistics derivativs: " + Arrays.toString(derivativeList.toArray()));
    }

    public Map<String, String> getCollsMap() {
        return colsMap;
    }

    public void setColsMap(Map<String, String> colsMap) {
        this.colsMap = colsMap;
    }

    public Map<String, WebStatsCounter> getWebStatsMap() {
        return ipWebStatsMap;
    }

    public Map<String, StatsDlpCounter> getStatsDlpMap() {
        return ipStatsDlpMap;
    }

    public Map<String, StatsDlpColAccessCounter> getColAccessMap() {
        return ipColAccessMap;
    }

    public Map<String, StatsDlcKeywordsCounter> getKeywordsMap() {
        return ipKeywordsMap;
    }

    /**
     * Get the items counters for public access
     * @return
     */
    public Map<String, StatsObjectAccess> getItemsMap() {
        return ipItemsMap;
    }

    /**
     * Get the items counters for private access
     * @return
     */
    public Map<String, StatsObjectAccess> getPrivateItemsMap() {
        return ipItemsMapPrivate;
    }

    public void addAccess(String uri, String clientIp) throws UnsupportedEncodingException{
        int idx = uri.indexOf("?");
        String uriPart = null;
        String paramsPart = null;
        if(idx > 0){
            uriPart = uri.substring(0, idx);
            if(idx+1 < uri.length())
                paramsPart = uri.substring(idx + 1);
        }else
            uriPart = uri;

        String[] parts = uriPart.substring(1).split("/");
        if (parts.length >= 1 && parts.length <= 4) {

            addWebStatsAccess(clientIp);

            boolean pageCounted = addStatsDlpAccess(clientIp, parts, paramsPart);

            if (StringUtils.isNotBlank(paramsPart)) {
                addKeywordAccess(clientIp, parts, paramsPart);
            }

            if (parts.length > 2 && StringUtils.isNotBlank(parts[2]) && parts[1].contains("collection")) {
                // Collections: /dc/dams_collections/bbxxxxxxxx?counter=1 or /dc/collection/bbxxxxxxxx?counter=1
                addCollectionAccess(clientIp, parts[2]);
            } else if (!pageCounted) {
                // other pages? skip for now.
                log.warn("DAMS stats access skip uri: " + uri);
            }
        } else {
            log.warn("DAMS stats unknow uri: " + uri);
        }
    }

    public void addObject(String uri, boolean isPrivateAccess, String clientIp){
        String subjectId = "";
        String fileName = "";

        String uriPart = null;
        String paramsPart = null;
        int idx = uri.indexOf("?");
        if(idx > 0){
            uriPart = uri.substring(0, idx);
            if(idx + 1 < uri.length())
                paramsPart = uri.substring(idx + 1);
        }else
            uriPart = uri;

        String[] parts = uriPart.substring(1).split("/");
        if(parts.length >= 3 && parts.length <=5 && parts[2] !=null)
            // /dc/object/oid/_cid_fid/download
            subjectId = parts[2];
        else{
            log.warn("DAMS stats unknown uri: " + uri);
            return;
        }
        
        if(StringUtils.isBlank(clientIp)) {
            log.warn("Invalid client IP " + clientIp + " in request " + uri + ".");
            return;
        }        

        if(parts.length >= 4 && parts[3] != null && parts[3].startsWith("_"))
            fileName = parts[3];
        
        if(subjectId == null || subjectId.length()!= 10){
            log.warn("Invalid subject " + subjectId + ": " + uri);
            return;
        }

        // initiate the WebStatsAccess object if not being initiated for the request IP
        initWebStatsAccess(clientIp);

        Map<String, StatsObjectAccess> iMap = ipItemsMap;
        if (isPrivateAccess)
            iMap = ipItemsMapPrivate;

        StatsObjectAccess objAccess = iMap.get(clientIp);
        if(objAccess == null){
            objAccess = new StatsObjectAccess();
            iMap.put(clientIp, objAccess);
        }

        // differentiate the counts for file download and object access/hits
        int file_name_idx = fileName.lastIndexOf("_");
        String fileSubfix = file_name_idx >= 0 ? fileName.substring(file_name_idx) : "";

        if (uri.indexOf("/download") > 0) {
            objAccess.increaseFileDownloads(subjectId, fileName, clientIp);
        } else if (StringUtils.isNotBlank(fileName) && derivativeList.indexOf(fileSubfix) < 0) {
            // count all source files as download
            objAccess.increaseFileDownloads(subjectId, fileName, clientIp);
        } else {
            objAccess.increaseObjectAccess(subjectId, fileName, clientIp);
        }
    }

    private List<String> getDerivativesList() {
        // DEFAULT_DERIVATIVES: 2,3,4,5,6,7
        List<String> derSufixes = new ArrayList<String>(Arrays.asList(Constants.DEFAULT_DERIVATIVES.split(",")));
        for (int i=0; i < derSufixes.size(); i++) {
            derSufixes.set(i, "_" + derSufixes.get(i) + ".jpg");
        }

        String[] dersAdditional = {"_2.mp3", "_2.mp4"};
        derSufixes.addAll(Arrays.asList(dersAdditional));
        return derSufixes;
    }

    public String getParamValue(String param){
        String[] pair = param.split("=");
        if(pair.length > 1 && pair[1] != null)
            return pair[1].trim();
        else
            return "";
    }

    /*
     * Initiate the WebStatsCounter object for the client request if not initiated
     * @param clientIp
     * @return WebStatsCounter
     */
    private WebStatsCounter initWebStatsAccess(String clientIp) {
        WebStatsCounter webStatsCounter = ipWebStatsMap.get(clientIp);
        if (webStatsCounter == null) {
            webStatsCounter = new WebStatsCounter(clientIp);
            ipWebStatsMap.put(clientIp, webStatsCounter);
        }
        return webStatsCounter;
    }

    /*
     * Add dams access
     * @param clientIp
     */
    private void addWebStatsAccess(String clientIp) {
        WebStatsCounter webStatsCounter = initWebStatsAccess(clientIp);
        webStatsCounter.addAccess();
    }

    /*
     * Add page access
     * @param clientIp
     * @param paths
     * @param paramStr
     * @return
     * @throws UnsupportedEncodingException
     */
    private boolean addStatsDlpAccess(String clientIp, String[] paths, String paramStr)
            throws UnsupportedEncodingException {
        StatsDlpCounter statsDlpCounter = ipStatsDlpMap.get(clientIp);
        if (statsDlpCounter == null) {
            statsDlpCounter = new StatsDlpCounter();
            ipStatsDlpMap.put(clientIp, statsDlpCounter);
        }
        return statsDlpCounter.addAccess(paths, paramStr);
    }

    /*
     * Add keywords use in search
     * @param clientIp
     * @param paths
     * @param paramStr
     * @throws UnsupportedEncodingException
     */
    private void addKeywordAccess(String clientIp, String[] paths, String paramStr)
            throws UnsupportedEncodingException {
        StatsDlcKeywordsCounter keywordsCounter = ipKeywordsMap.get(clientIp);
        if (keywordsCounter == null) {
            keywordsCounter = new StatsDlcKeywordsCounter();
            ipKeywordsMap.put(clientIp, keywordsCounter);
        }
        keywordsCounter.addAccess(paths, paramStr);
    }

    /*
     * Add collection access
     * @param clientIp
     * @param collectionId
     * @throws UnsupportedEncodingException
     */
    private void addCollectionAccess(String clientIp, String collectionId)
            throws UnsupportedEncodingException {
        StatsDlpColAccessCounter colAccessCounter = ipColAccessMap.get(clientIp);
        if (colAccessCounter == null) {
            colAccessCounter = new StatsDlpColAccessCounter(colsMap);
            ipColAccessMap.put(clientIp, colAccessCounter);
        }

        colAccessCounter.addAccess(collectionId);
    }

    public synchronized void export(Connection con) throws Exception {
        int returnValue = -1;
        PreparedStatement ps = null;

        con.setAutoCommit(false);

        //Update record for the calendar date
        if (update) {
            try {
                ps = con.prepareStatement(WEB_STATS_DELETE_RECORD);
                ps.setString(1, dateFormat.format(calendar.getTime()));
                ps.setString(2, appName);
                returnValue = ps.executeUpdate();
                log.info("Deleted " + appName + " statistics record for date " + dateFormat.format(calendar.getTime()));
            } finally {
                Statistics.close(ps);
                ps = null;
            }
        } else {
            if (isRecordExist(con, calendar.getTime())) {
                log.warn(appName + " statistics record for date " + dateFormat.format(calendar.getTime()) + " exists.");
                return;
            }
        }

        PreparedStatement psWebStats = null;
        PreparedStatement psStatsDlp = null;
        PreparedStatement psStatsDlpColAccess = null;
        PreparedStatement psStatsDlpObjectAccess = null;
        PreparedStatement psStatsFileDownload = null;
        PreparedStatement psStatsDlpKeyWord = null;
        try {
            psWebStats = con.prepareStatement(WEB_STATS_INSERT);
            psStatsDlp = con.prepareStatement(STATS_DLP_INSERT);
            psStatsDlpColAccess = con.prepareStatement(STATS_DLP_COLLECTION_ACCESS_INSERT);
            psStatsDlpObjectAccess = con.prepareStatement(STATS_DLP_OBJECT_ACCESS_INSERT);
            psStatsFileDownload = con.prepareStatement(STATS_FILE_DOWNLOAD_INSERT);
            psStatsDlpKeyWord = con.prepareStatement(STATS_DLC_KEYWORDS_INSERT);
            for (String clientIp : ipWebStatsMap.keySet()) {
                int nextId = getNextId(con);

                //WEB_STATS insert
                psWebStats.setInt(1, nextId);
                psWebStats.setDate(2, java.sql.Date.valueOf(dateFormat.format(calendar.getTime())));
                psWebStats.setString(4, appName);
                returnValue = ipWebStatsMap.get(clientIp).export(psWebStats);
                psWebStats.clearParameters();

                //STATS_DLP insert
                StatsDlpCounter statsDlpCounter = ipStatsDlpMap.get(clientIp);
                if (statsDlpCounter == null)
                    statsDlpCounter = new StatsDlpCounter(); // If no page hits, initiate default
                psStatsDlp.setInt(1, nextId);
                returnValue = statsDlpCounter.export(psStatsDlp);
                psStatsDlp.clearParameters();

                //STATS_DLP_COLLECTION_ACCESS_INSERT insert
                StatsDlpColAccessCounter statsDlpColAccessCounter = ipColAccessMap.get(clientIp);
                if (statsDlpColAccessCounter != null) {
                    statsDlpColAccessCounter.export(psStatsDlpColAccess, nextId);
                    psStatsDlpColAccess.clearParameters();
                }

                //Keywords/Phrases STATS_DLC_KEYWORDS_INSERT insert
                StatsDlcKeywordsCounter statsDlcKeywordsCounter = ipKeywordsMap.get(clientIp);
                if (statsDlcKeywordsCounter != null) {
                    statsDlcKeywordsCounter.export(psStatsDlpKeyWord, nextId);
                    psStatsDlpKeyWord.clearParameters();
                }

                // Eliminate counts from curator access with redirects from public access
                for (String ip : ipItemsMapPrivate.keySet()) {
                    if (ipItemsMap.containsKey(ip)) {
                        StatsObjectAccess pubObjAccess = ipItemsMap.get(ip);
                        StatsObjectAccess privObjAccess = ipItemsMapPrivate.get(ip);
                        for (String subject : privObjAccess.getObjectCounters().keySet()) {
                            ObjectCounter pubCounter = pubObjAccess.getCounter(subject);
                            ObjectCounter privCounter = privObjAccess.getCounter(subject);
                            if (pubCounter != null) {
                                int pubAccess = pubCounter.getAccess() - privCounter.getAccess();
                                pubCounter.setAccess(pubAccess > 0 ? pubAccess : 0);

                                int pubViews = pubCounter.getView() - privCounter.getView();
                                pubCounter.setView(pubViews > 0 ? pubViews : 0);
                            }
                        }
                    }
                }

                StatsObjectAccess objCounter = ipItemsMap.get(clientIp);
                StatsObjectAccess objCounterPrivate = ipItemsMapPrivate.get(clientIp);

                //STATS_DLP_OBJECT_ACCESS_INSERT insert
                if (objCounter != null) {
                    persistObjectAccessStats (nextId, psStatsDlpObjectAccess, objCounter, false);
                    psStatsDlpObjectAccess.clearParameters();
                }

                if (objCounterPrivate != null) {
                    persistObjectAccessStats (nextId, psStatsDlpObjectAccess, objCounterPrivate, true);
                    psStatsDlpObjectAccess.clearParameters();
                }

                //STATS_FILE_DOWNLOAD_INSERT insert
                if (objCounter != null) {
                    persistFileDownloadStats (nextId, psStatsFileDownload, objCounter, false);
                    psStatsFileDownload.clearParameters();
                }

                if (objCounterPrivate != null) {
                    persistFileDownloadStats (nextId, psStatsFileDownload, objCounterPrivate, true);
                    psStatsFileDownload.clearParameters();
                }
            }

            con.commit();
        } finally {
            con.setAutoCommit(true);

            close(psWebStats);
            close(psStatsDlp);
            close(psStatsDlpColAccess);
            close(psStatsDlpObjectAccess);
            close(psStatsFileDownload);
            close(psStatsDlpKeyWord);
        }
        log.info("Inserted " + appName + " statistics record for " + dateFormat.format(calendar.getTime()));
    }

    private int persistObjectAccessStats (int statsId, PreparedStatement ps, StatsObjectAccess objectAccess, boolean isPrivate) {
        int returnValue = 0;
        Map<String, ObjectCounter> objectCounters = objectAccess.getObjectCounters();
        for (String sid : objectCounters.keySet()) {
            ObjectCounter objCounter = objectCounters.get(sid);
            try {
                Document doc = Statistics.cacheGet(sid);
                if (doc == null) {
                    doc = Statistics.getRecordForStats(sid);
                    Statistics.cacheAdd(sid, doc);
                }
    
                String unitId = getUnitCode(doc);;
                String colId = getCollection(doc);
                ps.setInt(1, statsId);
                ps.setBoolean(2, isPrivate);
                ps.setString(3, unitId);
                ps.setString(4, colId);
                ps.setString(5, sid);
                returnValue = objCounter.export(ps);
                ps.clearParameters();
            } catch (Exception ex) {
                ex.printStackTrace();
                log.error("Stats insert failed: " + objCounter.toString());
            }
        }
        return returnValue;
    }

    private int persistFileDownloadStats (int id, PreparedStatement ps, StatsObjectAccess objectAccess, boolean isPrivate)
            throws Exception {
        int returnValue = 0;
        Map<String, FileDownloadCounter> fileDownloads = objectAccess.getFileDownloads();
        for (FileDownloadCounter fCounter : fileDownloads.values()) {
            String sid = fCounter.getOid();
            Document doc = Statistics.cacheGet(sid);
            if (doc == null) {
                doc = Statistics.getRecordForStats(sid);
                Statistics.cacheAdd(sid, doc);

            }

            try {
                String unitId = getUnitCode(doc);;
                String colId = getCollection(doc);
                ps.setInt(1, id);
                ps.setBoolean(2, isPrivate);
                ps.setString(3, unitId);
                ps.setString(4, colId);
                ps.setString(5, sid);
                returnValue = fCounter.export(ps);
                ps.clearParameters();
            }catch(Exception ex) {
                ex.printStackTrace();
                log.error("Stats insert failed: " + fCounter.toString() );
            }
        }
        return returnValue;
    }

    public String getUnitCode(Document doc) {
        if (doc != null) {
            Node node = doc.selectSingleNode("//doc/arr[@name='unit_code_tesim']/str");
            if (node != null) {
                String id = node.getStringValue();
                return id.substring(id.lastIndexOf("/") + 1);
            }
        }
        return null;
    }

    public String getCollection(Document doc) {
        if (doc != null) {
            Node node = doc.selectSingleNode("//doc/arr[@name='collections_tesim']/str");
            if (node != null) {
                return node.getText();
            }
        }
        return null;
    }

    public void print(){
        for (WebStatsCounter webStatsCounter : ipWebStatsMap.values()) {
            System.out.println("-------------------------\n");

            System.out.println("" + webStatsCounter);

            String clientIp = webStatsCounter.getClientIp();
            if (ipStatsDlpMap.containsKey(clientIp)) {
                System.out.println("\nPage Access:\n" + ipStatsDlpMap.get(clientIp));
            }

            if (ipColAccessMap.containsKey(clientIp)) {
                System.out.println("\nCollection access:\n" + ipColAccessMap.get(clientIp));
            }

            if (ipKeywordsMap.containsKey(clientIp)) {
                System.out.println("\nKeywords/Phrasees used:\n" + ipKeywordsMap.get(clientIp));
            }

            if (ipItemsMap.containsKey(clientIp)) {
                System.out.println("\nPublic object access:\n" + ipItemsMap.get(clientIp));
            }

            if (ipItemsMapPrivate.containsKey(clientIp)) {
                System.out.println("\nCurator object access:\n" + ipItemsMapPrivate.get(clientIp));
            }
        }
    }

    /**
     * Concatenate path elements in to url string
     * @param paths
     * @return
     */
    public static String toUrl(String[] paths) {
        String url = "";
        for (String path : paths) {
            url += "/" + path;
        }
        return url;
    }
}
