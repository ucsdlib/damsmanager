package edu.ucsd.library.xdre.web;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.harvesting.CilApiClient;
import edu.ucsd.library.xdre.harvesting.CilApiDownloader;
import edu.ucsd.library.xdre.harvesting.CilHarvesting;
import edu.ucsd.library.xdre.harvesting.FieldMappings;
import edu.ucsd.library.xdre.harvesting.MetadataSource;
import edu.ucsd.library.xdre.tab.TabularRecordBasic;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * Class CILHarvestingController perform task for CIL harvesting
 * @author lsitu
 *
 */
public class CILHarvestingTaskController implements Controller {
    private static Logger log = Logger.getLogger(CILHarvestingTaskController.class);
    private static boolean harvesting = false;

    public static String DAMS42JSON_XSL_FILE = "/resources/dams42json.xsl";

    // CIL harvest metadata processed folder
    public static String CIL_HARVEST_METADATA_PROCESSED = "metadata_processed";
    // CIL harvest source JSON folder
    public static String CIL_HARVEST_METADATA_SOURCE = "metadata_source";
    public static String CIL_HARVEST_PATH_PREFIX = "cil_harvest_";
    public static String CIL_HARVEST_MAPPING_FILE = "/resources/CIL Processing and Mapping Instructions.xlsx";
    public static String EXCEL_HEADINGS_CSV_FILE = "cil_excel_object_input.csv";
    public static String EXCEL_SUBJECTS_CSV_FILE = "cil_excel_subject_headings.csv";

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        OutputStream out = null;
        try {
            out = response.getOutputStream();

            // lastModified to control source json download with modified date after.
            String lastModifiedParam = request.getParameter("lastModified");
            // The designated date for the harvest folder
            String harvestDateParam = request.getParameter("harvestDate");

            // Records with date modified after lastModified
            Date lastModified = null;
            if (!StringUtils.isBlank(lastModifiedParam )) {
                lastModified = new SimpleDateFormat(CilApiDownloader.DATE_FORMAT).parse(lastModifiedParam );
            }

            // Set harvest date for the ingest folder
            Date harvestDate = Calendar.getInstance().getTime();
            if (!StringUtils.isBlank(harvestDateParam)) {
                harvestDate = new SimpleDateFormat(CilApiDownloader.DATE_FORMAT).parse(harvestDateParam);
            }

            List<String> sourceJsonsAdded = performHarvestingTask(harvestDate, lastModified);

            if (sourceJsonsAdded.size() > 0) {
                out.write(("Successfully converted " + sourceJsonsAdded.size() + " JSON source files.").getBytes("UTF-8"));
            } else {
                out.write(("No JSON source files are processed.").getBytes("UTF-8"));
            }
        }catch (Exception e){
            e.printStackTrace();
            out.write(("Error convert JSON source: " + e.getMessage()).getBytes("UTF-8"));
        }finally {
            if(out != null)
                out.close();
        }
        return null;
    }

    /**
     * Download JSON source from CIL REST API and perform metadata conversion.
     * @return
     * @throws Exception
     */
    public static List<String> performHarvestingTask(Date dateHarvest, Date lastModified) throws Exception {
        
        CilApiClient cilApiClient = null;
        List<String> sourceJsonsAdded = null;
        if (assignHarvestTask()) {
            try {
                cilApiClient = new CilApiClient();

                if (lastModified == null) {
                    lastModified = getLastHarvestData();
                }

                CilApiDownloader cilApiDownloader = new CilApiDownloader(cilApiClient, dateHarvest, lastModified);
                cilApiDownloader.download();

                sourceJsonsAdded = performHarvestingTask(cilApiDownloader.getHarvestDirectory());
            } catch (Exception ex) {
                ex.printStackTrace();
                log.error("CIL harvesting failed: " + ex.getMessage());
                throw ex;
            } finally {
                endHarvestTask(); // release the CIL harvest process

                if(cilApiClient != null) {
                    cilApiClient.close();
                }
            }
        } else {
            throw new Exception("CIL havesting process is already running ...");
        }
        return sourceJsonsAdded;
    }

    /*
     * Retrieve the most recent harvest date by the timestamp in the folder name
     * @return
     */
    private static Date getLastHarvestData() {
        Date lastHarvestDate = null;
        File[] cilDirs = new File(Constants.CIL_HARVEST_DIR).listFiles();
        for (File cilDir : cilDirs) {
            if (cilDir.isDirectory() && cilDir.getName().startsWith(CIL_HARVEST_PATH_PREFIX)) {
                File metadataProcessed = new File(cilDir.getAbsolutePath(), CIL_HARVEST_METADATA_PROCESSED);
                File excelHeadingsFile = new File(metadataProcessed.getAbsolutePath(),
                        EXCEL_HEADINGS_CSV_FILE);
                if (excelHeadingsFile.exists() && excelHeadingsFile.length() > 0) {
                    SimpleDateFormat dateFormat = new SimpleDateFormat(CilApiDownloader.DATE_FORMAT);
                    try {
                        Date harvestDate = dateFormat.parse(cilDir.getName().replace(CIL_HARVEST_PATH_PREFIX, ""));
                        if (lastHarvestDate == null
                                || harvestDate.compareTo(lastHarvestDate) > 0) {
                            lastHarvestDate = harvestDate;
                        }
                    } catch (ParseException ex) {
                        ex.printStackTrace();
                    }
                }
            }
        }
        return lastHarvestDate;
    }

    /*
     * Method to ensure that no other processes will run while harvest started.
     * This will reject all other process when a process is running.
     * @return
     */
    private static synchronized boolean assignHarvestTask() {
        if (harvesting == true) {
            log.warn("CIL Harvesting task is already started ...");
            return false;
        } else {
            harvesting = true;

            log.info("CIL Harvesting task is started ...");
        }
        return harvesting;
    }

    /*
     * Reset the flag for CIL harvesting process.
     */
    private static synchronized void endHarvestTask() {
        if (harvesting == true) {
            harvesting = false;

            log.info("CIL Harvesting task is ended ...");
        }
    }

    /**
     * Function to lookup json files added and perform processing.
     * @return
     * @throws Exception
     */
    public synchronized static List<String> performHarvestingTask(String harvestDirectory) throws Exception{
        DAMSClient damsClient = null;
        List<String> sourceJsonsAdded = new  ArrayList<>();

        try {
            damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
            File sourceFiles = new File(harvestDirectory, MetadataSource.METADATA_SOURCE_FOLDER);
            List<String> sourceJsons = new  ArrayList<>();
            if (sourceFiles.exists()) {
                addSourceFiles(damsClient, sourceJsons, sourceFiles);
            }

            if (sourceJsons.size() > 0) {
                log.info("Detected " + sourceJsons.size() + " CIL JSON source files  in directory " + sourceFiles.getAbsolutePath() + ".");

                sourceJsonsAdded.addAll(sourceJsons);

                // process the source JSON
                InputStream mappingsInput = null;
                InputStream dams42jsonInput = getDams42JsonXsl();

                // retrieve the CIL Processing and Mapping Instructions file content
                String cilMappingFile = StringUtils.isNotBlank(Constants.CIL_HARVEST_MAPPING_FILE)
                        ? Constants.CIL_HARVEST_MAPPING_FILE : CIL_HARVEST_MAPPING_FILE;
                if (new File(cilMappingFile).exists()) {
                    mappingsInput = new FileInputStream(cilMappingFile);
                } else {
                    // use the default CIL Processing and Mapping Instructions.xlsx file provided in the code
                    mappingsInput = CILHarvestingTaskController.class.getResourceAsStream(cilMappingFile);
                }

                String message = "";
                File destMetadataFile = null;
                File destSubjectsFile = null;
                try(InputStream mappingsIn = mappingsInput; InputStream dams42jsonIn = dams42jsonInput;) {
                    FieldMappings fieldMapping = new FieldMappings(mappingsIn);
                    CilHarvesting cilHarvesting = new CilHarvesting(
                            fieldMapping.getFieldMappings(),
                            fieldMapping.getConstantFields(),
                            sourceJsonsAdded);

                    String csvObjectString = cilHarvesting.toCSV(dams42jsonIn);
                    File metadataProcessedDir = new File(harvestDirectory, CIL_HARVEST_METADATA_PROCESSED);
                    if (!metadataProcessedDir.exists())
                        metadataProcessedDir.mkdirs();

                    destMetadataFile = new File (metadataProcessedDir, EXCEL_HEADINGS_CSV_FILE);
                    writeContent(destMetadataFile.getAbsolutePath(), csvObjectString);

                    // Export subject headings
                    String csvSubjectString = cilHarvesting.getSubjectHeadingsCsv();
                    destSubjectsFile = new File (metadataProcessedDir, EXCEL_SUBJECTS_CSV_FILE);
                    writeContent(destSubjectsFile.getAbsolutePath(), csvSubjectString);

                    message = "Successfully converted " + sourceJsonsAdded.size() + " CIL JSON source files: "
                            + destMetadataFile.getAbsolutePath();

                    log.info(message);
                } catch (Exception e) {
                    e.printStackTrace();
                    message = "Failed to convert " + sourceJsonsAdded.size() + " CIL JSON source files: "
                            + (destMetadataFile != null ? destMetadataFile.getAbsolutePath() : "");
                    log.error(message, e);
                    throw new Exception(message);
                }

             // notify DDOM users by email for the status of CIL harvesting
                try {
                    String[] emails = Constants.CIL_HARVEST_NOTIFY_EMAILS.split("\\,");
                    String sender = Constants.MAILSENDER_DAMSSUPPORT;

                    DAMSClient.sendMail(sender, emails, "CIL Harvesting Status Report", message, "text/html", "smtp.ucsd.edu");
                } catch (Exception e) {
                    e.printStackTrace();
                    log.error("Failed to send mail for CIL harvesting: " + e.getMessage());
                }
            } else {
                log.info("No CIL JSON source files were detected to be added.");
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            log.error("CIL harvesting failed: " + ex.getMessage());
            throw ex;
        } finally {
            if(damsClient != null)
                damsClient.close();
        }

        return sourceJsonsAdded;
    }

    /**
     * Retrieve the dams42Json XSL as InputStream from configuration if there's any,
     * or use the default provided in source code
     * @return
     * @throws FileNotFoundException
     */
    public static InputStream getDams42JsonXsl() throws FileNotFoundException {
        InputStream dams42jsonIn = null;
        // retrieve the dams42json xsl content
        String dams42jsonXsl = StringUtils.isNotBlank(Constants.DAMS42JSON_XSL_FILE) 
                ? Constants.DAMS42JSON_XSL_FILE : DAMS42JSON_XSL_FILE;
        if (new File(dams42jsonXsl).exists()) {
            dams42jsonIn = new FileInputStream(dams42jsonXsl);
        } else {
            // use the default dams42json.xsl file provided in the source code
            dams42jsonIn = CILHarvestingTaskController.class.getResourceAsStream(DAMS42JSON_XSL_FILE);
        }
        return dams42jsonIn;
    }

    /**
     * Write content to file
     * @param filePath
     * @param content
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private static File writeContent(String filePath, String content) throws FileNotFoundException, IOException {
        byte[] buf = new byte[4096];
        File destFile = new File(filePath);

        try (InputStream in = new ByteArrayInputStream(content.getBytes("UTF-8"));
                OutputStream out = new FileOutputStream(destFile)) {
            int bytesRead = 0;
            while((bytesRead = in.read(buf)) > 0) {
                out.write(buf, 0, bytesRead);
            }
        }

        return destFile;
    }

    /**
     * Add json source files to list
     * @param sourceFiles
     * @param parentDir
     * @throws Exception 
     */
    private static void addSourceFiles(DAMSClient damsClient, List<String> sourceFiles, File parentDir) throws Exception {
        for (File file : parentDir.listFiles()) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".json")) {
                if (!isIngested(damsClient, file.getName())) {
                    sourceFiles.add(file.getAbsolutePath());
                }
            }
        }
    }

    /*
     * Check whether the JSON file is ingest or not
     * query dams:Note with matching for type: identifier, displayLabel: samplenumber, and fileName.
     * @param damsClient
     * @param fileName
     * @return
     * @throws Exception
     */
    private static boolean isIngested(DAMSClient damsClient, String fileName) throws Exception {
        String sampleNumber = fileName.substring(0, fileName.indexOf("."));
        String sparql = "PREFIX rdf: <" + TabularRecordBasic.rdfNS.getURI() + ">\n"
                + " PREFIX dams: <" + TabularRecordBasic.damsNS.getURI() + ">\n"
                + " SELECT ?sub WHERE {?sub dams:note ?o . ?o rdf:type ?_clazz . ?_clazz rdf:label '\"dams:Note\"'"
                + " . ?o dams:type '\"identifier\"' . ?o dams:displayLabel '\"samplenumber\"'"
                + " . ?o rdf:value '\"" + sampleNumber + "\"' }";
        List<Map<String, String>> results = damsClient.sparqlLookup(sparql);
        return results.size() > 0;
    }
}
