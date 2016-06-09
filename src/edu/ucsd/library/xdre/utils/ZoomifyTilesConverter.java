package edu.ucsd.library.xdre.utils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

/**
 * Utility class to create zoomify tiles.
 * @author lsitu
 **/
public class ZoomifyTilesConverter
{

	private String command = null;       // commandline zoomify script to run
	private String repositoryUrl = null; // repository url to download source files
	private String fileStoreDir = null;  // filestore to use

	/**
	 * Construct a default ZoomifyTilesConverter object with a command.
	 * @param command Full path to the locally-installed zoomify script
	**/
	public ZoomifyTilesConverter( String command ) {
		this.command = command;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getRepositoryUrl() {
		return repositoryUrl;
	}

	public void setRepositoryUrl(String repositoryUrl) {
		this.repositoryUrl = repositoryUrl;
	}

	public String getFileStoreDir() {
		return fileStoreDir;
	}

	public void setFileStoreDir(String fileStoreDir) {
		this.fileStoreDir = fileStoreDir;
	}

	/**
	 * Create zoomify tiles
	 * @param oid
	 * @param cid
	 * @param fid
	 * @throws Exception
	 */
	public boolean createZoomifyTiles(String oid, String cid, String fid) throws Exception {
		String fileUrl = oid + "/" + (StringUtils.isNotBlank(cid) ? cid + "/" : "") + fid;
		return createZoomifyTiles( fileUrl );
	}

	/**
	 * Create zoomify tiles with a file uri
	 * @param fileUri
	 * @return
	 * @throws Exception
	 */
	public boolean createZoomifyTiles( String fileUrl) throws Exception{
		// Build the zoomify command to create zoomify tiles
		List<String> cmd = new ArrayList<>();
		cmd.add( command );
		cmd.add( fileUrl );

		// set repository URL
		if (StringUtils.isNotBlank(repositoryUrl))
			cmd.add(repositoryUrl);
		else
			cmd.add(Constants.DAMS_STORAGE_URL);

		// set local filestore 
		if (StringUtils.isNotBlank(fileStoreDir))
			cmd.add(fileStoreDir);
		else
			cmd.add(Constants.FILESTORE_DIR);

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
			for ( String line = null; (line=buf.readLine()) != null; ){
				log.append( line + "\n" );
			}

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
