package edu.ucsd.library.xdre.utils;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

/**
 * Test methods for Watermarking class.
 * Need to install python script https://github.com/ucsdlib/watermark.
 * @author lsitu
 *
 */
public class WatermarkingTest extends UnitTestBasic {

    @Before
    public void init() {
        Constants.DAMS_STAGING = "dams_staging";
        Constants.WATERMARK_COMMAND = "/usr/local/bin/watermark.sh";
    }

    @Test
    public void testCreateWatermarkImage() throws Exception {
        File src = getResourceFile("test1.pdf");
        File dst = new File(Constants.DAMS_STAGING + ImageMagick.WATERMARK_LOCATION,
                "watermarkedPdf.pdf");
        dst.deleteOnExit();

        Watermarking command = new Watermarking();
        boolean created = command.createWatermarkedDerivative(src.getAbsolutePath(), dst.getAbsolutePath());
        assertTrue(created);
        assertTrue("Watermarked PDF doesn't exist!", dst.exists() && dst.length() > 0);
    }
}
