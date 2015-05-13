package edu.ucsd.library.xdre.tab;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import edu.ucsd.library.xdre.collection.CollectionHandler;
import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * FilesCheck for Files Match and Files Validation
 * 
 * @author lsitu
 * @since 2015-05-04
**/
public class FilesChecker {

	private Map<String, File> sourceFiles = null;
	private List<String> fileNames = null;
	private List<String> matchedFiles = null;
	private List<String> missingFiles = null;
	private Map<String, File> extraFiles = null;
	private Map<File, String> invalidFiles = null;
	private String command = "exiftool";
	private static final String ERROR = "Error";
	private static final String WARNING = "Warning";

	/**
	 * Constructor with list of source file paths
	 * @param fileNames
	 * @param filesPaths
	 * @throws Exception
	 */
	public FilesChecker (List<String> fileNames, String[] filesPaths) throws Exception
	{
		this.fileNames = fileNames;
		sourceFiles = new TreeMap<String, File>();
		if(filesPaths != null){
			File file = null;
			// List the source files
			for(int i=0; i<filesPaths.length; i++){
				file = new File(filesPaths[i]);
				if(file.exists()){
					CollectionHandler.listFile(sourceFiles, file);
				}
			}
		}
		init();
	}

	/**
	 * Constructor with collection of source files
	 * @param fileNames
	 * @param sourceFiles
	 */
	public FilesChecker (List<String> fileNames, Map<String, File> sourceFiles)
	{
		this.fileNames = fileNames;
		this.sourceFiles = sourceFiles;
		init();
	}

	private void init(){
		matchedFiles = new ArrayList<String> ();
		missingFiles = new ArrayList<String> ();
		extraFiles = new TreeMap<String, File> ();
		invalidFiles = new TreeMap<File, String> ();
	}
	
	public List<String> getMatchedFiles() {
		return matchedFiles;
	}

	public List<String> getMissingFiles() {
		return missingFiles;
	}

	public Map<String, File> getExtraFiles() {
		return extraFiles;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public Map<File, String> getInvalidFiles() {
		return invalidFiles;
	}

	/**
	 * Check for files matching
	 * @return
	 */
	public boolean filesMatch ()
	{
		extraFiles.putAll(sourceFiles);
		for (String fileName : fileNames) {
			if (StringUtils.isNotBlank(fileName) && sourceFiles.containsKey(fileName)) {
				matchedFiles.add(fileName);
				extraFiles.remove(fileName);
			}
			else
			{
				missingFiles.add(fileName);
			}
		}
		return missingFiles.size() <= 0;
	}

	/**
	 * Validate files that are matched the metadata
	 * @return
	 */
	public boolean filesValidate ()
	{
		//validate the match files
		if (matchedFiles.size() == 0)
			filesMatch ();
		for (String fileName : matchedFiles) {
			File src = sourceFiles.get(fileName);
			Map<String, String> m = extractMetadata(src);
			if (m.containsKey(ERROR) || m.containsKey(WARNING)) {
				String message = "";
				if (m.containsKey(ERROR))
					message = ERROR + ": " + m.get(ERROR);
				if (m.containsKey(WARNING))
					message += (message.length() > 0 ? " || " : "") + WARNING + ": " + m.get(WARNING);

				invalidFiles.put(src, message);
			}
		}
		return invalidFiles.size() <= 0;
	}

	private Map<String, String> extractMetadata(File src) 
	{
		Reader reader = null;
		InputStream in = null;
		BufferedReader buf = null;
		StringBuffer log = null;
		Process proc = null;
		Map<String, String> metadata = new HashMap<String, String>();
		try 
		{
			// Build the command for file validation
			List<String> cmd = new ArrayList<>();
			cmd.add( command );
			cmd.add( src.getAbsolutePath());
			
			log = new StringBuffer();
			// Execute the command
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);
			proc = pb.start();
			
			in = proc.getInputStream();
			reader = new InputStreamReader(in);
			buf = new BufferedReader(reader);
			for ( String line = null; (line=buf.readLine()) != null; ) 
			{
				String[] pair = line.split("\\:");
				String v = "";
				if (pair.length == 2)
					v = StringUtils.isBlank(pair[1]) ? "" : pair[1].trim();

				addMetadata(metadata, pair[0].trim(), v);
			}

			// Wait for the process to exit
			int status = proc.waitFor();
			if ( status != 0 ) 
			{
				// Output error messages
				in = proc.getErrorStream();
				reader = new InputStreamReader(in);
				buf = new BufferedReader(reader);
				for ( String line = null; (line=buf.readLine()) != null; ) 
					log.append( line + "\n" );

				addMetadata(metadata, ERROR, log.toString());
			}
		} 
		catch ( Exception ex ) 
		{
			ex.printStackTrace();
			addMetadata(metadata, ERROR, ex.getMessage());
		} 
		finally 
		{
			DAMSClient.close(in);
			DAMSClient.close(reader);
			DAMSClient.close(buf);
			if(proc != null)
			{
				proc.destroy();
				proc = null;
			}
		}
		return metadata;
	}

	private void addMetadata(Map<String, String> metadata, String name, String value)
	{
		String v = metadata.get(name);
		// concatenate the values with pine
		if (StringUtils.isNotBlank(v) && StringUtils.isNotBlank(value))
			v += " | " + value;
		else if (StringUtils.isNotBlank(value))
			v = value;

		metadata.put(name, v);
	}
}
