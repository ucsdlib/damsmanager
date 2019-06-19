package edu.ucsd.library.xdre.utils;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

/**
 * Test methods for ImageWatermarking class
 * @author lsitu
 *
 */
public class ImageWatermarkingTest extends UnitTestBasic {
    @Before
    public void init() {
        Constants.DAMS_STAGING = "dams_staging";
        Constants.IMAGEMAGICK_COMMAND = "/usr/local/bin/convert";
    }

    @Test
    public void testCreateWatermarkImage() throws Exception {
        File src = getResourceFile("image.jpg");
        String dst = new File(Constants.DAMS_STAGING + ImageMagick.WATERMARK_LOCATION,
                "image_watermarked.jpg").getAbsolutePath();

        ImageWatermarking command = new ImageWatermarking(Constants.IMAGEMAGICK_COMMAND);
        boolean created = command.createWatermarkedDerivative(src.getAbsolutePath(), dst);
        assertTrue(created);
        File dstFile = new File(dst);
        assertTrue("Watermarked image doesn't exist!", dstFile.exists() && dstFile.length() > 0);

        File watermark = getResourceFile("watermark.png");
        command = new ImageWatermarking(Constants.IMAGEMAGICK_COMMAND);
        created = command.createWatermarkedDerivative(src.getAbsolutePath(), dst, watermark.getAbsolutePath());
        assertTrue(created);
        dstFile = new File(dst);
        assertTrue("Watermarked image doesn't exist!", dstFile.exists() && dstFile.length() > 0);
    }
}
