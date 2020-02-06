package edu.ucsd.library.xdre.web;

import java.io.File;
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

import edu.ucsd.library.xdre.model.DAMSCollection;
import edu.ucsd.library.xdre.tab.ExcelSource;
import edu.ucsd.library.xdre.tab.RecordUtil;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;


 /**
 * Class CollectionImportController, the model for the CollectionImport view
 *
 * @author lsitu@ucsd.edu
 */
public class CollectionImportController implements Controller {
	public static final String[] COLLECTION_TYPES = {"AssembledCollection", "ProvenanceCollection", "ProvenanceCollectionPart"};
	public static final String[] VISIBILITY_VALUES = {"curator", "local", "public"};
	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String collectionId =  request.getParameter("category");
		String collType = request.getParameter("collType");
		String collTitle = request.getParameter("collTitle");
		String unit =  request.getParameter("unit");
		String parentCollection =  request.getParameter("parentCollection");
		String visibility = request.getParameter("visibility");
		String message = request.getParameter("message");

		HttpSession session = request.getSession();	
		DAMSClient damsClient = null;
		DAMSCollection collection = null;
		Map dataMap = new HashMap();
		try{
			
			// initiate column name and control values for validation
			String validateTemplate = request.getServletContext().getRealPath("files/xls_standard_input_template.xlsx");
			try {
				ExcelSource.initControlValues(new File( validateTemplate));
			} catch (Exception e) {
				e.printStackTrace();
			}

			String userId = request.getRemoteUser();
			damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
			damsClient.setTripleStore(Constants.DEFAULT_TRIPLESTORE);
			damsClient.setUser(userId);
			session.setAttribute("user", userId);

			if (StringUtils.isNotBlank(collectionId)) {
				collection = DAMSCollection.getRecord(damsClient, collectionId);
				parentCollection =  collection.getParent();
				collTitle = collection.getTitle();
				unit =  collection.getUnit();
				collType = collection.getType();
				visibility = collection.getVisibility();
			} else {
				collTitle = null;
				parentCollection =  null;
				unit =  null;
				collType = null;
				visibility = "curator";
			}

			List<String> tsSrcs = damsClient.listTripleStores();

			message = !StringUtils.isBlank(message) ? message : (String)session.getAttribute("message");
			session.removeAttribute("message");

			JSONArray accessValues = new JSONArray();
			accessValues.addAll(Arrays.asList(RecordUtil.ACCESS_VALUES));

			visibility = StringUtils.isNotBlank(visibility)?visibility : "curator";

			Map<String, String> collectionMap = damsClient.listCollections();
			Map<String, String> unitsMap = damsClient.listUnits();
			
			message = (StringUtils.isNotBlank(message) || StringUtils.isBlank(collectionId)) ? message : (String)request.getSession().getAttribute("message");
			session.removeAttribute("message");

			dataMap.put("categories", collectionMap);
			dataMap.put("category", collectionId);
			dataMap.put("units", unitsMap);
			dataMap.put("unit", unit);
			dataMap.put("collTypes", Arrays.asList(COLLECTION_TYPES));
			dataMap.put("collType", collType);
			dataMap.put("visibilities", Arrays.asList(VISIBILITY_VALUES));
			dataMap.put("visibility", visibility);
			dataMap.put("parentCollection", parentCollection);
			dataMap.put("collTitle", collTitle);
			dataMap.put("message", message);
		
		} catch (Exception e) {
			e.printStackTrace();
			message += e.getMessage();
		}finally{
			if(damsClient != null)
				damsClient.close();
		}
		return new ModelAndView("collectionImport", "model", dataMap);
	}
 }
