package edu.ucsd.library.xdre.web;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.harvesting.CilHarvesting;
import edu.ucsd.library.xdre.harvesting.FieldMappings;
import edu.ucsd.library.xdre.utils.Constants;

/**
 * Class CILHarvestingController perform task for CIL harvesting
 * @author lsitu
 *
 */
public class CILHarvestingTaskController implements Controller {
    private static Logger log = Logger.getLogger(CILHarvestingTaskController.class);

    public static String DAMS42JSON_XSL_FILE = "/resources/dams42json.xsl";

    // CIL harvest metadata processed folder
    public static String CIL_HARVEST_METADATA_PROCESSED = "metadata_processed";
    // CIL harvest source JSON folder
    public static String CIL_HARVEST_METADATA_SOURCE = "metadata_source";
    public static String CIL_HARVEST_PATH_PREFIX = "cil_harvest_";
    public static String CIL_HARVEST_MAPPING_FILE = "/resources/CIL Processing and Mapping Instructions.xlsx";
    public static String EXCEL_HEADINGS_CSV_FILE = "cil_excel_headings.csv";

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        OutputStream out = null;
        try {
            out = response.getOutputStream();

            List<String> sourceJsonsAdded = performHarvestingTask();

            if (sourceJsonsAdded.size() > 0) {
                out.write(("Successfully converted " + sourceJsonsAdded.size() + " JSON source files.").getBytes("UTF-8"));
            } else {
                out.write(("No JSON source files are processed.").getBytes("UTF-8"));
            }
        }catch (Exception e){
            e.printStackTrace();
        }finally {
            if(out != null)
                out.close();
        }
        return null;
    }

    /**
     * Function to lookup json files added and perform processing.
     * @return
     * @throws Exception
     */
    public synchronized static List<String> performHarvestingTask() throws Exception{
        List<String> sourceJsonsAdded = new  ArrayList<>();
        File[] cilDirs = new File(Constants.CIL_HARVEST_DIR).listFiles();
        for (File cilDir : cilDirs) {
            if (cilDir.isDirectory() && cilDir.getName().startsWith(CIL_HARVEST_PATH_PREFIX)) {

                List<String> sourceJsons = new  ArrayList<>();
                File[] dirs = cilDir.listFiles();
                for (File dir : dirs) {
                    if (dir.getName().equalsIgnoreCase(CIL_HARVEST_METADATA_SOURCE)) {
                        File metadataProcessedDir = new File(dir.getParentFile(), CIL_HARVEST_METADATA_PROCESSED);
                        if (!metadataProcessedDir.exists()) {
                            // source json files haven't been processed
                            addSourceFiles(sourceJsons, dir);
                        } else {
                            // source json files processed, check for completion
                            File excelHeadingsFile = new File(metadataProcessedDir.getAbsolutePath(), EXCEL_HEADINGS_CSV_FILE);
                            if (!excelHeadingsFile.exists() || excelHeadingsFile.length() <= 0) {
                                // metadata processed not finished
                                addSourceFiles(sourceJsons, dir);
                            }
                        }
                    }
                }

                if (sourceJsons.size() > 0) {
                    log.info("Detected " + sourceJsons.size() + " CIL JSON source files  in directory " + cilDir.getAbsolutePath() + ".");

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

                    try(InputStream mappingsIn = mappingsInput; InputStream dams42jsonIn = dams42jsonInput;) {
                        FieldMappings fieldMapping = new FieldMappings(mappingsIn);
                        CilHarvesting cilHarvesting = new CilHarvesting(
                                fieldMapping.getFieldMappings(),
                                fieldMapping.getConstantFields(),
                                sourceJsonsAdded);

                        String csvString = cilHarvesting.toCSV(dams42jsonIn);
                        File metadataProcessedDir = new File(cilDir.getPath(), CIL_HARVEST_METADATA_PROCESSED);
                        if (!metadataProcessedDir.exists())
                            metadataProcessedDir.mkdirs();

                        File destMetadataFile = new File (metadataProcessedDir, EXCEL_HEADINGS_CSV_FILE);
                        writeContent(destMetadataFile.getAbsolutePath(), csvString);
                    }

                    log.info("Finished converting " + sourceJsonsAdded.size() + " CIL JSON source files.");
                } else {
                    log.info("No CIL JSON source files were detected to be added.");
                }
            }
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
     */
    private static void addSourceFiles(List<String> sourceFiles, File parentDir) {
        for (File file : parentDir.listFiles()) {
            if (file.isFile() && file.getName().toLowerCase().endsWith(".json")) {
                sourceFiles.add(file.getAbsolutePath());
            }
        }
    }
}
