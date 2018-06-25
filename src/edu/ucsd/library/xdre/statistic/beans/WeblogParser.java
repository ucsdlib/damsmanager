package edu.ucsd.library.xdre.statistic.beans;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;

/**
 * 
 * Class WeblogParser, provide utility function to parse the Apache weblog request.
 * @author lsitu
 */
public class WeblogParser {
    private String hostRegex = " ([\\w.]+)";         // host
    private String ipRegex = " ([\\d.]+)";           // client IP
    private String dateTimeRegex = " \\[([\\w:/]+\\s[+\\-]\\d{4})\\]"; // request datetime
    private String requestRegex = " \"(.+?)\"";      // request method and request uri
    private String statusRegex = " (\\d{3})";        // http status code
    private String bytesRegex = " (\\d+|(.+?))";     // size
    private String referrerRegex = " \"(.*)\"";      // referrer
    private String userAgentRegex = " \"(.*)\"";     // user agent

    private Pattern accessWeblogPattern = null;

    public WeblogParser() {
        String patternRegex = ".*:" + hostRegex + ipRegex + " - -" + dateTimeRegex + requestRegex + statusRegex
                + bytesRegex + referrerRegex + userAgentRegex;
        accessWeblogPattern = Pattern.compile(patternRegex, Pattern.CASE_INSENSITIVE);
    }

    /**
     * Parse the client request logged in the weblog
     * @param line
     * @return
     */
    public StatsRequest parse(String line) {
        Matcher weblogMatcher = accessWeblogPattern.matcher(line);
        if (weblogMatcher.matches()) {
            String hostName = weblogMatcher.group(1);
            String clientIp = weblogMatcher.group(2);
            String requestDateTime = weblogMatcher.group(3);

            String httpMethod = "";
            String requestUri = "";
            String protocol = "";
            if (StringUtils.isNotBlank(weblogMatcher.group(4))) {
                String[] requestTokens = weblogMatcher.group(4).split(" ");
                httpMethod = requestTokens[0];
                requestUri = requestTokens[1];
                protocol = requestTokens[2];
            }

            String status = weblogMatcher.group(5);
            String size = weblogMatcher.group(6);
            String referrer = weblogMatcher.group(8);
            String useAgent = weblogMatcher.group(9);

            return new StatsRequest(hostName, clientIp, requestDateTime,
                httpMethod, requestUri, protocol, status, size, referrer, useAgent);
        }
        return null;
    }
}
