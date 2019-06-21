package edu.ucsd.library.xdre.utils;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.apache.commons.lang3.StringUtils;

/**
 * Interface to create watermarked image with convert command through ImageMagick.
 * @author lsitu@ucsd.edu
**/
public class ImageWatermarking extends Watermarking
{
    public ImageWatermarking()
    {
        this( "convert" );
    }

    /**
     * Constructor for ImageWatermarking object.
     * @param command Full path to the ImageMagick command
    **/
    public ImageWatermarking( String command )
    {
        super(command);
    }

    /**
     * Generate watermarked image with default image watermark
     * @param src
     * @param dst
     * @return
     * @throws Exception
     */
    public boolean createWatermarkedDerivative( String src, String dst ) throws Exception
    {
        return createWatermarkedDerivative(src, dst, null);
    }

    /**
     * Create watermarked derivative
     * @param oid
     * @param cid
     * @param sfid source file id
     * @param dfid destination file id of the watermarking derivative
     * @return the watermarked file created
     * @throws Exception
     */
    public File createWatermarkedDerivative( String oid, String cid, String sfid, String dfid ) throws Exception
    {
        File srcFile = localArkFile(oid, cid, sfid);
        File dstFile = watermarkFile(oid, cid, dfid);
        createWatermarkedDerivative( srcFile.getAbsolutePath(), dstFile.getAbsolutePath(), Constants.WATERMARK_IMAGE );
        return dstFile;
    }

    /**
     * Generate watermarked image with scaling and positioning
     * basing on the width and height of the source image.
     * @param src
     * @param dst
     * @param imageWatermark
     * @return
     * @throws Exception
     */
    public boolean createWatermarkedDerivative( String src, String dst, String watermark )
            throws Exception
    {
        // retrieve the default image watermark from source code when no image watermark provided
        if (StringUtils.isBlank(watermark)) {
            watermark = defaultImageWatermark();
        }

        // build the command
        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add( command );
        cmd.add( src );            // source file

        // image watermark processing options
        cmd.add( "(" );

        cmd.add( watermark ); // image watermark
        cmd.add( "-background" );  // background none
        cmd.add( "none" );

        // calculate the source and watermark images for scaling
        BufferedImage img = ImageIO.read(new File(src));
        int iw = img.getWidth();
        int ih = img.getHeight();
        BufferedImage imgWatermark = ImageIO.read(new File(watermark));
        int iwWatermark = imgWatermark.getWidth();

        String gravity = "South";
        if (iw < ih) {
            gravity = "West";

            if (iwWatermark > ih) {
                cmd.add( "-geometry" ); // scale the image watermark
                cmd.add( ih*100/iwWatermark + "%" );
            }

            cmd.add( "-rotate" );   // rotate -90 degree when image width < height
            cmd.add( "90" );
        } else if (iwWatermark > iw) {
            cmd.add( "-geometry" ); // scale the image watermark
            cmd.add( iw*100/iwWatermark + "%" );
        }

        cmd.add( ")" );

        cmd.add( "-gravity" );      // image watermark position
        cmd.add( gravity );
        cmd.add( "-compose" );      // compose option
        cmd.add( "over" );
        cmd.add( "-composite" );    // composite option for watermarking

        cmd.add( dst );             // destination file

        return execute(cmd);
    }

    /*
     * Retrieve the default image watermark provided by source code.
     * @return
     * @throws IOException
     */
    private String defaultImageWatermark() throws IOException {
        String watermarkFileName = "watermark.png";
        File watermarkFile = new File(Constants.DAMS_STAGING + WATERMARK_LOCATION, watermarkFileName);
        if (!watermarkFile.exists() || watermarkFile.length() > 0) {
            byte[] buf = new byte[4096];
            try(InputStream in = getClass().getResourceAsStream("/resources/" + watermarkFileName);
                    FileOutputStream out = new FileOutputStream(watermarkFile)) {

                int bytesRead = 0;
                while ((bytesRead = in.read(buf)) > 0) {
                    out.write(buf, 0, bytesRead);
                }
            }
        }
        return watermarkFile.getAbsolutePath();
    }
}
