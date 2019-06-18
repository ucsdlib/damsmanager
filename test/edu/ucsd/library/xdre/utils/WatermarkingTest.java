package edu.ucsd.library.xdre.utils;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.junit.Before;
import org.junit.Test;

/**
 * Test methods for Watermarking class.
 * Need to install python script https://github.com/ucsdlib/watermark.
 * @author lsitu
 *
 */
public class WatermarkingTest {

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

    private File getResourceFile(String fileName) throws IOException {
        File resourceFile = new File(fileName);
        resourceFile.deleteOnExit();

        byte[] buf = new byte[4096];
        try(InputStream in = getClass().getResourceAsStream("/resources/" + fileName);
                FileOutputStream out = new FileOutputStream(resourceFile)) {

            int bytesRead = 0;
            while ((bytesRead = in.read(buf)) > 0) {
                out.write(buf, 0, bytesRead);
            }
        }
        return resourceFile;
    }
}
