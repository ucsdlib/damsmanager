package edu.ucsd.library.xdre.statistics.beans;

import static org.junit.Assert.assertEquals;

import java.io.IOException;

import org.junit.BeforeClass;
import org.junit.Test;

import edu.ucsd.library.xdre.statistic.beans.StatsRequest;
import edu.ucsd.library.xdre.statistic.beans.WeblogParser;

/**
 * Tests methods for WeblogParser class
 * @author lsitu
 *
 */
public class WeblogParserTest {

    private static WeblogParser weblogParser = null;

    @BeforeClass
    public static void init() throws IOException {
        weblogParser = new WeblogParser();
    }

    @Test
    public void testParseRequest() throws Exception {
        String weblogRequest = "May 10 00:01:01 library apache[1829]: library.ucsd.edu 168.0.0.1 - - [10/May/2018:00:01:01 -0700] "
                + "\"GET /dc/object/bbxxxxxxxx HTTP/1.1\" 200 20674 \"https://www.example.com/\" \"Mozilla/5.0\"";

        StatsRequest statsRequest = weblogParser.parse(weblogRequest);
        assertEquals("Host name need to be matched", "library.ucsd.edu", statsRequest.getHostName());
        assertEquals("Client IP address need to be matched", "168.0.0.1", statsRequest.getClientIp());
        assertEquals("Request URI need to be matched", "/dc/object/bbxxxxxxxx", statsRequest.getRequestUri());
        assertEquals("Referrer need to be matched", "https://www.example.com/", statsRequest.getReferrer());
        assertEquals("Request user agent need to be matched", "Mozilla/5.0", statsRequest.getUseAgent());
        assertEquals("Request string need to be matched", weblogRequest, "May 10 00:01:01 library apache[1829]: " + statsRequest.toString());
    }

    @Test
    public void testParseRequestSpecial() throws Exception {
        String weblogRequest = "May 10 00:01:01 library apache[1829]: library.ucsd.edu 168.0.0.1 - - [10/May/2018:00:01:01 -0700] "
                + "\"GET /dc/object/bbxxxxxxxx HTTP/1.1\" 301 - \"-\" \"\"";

        StatsRequest statsRequest = weblogParser.parse(weblogRequest);
        assertEquals("Host name need to be matched", "library.ucsd.edu", statsRequest.getHostName());
        assertEquals("Client IP address need to be matched", "168.0.0.1", statsRequest.getClientIp());
        assertEquals("Request URI need to be matched", "/dc/object/bbxxxxxxxx", statsRequest.getRequestUri());
        assertEquals("Referrer need to be matched", "-", statsRequest.getReferrer());
        assertEquals("Request user agent need to be matched", "", statsRequest.getUseAgent());
        assertEquals("Request string need to be matched", weblogRequest, "May 10 00:01:01 library apache[1829]: " + statsRequest.toString());
    }
}
