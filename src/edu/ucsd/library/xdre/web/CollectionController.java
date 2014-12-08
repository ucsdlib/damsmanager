package edu.ucsd.library.xdre.web;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.QName;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.tab.TabularRecord;
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
	public static enum Action {create, edit, save}

    private String madsURI = null;
    private String damsURI = null;
    private String rdfURI = null;
    private Namespace madsNS = null;
    private Namespace damsNS = null;
    private Namespace rdfNS = null;
	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String action = request.getParameter("action");
		String collectionId =  request.getParameter("category");
		String parentCollection =  request.getParameter("parentCollection");
		String collTitle = request.getParameter("collTitle");
		String unit =  request.getParameter("unit");
		String collType = request.getParameter("collType");
		String visibility = request.getParameter("visibility");
		String message = request.getParameter("message");

	    madsURI = Constants.NS_PREFIX_MAP.get("mads");
	    damsURI = Constants.NS_PREFIX_MAP.get("dams");
	    rdfURI = Constants.NS_PREFIX_MAP.get("rdf");
	    madsNS = new Namespace("mads", madsURI);
	    damsNS = new Namespace("dams", damsURI);
	    rdfNS = new Namespace("rdf", rdfURI);
	    
		HttpSession session = request.getSession();
		DAMSClient damsClient = null;
		Map dataMap = new HashMap();
		try{
			damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
			damsClient.setTripleStore(Constants.DEFAULT_TRIPLESTORE);
			Map<String, String> collectionMap = damsClient.listCollections();
			Map<String, String> unitsMap = damsClient.listUnits();
			Document doc = null;
			
			if (StringUtils.isBlank(action)) {
				action = Action.create.name();
			}
			
			switch (Action.valueOf(action)) {
				case edit :
					doc = damsClient.getRecord(collectionId);
					break;
				case save :
					if (StringUtils.isBlank(collectionId)) {
						if (StringUtils.isBlank(collTitle)) {
							message = "Error: Collection title is missing. Please enter a collection title.";
							action = Action.create.name();
						}
						
						if (StringUtils.isBlank(message)) {
							collectionId = checkRecord (collTitle, damsClient);
							if (StringUtils.isNotBlank(collectionId)) {
								message = "Error: Collection with title " + collTitle + " exists: " + collectionId + ". Please use another collection title.";
								action = Action.create.name();
							} else {
								collectionId = toDamsUrl(damsClient.mintArk(Constants.DEFAULT_ARK_NAME));
						    	doc = new DocumentFactory().createDocument();
						    	Element rdf = TabularRecord.createRdfRoot (doc);
						    	
						    	// Collection record
						        Element root = rdf.addElement(new QName(collType, damsNS)).addAttribute(new QName("about", rdfNS), collectionId);
						       
						        // Collection title
						        addTitle(root, collTitle);
						        
						        // Add parent collection linkings
						        if (StringUtils.isNotBlank(parentCollection)) {
						        	addCollectionLink(root, damsClient.getCollectionType(parentCollection), parentCollection);
						        	// Add child reference to the parent collection
						        	Document docParent = damsClient.getRecord(parentCollection);
						        	Node collNode = docParent.selectSingleNode("//*[contains(@rdf:about, '" + parentCollection + "')]");
						        	((Element)collNode).addElement(new QName("has" + collType.replace("ProvenanceCollectionPart", "Part"), damsNS))
						        			.addAttribute(new QName("resource", rdfNS), collectionId);
						        	rdf.add(collNode.detach());
						        }
						        
						        // Add unit linking
						        root.addElement(new QName("unit", damsNS)).addAttribute(new QName("resource", rdfNS), unit);
						        
						        // Add visibility property
						        if (StringUtils.isNotBlank(visibility))
						        	root.addElement(new QName("visibility", damsNS)).setText(visibility);
								
								if (damsClient.updateObject(collectionId, doc.asXML(), Constants.IMPORT_MODE_ALL)) {
									
									message = "Successfully saved collection " + collectionId + ".";
									// Update SOLR
									if (!damsClient.solrUpdate(collectionId))
										message = "Collection " + collectionId + "saved successfully. But failed to update SOLR.";
									
									if (StringUtils.isNotBlank(parentCollection)) {
										if (damsClient.solrUpdate(parentCollection)) 
											message += " Failed to update SOLR for parent collection " + parentCollection + ".";
									}
									collTitle = "";
								} else {
									message = "Failed to save collection \"" + collTitle + "\": " + collectionId + ".";
								}
								action = Action.create.name();
							}
						}
					}
					break;
				default :
					visibility = VISIBILITY_VALUES[0];
					break;
			
			}
			
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
			dataMap.put("action", action);
			dataMap.put("message", message);

		} catch (Exception e) {
			e.printStackTrace();
			message += e.getMessage();
		}finally{
			if(damsClient != null)
				damsClient.close();
		}
		return new ModelAndView("collection", "model", dataMap);
	}
	
	private List<String> getCollectionTypes() {
		return Arrays.asList(COLLECTION_TYPES);
	}
	
	private List<String> getVisibilityValues() {
		return Arrays.asList(VISIBILITY_VALUES);
	}

	private String toDamsUrl(String recordId){
		if(!recordId.startsWith("http")){
			String arkUrlBase = Constants.DAMS_ARK_URL_BASE;
			String arkOrg = Constants.ARK_ORG;
			recordId = arkUrlBase + (arkUrlBase.endsWith("/")?"":"/") + (recordId.indexOf('/')>0?recordId:arkOrg+"/"+recordId);
		}
		return recordId;
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
	
	
	private void addTitle( Element parent, String value )
	{
		Element titleElem = parent.addElement(new QName("title", damsNS)).addElement(new QName("Title", madsNS));
		titleElem.addElement(new QName("authoritativeLabel", madsNS)).setText(value);
		Element elemList = titleElem.addElement("mads:elementList",madsURI);
		elemList.addAttribute(new QName("parseType",rdfNS), "Collection");
		elemList.addElement(new QName("MainTitleElement", madsNS)).addElement(new QName("elementValue", madsNS)).setText(value);
	}
	
	private void addCollectionLink(Element parent, String collectionType, String collId) {
		int idx = collectionType.indexOf(":");
		String collPredicateName = "";
		// Setting specific collection predicate for the parent collection.
		if (idx > 0) {
			collPredicateName = collectionType.substring(collectionType.indexOf(":") + 1);
			collPredicateName = collectionType.substring(0, collectionType.indexOf(":") + 1)
					+ collectionType.substring(0, 1).toLowerCase() + collectionType.substring(1);
			parent.addElement(collPredicateName).addAttribute(new QName("resource",
					new Namespace("rdf", Constants.NS_PREFIX_MAP.get("rdf"))), collId);
		}else{
			collPredicateName = collectionType.substring(0, 1).toLowerCase() + collectionType.substring(1);
			Element elem = parent.addElement(new QName(collPredicateName, damsNS));
			elem.addAttribute(new QName("resource", rdfNS), collId);
		}
	}
 }
