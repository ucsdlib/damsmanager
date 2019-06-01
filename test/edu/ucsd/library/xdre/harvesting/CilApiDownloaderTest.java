package edu.ucsd.library.xdre.harvesting;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.web.CILHarvestingTaskController;

/**
 * Test methods for CilApiDownloader class
 * @author lsitu
 *
 */
public class CilApiDownloaderTest {
    private File metadataSourceDir = null;
    private CilApiClient cilApiClient = null;
    private CilApiDownloader cilApiDownloader = null;

    @Before
    public void init() throws Exception {
        Constants.CIL_HARVEST_API = "https://cilia.crbs.ucsd.edu/rest/";
        Constants.CIL_CONTENT_URL_BASE = "https://cildata.crbs.ucsd.edu/";
        Constants.CIL_HARVEST_API_USER = "ucsd_lib";
        Constants.CIL_HARVEST_API_PWD = "xxxxx";
        Constants.CIL_HARVEST_DIR = new File("").getAbsolutePath() + File.separatorChar + "rdcp_staging";

        cilApiClient = new CilApiClient(Constants.CIL_HARVEST_API);
        String cilDataFolder = CILHarvestingTaskController.CIL_HARVEST_PATH_PREFIX
                + new SimpleDateFormat("yyyy-MM-dd").format(Calendar.getInstance().getTime());
        File jsonSourceDir = new File(Constants.CIL_HARVEST_DIR + File.separatorChar + cilDataFolder,
                CILHarvestingTaskController.CIL_HARVEST_METADATA_SOURCE);
        if (!jsonSourceDir.exists())
            jsonSourceDir.mkdirs();

        metadataSourceDir = new File(Constants.CIL_HARVEST_DIR + File.separatorChar + cilDataFolder,
                CILHarvestingTaskController.CIL_HARVEST_METADATA_SOURCE);
        if (!metadataSourceDir.exists())
            metadataSourceDir.mkdirs();

    }

    @After
    public void done() throws IOException {
        if (cilApiClient != null) {
            cilApiClient.close();
        }

        if (Constants.CIL_HARVEST_DIR.endsWith(File.separatorChar + "rdcp_staging")) {
            FileUtils.deleteDirectory(new File(Constants.CIL_HARVEST_DIR));
        }
    }

    @Test
    public void testGetCilIds() throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2018);
        Date lastModified = cal.getTime();
        cilApiDownloader = new CilApiDownloader(cilApiClient, null, lastModified);
        assertTrue(cilApiDownloader.getCilIds().size() > 0);
    }
}
