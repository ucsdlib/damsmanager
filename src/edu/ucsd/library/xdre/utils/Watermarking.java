package edu.ucsd.library.xdre.utils;

import java.io.File;
import java.util.ArrayList;

/**
 * Interface to create watermarked PDF derivatives.
 * Need to install the program on Git for watermarking PDF:
 * https://github.com/ucsdlib/watermark
 * @author lsitu@ucsd.edu
**/
public class Watermarking extends ProcessBasic
{
    // Location to store temporary files created for watermarking
    protected String WATERMARK_LOCATION = "/darry/watermark";

    /**
     * Constructor for Watermarking with the the locally-installed program.
     * @param command
     *  /usr/local/bin/watermark.sh /tmp/test.pdf /tmp/test1a.pdf
    **/
    public Watermarking()
    {
        this( Constants.WATERMARK_COMMAND );
    }

    /**
     * Create an PdfWatermarking object.
     * @param command Full path to the locally-installed program for PDF watermarking
     *  /usr/local/bin/watermark.sh /tmp/test.pdf /tmp/test1a.pdf
    **/
    public Watermarking( String command )
    {
        super(command);
        init();
    }

    /**
     * Generate watermarked derivative
    * @param src
     * @param dst
     * @return
     * @throws Exception
     */
    public boolean createWatermarkedDerivative( String src, String dst ) throws Exception
    {
        // build the command
        ArrayList<String> cmd = new ArrayList<String>();
        cmd.add( command );
        cmd.add( src );            // source file

        cmd.add( dst );            // destination file

        return execute(cmd);
    }

    /**
     * Initialize the location to store temp files created for watermarking
     */
    protected void init() {
        // Create the directory in dams staging to hold the temporary files for ingest
        File tmpDir = new File(Constants.DAMS_STAGING + WATERMARK_LOCATION);
        if(!tmpDir.exists()){
            tmpDir.mkdirs();
        }
    }
}
