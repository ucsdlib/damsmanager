package edu.ucsd.library.xdre.statistics.beans;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.ucsd.library.xdre.statistic.analyzer.DAMStatistic;
import edu.ucsd.library.xdre.statistic.beans.FileDownloadCounter;
import edu.ucsd.library.xdre.statistic.beans.ObjectCounter;
import edu.ucsd.library.xdre.statistic.beans.StatsRequest;
import edu.ucsd.library.xdre.statistic.beans.WeblogParser;

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
}
