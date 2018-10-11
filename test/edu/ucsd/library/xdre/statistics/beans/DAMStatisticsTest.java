package edu.ucsd.library.xdre.statistics.beans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.ucsd.library.xdre.statistic.analyzer.DAMStatistic;
import edu.ucsd.library.xdre.statistic.analyzer.LogAnalyzer;
import edu.ucsd.library.xdre.statistic.beans.FileDownloadCounter;
import edu.ucsd.library.xdre.statistic.beans.ObjectCounter;
import edu.ucsd.library.xdre.statistic.beans.StatsDlcKeywordsCounter;
import edu.ucsd.library.xdre.statistic.beans.StatsDlpColAccessCounter;
import edu.ucsd.library.xdre.statistic.beans.StatsDlpCounter;
import edu.ucsd.library.xdre.statistic.beans.StatsRequest;
import edu.ucsd.library.xdre.statistic.beans.WebStatsCounter;
import edu.ucsd.library.xdre.statistic.beans.WeblogParser;
import edu.ucsd.library.xdre.utils.Constants;

/**
 * Tests methods for WeblogParser class
 * @author lsitu
 *
 */
public class DAMStatisticsTest {

    private static WeblogParser weblogParser = null;

    private static DAMStatistic damsStats = null;

    @BeforeClass
    public static void init() throws IOException {
        weblogParser = new WeblogParser();
        damsStats = new DAMStatistic("pas");
    }

    @Test
    public void testPublicObjectView() throws Exception {
        String weblogRequest = "May 10 00:01:01 library apache[1829]: library.ucsd.edu 168.0.0.1 - - [10/May/2018:00:01:01 -0700] "
                + "\"GET /dc/object/bbxxxxxxxx HTTP/1.1\" 200 1234 \"https://www.example.com/\" \"Mozilla/5.0\"";

        StatsRequest statsRequest = weblogParser.parse(weblogRequest);
        assertNotNull(statsRequest);

        damsStats.addObject(statsRequest.getRequestUri(), false, statsRequest.getClientIp());
        ObjectCounter counter = damsStats.getItemsMap().get("168.0.0.1").getCounter("bbxxxxxxxx");
        assertNotNull(counter);
        assertEquals("Public access count need be matched", 1, counter.getView());
    }

    @Test
    public void testPrivateObjectView() throws Exception {
        String weblogRequest = "May 10 00:01:01 library apache[1829]: library.ucsd.edu 168.0.0.1 - - [10/May/2018:00:01:01 -0700] "
                + "\"GET /dc/object/bbxxxxxxxx?access=curator HTTP/1.1\" 302 111 \"https://library.ucsd.edu/dc/search?utf8=%E2%9C%93&q=id%3A+19\" \"Mozilla/5.0\"";

        StatsRequest statsRequest = weblogParser.parse(weblogRequest);
        assertNotNull(statsRequest);

        damsStats.addObject(statsRequest.getRequestUri(), true, statsRequest.getClientIp());
        ObjectCounter counter = damsStats.getPrivateItemsMap().get("168.0.0.1").getCounter("bbxxxxxxxx");
        assertNotNull(counter);
        assertEquals("Private access count need to be matched", 1, counter.getView());
    }

    @Test
    public void testPublicFileDownloads() throws Exception {
        String weblogRequest = "May 10 00:01:01 library apache[1829]: library.ucsd.edu 168.0.0.1 - - [10/May/2018:00:01:01 -0700] "
                + "\"GET /dc/object/bbxxxxxxxx/_1.zip HTTP/1.1\" 200 20674 \"https://library.ucsd.edu/dc/object/bbxxxxxxxx\" \"Mozilla/5.0\"";

        StatsRequest statsRequest = weblogParser.parse(weblogRequest);
        assertNotNull(statsRequest);

        damsStats.addObject(statsRequest.getRequestUri(), false, statsRequest.getClientIp());
        FileDownloadCounter counter = damsStats.getItemsMap().get("168.0.0.1").getFileDownloadCounter("_1.zip::bbxxxxxxxx");
        assertNotNull(counter);
        assertEquals("Public file download need be matched", 1, counter.getView());
    }

