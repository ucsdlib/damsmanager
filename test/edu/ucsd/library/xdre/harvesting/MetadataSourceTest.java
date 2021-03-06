package edu.ucsd.library.xdre.harvesting;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
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
public class MetadataSourceTest {
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
    public void testDownload() throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.YEAR, 2018);
        Date lastModified = cal.getTime();

        cilApiDownloader = new CilApiDownloader(cilApiClient, null, lastModified);
        assertTrue(cilApiDownloader.getCilIds().size() > 0);

        String cilId = cilApiDownloader.getCilIds().get(0);
        MetadataSource source = new MetadataSource(cilId, cilApiClient);
        String file = source.download(metadataSourceDir.getParent());

        File sourceFile = new File(file);
        assertTrue(sourceFile.exists() && sourceFile.length() > 0);

        File contentsDir = new File(metadataSourceDir.getParent(), ContentFile.CONTENT_FILE_FOLDER);
        assertTrue(contentsDir.listFiles().length > 0);
    }

    @Test
    public void testRenamePatternMatching() {
        String filePath = "/ccdb/telescience/home/CCDB_DATA_USER.portal/CCDB_DATA_USER/acquisition/project_20099"
                + "/microscopy_5194144/reconstruction/3view-stack-final-bin5.mrc";
        assertTrue("Pattern 1 with 'microscopy_' isn't matched.", MetadataSource.renamePatternMatched(filePath));

        filePath = "/media/images/50581/3view-stack-final-bin10.mrc";
        assertTrue("Pattern 2 with 'images' isn't matched.", MetadataSource.renamePatternMatched(filePath));

        filePath = "/media/images/50581/50581.mrc";
        assertFalse("Pattern 3 with 'images' and object id matched!", MetadataSource.renamePatternMatched(filePath));
    }

    @Test
    public void testGetFileName() {
        String filePath = "/ccdb/telescience/home/CCDB_DATA_USER.portal/CCDB_DATA_USER/acquisition/project_20099"
                + "/microscopy_5194144/reconstruction/3view-stack-final-bin5.mrc";
        assertEquals("microscopy_5194144-3view-stack-final-bin5.mrc", MetadataSource.getFileName(filePath));

        filePath = "/media/images/50581/3view-stack-final-bin10.mrc";
        assertEquals("50581-3view-stack-final-bin10.mrc", MetadataSource.getFileName(filePath));

        filePath = "/media/images/50581/50581.mrc";
        assertEquals("50581.mrc", MetadataSource.getFileName(filePath));
    }
}
