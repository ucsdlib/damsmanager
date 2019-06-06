package edu.ucsd.library.xdre.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.ucsd.library.xdre.harvesting.CilHavestingTestBase;
import edu.ucsd.library.xdre.utils.Constants;

/**
 * Test methods for CILHarvestingTaskController class
 * @author lsitu
 *
 */
public class CILHarvestingTaskControllerTest extends CilHavestingTestBase {

    private String harvestDirectory = null;
    private File metadataProcessedDir = null;
    private List<File> jsonFiles = new ArrayList<>(); 

    @Before
    public void init() throws IOException {
        Constants.DAMS_STORAGE_URL = "http://localhost:8080/dams/api";
        Constants.CIL_HARVEST_DIR = new File("").getAbsolutePath() + File.separatorChar + "rdcp_staging";
        String cilDataFolder = CILHarvestingTaskController.CIL_HARVEST_PATH_PREFIX
                + new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
        harvestDirectory = new File(Constants.CIL_HARVEST_DIR, cilDataFolder).getAbsolutePath();
        File jsonSourceDir = new File(harvestDirectory,
                CILHarvestingTaskController.CIL_HARVEST_METADATA_SOURCE);
        if (!jsonSourceDir.exists())
            jsonSourceDir.mkdirs();

        metadataProcessedDir = new File(Constants.CIL_HARVEST_DIR + File.separatorChar + cilDataFolder,
                CILHarvestingTaskController.CIL_HARVEST_METADATA_PROCESSED);
        if (!metadataProcessedDir.exists())
            metadataProcessedDir.mkdirs();

        jsonFiles.add(createJsonTestFile(new File(jsonSourceDir, "test123a.json").getAbsolutePath()));
        jsonFiles.add(createJsonDataFile(new File(jsonSourceDir, "test123b.json").getAbsolutePath()));
    }

    @After
    public void done() throws IOException {
        if (Constants.CIL_HARVEST_DIR.endsWith(File.separatorChar + "rdcp_staging")) {
            FileUtils.deleteDirectory(new File(Constants.CIL_HARVEST_DIR));
        }
    }

    @Test
    public void testPerformHarvestingTask() throws Exception {
        List<String> files = CILHarvestingTaskController.performHarvestingTask(harvestDirectory);
        assertEquals(2, files.size());
        assertTrue(files.contains(jsonFiles.get(0).getAbsolutePath()));
        assertTrue(files.contains(jsonFiles.get(1).getAbsolutePath()));
 
        File csvMetadataFile = new File(metadataProcessedDir,  CILHarvestingTaskController.EXCEL_HEADINGS_CSV_FILE);
        assertTrue(csvMetadataFile.exists() && csvMetadataFile.length() > 0);

        String csvValue = getFileContent(csvMetadataFile);
        assertTrue(csvValue.contains("Unique ID,Level,"));
        assertTrue(csvValue.contains("test123a,Object,"));
        assertTrue(csvValue.contains(",Gustafsdottir et al. (doi:10.1371/journal.pone.0080999),"));
        assertTrue(csvValue.contains("test123a,Component,test123a.json,data-service"));

        assertTrue(csvValue.contains("test123b,Object,,"));
        assertTrue(csvValue.contains("test123b,Component,test123b.json,data-service"));
    }
}
