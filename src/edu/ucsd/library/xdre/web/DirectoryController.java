package edu.ucsd.library.xdre.web;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FilenameUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.utils.Constants;


 /**
 * Class DirectoryController, the Model of the Staging Area directory picker for JETL
 *
 * @author lsitu@ucsd.edu
 */
public class DirectoryController implements Controller {

	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String dirFilter = request.getParameter("filter");
		String damsStaging = Constants.DAMS_STAGING;
		
		File saFile = null;
		String[] saFiles = null;
		// With wildcard filtering
		if(dirFilter != null && (dirFilter=dirFilter.trim()).length() > 0){
			damsStaging += "/" + dirFilter;
			saFile = new File(damsStaging);
			final String dirName = saFile.getName();
			saFile = saFile.getParentFile();
			FilenameFilter filter = new FilenameFilter(){
				public boolean accept(File dir, String name) {
					return FilenameUtils.wildcardMatchOnSystem(name.toLowerCase(), dirName);
				}
			};
			saFiles = saFile.list(filter);
		}else{
			saFile = new File(damsStaging);
			saFiles = saFile.list();
		}
		
		File tmpFile = null;
		JSONObject saObj = new JSONObject();
		JSONArray dirsArr = new JSONArray();
		String rootMessage = "[Staging Area]";
		if(saFiles == null || saFiles.length == 0)
			rootMessage = "[No results: " + saFile.getAbsolutePath() + "]";
		
		File sDir = new File(Constants.DAMS_STAGING);
		File pFile = saFile;
		if(pFile.compareTo(sDir) > 0){
			List<String> folders = new ArrayList<String>();
			do{
				folders.add(0, pFile.getName());
				pFile = pFile.getParentFile();
			}while(pFile.compareTo(sDir) > 0);
			for(int i=0; i<folders.size(); i++){
				tmpFile = new File(pFile.getPath() + File.separatorChar + folders.get(i));
				appendFolder(dirsArr, tmpFile, false);
				dirsArr = (JSONArray) ((JSONObject)dirsArr.get(0)).get(folders.get(i));
				pFile = tmpFile;
			}
		}
		
		saObj.put(rootMessage, dirsArr);
		if(saFiles != null){
			Arrays.sort(saFiles);
			for(int i = 0; i<saFiles.length; i++){
				tmpFile = new File(saFile.getPath() + File.separatorChar + saFiles[i]);
				if(tmpFile.isDirectory()){
					appendFolder(dirsArr, tmpFile, true);
				}
			}
		}
		//System.out.println("Directory Tree: " + saObj.toString());
		Map dataMap = new HashMap();
		dataMap.put("stagingArea", Constants.DAMS_STAGING);
		dataMap.put("dirPaths", saObj.toString());
		return new ModelAndView("directory", "model", dataMap);
	}
	
	private void appendFolder(JSONArray parent, File file, boolean listAll){
		String folderName = getFolderName(file);
		if(hasDirectory(file)){
			JSONObject tmpObj = new JSONObject();
			JSONArray tmpArr = new JSONArray();
			tmpObj.put(folderName, tmpArr);
			parent.add(tmpObj);
			if(listAll){
				String[] files = file.list();
				Arrays.sort(files);
				File tmpFile = null;
				for(int i=0; i<files.length; i++){
					tmpFile = new File(file.getPath() + File.separatorChar + files[i]);				
					if(tmpFile.isDirectory())
						appendFolder(tmpArr, tmpFile, listAll);
				}
			}
		}else
			parent.add(folderName);
	}
	
	public static boolean hasDirectory(File file){
		if(file.isDirectory()){
			File[] files = file.listFiles();
			for(int i=0; i<files.length; i++){
				if(files[i].isDirectory())
					return true;
			}
		}
		return false;
	}
	
	public static String getFolderName(File directory){
		return directory.getName();
	}
 }