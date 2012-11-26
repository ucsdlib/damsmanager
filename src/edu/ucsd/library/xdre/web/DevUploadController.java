package edu.ucsd.library.xdre.web;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;


 /**
 * Class DirectoryController, the Model of the Staging Area directory picker for JETL
 *
 * @author lsitu@ucsd.edu
 */
public class DevUploadController implements Controller {

	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String fs = request.getParameter("fileStore");
		File saFile = new File(Constants.DAMS_STAGING);
		String[] saFiles = saFile.list();
		File tmpFile = null;
		String fileName = null;
		JSONObject saObj = new JSONObject();
		JSONArray dirsArr = new JSONArray();
		saObj.put(Constants.DAMS_STAGING, dirsArr);
		Arrays.sort(saFiles);
		DAMSClient damsClient = null;
		for(int i = 0; i<saFiles.length; i++){
			tmpFile = new File(saFile.getPath() + File.separatorChar + saFiles[i]);
			if(tmpFile.isDirectory()){
				JSONObject parentObj = new JSONObject();
				JSONArray childsArr = new JSONArray();
				dirsArr.add(parentObj);
				parentObj.put(tmpFile.getName(), childsArr);
				appendFolder(childsArr, tmpFile);
			}else{
				fileName = tmpFile.getName();
				if(fileName.startsWith("20775-") && fileName.length() > 20 && (fileName.charAt(18) == '-' || fileName.endsWith("-mets.xml") || fileName.endsWith("-rdf.xml")))
					dirsArr.add(tmpFile.getName());
			}
		}
		
		damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
		//System.out.println("Directory Tree: " + saObj.toString());
		if(fs == null || (fs=fs.trim()).length() == 0)
			fs = Constants.DEFAULT_FILESTORE;
		List<String> fsSrcs = damsClient.listFileStores();

		String message = request.getParameter("message");
		boolean status = request.getParameter("status")!=null;
		Map dataMap = new HashMap();
		dataMap.put("status", status);
		dataMap.put("devFileStore", Constants.DAMS_STAGING);
		dataMap.put("dirPaths", saObj);
		dataMap.put("message", (message==null?"":message));
		dataMap.put("filestores", fsSrcs);
		dataMap.put("filestore", fs);
		dataMap.put("filestoreDefault", Constants.DEFAULT_FILESTORE);
		return new ModelAndView("devUpload", "model", dataMap);
	}
	
	private boolean appendFolder(JSONArray parent, File file){
		File[] files = file.listFiles();
		File tmpFile = null;
		String fileName = null;
		Arrays.sort(files);
		for(int i=0; i<files.length; i++){
			tmpFile = files[i];				
			if(tmpFile.isDirectory()){
				JSONObject parentObj = new JSONObject();
				JSONArray childsArr = new JSONArray();
				if(appendFolder(childsArr, tmpFile)){
					parent.add(parentObj);
					parentObj.put(getFolderName(tmpFile), childsArr);
				}
			}else{
				fileName = tmpFile.getName();
				if(fileName.startsWith("20775-") && fileName.length() > 20 && (fileName.charAt(18) == '-' || fileName.endsWith("-mets.xml") || fileName.endsWith("-rdf.xml")))
					parent.add(fileName);
			}
		}
		if(parent.size() > 0)
			return true;
		else
			return false;
	}

	public static String getFolderName(File directory){
		return directory.getName();
	}
 }