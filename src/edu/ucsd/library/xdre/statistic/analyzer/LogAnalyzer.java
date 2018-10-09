package edu.ucsd.library.xdre.statistic.analyzer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Connection;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import edu.ucsd.library.xdre.statistic.beans.StatsRequest;
import edu.ucsd.library.xdre.statistic.beans.WeblogParser;
import edu.ucsd.library.xdre.utils.Constants;

/**
 * Class LogAnalyzer
 *
 * @author lsitu@ucsd.edu
 */
public class LogAnalyzer{
    public static String SPEC_COLL_URL = "//library.ucsd.edu/speccoll/";

    private static Logger log = Logger.getLogger(LogAnalyzer.class);
    private Calendar calendar = null;
    private DAMStatistic pasStats = null;
    //private DAMStatistic casStats = null;
    private boolean update = false;
    private Pattern searchEnginePatterns = null;
    private WeblogParser weblogParser = null;

    private Pattern ipFilterPatterns = null;
    private Map<String, Boolean> ipCache = new HashMap<>();
    private Map<String, Boolean> userAgentCache = new HashMap<>();

    public LogAnalyzer () throws Exception{
        this(new HashMap<String, String>());
    }
    
    public LogAnalyzer (Map<String, String> collsMap) throws Exception{
        pasStats = new DAMStatistic("pas");
        //casStats = new DAMStatistic("cas");
        pasStats.setColsMap(collsMap);
        //casStats.setCollsMap(collsMap);

        String seUserAgentPatterns = Constants.STATS_SE_PATTERNS + (Constants.STATS_SE_PATTERNS.endsWith("|") ? "" : "|");
        seUserAgentPatterns += getSearchEnginePatterns();

        log.info("Search engine filter patterns: " + seUserAgentPatterns);
        searchEnginePatterns = Pattern.compile(seUserAgentPatterns.toLowerCase());

        log.info("IP filter patterns: " + Constants.STATS_IP_FILTER);
        ipFilterPatterns = Pattern.compile(Constants.STATS_IP_FILTER);

        weblogParser = new WeblogParser();
    }

    public void setIpFilter(String ipFilter) {
        ipFilterPatterns = Pattern.compile(ipFilter);
    }

    public DAMStatistic getPasStats() {
        return pasStats;
    }

    public void analyze(File logFile) throws IOException{

        String line = null;
        String uri = null;
        InputStream in = null;
        InputStream gzipIn = null;
        Reader reader = null;
        
        BufferedReader bReader = null;
        //int idx = -1;
        calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        try {
            String fileName = logFile.getName();
            String[] dateArr = fileName.substring(fileName.indexOf('.')+1, fileName.lastIndexOf('.')).split("-");
            calendar.set(Calendar.YEAR, Integer.parseInt(dateArr[0]));
            calendar.set(Calendar.MONTH, Integer.parseInt(dateArr[1])-1);
            calendar.set(Calendar.DATE, Integer.parseInt(dateArr[2]));
            log.info("Processing log file " + fileName + " for record " + pasStats.formatDate(calendar.getTime()));

            if (logFile.getName().endsWith(".gz")){
                in = new FileInputStream(logFile);
                gzipIn = new GZIPInputStream(in);
                reader = new InputStreamReader(gzipIn);
            }else{
                in = new FileInputStream(logFile);
                reader = new InputStreamReader(in);
            }
            
            bReader = new BufferedReader(reader);
            int spIdx = -1;
            while((line=bReader.readLine()) != null){
                spIdx = line.indexOf("GET /dc");
                if(spIdx > 0 && line.indexOf(Constants.CLUSTER_HOST_NAME + " ") > 0 && !excludeFromStats(line)) {
                    int fromIdx = line.indexOf(" \"", spIdx);
                    if (fromIdx < 0) {
                        log.warn("Invalid client request: " + line);
                        continue;
                    }
                    // ignore spiders/search engines access
                    if (isBotAccess(line, fromIdx) && line.indexOf(SPEC_COLL_URL) < 0)
                        continue;

                    StatsRequest statsRequest = weblogParser.parse(line);
                    if (statsRequest == null || !isValidUri(statsRequest.getRequestUri())
                            || StringUtils.isBlank(statsRequest.getClientIp())) {
                        log.warn("Invalid client request: " + line);
                        continue;
                    }

                    // exclude requests that were failed
                    String httpStatus = statsRequest.getStatus();
                    if ((httpStatus.startsWith("4") || httpStatus.startsWith("5")))
                        continue;

                    // exclude accesses by IP
                    if (filterIp(statsRequest.getClientIp()))
                        continue;

                    uri = statsRequest.getRequestUri();
                    String clientIp = statsRequest.getClientIp();
                    if(uri != null){
                        //idx = uri.indexOf("&user=");
                        String[] uriParts = (uri.length()>1?uri.substring(1):uri).split("/");
                        
                        // ignore varieties of formatted metadata views like /dc/object/bb55641580.rdf
                        if(uriParts.length == 3 && uriParts[2].length() > 10 && uriParts[2].charAt(10) == '.')
                            continue;

                        if(uriParts.length>1 && uriParts[1].equals("object")){
                            //Object access: /dc/object/oid/_cid_fid
                            int uidIdx = uri.indexOf("access=curator");

                            if (!(httpStatus.startsWith("2") || httpStatus.startsWith("3")))
                                continue;

                            if (uidIdx > 0 && uriParts.length >= 4 && uriParts[3].startsWith("_"))
                                // file access from curator
                                pasStats.addObject(uri, true, clientIp);
                            else if (uidIdx > 0 && httpStatus.startsWith("3"))
                                // view from curator with redirect
                                pasStats.addObject(uri, true, clientIp);
                            else if (!httpStatus.startsWith("3")) {
                                // access with no redirect
                                if (line.indexOf(SPEC_COLL_URL, spIdx) > 0) {
                                    // access from MSCL exhibits: http://library.ucsd.edu/speccoll/
                                    addMsclStats(uri, clientIp);
                                } else {
                                    pasStats.addObject(uri, false, clientIp);
                                }
                            }
                        }else{
                            //Home Page: /dc
                            //Search: /dc/search?utf8=%E2%9C%93&q=wagner
                            //Facet Browse: /dc/search?utf8=%E2%9C%93&f%5Bcollection_sim%5D%5B%5D=Dr.+Seuss+Political+Cartoons&q=some+people
                            //Collections Browser: /dc/search?utf8=%E2%9C%93 ?????????????????
                            //Collections page: /dc/collections
                            //DLP Collections page: /dc/dlp/collections
                            //RCI collections page: /dc/rci/collections
                            //Collections access: /dc/dams_collections/bb2936476d?counter=1
                            pasStats.addAccess(uri, clientIp);
                            
                        }
                            
                    }
                }
            }
        }finally{
            if(in != null){
                try {
                    in.close();
                } catch (IOException e) {}
                in = null;
            }
            if(gzipIn != null){
                try {
                    gzipIn.close();
                } catch (IOException e) {}
                gzipIn = null;
            }
            if(bReader != null){
                try {
                    bReader.close();
                } catch (IOException e) {}
                bReader = null;
            }
            if(reader != null){
                try {
                    reader.close();
                } catch (IOException e) {}
                reader = null;
            }
        }
    }
    