    @Test
    public void testPrivateFileDownloads() throws Exception {
        String weblogRequest = "May 10 00:01:01 library apache[1829]: library.ucsd.edu 168.0.0.1 - - [10/May/2018:00:01:01 -0700] "
                + "\"GET /dc/object/bbxxxxxxxx/_1.zip?access=curator HTTP/1.1\" 302 111 \"https://library.ucsd.edu/dc/object/bbxxxxxxxx\" \"Mozilla/5.0\"";

        StatsRequest statsRequest = weblogParser.parse(weblogRequest);
        assertNotNull(statsRequest);

        damsStats.addObject(statsRequest.getRequestUri(), true, statsRequest.getClientIp());
        FileDownloadCounter counter = damsStats.getPrivateItemsMap().get("168.0.0.1").getFileDownloadCounter("_1.zip::bbxxxxxxxx");
        assertNotNull(counter);
        assertEquals("Private file download need to be matched", 1, counter.getView());
    }

    @Test
    public void testFilterIp() throws Exception {
        String ipFilter = "(220.243.135|220.243.136).*|52.83.219.14|52.83.191.29";
        LogAnalyzer analyzer = new LogAnalyzer();
        analyzer.setIpFilter(ipFilter);
        assertTrue(analyzer.filterIp("220.243.135.1"));
        assertTrue(analyzer.filterIp("52.83.219.14"));
    }

    @Test
    public void testExcludeIp() throws Exception {
        String ipFilter = "(220.243.135|220.243.136).*|52.83.219.14|52.83.191.29";
        Constants.STATS_SE_PATTERNS = "\"-\" \".*(bot|crawler|spider|robot|crawling).*|SortSiteCmd/|Siteimprove.com|Dispatch/|Disqus/|Photon/|weborama-fetcher";
        Constants.STATS_SE_DATA_LOCATION = "http://librarytest.ucsd.edu/damsmanager/files/crawler-user-agents.json";
        Constants.CLUSTER_HOST_NAME = "library";
        Constants.STATS_IP_FILTER = ipFilter;
        LogAnalyzer analyzer = new LogAnalyzer();

        String weblogRequest1 = "May 10 00:01:01 library apache[1829]: library.ucsd.edu 220.243.135.1 - - [10/May/2018:00:01:01 -0700] "
                + "\"GET /dc HTTP/1.1\" 200 111 \"-\" \"Mozilla/5.0\"";
        String weblogRequest2 = "May 10 00:01:01 library apache[1829]: library.ucsd.edu 52.83.219.14 - - [10/May/2018:00:01:01 -0700] "
                + "\"GET /dc HTTP/1.1\" 200 111 \"-\" \"Mozilla/5.0\"";
        String weblogRequest3 = "May 10 00:01:01 library apache[1829]: library.ucsd.edu 168.0.0.1 - - [10/May/2018:00:01:01 -0700] "
                + "\"GET /dc HTTP/1.1\" 200 111 \"-\" \"Mozilla/5.0\"";

        File logFile = createWeblogFile(weblogRequest1 + "\n" + weblogRequest2 + "\n" + weblogRequest3 + "\n");
        analyzer.analyze(logFile);

        Map<String, WebStatsCounter> webStatsMap = analyzer.getPasStats().getWebStatsMap();
        assertFalse("Should not include stats data from filtered IP 220.243.135.1!", webStatsMap.containsKey("220.243.135.1"));
        assertFalse("Should not include stats data from filtered IP 52.83.219.14!", webStatsMap.containsKey("52.83.219.14"));
        assertTrue("Should include stats data from IP 168.0.0.1!", webStatsMap.containsKey("168.0.0.1"));
        assertEquals("Number of accesses from IP 168.0.0.1 doesn't match!", 1, webStatsMap.get("168.0.0.1").getNumAccess());
    }

