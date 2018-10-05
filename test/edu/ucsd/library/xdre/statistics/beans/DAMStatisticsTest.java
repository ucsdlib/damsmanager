package edu.ucsd.library.xdre.statistics.beans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.ucsd.library.xdre.statistic.analyzer.DAMStatistic;
import edu.ucsd.library.xdre.statistic.analyzer.LogAnalyzer;
import edu.ucsd.library.xdre.statistic.beans.FileDownloadCounter;
import edu.ucsd.library.xdre.statistic.beans.ObjectCounter;
import edu.ucsd.library.xdre.statistic.beans.StatsRequest;
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
        ObjectCounter counter = damsStats.getItemsMap().get("bbxxxxxxxx").getCounter("168.0.0.1");
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
        ObjectCounter counter = damsStats.getPrivateItemsMap().get("bbxxxxxxxx").getCounter("168.0.0.1");
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
        FileDownloadCounter counter = damsStats.getItemsMap().get("bbxxxxxxxx").getFileDownloadCounter("_1.zip::168.0.0.1");
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
        FileDownloadCounter counter = damsStats.getPrivateItemsMap().get("bbxxxxxxxx").getFileDownloadCounter("_1.zip::168.0.0.1");
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

        assertEquals("Number of accesses doesn't match!", 1, analyzer.getPasStats().getNumAccess());
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
