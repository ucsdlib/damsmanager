package edu.ucsd.library.xdre.utils;

import java.io.File;
import java.io.IOException;
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
        createWatermarkedDerivative( srcFile.getAbsolutePath(), dstFile.getAbsolutePath() );
        return dstFile;
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

    /**
     * Construct the local file in filestore
     * @param oid
     * @param cid
     * @param fid
     * @return
     */
    protected File localArkFile(String oid, String cid, String fid) {
        // Local file support only
        String ark = DAMSClient.stripID(oid);
        String fsDir = Constants.FILESTORE_DIR+ "/" + DAMSClient.pairPath(ark);
        String fName = Constants.ARK_ORG + "-" + ark + "-" + (cid==null||cid.length()==0?"0-":cid+"-") + fid;
        return new File(fsDir, fName);
    }

    /**
     * Create temporary file for watermarking
     * @param oid
     * @param cid
     * @param fid
     * @return
     * @throws IOException 
     */
    protected File watermarkFile(String oid, String cid, String fid) throws IOException {
        String ark = DAMSClient.stripID(oid);
        String fName = Constants.ARK_ORG + "-" + ark + "-" + (cid==null||cid.length()==0?"0-":cid+"-") + fid;
        File tmpDir = new File(Constants.DAMS_STAGING + WATERMARK_LOCATION);

        File watermarkedFile = File.createTempFile("watermarked", "-" + fName, tmpDir);
        if (watermarkedFile.exists()) {
            // delete the watermared file if it exists to avoid complains.
            watermarkedFile.delete();
        }

        watermarkedFile.deleteOnExit();
        return watermarkedFile;
    }
}