    @Test
    public void testPageAccessesByIp() throws Exception {
        String ipFilter = "(220.243.135|220.243.136).*|52.83.219.14|52.83.191.29";
        Constants.STATS_SE_PATTERNS = "\"-\" \".*(bot|crawler|spider|robot|crawling).*|SortSiteCmd/|Siteimprove.com|Dispatch/|Disqus/|Photon/|weborama-fetcher";
        Constants.STATS_SE_DATA_LOCATION = "http://librarytest.ucsd.edu/damsmanager/files/crawler-user-agents.json";
        Constants.CLUSTER_HOST_NAME = "library";
        Constants.STATS_IP_FILTER = ipFilter;
        LogAnalyzer analyzer = new LogAnalyzer();

        String weblogRequests1 = "May 10 00:01:01 library apache[1829]: library.ucsd.edu 168.0.0.1 - - [10/May/2018:00:01:01 -0700] "
                + "\"GET /dc HTTP/1.1\" 200 111 \"-\" \"Mozilla/5.0\"" + "\n"
                + "May 10 00:01:01 library apache[1829]: library.ucsd.edu 168.0.0.1 - - [10/May/2018:00:01:01 -0700] "
                + "\"GET /dc/search?q=abc HTTP/1.1\" 200 111 \"-\" \"Mozilla/5.0\"" + "\n"
                + "May 10 00:01:01 library apache[1829]: library.ucsd.edu 168.0.0.1 - - [10/May/2018:00:01:02 -0700] "
                + "\"GET /dc/search/facet/subject_topic_sim?facet.sort=index HTTP/1.1\" 200 111 \"-\" \"Mozilla/5.0\"" + "\n"
                + "May 10 00:01:01 library apache[1829]: library.ucsd.edu 168.0.0.1 - - [10/May/2018:10:01:03 -0700] "
                + "\"GET /dc/collection/bbxxxxxxxx HTTP/1.1\" 200 111 \"-\" \"Mozilla/5.0\"" + "\n";

                String weblogRequests2 = "May 10 00:01:01 library apache[1829]: library.ucsd.edu 168.0.0.2 - - [10/May/2018:00:01:01 -0700] "
                + "\"GET /dc/search?q=abc HTTP/1.1\" 200 111 \"-\" \"Mozilla/5.0\"" + "\n"
                + "May 10 00:01:01 library apache[1829]: library.ucsd.edu 168.0.0.2 - - [10/May/2018:10:01:01 -0700] "
                + "\"GET /dc/search?q=abcde HTTP/1.1\" 200 111 \"-\" \"Mozilla/5.0\"" + "\n";

        File logFile = createWeblogFile(weblogRequests1 + weblogRequests2);
        analyzer.analyze(logFile);

        Map<String, StatsDlpCounter> statsDlpMap = analyzer.getPasStats().getStatsDlpMap();
        assertEquals("Number of client IPs doesn't match!", 2, statsDlpMap.size());

        assertTrue("Should include stats data from IP 168.0.0.1!", statsDlpMap.containsKey("168.0.0.1"));
        assertEquals("Number of home pages doesn't match!", 1, statsDlpMap.get("168.0.0.1").getNumHomePage());
        assertEquals("Number of searches doesn't match!", 1, statsDlpMap.get("168.0.0.1").getNumSearch());
        assertEquals("Number of browses doesn't match!", 1, statsDlpMap.get("168.0.0.1").getNumBrowse());

        assertTrue("Should include stats data from IP 168.0.0.2!", statsDlpMap.containsKey("168.0.0.2"));
        assertEquals("Number of search doesn't match!", 2, statsDlpMap.get("168.0.0.2").getNumSearch());
    }

    @Test
    public void testKeywordPhraseByIp() throws Exception {
        String ipFilter = "(220.243.135|220.243.136).*|52.83.219.14|52.83.191.29";
        Constants.STATS_SE_PATTERNS = "\"-\" \".*(bot|crawler|spider|robot|crawling).*|SortSiteCmd/|Siteimprove.com|Dispatch/|Disqus/|Photon/|weborama-fetcher";
        Constants.STATS_SE_DATA_LOCATION = "http://librarytest.ucsd.edu/damsmanager/files/crawler-user-agents.json";
        Constants.CLUSTER_HOST_NAME = "library";
        Constants.STATS_IP_FILTER = ipFilter;
        LogAnalyzer analyzer = new LogAnalyzer();

        String weblogRequests = "May 10 00:01:01 library apache[1829]: library.ucsd.edu 168.0.0.1 - - [10/May/2018:00:01:01 -0700] "
        + "\"GET /dc/search?q=abcd HTTP/1.1\" 200 111 \"-\" \"Mozilla/5.0\"" + "\n"
        + "May 10 00:01:01 library apache[1829]: library.ucsd.edu 168.0.0.1 - - [10/May/2018:10:01:03 -0700] "
        + "\"GET /dc/search?q=abcd HTTP/1.1\" 200 111 \"-\" \"Mozilla/5.0\"" + "\n"
        + "May 10 00:01:01 library apache[1829]: library.ucsd.edu 168.0.0.1 - - [10/May/2018:10:01:03 -0700] "
        + "\"GET /dc/search?q=%22abc%20abcd%22 HTTP/1.1\" 200 111 \"-\" \"Mozilla/5.0\"" + "\n";

        File logFile = createWeblogFile(weblogRequests);
        analyzer.analyze(logFile);

        Map<String, StatsDlcKeywordsCounter> dlpKeywordMap = analyzer.getPasStats().getKeywordsMap();
        assertEquals("Number of client IPs doesn't match!", 1, dlpKeywordMap.size());

        assertTrue("Should include stats data from IP 168.0.0.1!", dlpKeywordMap.containsKey("168.0.0.1"));
        assertTrue("Should include keyword 'abcd'!", dlpKeywordMap.get("168.0.0.1").getKeywordsMap().containsKey("abcd"));
        assertEquals("The counts for keyword 'abcd' doesn't match!", 2, dlpKeywordMap.get("168.0.0.1").getKeywordsMap().get("abcd").intValue());

        assertTrue("Should include phrase 'abc abcd'!", dlpKeywordMap.get("168.0.0.1").getPhrasesMap().containsKey("abc abcd"));
        assertEquals("The counts for phrase 'abc abcd' doesn't match!", 1, dlpKeywordMap.get("168.0.0.1").getPhrasesMap().get("abc abcd").intValue());
    }

