package edu.ucsd.library.xdre.harvesting;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.JSONParser;

import edu.ucsd.library.xdre.utils.Constants;

/**
 * class MetadataSource to download source metadata
 * @author lsitu
 */
public class MetadataSource extends ContentFile {
    private static Logger log = Logger.getLogger(MetadataSource.class);

    public static String METADATA_SOURCE_FOLDER ="metadata_source";

    private static String CIL_ID_PREFIX = "CIL_";

    private static String API_DOCUMENT_PATH ="public_documents/";

    private static String FILE_NAME_PATTERN_RENAME = ".*/(microscopy_[0-9]+/reconstruction/3view-stack.*|images/[0-9]+/3view-stack.*\\.mrc)";

    public static final String CIL_SOURCE = "_source";
    private static final String CIL_CCDB_KEY = "CIL_CCDB";
    private static final String DATA_TYPE_KEY = "Data_type";
    private static final String VIDEO_KEY = "Video";
    private static final String CIL_KEY = "CIL";
    private static final String IMAGE_FILES_KEY = "Image_files";
    private static final String FILE_PATH_KEY = "File_path";

    private static final String ALTERNATIVE_IMAGE_FILES_KEY = "Alternative_image_files";
    private static final String ALTERNATIVE_URL_POSTFIX_KEY = "URL_postfix";

    private static Pattern renameFilePattern = Pattern.compile(FILE_NAME_PATTERN_RENAME);

    private String cilId = null;
    public MetadataSource(String cilId, CilApiClient cilApiClient) {
        super(ensureUrlBaseFormat(Constants.CIL_HARVEST_API) + API_DOCUMENT_PATH + cilId, cilApiClient);
        this.cilId = cilId;
    }

    /**
     * Download the JSON source file and contents files for the cil object
     * @param harvestDate harvest date
     * @throws Exception
     */
    public String download(String harvestDirectory) throws Exception {
        String sourceId = cilId.replace(CIL_ID_PREFIX, "");
        String jsonFilename = sourceId + ".json";

        // Save JSON source file to 'metadata_source' folder
        String jsonFile = save(harvestDirectory, jsonFilename);

        JSONParser jsonParser = new JSONParser();
        try (FileReader reader = new FileReader(jsonFile)) {

            //Read JSON source from 'metadata_source' folder
            JSONObject source = (JSONObject)jsonParser.parse(reader);
            JSONObject ccdb = (JSONObject)source.get(CIL_CCDB_KEY);
            JSONObject dataType = (JSONObject)ccdb.get(DATA_TYPE_KEY);

            if (dataType == null)
                throw new Exception("Missing Data_type section in CIL object " + cilId + ": " + uri);

            Object videoFormat = dataType.get(VIDEO_KEY);

            if (videoFormat == null)
                throw new Exception("Missing Video format value in CIL object " + cilId + ": " + uri);

            JSONObject cil = (JSONObject)ccdb.get(CIL_KEY);

            // download contents: https://cildata.crbs.ucsd.edu/media/#{videos | images}/#{filePath}
            JSONArray imageFiles = (JSONArray)cil.get(IMAGE_FILES_KEY);

            if (imageFiles != null) {
                for (int i = 0; i < imageFiles.size(); i++) {
                    String fileUrl = null;
                    String fileLocation = null;

                    try {
                        String filePath = (String)((JSONObject)imageFiles.get(i)).get(FILE_PATH_KEY);

                        fileUrl = ensureUrlBaseFormat(Constants.CIL_CONTENT_URL_BASE) +
                                ((boolean)videoFormat ? VIDEO_PATH : IMAGE_PATH) + sourceId + "/" + filePath;
                        ContentFile contentFile = new ContentFile(fileUrl, cilApiClient);
                        fileLocation = contentFile.save(harvestDirectory, filePath);

                        String message = "Downloaded content " + fileUrl + ": " + fileLocation;
                        logMessage(harvestDirectory, message);
                    } catch (FileNotFoundException ex) {
                        String error = "Content file doesn't exist: " + fileUrl;
                        log.error(error);
                        logMessage(harvestDirectory, error + ": " + ex.getMessage());
                    } catch (Exception ex) {
                        String error = "Error downloading content file " + fileUrl;
                        log.error(error + ".", ex);
                        logMessage(harvestDirectory, error + ": " + ex.getMessage());
                    }
                }
            }

            // download alternate files: https://cildata.crbs.ucsd.edu/#{URL_postfix}
            JSONArray alternativeFiles = (JSONArray)cil.get(ALTERNATIVE_IMAGE_FILES_KEY);
            if (alternativeFiles != null) {
                for (int i = 0; i < alternativeFiles.size(); i++) {
                    String contentUrl = null;
                    String fileLocation = null;
                    try {
                        String urlPostfix = (String)((JSONObject)alternativeFiles.get(i)).get(ALTERNATIVE_URL_POSTFIX_KEY);
                        contentUrl = ensureUrlBaseFormat(Constants.CIL_CONTENT_URL_BASE) + urlPostfix;

                        ContentFile contentFile = new ContentFile(contentUrl, cilApiClient);
                        fileLocation = contentFile.save(harvestDirectory, getFileName(urlPostfix));

                        String message = "Downloaded alternative file " + contentUrl + ": " + fileLocation;
                        logMessage(harvestDirectory, message);
                    } catch (FileNotFoundException ex) {
                        String error = "Alternative file doesn't exist: " + contentUrl;
                        log.error(error);
                        logMessage(harvestDirectory, error + ": " + ex.getMessage());
                    } catch (Exception ex) {
                        String error = "Error downloading alternative file " + contentUrl;
                        log.error(error + ".", ex);
                        logMessage(harvestDirectory, error + ": " + ex.getMessage());
                    }
                }
            }
        }

        return jsonFile;
    }

