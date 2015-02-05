package edu.ucsd.library.xdre.web;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.tab.RecordUtil;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;


 /**
 * Class ExcelImportController, the model for the ExcelImport view
 *
 * @author lsitu@ucsd.edu
 */
public class ExcelImportController implements Controller {

	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String ds = request.getParameter("ts");
		String collectionId =  request.getParameter("category");
		String unit =  request.getParameter("unit");
		String message = request.getParameter("message");
		String fileStore = request.getParameter("fs");
		String licenseBeginDate = request.getParameter("licenseBeginDate");
		String licenseEndDate = request.getParameter("licenseEndDate");

		HttpSession session = request.getSession();	
		DAMSClient damsClient = null;
		Map dataMap = new HashMap();
		try{
			if(ds == null)
				ds = Constants.DEFAULT_TRIPLESTORE;

			
			damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
			damsClient.setTripleStore(ds);


			Map<String, String> collectionMap = damsClient.listCollections();
			Map<String, String> unitsMap = damsClient.listUnits();
			List<String> tsSrcs = damsClient.listTripleStores();
			List<String> fsSrcs = damsClient.listFileStores();
			String fsDefault = damsClient.defaultFilestore();
			if(fileStore == null || fileStore.length() == 0)
				fileStore = fsDefault;

			message = (!StringUtils.isBlank(message) || StringUtils.isBlank(collectionId)) ? message : (String)session.getAttribute("message");
			session.removeAttribute("message");

			JSONArray accessValues = new JSONArray();
			accessValues.addAll(Arrays.asList(RecordUtil.ACCESS_VALUES));

			dataMap.put("categories", collectionMap);
			dataMap.put("category", collectionId);
			dataMap.put("units", unitsMap);
			dataMap.put("unit", unit);
			dataMap.put("message", message);
			dataMap.put("triplestore", ds);
			dataMap.put("triplestores", tsSrcs);
			dataMap.put("filestores", fsSrcs);
			dataMap.put("filestore", fileStore);
			dataMap.put("filestoreDefault", fsDefault);
			dataMap.put("copyrightStatus", RecordUtil.COPYRIGHT_VALUES);
			dataMap.put("program", RecordUtil.PROGRAM_VALUES);
			dataMap.put("accessOverride", accessValues);
			dataMap.put("licenseBeginDate", licenseBeginDate);
			dataMap.put("licenseEndDate", licenseEndDate);
		
		
		} catch (Exception e) {
			e.printStackTrace();
			message += e.getMessage();
		}finally{
			if(damsClient != null)
				damsClient.close();
		}
		return new ModelAndView("excelImport", "model", dataMap);
	}
 }