    @Test
    public void testCollectionAccessByIp() throws Exception {
        String ipFilter = "(220.243.135|220.243.136).*|52.83.219.14|52.83.191.29";
        Constants.STATS_SE_PATTERNS = "\"-\" \".*(bot|crawler|spider|robot|crawling).*|SortSiteCmd/|Siteimprove.com|Dispatch/|Disqus/|Photon/|weborama-fetcher";
        Constants.STATS_SE_DATA_LOCATION = "http://librarytest.ucsd.edu/damsmanager/files/crawler-user-agents.json";
        Constants.CLUSTER_HOST_NAME = "library";
        Constants.STATS_IP_FILTER = ipFilter;
        LogAnalyzer analyzer = new LogAnalyzer();

        String weblogRequests1 = "May 10 00:01:01 library apache[1829]: library.ucsd.edu 168.0.0.1 - - [10/May/2018:10:01:01 -0700] "
                + "\"GET /dc/collection/bbxxxxxxxx HTTP/1.1\" 200 111 \"-\" \"Mozilla/5.0\"" + "\n";

        String weblogRequests2 = "May 10 00:01:01 library apache[1829]: library.ucsd.edu 168.0.0.2 - - [10/May/2018:10:01:01 -0700] "
                + "\"GET /dc/collection/bbxxxxxxxx HTTP/1.1\" 200 111 \"-\" \"Mozilla/5.0\"" + "\n"
                + "May 10 00:01:01 library apache[1829]: library.ucsd.edu 168.0.0.2 - - [10/May/2018:10:01:02 -0700] "
                + "\"GET /dc/collection/bbxxxxxxxx HTTP/1.1\" 200 111 \"-\" \"Mozilla/5.0\"" + "\n";

        File logFile = createWeblogFile(weblogRequests1 + weblogRequests2);
        analyzer.analyze(logFile);

        Map<String, StatsDlpColAccessCounter> dlpColAccessMap = analyzer.getPasStats().getColAccessMap();
        assertEquals("Number of client IPs doesn't match!", 2, dlpColAccessMap.size());

        assertTrue("Should include stats data from IP 168.0.0.1!", dlpColAccessMap.containsKey("168.0.0.1"));
        assertTrue("Should include stats data for collection bbxxxxxxxx from IP 168.0.0.1!",
                dlpColAccessMap.get("168.0.0.1").getCollsAccessMap().containsKey("bbxxxxxxxx"));
        assertEquals("The counts for collection bbxxxxxxxx from IP 168.0.0.1 doesn't match!", 1,
                dlpColAccessMap.get("168.0.0.1").getCollsAccessMap().get("bbxxxxxxxx").intValue());

        assertTrue("Should include stats data from IP 168.0.0.2!", dlpColAccessMap.containsKey("168.0.0.2"));
        assertTrue("Should include stats data for collection bbxxxxxxxx from IP 168.0.0.2!",
                dlpColAccessMap.get("168.0.0.2").getCollsAccessMap().containsKey("bbxxxxxxxx"));
        assertEquals("The counts for collection bbxxxxxxxx from IP 168.0.0.2 doesn't match!", 2,
                dlpColAccessMap.get("168.0.0.2").getCollsAccessMap().get("bbxxxxxxxx").intValue());
    }

    private File createWeblogFile(final String content) throws IOException {
        final File weblogFile =  new File("httpd.2018-08-28.txt");
        weblogFile.deleteOnExit();
        try (final FileWriter fw = new FileWriter(weblogFile)) {
            fw.write(content);
        }
        return weblogFile;
    }
}
