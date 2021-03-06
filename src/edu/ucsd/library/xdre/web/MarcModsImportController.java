package edu.ucsd.library.xdre.web;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.json.simple.JSONArray;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.tab.RecordUtil;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;


 /**
 * Class ModsController, the model for the AtImport view
 *
 * @author lsitu@ucsd.edu
 */
public class MarcModsImportController implements Controller {

	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String ds = request.getParameter("ts");
		String collectionId =  request.getParameter("category");
		String unit =  request.getParameter("unit");
		String reset = request.getParameter("reset");
		String message = request.getParameter("message");
		String source = request.getParameter("source");
		String bibNumber = request.getParameter("bib");
		String modsXml = request.getParameter("mods");
		String copyrightStatus = request.getParameter("copyrightStatus");
		String copyrightJurisdiction = request.getParameter("copyrightJurisdiction");
		String copyrightOwner = request.getParameter("copyrightOwner");
		String program = request.getParameter("program");
		String accessOverride = request.getParameter("accessOverride");
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

			message = (!StringUtils.isBlank(message) || StringUtils.isBlank(collectionId)) ? message : (String)request.getSession().getAttribute("message");
			request.getSession().removeAttribute("message");

			JSONArray accessValues = new JSONArray();
			accessValues.addAll(Arrays.asList(RecordUtil.ACCESS_VALUES));

			Map<String, String> countryCodes = getCountryCodes (request);
			
			dataMap.put("categories", collectionMap);
			dataMap.put("category", collectionId);
			dataMap.put("units", unitsMap);
			dataMap.put("unit", unit);
			dataMap.put("message", message);
			dataMap.put("triplestore", ds);
			dataMap.put("triplestores", tsSrcs);
			dataMap.put("copyrightStatus", RecordUtil.COPYRIGHT_VALUES);
			dataMap.put("program", RecordUtil.PROGRAM_VALUES);
			dataMap.put("accessOverride", accessValues);
			dataMap.put("licenseBeginDate", licenseBeginDate);
			dataMap.put("licenseEndDate", licenseEndDate);
			dataMap.put("countryCodes", countryCodes);
		
		
		} catch (Exception e) {
			e.printStackTrace();
			message += e.getMessage();
		}finally{
			if(damsClient != null)
				damsClient.close();
		}
		return new ModelAndView("marcModsImport", "model", dataMap);
	}
	
	public static Map<String, String> getCountryCodes (HttpServletRequest request) throws DocumentException {
		Map<String, String> countryCodes = new TreeMap<>();
		File dataFile = new File(request.getSession().getServletContext().getRealPath("files/country-code-iso-3166-all.xml"));
		SAXReader reader = new SAXReader();
		Document dataDoc = reader.read(dataFile);
		List<Node> nodes = dataDoc.selectNodes("//country");
		for (Node node : nodes) {
			String name = node.selectSingleNode("@name").getStringValue().trim();
			String alpha2 = node.selectSingleNode("@alpha-2").getStringValue().trim();
			countryCodes.put(name, alpha2);
		}
		
		return countryCodes;
	}
 }
