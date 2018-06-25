package edu.ucsd.library.xdre.statistic.beans;

/**
 * 
 * Class StatsRequest - data structure of client request for dams statistics.
 * @author lsitu@ucsd.edu
 */
public class StatsRequest {
    private String hostName = null;
    private String requestDateTime = null;
    private String clientIp = null;
    private String httpMethod = null;
    private String requestUri = null;
    private String protocol = null;
    private String status = null;
    private String size = null;
    private String referrer = null;
    private String useAgent = null;

    public StatsRequest() {
    }

    public StatsRequest(String hostName, String clientIp, String requestDateTime, String httpMethod,
            String requestUri, String protocol, String status, String size, String referrer, String useAgent) {
        this.hostName = hostName;
        this.requestDateTime = requestDateTime;
        this.clientIp = clientIp;
        this.httpMethod = httpMethod;
        this.requestUri = requestUri;
        this.protocol = protocol;
        this.status = status;
        this.size = size;
        this.referrer = referrer;
        this.useAgent = useAgent;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getRequestDateTime() {
        return requestDateTime;
    }

    public void setRequestDateTime(String requestDateTime) {
        this.requestDateTime = requestDateTime;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }

    public String getClientIp() {
        return clientIp;
    }

    public void setClientIp(String clientIp) {
        this.clientIp = clientIp;
    }

    public String getRequestUri() {
        return requestUri;
    }

    public void setRequestUri(String requestUri) {
        this.requestUri = requestUri;
    }

    public String getProtocol() {
        return protocol;
    }

    public void setProtocol(String protocol) {
        this.protocol = protocol;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getReferrer() {
        return referrer;
    }

    public void setReferrer(String referrer) {
        this.referrer = referrer;
    }

    public String getUseAgent() {
        return useAgent;
    }

    public void setUseAgent(String useAgent) {
        this.useAgent = useAgent;
    }

    public String toString() {
        return hostName + " " + clientIp + " - - [" + requestDateTime + "] \"" + httpMethod + " " + requestUri + " " +
                protocol + "\" " + status + " " + size + " \"" +  referrer + "\" \"" + useAgent + "\"";
    }
}
