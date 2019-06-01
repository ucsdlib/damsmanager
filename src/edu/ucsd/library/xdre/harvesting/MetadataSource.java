package edu.ucsd.library.xdre.harvesting;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;

import org.apache.log4j.Logger;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
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

    public static final String CIL_SOURCE = "_source";
    private static final String CIL_CCDB_KEY = "CIL_CCDB";
    private static final String DATA_TYPE_KEY = "Data_type";
    private static final String VIDEO_KEY = "Video";
    private static final String CIL_KEY = "CIL";
    private static final String IMAGE_FILES_KEY = "Image_files";
    private static final String FILE_PATH_KEY = "File_path";

    private static final String ALTERNATIVE_IMAGE_FILES_KEY = "Alternative_image_files";
    private static final String ALTERNATIVE_URL_POSTFIX_KEY = "URL_postfix";

    private String cilId = null;
    public MetadataSource(String cilId, CilApiClient cilApiClient) {
        super(Constants.CIL_HARVEST_API + API_DOCUMENT_PATH + cilId, cilApiClient);
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
            JSONObject data = (JSONObject)jsonParser.parse(reader);
            JSONObject source = (JSONObject)data.get(CIL_SOURCE);
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
            for (int i = 0; i < imageFiles.size(); i++) {
                String fileUrl = null;
                String fileLocation = null;

                try {
                    String filePath = (String)((JSONObject)imageFiles.get(i)).get(FILE_PATH_KEY);
    
                    fileUrl = Constants.CIL_CONTENT_URL_BASE + 
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

            // download alternate files: https://cildata.crbs.ucsd.edu/#{URL_postfix}
            JSONArray alternativeFiles = (JSONArray)cil.get(ALTERNATIVE_IMAGE_FILES_KEY);
            if (alternativeFiles != null) {
                for (int i = 0; i < alternativeFiles.size(); i++) {
                    String contentUrl = null;
                    String fileLocation = null;
                    try {
                        String urlPostfix = (String)((JSONObject)alternativeFiles.get(i)).get(ALTERNATIVE_URL_POSTFIX_KEY);
                        contentUrl = Constants.CIL_CONTENT_URL_BASE + urlPostfix;
    
                        String[] filePaths = urlPostfix.split("/");
                        String fileName = filePaths[filePaths.length - 1];
    
                        ContentFile contentFile = new ContentFile(contentUrl, cilApiClient);
                        fileLocation = contentFile.save(harvestDirectory, fileName);

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
}
