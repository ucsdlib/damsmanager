package edu.ucsd.library.xdre.utils;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.List;

/**
 * 
 * FileUtils: Utility class to filter files.
 * @author lsitu@ucsd.edu
 */
public class FileUtils {
	/**
	 * Filter files with Extensions fileExtensions
	 * @param files
	 * @param fileExtensions
	 * @return
	 */
	public static List<File> filterFiles(File[] files, String[] fileExtensions) {
		final List<File> resultFiles = new ArrayList<File>();
		TargetFileFilter filter = new TargetFileFilter(fileExtensions);
		for (File file : files) {
			if (filter.accept(file)) {
				resultFiles.add(file);
			}
		}
		return resultFiles;
	}
	
	/**
	 * Filter files with Extensions fileExtensions
	 * @param files
	 * @param fileExtensions
	 * @return
	 */
	public static List<File> filterFiles(List<File> files, String[] fileExtensions) {
		return filterFiles(files.toArray(new File[files.size()]), fileExtensions);
	}
	
	/**
	 * TargetFileFilter class
	 */
	static class TargetFileFilter implements FileFilter {
	  private String[] fileExtensions = null;
	 
	  public TargetFileFilter (String[]fileExtensions){
		  this.fileExtensions = fileExtensions;
	  }
	  public boolean accept(File file)
	  {
	    for (String extension : fileExtensions)
	    {
	      if (file.getName().toLowerCase().endsWith(extension))
	      {
	        return true;
	      }
	    }
	    return false;
	  }
	}
}
