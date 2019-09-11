package edu.ucsd.library.xdre.harvesting;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * class ContentFile construct content file for downloading contents
 * @author lsitu
 */
public class ContentFile {

    public static SimpleDateFormat LOG_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
    protected static final int BUFFER_SIZE = 5120;

    protected static final String VIDEO_PATH = "media/videos/";
    protected static final String IMAGE_PATH = "media/images/";
    protected static final String CONTENT_FILE_FOLDER ="content_files";

    protected String uri = null;
    protected CilApiClient cilApiClient = null;
    public ContentFile(String uri, CilApiClient cilApiClient) {
        this.uri = uri;
        this.cilApiClient = cilApiClient;
    }

    /**
     * Get the original source uri of the content file.
     * @return
     */
    public String getUri() {
        return uri;
    }

    /**
     * Save content to destination.
     * @throws Exception
     * @param basicDir the basic directory
     * @param contentFile the filename
     **/
    public String save(String basicDir, String fileName) throws Exception {
        File contentDir = new File(basicDir, CONTENT_FILE_FOLDER);
        if (!contentDir.exists()) {
            contentDir.mkdirs();
        }

        // Skip download the content file if its exists.
        // This could be downloaded in last time during initiation.
        File destFile = new File(contentDir.getAbsolutePath(), fileName);
        if (destFile.exists() && destFile.length() > 0) {
            return destFile.getAbsolutePath();
        }

        return  writeFile(contentDir.getAbsolutePath(), fileName);
    }

    /**
     * Retrieve content from url and write to destination.
     * @throws Exception
     * @param destFile the destination to write the content
     **/
    protected String writeFile(String basicDir, String fileName) throws Exception {
        String destFile = new File(basicDir, fileName).getAbsolutePath();

        cilApiClient.downloadFile(uri, destFile);

        return destFile;
    }

    /**
     * Append message to the download log
     * @param message
     * @throws IOException
     */
    public static void logMessage(String logDir, String message) throws IOException {
        try (BufferedWriter out = new BufferedWriter( 
                          new FileWriter(new File(logDir, "download.log"), true))) { 
            out.write(LOG_DATE_FORMAT.format(Calendar.getInstance().getTime()) + " " + message + "\n");
        }
    }

    /**
     * Ensure url base ended with forward slash
     * @param urlBase
     * @return
     */
    public static String ensureUrlBaseFormat(String urlBase) {
        return urlBase + (urlBase.endsWith("/") ? "" : "/");
    }
}
