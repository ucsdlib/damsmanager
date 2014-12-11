package edu.ucsd.library.xdre.model;

import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.QName;

import edu.ucsd.library.xdre.tab.TabularRecord;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * DAMSCollection class
 * @author lsitu
 *
 */
public class  DAMSCollection extends DAMSResource {
	
	protected String title = null;
	protected String unit = null;
	protected String visibility = null;
	protected String parent = null;
	protected Document record = null;
	protected DAMSClient damsClient = null;
	protected boolean isModified = false;
	protected boolean isNew = false;
	protected boolean isSaving = false;

	public DAMSCollection (String id, String type) {
		this (null, id, type);
	}
	
	public DAMSCollection (DAMSClient damsClient, String id, String type) {
		this (damsClient, id, type, null, null, null, null);
		this.damsClient = damsClient;
	}
	
	public DAMSCollection (DAMSClient damsClient, String id, String type,
			String title, String unit, String visibility, String parent) {
		super (id, type);
		this.damsClient = damsClient;
		this.title = title;
		this.unit = unit;
		this.visibility = visibility;
		this.parent = parent;
		
	}
	
	@Override
	public Element serialize() throws Exception {
		updateRecord();
		return record.getRootElement();
	}

	public boolean save() throws Exception {
		isSaving = true;
		if(updateRecord())
			return damsClient.updateObject(id, record.asXML(), Constants.IMPORT_MODE_ALL);
		else
			return false;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String getVisibility() {
		return visibility;
	}

	public void setVisibility(String visibility) {
		this.visibility = visibility;
	}

	public String getParent() {
		return parent;
	}

	public void setParent(String parent) {
		this.parent = parent;
	}

	public Document getRecord() {
		return record;
	}

	public void setRecord(Document record) {
		this.record = record;
	}

	public DAMSClient getDamsClient() {
		return damsClient;
	}

	public void setDamsClient(DAMSClient damsClient) {
		this.damsClient = damsClient;
	}
	
	public boolean isModified() {
		return isModified;
	}

	public boolean isNew() {
		return isNew;
	}

	public void setNew(boolean isNew) {
		this.isNew = isNew;
	}

	public boolean isEquals (Object o, Object n) {
		if (o == null && n == null)
			return true;
		else if (o != null && n != null) {
			return o.equals(n);
		} else 
			return false;
	}

	private boolean updateRecord() throws Exception {
		DAMSCollection collection = null;
		Document collDoc = null;
		Element rdf = null;
		Element collRecord = null;

		if ( StringUtils.isNotBlank(id) && !isNew ) {

			collection = DAMSCollection.getRecord(damsClient, id);
			collDoc = collection.getRecord();
			rdf = collDoc.getRootElement();
			collRecord = (Element)rdf.selectSingleNode("*[contains(@rdf:about, '" + id + "')]");

			// update collection type
			if ( !isEquals(collection.type, type) ) {
				isModified = true;
				collRecord.setQName(new QName(type, damsNS));
				
				if ( StringUtils.isNotBlank(parent) && isEquals(collection.parent, parent) ) {
					// update the predicate parent child linking
					Document docParent = damsClient.getRecord(parent);
		        	Node collNode = docParent.selectSingleNode("//*[contains(@rdf:about, '" + parent + "')]");
		        	// replace the old linking
		        	removeReference (collNode, parent);
		        	((Element)collNode).addElement(new QName("has" + type.replace("ProvenanceCollectionPart", "Part"), damsNS))
		        			.addAttribute(new QName("resource", rdfNS), parent);
		        	rdf.add(collNode.detach());
				}
			}

			// update collection title
			if ( !isEquals(collection.title, title) ) {
				isModified = true;
				List<Node> nodes = collRecord.selectNodes("dams:title");
				for (Node node : nodes)
					node.detach();
				collRecord.addElement(new QName("title", damsNS)).add(new MadsTitle(title).serialize());
			}

			// update unit
			if ( !isEquals(collection.unit, unit) ) {
				isModified = true;
				List<Node> nodes = collRecord.selectNodes("dams:unit");
				if ( nodes.size() > 0) {
					for (Node node : nodes)
						node.detach();
				}
				collRecord.addElement(new QName("unit", damsNS)).addAttribute(new QName("resource", rdfNS), unit);
			}
			
			// update visibility
			if ( !isEquals(collection.visibility, visibility) ) {
				isModified = true;
				List<Node> nodes = collRecord.selectNodes("dams:visibility");
				if ( nodes.size() > 0) {
					for (Node node : nodes)
						node.detach();
				}
				collRecord.addElement(new QName("visibility", damsNS)).setText(visibility);
			}		

			// update parent linking
	        if ( !isEquals(collection.parent, parent) ) {
	        	isModified = true;
	        	
	        	// remove the linkings related the old parent record
	        	if ( StringUtils.isNotBlank(collection.parent)) {
		        	Node oParentRecord = damsClient.getRecord(collection.parent)
		        			.selectSingleNode("//*[contains(@rdf:about, '" + collection.parent + "')]");
		        	// remove the linking from the original parent record
		        	removeReference(oParentRecord, id);

			        // update the original parent record 
		        	rdf.add(oParentRecord.detach());
		        	
		        	// remove the original parent linking from the collection record
		        	removeReference (collRecord, collection.parent);
	        	}
				
	        	// add linkings related to the new parent
	        	if ( StringUtils.isNotBlank(parent) ) { 
	        		// linking to the new parent collection
		        	Document docParent = damsClient.getRecord(parent);
		        	addCollectionLink(collRecord, damsClient.getCollectionType(parent), parent);
			        
		        	// child linking in the new parent collection 
		        	Node collNode = docParent.selectSingleNode("//*[contains(@rdf:about, '" + parent + "')]");
		        	((Element)collNode).addElement(new QName("has" + type.replace("ProvenanceCollectionPart", "Part"), damsNS))
		        			.addAttribute(new QName("resource", rdfNS), id);
		        	rdf.add(collNode.detach());
	        	}
	        }
	        
	        // Removed the extent note when saving the collection record
	        if (isModified && isSaving) {
	        	removeExtentNote(collDoc);
	        }

	        record = collDoc;
		} else {
			isNew = true;
			// create new collection record
			if (StringUtils.isBlank(id))
				id = toDamsUrl(damsClient.mintArk(Constants.DEFAULT_ARK_NAME));

			collDoc = new DocumentFactory().createDocument();
	    	rdf = TabularRecord.createRdfRoot (collDoc);
	    	
	    	// collection
	    	collRecord = rdf.addElement(new QName(type, damsNS));
	    	collRecord.addAttribute(new QName("about", rdfNS), id);
	        
	        // collection title
	        collRecord.addElement(new QName("title", damsNS)).add(new MadsTitle(title).serialize());

			// add unit
			if ( StringUtils.isNotBlank(unit) ) {
				collRecord.addElement(new QName("unit", damsNS)).addAttribute(new QName("resource", rdfNS), unit);
			}
			
			// add visibility
			if ( StringUtils.isNotBlank(visibility) ) {
				collRecord.addElement(new QName("visibility", damsNS)).setText(visibility);
			}	        
	        
	        // parent collection
	        if (StringUtils.isNotBlank(parent)) {
	        	Document docParent = damsClient.getRecord(parent);
	        	addCollectionLink(collRecord, damsClient.getCollectionType(parent), parent);
	
	        	// Add child reference to the parent collection
	        	Node collNode = docParent.selectSingleNode("//*[contains(@rdf:about, '" + parent + "')]");
	        	((Element)collNode).addElement(new QName("has" + type.replace("ProvenanceCollectionPart", "Part"), damsNS))
	        			.addAttribute(new QName("resource", rdfNS), id);
	        	rdf.add(collNode.detach());
	        }
	        
	        this.record = collDoc;
		}
		return isModified || isNew;		
	}

	private String toDamsUrl(String recordId){
		if(!recordId.startsWith("http")){
			String arkUrlBase = Constants.DAMS_ARK_URL_BASE;
			String arkOrg = Constants.ARK_ORG;
			recordId = arkUrlBase + (arkUrlBase.endsWith("/")?"":"/") + (recordId.indexOf('/')>0?recordId:arkOrg+"/"+recordId);
		}
		return recordId;
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

	private void removeReference (Node node, String resUri) {
		if (node != null) {
	    	List<Node> resNodes = node.selectNodes("//*[contains(@rdf:resource, '" + resUri + "')]");
	    	for (Node resNode : resNodes) {
	    		resNode.detach();
	    	}
		}
	}

	private void removeExtentNote(Document doc){
    	List<Node> nodes = doc.selectNodes("//dams:note/dams:Note[dams:type='extent' and (substring-after(rdf:value, ' ')='digital objects.' or rdf:value='1 digital object.')]");
    	for (Node node : nodes)
    		node.getParent().detach();
	}

	public static DAMSCollection getRecord(DAMSClient damsClient, String id) throws Exception {
		Document record = damsClient.getRecord(id);
		Node collNode = record.selectSingleNode("/rdf:RDF/*[contains(name(), 'Collection')] | *[contains(name(), 'Collection')]");
		DAMSCollection damsColl = new DAMSCollection (damsClient, collNode.selectSingleNode("@rdf:about").getStringValue(), collNode.getName());
		
		// Collection title
		damsColl.title = collNode.selectSingleNode("dams:title/mads:Title/mads:authoritativeLabel").getText();
		
		// visibility
		Node visibilityNode = collNode.selectSingleNode("dams:visibility");
		if (visibilityNode != null)
			damsColl.visibility = visibilityNode.getText();

		// unit
		Node unitNode = collNode.selectSingleNode("dams:unit/@rdf:resource");
		if (unitNode != null)
			damsColl.unit = unitNode.getStringValue();

		// parent
		Node parentNode = collNode.selectSingleNode("*[local-name()='assembledCollection' or local-name()='provenanceCollection'" +
				" or local-name()='provenanceCollectionPart' or local-name() = 'collection']/@rdf:resource");
		if (parentNode != null) {
			damsColl.parent = parentNode.getStringValue();
		}
		
		damsColl.record = record;
		return damsColl;
	}
}
