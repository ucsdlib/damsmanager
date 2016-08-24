package edu.ucsd.library.xdre.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class to create audio/video derivatives with FFMPEG.
 * @author lsitu
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

	/**
	 * method to embed metadata
	 * @param oid
	 * @param cid
	 * @param mfid
	 * @param dfid
	 * @param params
	 * @param metadata
	 * @return
	 * @throws Exception
	 */
	public File metadataEmbed(String oid, String cid, String mfid, String dfid, String params,
			Map<String, String> metadata) throws Exception {
		return createDerivative(oid, cid, mfid, dfid, params, metadata);
	}

	/**
	 * Create derivative
	 * @param oid
	 * @param cid
	 * @param mfid
	 * @param dfid
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public File createDerivative(String oid, String cid, String mfid, String dfid, String params) throws Exception {
		return createDerivative(oid, cid, mfid, dfid, params);
	}

	/**
	 * Create derivative with embedded metadata
	 * @param oid
	 * @param cid
	 * @param mfid
	 * @param dfid
	 * @param params
	 * @param metadata
	 * @return
	 * @throws Exception
	 */
	public File createDerivative(String oid, String cid, String mfid, String dfid, String params,
			Map<String, String> metadata) throws Exception {
		File src = createArkFile(oid, cid, mfid);
		File dst =  createArkFile(oid, cid, dfid);

		if( !src.exists() ) {
			// XXX Implementation to retrieve master file to local disk???
			throw new Exception ("Master file " + src.getPath() + " doesn't exists.");
		}

		// Create the directory in dams staging to hold the temporary files created by ffmpeg for ingest
		File tmpDir = new File(Constants.DAMS_STAGING + "/darry/ffmpeg");
		if(!tmpDir.exists()){
			if(new File(Constants.DAMS_STAGING + "/darry").exists()) {
				tmpDir.mkdir();
			} else {
				tmpDir.mkdirs();
			}
		}

		dst = File.createTempFile("ffmpeg_tmp", oid+"-"+dst.getName(), tmpDir);
		boolean succssful = createDerivative( src, dst, params, metadata );
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

	/**
	 * Create derivative
	 * @param src
	 * @param dst
	 * @param params
	 * @return
	 * @throws Exception
	 */
	public boolean createDerivative( File src, File dst, String params ) throws Exception{
		return createDerivative( src, dst, params );
	}

	/**
	 * Create derivative
	 * @param src
	 * @param dst
	 * @param params
	 * @return metadata
	 * @return
	 * @throws Exception
	 */
	public boolean createDerivative( File src, File dst, String params,
			Map<String, String> metadata ) throws Exception{
		// Build the ffmpeg command to create derivative
		List<String> cmd = new ArrayList<>();
		cmd.add( command );
		cmd.add( "-i" );
		cmd.add( src.getAbsolutePath());
		if (StringUtils.isNotBlank(params))
			cmd.addAll(Arrays.asList(params.split(" ")));

		if (metadata != null) {
			for (String fieldName : metadata.keySet()) {
				cmd.add("-metadata");
				cmd.add(fieldName + "=" + metadata.get(fieldName));
			}
		}
			
		cmd.add( dst.getAbsolutePath() );

		return exec(cmd);
	}

	private boolean exec(List<String> cmd) throws Exception {
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

	private File createArkFile(String oid, String cid, String fid) {
		// Local file support only
		String fsDir = Constants.FILESTORE_DIR+ "/" + DAMSClient.pairPath(DAMSClient.stripID(oid));
		String fName = Constants.ARK_ORG + "-" + oid + "-" + (cid==null||cid.length()==0?"0-":cid+"-") + fid;
		return new File(fsDir, fName);
	}

	private List<String> getStreams (String filename) {
		List<String> streams = new ArrayList<>();

		Process proc = null;
		try
		{
			// Build the command to extract video/audio tracks
			String[] params = {command, "-i", filename};
			ProcessBuilder pb = new ProcessBuilder(Arrays.asList(params));
			proc = pb.start();

			BufferedReader input = new BufferedReader(
				new InputStreamReader( proc.getInputStream() )
			);
			BufferedReader error = new BufferedReader(
				new InputStreamReader( proc.getErrorStream() )
			);

			// Skip the input stream
			String line = input.readLine();
			while( line != null )
			{
				line = input.readLine();
			}

			// Parse the metadata in error stream:
			// Stream #0:2(eng): Audio: pcm_s16le (sowt / 0x74776F73), 48000 Hz, mono, s16, 768 kb/s (default)
			while( (line=error.readLine()) != null )
			{
				if ( line.indexOf( "Stream #") != -1
					&& (line.indexOf("Video:") != -1 || line.indexOf("Audio:") != -1))
				{
					String trackId = line.substring(0, line.indexOf("(")).replace("Stream #", "").trim();
					streams.add( trackId );
				}
			}
			proc.waitFor();
		}
		catch( Exception e )
		{
			e.printStackTrace();
		}
		finally
		{
			if (proc != null)
			{
				proc.destroy();
			}
		}
		return streams;
	}
}
