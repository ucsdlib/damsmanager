package edu.ucsd.library.xdre.web;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.jetl.organizer.UploadTaskOrganizer;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;


 /**
 * Class CollectionManagementController, the Model of the collection management
 * home page
 *
 * @author lsitu@ucsd.edu
 */
public class JetlLoadingController implements Controller {

	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		
		String filePath = null;
		String filePrefix = null;
		String fileFilter = null;
		String arkSetting = null;
		String preferedOrder = null;
		
		String ds = request.getParameter("ds");
		String collectionId =  request.getParameter("collection");
		String reset = request.getParameter("reset");
		String message = request.getParameter("message");
		String fileStore = request.getParameter("fileStore");

		HttpSession session = request.getSession();	
		Map<String, String> collectionMap = null;
		DAMSClient damsClient = null;
		try{
			if(ds == null)
				ds = Constants.DEFAULT_TRIPLESTORE;

			
			damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
			if(collectionId != null || request.getParameter("category")!=null){
				if(reset != null){
					filePath = "";
					//Retrieve default value in the triplestore, then convert it to application value
					String defaultArkSetting = "";
					if(defaultArkSetting != null)
						arkSetting = String.valueOf(convertArkSetting(defaultArkSetting));
					fileStore = "";
					session.removeAttribute("filePath");
					session.removeAttribute("filePrefix");
					session.removeAttribute("fileFilter");
					session.removeAttribute("category");
					session.removeAttribute("arkSetting");
					session.removeAttribute("preferedOrder");
					session.removeAttribute("fileStore");
				}else{
					filePath = (String) session.getAttribute("filePath");
					filePrefix = (String) session.getAttribute("filePrefix");
					fileFilter = (String) session.getAttribute("fileFilter");
					collectionId = (String) session.getAttribute("category");
					arkSetting = (String) session.getAttribute("arkSetting");
					preferedOrder = (String) session.getAttribute("preferedOrder");
					fileStore = (String) session.getAttribute("fileStore");
				}
			}
			
		} catch (Exception e) {
			e.printStackTrace();
			message += e.getMessage();
		}

		List<String> tsSrcs = damsClient.listTripleStores();
		List<String> fsSrcs = damsClient.listFileStores();

		if(fileStore == null || fileStore.length() == 0)
			fileStore = Constants.DEFAULT_FILESTORE;
		Map dataMap = new HashMap();
		dataMap.put("collections", collectionMap);
		dataMap.put("collectionId", collectionId);
		dataMap.put("stagingArea", Constants.DAMS_STAGING);
		dataMap.put("filePath", filePath);
		dataMap.put("filePrefix", filePrefix);
		dataMap.put("fileFilter", fileFilter);
		dataMap.put("arkSetting", arkSetting);
		dataMap.put("message", message);
		dataMap.put("triplestore", ds);
		dataMap.put("triplestores", tsSrcs);
		dataMap.put("filestores", fsSrcs);
		dataMap.put("filestore", fileStore);
		dataMap.put("filestoreDefault", Constants.DEFAULT_FILESTORE);
		dataMap.put("preferedOrder", preferedOrder);
		return new ModelAndView("jetl", "model", dataMap);
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