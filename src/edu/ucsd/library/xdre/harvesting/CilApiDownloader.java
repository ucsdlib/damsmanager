package edu.ucsd.library.xdre.harvesting;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.web.CILHarvestingTaskController;

/**
 * Class CilApiDownloader to download JSON source and content files from CIL API
 * @author lsitu
 */
public class CilApiDownloader {
    private static Logger log = Logger.getLogger(CilApiDownloader.class);

    public static final String DATE_FORMAT = "yyyy-MM-dd";

    private static int MAX_BATCH_SIZE = 500;
    private static String PUBLIC_IDS_PATH = "public_ids?";

    private List<String> cilIds = null;
    private String  harvestDirectory = null;
    private CilApiClient cilApiClient = null;

    public CilApiDownloader(CilApiClient cilApiClient, Date dateHarvest, Date lastModified)
            throws Exception {
        this.cilApiClient = cilApiClient;

        if (dateHarvest == null)
            dateHarvest = Calendar.getInstance().getTime();

        SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
        String harvestDate = dateFormat.format(dateHarvest);

        File harvestDir = new File(Constants.CIL_HARVEST_DIR,
                CILHarvestingTaskController.CIL_HARVEST_PATH_PREFIX + harvestDate);
        if (!harvestDir.exists()) {
            harvestDir.mkdirs();
        }

        harvestDirectory = harvestDir.getAbsolutePath() + File.separatorChar;

        logMessage("CIL harvest " + harvestDirectory
                + (lastModified == null ? " initiation." : " with lastModifiedDate: " + dateFormat.format(lastModified)));

        cilIds = searchCilIds(lastModified);

        Collections.sort(cilIds);
    }

    /**
     * Download JSON source files and content files
     * @return CIL objects retrieved
     * @throws Exception
     */
    public int download() throws Exception {
        logMessage("CIL JSON source files found: " + cilIds.size());

        for (String cilId : cilIds) {
            try {
                MetadataSource metadataSource = new MetadataSource(cilId, cilApiClient);

                logMessage("Downloading object " + cilId + " from " + metadataSource.getUri());

                String jsonFile = metadataSource.download(harvestDirectory);
                logMessage("JSON source " + metadataSource.getUri() + " downloaded: " + jsonFile);
            } catch (Exception ex) {
                String error = "Error in downloading JSON source " + cilId;
                log.error(error, ex);
                logMessage(error + ": " + ex.getMessage());
                throw ex;
            }
        }
        return cilIds.size();
    }

    /**
     * Get the root harvest directory for the current download
     * @return
     */
    public String getHarvestDirectory() {
        return this.harvestDirectory;
    }

    /**
     * Loop through to search CIL IDs by lastModified date
     * @param lastModified
     * @return
     * @throws Exception
     */
    private List<String> searchCilIds(Date lastModified) throws Exception {
        List<String> ids = new ArrayList<>();
        int start = 0;
        long totalFound = getTotalHits(lastModified);

        while (start < totalFound) {
            ids.addAll(getIdBatch(start, MAX_BATCH_SIZE, lastModified));
            start += MAX_BATCH_SIZE;
        }

        return ids;
    }

    /**
     * Retrieve the number of cil object since lastModifiedDate
     * @param damsClient
     * @param modifiedDate
     * @return
     * @throws Exception
     */
    private long getTotalHits(Date lastModified) throws Exception {
        String queryString = buildQueryString(0, 0, lastModified);
        String url = ContentFile.ensureUrlBaseFormat(Constants.CIL_HARVEST_API) + PUBLIC_IDS_PATH + queryString;

        JSONObject result = (JSONObject)JSONValue.parse(cilApiClient.getContentBodyAsString(url));
        return (long)((JSONObject)result.get("hits")).get("total");
    }

    /**
     * Retrieve the CIL IDs in batch
     * @param damsClient
     * @param start
     * @param size
     * @param modifiedDate
     * @return
     * @throws Exception
     */
    private List<String> getIdBatch(int start, int size, Date lastModified) throws Exception {
        List<String> ids = new ArrayList<>();

        String queryString = buildQueryString(start, size, lastModified);
        String url = Constants.CIL_HARVEST_API + PUBLIC_IDS_PATH + queryString;
        JSONObject result = (JSONObject)JSONValue.parse(cilApiClient.getContentBodyAsString(url));
        JSONArray itemsArr = (JSONArray)((JSONObject)result.get("hits")).get("hits");
        for (int i=0; i< itemsArr.size(); i++) {
            ids.add((String) ((JSONObject)itemsArr.get(i)).get("_id"));
        }

        return ids;
    }

    /**
     * Build the query string for CIL API
     * @param start
     * @param size
     * @param lastModified
     * @return
     * @throws UnsupportedEncodingException 
     */
    private String buildQueryString(int start, int size, Date lastModified) throws UnsupportedEncodingException {
        StringBuilder urlBuilder = new StringBuilder();
        urlBuilder.append(URLEncoder.encode("from", "UTF-8"));
        urlBuilder.append("=");
        urlBuilder.append(URLEncoder.encode("" + start, "UTF-8"));
        urlBuilder.append("&");
        urlBuilder.append(URLEncoder.encode("size", "UTF-8"));
        urlBuilder.append("=");
        urlBuilder.append(URLEncoder.encode("" + size, "UTF-8"));
        if (lastModified != null) {
            urlBuilder.append("&");
            urlBuilder.append(URLEncoder.encode("lastModified", "UTF-8"));
            urlBuilder.append("=");
            urlBuilder.append(URLEncoder.encode("" + lastModified.getTime()/1000, "UTF-8"));
        }

        return urlBuilder.toString();
    }

    /**
     * Get cilId list
     * @return
     */
    public List<String> getCilIds() {
        return cilIds;
    }

    /**
     * Append message to the CIL download log
     * @param message
     * @throws IOException
     */
    public void logMessage(String message) throws IOException {
        ContentFile.logMessage(harvestDirectory, message);
    }
}
