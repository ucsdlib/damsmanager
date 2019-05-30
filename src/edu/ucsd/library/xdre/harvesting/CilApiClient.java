package edu.ucsd.library.xdre.harvesting;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Properties;

import javax.security.auth.login.LoginException;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.dom4j.DocumentException;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.HttpClientBase;

/**
 * CilApiClient provide basic functions to pull data/content from CIL API
 * 
 * @author lsitu
 * 
 */
public class CilApiClient extends HttpClientBase {

    /**
     * Construct a CilApiClient object.
     * @throws IOException 
     * @throws LoginException 
     */
    public CilApiClient() throws LoginException, IOException {
        super(Constants.CIL_HARVEST_API, Constants.CIL_HARVEST_API_USER, Constants.CIL_HARVEST_API_PWD);
    }

    /**
     * Construct a CilApiClient object using basic CIL URL.
     * 
     * @param storageURL
     * @throws IOException
     * @throws LoginException
     */
    public CilApiClient(String cilApiBase) throws IOException, LoginException {
        super(cilApiBase, Constants.CIL_HARVEST_API_USER, Constants.CIL_HARVEST_API_PWD);
    }

    /**
     * Construct CilApiClient object and authenticate using the dams repository properties
     * information provided.
     * 
     * @param account
     * @param out
     * @throws IOException
     * @throws LoginException
     */
    public CilApiClient(Properties props) throws IOException, LoginException {
        super((String)props.get("cil.harvest.api"), (String)props.get("cil.harvest.api"), (String)props.get("cil.harvest.api"));
    }


    /**
     * Handle error codes and response header and body information.
     * @throws IOException 
     * @throws IllegalStateException 
     * @throws DocumentException 
     * @throws LoginException 
     * @throws Exception 
     */
    public void handleError(String format) throws IllegalStateException, IOException, DocumentException, LoginException {
        int status = response.getStatusLine().getStatusCode();
        Header[] headers = response.getAllHeaders();
        for (int i = 0; i < headers.length; i++) {
            log.debug(headers[i] + "; ");
        }

        String respContent = "";
        if ( !request.getMethod().equals("HEAD") ) {

            HttpEntity ent = response.getEntity();
            InputStream in = null;
            try{
                in = ent.getContent();
                String contentType = ent.getContentType().getValue();
                if(format != null && contentType.contains("json")){
                    Reader reader = new InputStreamReader(in);
                    JSONObject resultObj = (JSONObject) JSONValue.parse(reader);
                    respContent += resultObj.get("status") + " status code " + resultObj.get("statusCode")
                            + ". Error " + resultObj.get("message");
                    log.debug(resultObj.toString());
                    reader.close();
                } else {

                    byte[] buf = new byte[4096];
                    ByteArrayOutputStream out = new ByteArrayOutputStream();
                    int bRead = -1;

                    while((bRead=in.read(buf)) > 0)
                        out.write(buf, 0, bRead);
                    respContent += out.toString();
                    out.close();
                    log.debug(respContent);
                }
            }finally{
                close(in);
            }
        }

        //200 - OK: Success, object/file exists
        String reqInfo = request.getMethod() + " " + request.getURI();
        log.info( reqInfo + ": " + respContent);
        //401 - unauthorized access
        if (status == 401) {  
            throw new LoginException(reqInfo + ": " + respContent);
        //403 - Forbidden
        } else if (status == 403) {  
            if(respContent.indexOf("not exists") > 0)
                throw new FileNotFoundException(reqInfo + ": " + respContent);
            else 
                throw new IOException(reqInfo + ": " + respContent);
        //404 - Not Found: Object/file does not exist 
        } else if (status == 404) { 
            throw new FileNotFoundException(reqInfo + ": " + respContent);
            
        //500 - Internal Error: Other errors
        } else if (status == 500) {  
            throw new IOException(reqInfo + ": " + respContent);
        //Unknown status code
        } else { 
            throw new IOException(reqInfo + ": " + respContent);
        }
    }
}