    public String save(String basicDir, String fileName) throws Exception {
        File metadataSourceDir = new File(basicDir, METADATA_SOURCE_FOLDER);
        if (!metadataSourceDir.exists()) {
            metadataSourceDir.mkdirs();
        }
        return  writeFile(metadataSourceDir.getAbsolutePath(), fileName);
    }

    /**
     * Retrieve source JSON, extract source and write to destination.
     * @throws Exception
     * @param destFile the destination to write the content
     **/
    protected String writeFile(String basicDir, String fileName) throws Exception {
        String destFile = new File(basicDir, fileName).getAbsolutePath();

        String jsonValue = cilApiClient.getContentBodyAsString(uri);
        JSONObject json = (JSONObject)JSONValue.parse(jsonValue);
        JSONObject source = (JSONObject)json.get(CIL_SOURCE);

        int bytesRead = 0;
        try(InputStream in = new ByteArrayInputStream(source.toJSONString().getBytes("UTF-8"));
            OutputStream out = new FileOutputStream(destFile);) {
            byte[] buf = new byte[BUFFER_SIZE];
            while ((bytesRead = in.read(buf)) > 0) {
                out.write(buf, 0, bytesRead);
            }
        }

        return destFile;
    }

    /**
     * Determine whether a file path match a pattern for filename renaming.
     * @param filePath
     * @return
     */
    public static boolean renamePatternMatched(String filePath) {
        Matcher m = renameFilePattern.matcher(filePath);
        return m.find();
    }

    /**
     * Retrieve content file name basing on file path
     * @param filePath
     * @return
     */
    public static String getFileName(String filePath) {
        String[] filePaths = filePath.split("/");
        String fileName = filePaths[filePaths.length -1];
        if (renamePatternMatched(filePath.toLowerCase())) {
            // rename the content filename when matching the file path pattern
            if (filePaths[filePaths.length - 3].equals("images")) {
                fileName = filePaths[filePaths.length - 2] + "-" + fileName;
            } else if (filePaths[filePaths.length - 3].startsWith("microscopy_")) {
                fileName = filePaths[filePaths.length - 3] + "-" + fileName;
            } 
        }

        return fileName;
    }
}