    private void addMsclStats(String uri, String clientIp) {
        if (uri.indexOf("/_2.jpg") > 0) {
            // count as object view
            uri = uri.replace("/_2.jpg", "");
        }
        pasStats.addObject(uri, false, clientIp); 
    }
    
    private boolean isValidUri(String uri){
        if (StringUtils.isBlank(uri) || uri.indexOf("?") < 0 && uri.indexOf("&") > 0) {
            return false;
        }
        return true;
    }
    
    public void export(Connection con) throws Exception {
        pasStats.setCalendar(calendar);
        //casStats.setCalendar(calendar);
        pasStats.setUpdate(update);
        //casStats.setUpdate(update);
        pasStats.export(con);
        //casStats.export(con);
    }
    
    private boolean excludeFromStats(String value) {
        String[] excludePatterns = {"/ucsd.ico", "/fonts/", "/assets/", "/get_data/", "/users/", "/images/", "/zoom"};
        for (String excludePattern : excludePatterns) {
            if (value.contains(excludePattern))
                return true;
        }
        return false;
    }

    private boolean isBotAccess(String value, int fromIndex) {
        String valueToMatch = value.substring(fromIndex).toLowerCase();
        if (userAgentCache.containsKey(valueToMatch)) {
            return userAgentCache.get(valueToMatch);
        } else {
            Matcher matcher = searchEnginePatterns.matcher(value.substring(fromIndex).toLowerCase());
            boolean matched = matcher.find();
            userAgentCache.put(valueToMatch, matched);

            if (matched) {
                return true;
            }
        }
        return false;
    }

    /**
     * Determine whether the IP is included in IP filter.
     * @param ip
     * @return
     */
    public boolean filterIp(String ip) {
        if (ipCache.containsKey(ip)) {
            return ipCache.get(ip);
        } else {
            Matcher matcher = ipFilterPatterns.matcher(ip);
            boolean matched = matcher.find();
            ipCache.put(ip, matched);

            if (matched) {
                return true;
            }
        }
        return false;
    }

    public void print() {
        pasStats.print();
        //casStats.print();
    } 
    
    public boolean isUpdate() {
        return update;
    }

    public void setUpdate(boolean update) {
        this.update = update;
    }

    public static String getAttributeValue(Node node){
        if(node == null)
            return null;
        String val = node.getText();
        if(val != null){
            val = val.substring(val.indexOf("|||") + 3);
        }
        return val;
    }
    
    public static Document getSOLRResult(String solrBase, String solrCore, String params) throws DocumentException, MalformedURLException{
        URL url = new URL(solrBase + solrCore + "/select?" + params);
        SAXReader reader = new SAXReader();
        return reader.read(url);
    }

    /*
     * generate the search engine/crawler user agents pattern string from online json data source
     */
    private static String getSearchEnginePatterns() throws URISyntaxException, IOException {
        StringBuilder sePatternsBuilder = new StringBuilder();
        URL url = new URL(Constants.STATS_SE_DATA_LOCATION);
        try (Reader reader = new InputStreamReader(url.openConnection().getInputStream())) {
            JSONArray botsArray = (JSONArray)JSONValue.parse(reader);
            for (int i = 0; i < botsArray.size(); i++) {
                JSONObject bot = (JSONObject)botsArray.get(i);
                sePatternsBuilder.append(bot.get("pattern") + "|");
            }
        }

        String sePatterns = sePatternsBuilder.toString();
        return sePatterns.substring(0, sePatterns.length() - 1);
    }
}
