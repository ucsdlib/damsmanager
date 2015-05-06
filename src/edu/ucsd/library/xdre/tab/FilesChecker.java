package edu.ucsd.library.xdre.tab;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.lang3.StringUtils;

import edu.ucsd.library.xdre.collection.CollectionHandler;

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
}
