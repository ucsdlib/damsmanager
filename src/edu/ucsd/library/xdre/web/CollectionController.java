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
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Node;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.model.DAMSCollection;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;


 /**
 * Class CollectionController, the model/controller for the Collection record
 *
 * @author lsitu@ucsd.edu
 */
public class CollectionController implements Controller {
	public static final String[] COLLECTION_TYPES = {"AssembledCollection", "ProvenanceCollection", "ProvenanceCollectionPart"};
	public static final String[] VISIBILITY_VALUES = {"curator", "local", "public"};

	private static Logger log = Logger.getLogger(CollectionController.class);
	private static enum Action {create, edit, save}
	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String action = request.getParameter("actionValue");
		String collectionId =  request.getParameter("category");
		String collType = request.getParameter("collType");
		String collTitle = request.getParameter("collTitle");
		String unit =  request.getParameter("unit");
		String parentCollection =  request.getParameter("parentCollection");
		String visibility = request.getParameter("visibility");
		String message = request.getParameter("message");

		HttpSession session = request.getSession();
		DAMSClient damsClient = null;
		Map dataMap = new HashMap();
		DAMSCollection collection = null;

		if (action != null && Action.valueOf(action).equals(Action.save))
			log.info("{actionValue => " + action + ", category => " + collectionId + ", collType => " + collType + ",  collTitle => " + collTitle
					+ ", unit => " + unit + ", parentCollection => " + parentCollection + ", visibility => " + visibility  + ", message =>" + message + "}");
		
		try{
			damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
			damsClient.setTripleStore(Constants.DEFAULT_TRIPLESTORE);
			
			if (StringUtils.isBlank(action)) {
				action = Action.edit.name();
			}
			
			switch (Action.valueOf(action)) {
				case save :
					if (StringUtils.isBlank(collTitle)) {
						message = "Collection title is missing! Please enter a collection title.";
					}

					// Parent collection checking
					if (StringUtils.isNotBlank(parentCollection) && StringUtils.isNotBlank(collectionId) && parentCollection.endsWith(collectionId)) {
						message = "Parent collection can't be the same collection! Please choose another parent collection.";
					}

					// check title existing
					String checkId = checkRecord (collTitle, damsClient);
					if (StringUtils.isNotBlank(checkId) && (StringUtils.isBlank(collectionId)
							|| StringUtils.isNotBlank(collectionId) && !checkId.endsWith(collectionId))) {
						message = "Collection with title " + collTitle + " exists (" + checkId + ")! Please use another title.";
					}

					if (StringUtils.isBlank(message)) {	
						collection = new DAMSCollection (damsClient, collectionId, collType, collTitle, unit, visibility, parentCollection);
						if (collection.save()) {
							message = "Successfully";
							if (collection.isNew())
								message += " created ";
							else
								message += " updated ";
							message += "collection '" + collTitle + "' (" + collection.getId() + ").";

							// Update solr
							Document record = collection.getRecord();
							List<Node> nodes = record.selectNodes("//@rdf:about");
							for (Node node : nodes) {
								String resId = node.getStringValue();
								// Update SOLR
								if (!damsClient.solrUpdate(resId))
									message += " SOLR update failed for " + resId;
							}
						} else {
							message = "collection \"" + collTitle + "\"";
							if (collection.isNew())
								message = "Failed to create " + message;
							else if (!collection.isModified())
								message = "No changes submitted for " + message;
							else
								message = "Failed to update " + message;
							 message += (StringUtils.isNotBlank(collection.getId()) ? " (" + collection.getId() + ")" : "") + ".";
						}
						
						collectionId = collection.getId();
					}
					action = Action.edit.name();
					break;
				default :
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
					break;
			}

			Map<String, String> collectionMap = damsClient.listCollections();
			Map<String, String> unitsMap = damsClient.listUnits();
			
			message = (StringUtils.isNotBlank(message) || StringUtils.isBlank(collectionId)) ? message : (String)request.getSession().getAttribute("message");
			session.removeAttribute("message");

			dataMap.put("categories", collectionMap);
			dataMap.put("category", collectionId);
			dataMap.put("units", unitsMap);
			dataMap.put("unit", unit);
			dataMap.put("collTypes", getCollectionTypes());
			dataMap.put("collType", collType);
			dataMap.put("visibilities", getVisibilityValues());
			dataMap.put("visibility", visibility);
			dataMap.put("parentCollection", parentCollection);
			dataMap.put("collTitle", collTitle);
			dataMap.put("actionValue", action);
			dataMap.put("message", message);

		} catch (Exception e) {
			e.printStackTrace();
			message += e.getMessage();
		}finally{
			if(damsClient != null)
				damsClient.close();
		}
		
		// loging
		String requestAction = request.getParameter("actionValue");
		if (requestAction != null && Action.valueOf(requestAction).equals(Action.save) && collection != null) {
			log.info("Result: {actionValue => " + action + ", category => " + collection.getId() + ", collType => " + collection.getType()
					+ ",  collTitle => " + collection.getTitle() + ", unit => " + collection.getUnit() + ", parentCollection => " + collection.getParent()
					+ ", visibility => " + collection.getVisibility()  + ", message => " + message + "}");
			if (collection.getRecord() != null) {
				File rdf = new File(Constants.TMP_FILE_DIR, (StringUtils.isNotBlank(collectionId) ? collectionId.substring(collectionId.lastIndexOf("/") + 1) : "")
						+ "_" + System.currentTimeMillis() + ".rdf.xml");
				try{
					CollectionOperationController.writeXml(rdf, collection.getRecord().asXML());
				} finally {										  
					rdf.deleteOnExit();
				}
			}
		} else
			log.info("Message => " + message);
		return new ModelAndView("collection", "model", dataMap);
	}
	
	private List<String> getCollectionTypes() {
		return Arrays.asList(COLLECTION_TYPES);
	}
	
	private List<String> getVisibilityValues() {
		return Arrays.asList(VISIBILITY_VALUES);
	}
	
	private String checkRecord (String collTitle, DAMSClient damsClient) throws Exception {
		Map<String, String> collMap = damsClient.listCollections();
		for (String key : collMap.keySet()){
			String title = key.substring(0, key.lastIndexOf(" [")).trim();
			if (title.equalsIgnoreCase(collTitle)) {
				return collMap.get(key);
			}
		}
		return null;
	}
 }
