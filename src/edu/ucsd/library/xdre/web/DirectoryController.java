package edu.ucsd.library.xdre.web;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
		File saFile = new File(Constants.DAMS_STAGING);
		String[] saFiles = saFile.list();
		Arrays.sort(saFiles);
		File tmpFile = null;
		String folder = null;
		JSONObject saObj = new JSONObject();
		JSONArray dirsArr = new JSONArray();
		saObj.put("[Staging Area]", dirsArr);
		for(int i = 0; i<saFiles.length; i++){
			tmpFile = new File(saFile.getPath() + File.separatorChar + saFiles[i]);
			if(tmpFile.isDirectory()){
				appendFolder(dirsArr, tmpFile);
			}
		}
		//System.out.println("Directory Tree: " + saObj.toString());
		Map dataMap = new HashMap();
		dataMap.put("stagingArea", Constants.DAMS_STAGING);
		dataMap.put("dirPaths", saObj.toString());
		return new ModelAndView("directory", "model", dataMap);
	}
	
	private void appendFolder(JSONArray parent, File file){
		String folderName = getFolderName(file);
		if(hasDirectory(file)){
			JSONObject tmpObj = new JSONObject();
			JSONArray tmpArr = new JSONArray();
			tmpObj.put(folderName, tmpArr);
			parent.add(tmpObj);
			String[] files = file.list();
			Arrays.sort(files);
			File tmpFile = null;
			for(int i=0; i<files.length; i++){
				tmpFile = new File(file.getPath() + File.separatorChar + files[i]);				
				if(tmpFile.isDirectory())
					appendFolder(tmpArr, tmpFile);
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