package edu.ucsd.library.xdre.utils;

import java.io.Closeable;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.security.auth.login.LoginException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpHost;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScheme;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.AuthState;
import org.apache.http.auth.Credentials;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.protocol.ClientContext;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;
import org.apache.http.impl.conn.PoolingClientConnectionManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.ExecutionContext;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;
import org.apache.log4j.Logger;
import org.dom4j.DocumentException;

/**
 * HttpClientBase provide basic functions with HttpClient for REST API request
 *
 * @author lsitu
 */
public abstract class HttpClientBase {
    protected static final Logger log = Logger.getLogger(HttpClientBase.class);

    protected static final int BUFFER_SIZE = 5120;

    protected String storageURL = null;       // REST API URL Base
    protected HttpClient client = null;       // HttpClient object
    protected HttpRequestBase request = null; // HTTP request
    protected HttpResponse response = null;   // HTTP response
    protected HttpContext httpContext = null; // Http Context

    /**
     * Construct a HttpClientBase object.
     */
    public HttpClientBase() {}

    /**
     * Construct a CilApiBase object using storage URL, username and password.
     * 
     * @param storageURL
     * @throws IOException
     * @throws LoginException
     */
    public HttpClientBase(String storageUR, String user, String password) throws IOException, LoginException {
        if(storageUR.endsWith("/"))
            storageUR = storageUR.substring(0, storageUR.length() -1);
        this.storageURL = storageUR;
        client = createHttpClient(user, password);
    }

    /**
     * Create HttpClient with PreemptiveAuth when user and password provide 
     * @param userName
     * @param password
     * @return
     */
    private HttpClient createHttpClient(String userName, String password) {
        // disable timeouts
        BasicHttpParams params = new BasicHttpParams();
        params.setParameter( "http.socket.timeout",     new Integer(0) );
        params.setParameter( "http.connection.timeout", new Integer(0) );
      
        DefaultHttpClient client = new DefaultHttpClient( getConnectionManager(), params );

        // disable retries
        DefaultHttpRequestRetryHandler x = new DefaultHttpRequestRetryHandler(0, false);
        client.setHttpRequestRetryHandler( x );
        
        if (userName != null && userName.length() > 0) {
            client.getCredentialsProvider().setCredentials(
                    new AuthScope(AuthScope.ANY_HOST, AuthScope.ANY_PORT),
                    new UsernamePasswordCredentials(userName, password)
            );

            httpContext = new BasicHttpContext();

            // Generate BASIC scheme object and stick it to the local execution context
            BasicScheme basicAuth = new BasicScheme();
            httpContext.setAttribute("preemptive-auth", basicAuth);

            // Add as the first request interceptor
            client.addRequestInterceptor(new PreemptiveAuthInterceptor(), 0);
        }

        return client;
    }

    /**
     * Initiate ClientConnectionManager for http and https connections
     * @return
     */
    private ClientConnectionManager getConnectionManager(){

        ClientConnectionManager ccm = new PoolingClientConnectionManager();
        try {
            // Accept all SSL certificates
            X509TrustManager tm = new X509TrustManager() {
                public void checkClientTrusted(X509Certificate[] xcs, String string) throws CertificateException { }
                public void checkServerTrusted(X509Certificate[] xcs, String string) throws CertificateException { }
                public X509Certificate[] getAcceptedIssuers() { return null; }
            };

            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{tm}, null);
            SSLContext.setDefault(ctx);
            
            SSLSocketFactory ssf = new SSLSocketFactory(ctx,SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
            SchemeRegistry sr = ccm.getSchemeRegistry();
            sr.register(new Scheme("https", 443, ssf));
            sr.register(new Scheme("http", 80, new PlainSocketFactory()));
            
        } catch ( Exception ex ){
            ex.printStackTrace();
        }
        return ccm;
    }

    /**
     * Log response header and body information.
     * @throws IOException 
     * @throws IllegalStateException 
     * @throws DocumentException 
     * @throws LoginException 
     * @throws Exception 
     */
    public abstract void handleError(String format) throws IllegalStateException, IOException, DocumentException, LoginException;

    /**
     * Retrieve the HTTP content from a URL provided.
     * 
     * @param url
     * @return
     * @throws Exception 
     */
    public String getContentBodyAsString(String url) throws Exception {
        String result = "";
        HttpGet get = new HttpGet(url);
        int status = -1;
        try {
            status = execute(get);
            if(status == 200){              
                HttpEntity en = response.getEntity();
                Header encoding = en.getContentEncoding();
                result = EntityUtils.toString(en, (encoding==null?"UTF-8":encoding.getValue()));
            } else
                handleError(null);
        
        } finally {
            get.releaseConnection();
        }
        return result;
    }

    /**
     * Execute a http request
     * @param request
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     * @throws LoginException
     */
    public int execute(HttpRequestBase req) throws ClientProtocolException,
            IOException, LoginException {
        int statusCode = -1;
        response = null;
        request = req;
        try{
            if (httpContext != null) {
                response = client.execute(request, httpContext);
            } else {
                response = client.execute(request);
            }
        }finally{
            if(response != null)
                statusCode = response.getStatusLine().getStatusCode();
            log.info(statusCode + " " + req.getMethod() + " " + req.getURI());
        }

        return statusCode;
    }

    /**
     * Retrieve the HTTP content and save it to file.
     * @param url
     * @param fileName
     * @return
     * @throws Exception 
     */
    public void downloadFile(String url, String fileName) throws Exception {
        HttpGet get = new HttpGet(url);
        int status = -1;
        try {
            status = execute(get);
            int bytesRead = -1;
            if (status == 200){
                try (InputStream in = response.getEntity().getContent();
                        OutputStream out = new FileOutputStream(fileName);) {
                    byte[] buf = new byte[BUFFER_SIZE];
                    while ((bytesRead = in.read(buf)) > 0) {
                        out.write(buf, 0, bytesRead);
                    }
                }
            } else
                handleError(null);
        } finally {
            get.releaseConnection();
        }
    }

    /**
     * Close up IO resources
     * @param closeable
     */
    public static void close(Closeable closeable){
        if(closeable != null){
            try{
                closeable.close();
            }catch (IOException ioe){
                ioe.printStackTrace();
            }
            closeable = null;
        }
    }

    public void close(){
        client.getConnectionManager().shutdown();
    }

    /**
     * PreemptiveAuthInterceptor class for PreemptiveAuth
     * @author lsitu
     *
     */
    class PreemptiveAuthInterceptor implements HttpRequestInterceptor {
        public void process(final HttpRequest request, final HttpContext context) throws HttpException, IOException {
            AuthState authState = (AuthState) context.getAttribute(ClientContext.TARGET_AUTH_STATE);

            // If no auth scheme available yet, try to initialize it preemptively
            if (authState.getAuthScheme() == null) {
                AuthScheme authScheme = (AuthScheme) context.getAttribute("preemptive-auth");
                CredentialsProvider credsProvider = (CredentialsProvider) context.getAttribute(ClientContext.CREDS_PROVIDER);
                HttpHost targetHost = (HttpHost) context.getAttribute(ExecutionContext.HTTP_TARGET_HOST);
                if (authScheme != null) {
                    Credentials creds = credsProvider.getCredentials(new AuthScope(targetHost.getHostName(), targetHost.getPort()));
                    if (creds == null) {
                        throw new HttpException("No credentials for preemptive authentication");
                    }
                    authState.update(authScheme, creds);
                }
            }
        }
    }
}
