package edu.ucsd.library.xdre.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;

/**
 * Utility class to create video derivative with FFMPEG.
 * @author lsitu@ucsd.edu
**/
public class FFMPEGConverter
{

	private String command = null;

	/**
	 * Construct a default FFMPEGConverter object with command ffmpeg.
	 * 
	**/
	public FFMPEGConverter() {
		this.command = "ffmpeg";
	}
	
	/**
	 * Constructor for FFMPEGConverter object.
	 * @param command Full path to the locally-installed ffmpeg
	**/
	public FFMPEGConverter( String command ) {
		this.command = command;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public File createDerivative(String oid, String cid, String mfid, String dfid, String frameSize) throws Exception {
		File dst =  null;
		//try {
		File src = null;
		// Local file support only
		String fsDir = Constants.FILESTORE_DIR+ "/" + DAMSClient.pairPath(DAMSClient.stripID(oid));
		String mfName = Constants.ARK_ORG + "-" + oid + "-" + (cid==null||cid.length()==0?"0-":cid+"-")+mfid;
		String dfName = Constants.ARK_ORG + "-" + oid + "-" + (cid==null||cid.length()==0?"0-":cid+"-")+dfid;
		src = new File(fsDir, mfName);
		
		if( !src.exists() ) {
			// XXX Implementation to retrieve master file to local disk???
			throw new Exception ("Master file " + src.getPath() + " doesn't exists.");
		}

		File tmpDir = new File(Constants.DAMS_STAGING + "/ffmpeg");
		if(!tmpDir.exists())
			tmpDir.mkdir();
		dst = File.createTempFile("ffmpeg_tmp", oid+"-"+dfName, tmpDir);
		boolean succssful = createVideo( src, dst, frameSize );
		if ( !succssful ) {
			if(dst != null && dst.exists()){
				// Cleanup temp files
				try {
					dst.delete();
				} catch ( Exception e ) {
					e.printStackTrace();
				}
				dst = null;
			}
		}
		return dst;
	}
	

	private boolean createVideo( File src, File dst, String frameSize) throws Exception{
		// Build the ffmpeg command to create mp4 derivative for 720p HD resolution
		ArrayList<String> cmd = new ArrayList<String>();
		cmd.add( command );
		cmd.add( "-i" );
		cmd.add( src.getAbsolutePath() );
		cmd.add( "-y" );		// delete silently
		cmd.add( "-vcodec" );	// h264 encoding
		cmd.add( "libx264" );
		cmd.add( "-pix_fmt" );	// chrome sampling
		cmd.add( "yuv420p" );
		cmd.add( "-profile:v" );// profile baseline to have highest compatibility with target players
		cmd.add( "baseline" );
		cmd.add( "-vf" );		// de-interlaced/progressive
		cmd.add( "yadif" );	
		cmd.add( "-aspect" );	// aspect ratio 16:9
		cmd.add( "16:9" );
		cmd.add( "-s" );		// resize to specified pixel dimensions
		cmd.add( frameSize );
		cmd.add( "-b:a" );		// high quality video
		cmd.add( "192k" );
		cmd.add( "-b:v" );		// bit rate 4000k
		cmd.add( "4000k" );
		cmd.add( "-minrate" );	// min bit rate 3000k
		cmd.add( "3000k" );
		cmd.add( "-maxrate" );	// max bit rate 5000k
		cmd.add( "5000k" );
		cmd.add( "-threads" );	// threads 2
		cmd.add( "2" );
		cmd.add( "-pass" );		// pass 1
		cmd.add( "1" );
		cmd.add( dst.getAbsolutePath() );

		Reader reader = null;
		InputStream in = null;
		BufferedReader buf = null;
		StringBuffer log = null;
		Process proc = null;
		try {
			log = new StringBuffer();
			// Execute the command
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);
			proc = pb.start();
			
			in = proc.getInputStream();
			reader = new InputStreamReader(in);
			buf = new BufferedReader(reader);
			for ( String line = null; (line=buf.readLine()) != null; )
				log.append( line + "\n" );

			DAMSClient.close(in);
			DAMSClient.close(reader);
			DAMSClient.close(buf);
			// Wait for the process to exit
			int status = proc.waitFor();
			if ( status == 0 )
				return true;
			else {
				// Output error messages
				in = proc.getErrorStream();
				reader = new InputStreamReader(in);
				buf = new BufferedReader(reader);
				for ( String line = null; (line=buf.readLine()) != null; )
					log.append( line + "\n" );
				throw new Exception( log.toString() );
			}
		} catch ( Exception ex ) {
			throw new Exception( log.toString(), ex );
		} finally {
			DAMSClient.close(in);
			DAMSClient.close(reader);
			DAMSClient.close(buf);
			if(proc != null){
				proc.destroy();
				proc = null;
			}
		}
	}
}
