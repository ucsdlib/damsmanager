package edu.ucsd.library.xdre.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.ingest.assembler.UploadTaskOrganizer;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;


 /**
 * Class IngestController, the model for the Ingest view
 * home page
 *
 * @author lsitu@ucsd.edu
 */
public class IngestController implements Controller {

	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String filePath = null;
		String fileFilter = null;
		String preferedOrder = null;
		String arkSetting = null;
		String fileSuffixes = null;
		String fileUse = null;
		
		String ds = request.getParameter("ts");
		String collectionId =  request.getParameter("category");
		String repo =  request.getParameter("repo");
		String reset = request.getParameter("reset");
		String message = request.getParameter("message");
		String fileStore = request.getParameter("fs");

		HttpSession session = request.getSession();	
		DAMSClient damsClient = null;
		Map dataMap = new HashMap();
		try{
			if(ds == null)
				ds = Constants.DEFAULT_TRIPLESTORE;

			
			damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
			damsClient.setTripleStore(ds);
			if(collectionId == null && reset != null){
				filePath = "";
				fileStore = "";
				session.removeAttribute("filePath");
				session.removeAttribute("fileFilter");
				session.removeAttribute("collectionId");
				session.removeAttribute("arkSetting");
				session.removeAttribute("preferedOrder");
				session.removeAttribute("fileStore");
				session.removeAttribute("repo");
			 	session.removeAttribute("fileSuffixes");
			 	session.removeAttribute("fileUse");
			}else{
				filePath = (String) session.getAttribute("filePath");
				fileFilter = (String) session.getAttribute("fileFilter");
				arkSetting = (String) session.getAttribute("arkSetting");
				preferedOrder = (String) session.getAttribute("preferedOrder");
				fileSuffixes = (String) session.getAttribute("fileSuffixes");
				fileUse = (String) session.getAttribute("fileUse");
			}


			Map<String, String> collectionMap = damsClient.listCollections();
			Map<String, String> repoMap = damsClient.listRepositories();
			List<String> tsSrcs = damsClient.listTripleStores();
			List<String> fsSrcs = damsClient.listFileStores();
			String fsDefault = damsClient.defaultFilestore();
			if(fileStore == null || fileStore.length() == 0)
				fileStore = fsDefault;
			
			dataMap.put("categories", collectionMap);
			dataMap.put("category", collectionId);
			dataMap.put("repos", repoMap);
			dataMap.put("repo", repo);
			dataMap.put("stagingArea", Constants.DAMS_STAGING);
			dataMap.put("filePath", filePath);
			dataMap.put("fileFilter", fileFilter);
			dataMap.put("arkSetting", arkSetting);
			dataMap.put("message", message);
			dataMap.put("triplestore", ds);
			dataMap.put("triplestores", tsSrcs);
			dataMap.put("filestores", fsSrcs);
			dataMap.put("filestore", fileStore);
			dataMap.put("filestoreDefault", fsDefault);
			dataMap.put("fileSuffixes", fileSuffixes);
			dataMap.put("fileUse", fileUse);
		
		
		} catch (Exception e) {
			e.printStackTrace();
			message += e.getMessage();
		}finally{
			if(damsClient != null)
				damsClient.close();
		}
		return new ModelAndView("ingest", "model", dataMap);
	}
	
	private static int convertArkSetting(String arkSetting){
		int settingIndex = UploadTaskOrganizer.SIMPLE_LOADING;
		if(arkSetting.equals("one2many"))
			settingIndex = UploadTaskOrganizer.COMPLEXOBJECT_LOADING;
		else if(arkSetting.equals("one2two"))
			settingIndex = UploadTaskOrganizer.PAIR_LOADING;
		else if(arkSetting.equals("mix"))
			settingIndex = UploadTaskOrganizer.MIX_LOADING;
		else if(arkSetting.equals("share"))
			settingIndex = UploadTaskOrganizer.SHARE_ARK_LOADING;
		else if(arkSetting.equals("mixshare"))
			settingIndex = UploadTaskOrganizer.MIX_CO_SHARE_ARK_LOADING;
		
		return settingIndex;
	}
 }