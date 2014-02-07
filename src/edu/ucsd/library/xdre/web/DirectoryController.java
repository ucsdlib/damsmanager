package edu.ucsd.library.xdre.web;

import java.io.File;
import java.io.OutputStream;
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
		boolean subList = request.getParameter("subList") !=null;   // When parameter subList provided, listing the child directories only and return it in JSON data format.
		boolean listOnly = request.getParameter("listOnly") !=null; // When parameter listOnly provided, won't recursively list the whole directories but children and parent.
		String damsStaging = Constants.DAMS_STAGING;
		
		File saFile = null;
		String[] saFiles = null;
		// With wildcard filtering
		if(dirFilter != null && (dirFilter=dirFilter.trim()).length() > 0){
			damsStaging += (dirFilter.startsWith("/")?"":"/") + dirFilter;
			saFile = new File(damsStaging);
			final String dirName = saFile.getName();
			if(subList){
				saFiles = saFile.list();
			}else{
				saFile = saFile.getParentFile();
				String[] lFiles = saFile.list();
				int len = lFiles.length;
				List<String> filteredFiles = new ArrayList<String>();
				for(int i=0; i<len; i++){
					if(FilenameUtils.wildcardMatchOnSystem(lFiles[i].toLowerCase(), dirName))
						filteredFiles.add(lFiles[i]);
				}
				saFiles = filteredFiles.toArray(new String[filteredFiles.size()]);
			}
			
			
		}else{
			saFile = new File(damsStaging);
			saFiles = saFile.list();
		}
		
		File tmpFile = null;
		JSONObject saObj = new JSONObject();
		JSONArray dirsArr = new JSONArray();
		String rootMessage = "[Staging Area]";
		
		if(!saFile.equals(new File(Constants.DAMS_STAGING))){
			rootMessage = saFile.getName();
		}
		
		saObj.put(rootMessage, dirsArr);
		
		if(!subList){
			File sDir = new File(Constants.DAMS_STAGING);
			File pFile = saFile;
			if(pFile.compareTo(sDir) > 0){
				List<String> folders = new ArrayList<String>();
				do{
					folders.add(0, pFile.getName());
					pFile = pFile.getParentFile();
				}while(pFile.compareTo(sDir) > 0);
				
				String curFolder = null;
				for(int i=0; i<folders.size(); i++){
					curFolder = folders.get(i);
					tmpFile = new File(pFile.getPath() + File.separatorChar + curFolder);
					appendFolder(dirsArr, tmpFile, false);
					dirsArr = (JSONArray) ((JSONObject)dirsArr.get(0)).get(curFolder);
					pFile = tmpFile;
				}
			}
		}
		
		if(saFiles != null){
			Arrays.sort(saFiles);
			for(int i = 0; i<saFiles.length; i++){
				tmpFile = new File(saFile.getPath() + File.separatorChar + saFiles[i]);
				if(tmpFile.isDirectory()){
					appendFolder(dirsArr, tmpFile, !listOnly);
				}
			}
		}
		if(subList){
			response.setContentType("text/plain");
			OutputStream out = response.getOutputStream();
			out.write(saObj.get(rootMessage).toString().getBytes("UTF-8"));
			out.close();
			return null;
		}else{
			//System.out.println("Directory Tree: " + saObj.toString());
			Map dataMap = new HashMap();
			dataMap.put("stagingArea", Constants.DAMS_STAGING);
			dataMap.put("dirPaths", saObj.toString());
			return new ModelAndView("directory", "model", dataMap);
		}
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