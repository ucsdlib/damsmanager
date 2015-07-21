package edu.ucsd.library.xdre.collection;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.QName;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * 
 * CollectionReleaseHandler: batch release objects in a new/stub collection.
 * @author lsitu@ucsd.edu
 */
public class CollectionReleaseHandler extends CollectionHandler{
	public static final String PREDICATE_VISIBILITY = "visibility";

	public static final String PUBLIC_ACCESS = "public";
	public static final String LOCAL_ACCESS = "local";
	public static final String CURATOR_ACCESS = "curator";

	public static final String RELEASE_COLLECTION = "newRelease";
	public static final String RELEASE_MERGE = "mergeRelease";
	public static final String RELEASE_ONE_OFFS = "one-offsRelease";

	private static Logger log = Logger.getLogger(CollectionReleaseHandler.class);
 
	private int count = 0;
	private int failedCount = 0;

	private String releaseState = null;
	private String releaseOption = null;
	private String collectionToMerge = null;
	private StringBuilder tsUpdateFailed = new StringBuilder();
	private StringBuilder solrUpdateFailed = new StringBuilder();
	/**
	 * Constructor for ObjectReleaseHandler
	 * @param damsClient
	 * @param collectionId: the new/stub collection to release
	 * @throws Exception
	 */
	public CollectionReleaseHandler(DAMSClient damsClient, String collectionId) throws Exception{
		this(damsClient, collectionId, PUBLIC_ACCESS, RELEASE_COLLECTION);
	}

	/**
	 * Constructor for ObjectReleaseHandler
	 * @param damsClient
	 * @param collectionId: the new/stub collection to release
	 * @param releaseState: public, local, curator
	 * @param releaseOption: new collection, merge, one-offs
	 * @throws Exception
	 */
	public CollectionReleaseHandler(DAMSClient damsClient, String collectionId, String releaseState, String releaseOption) 
			throws Exception{
		super(damsClient, collectionId);
		this.releaseState = releaseState;
		this.releaseOption = releaseOption;
	}

	
	public String getReleaseState() {
		return releaseState;
	}

	public void setReleaseState(String releaseState) {
		this.releaseState = releaseState;
	}

	public String getReleaseOption() {
		return releaseOption;
	}

	public void setReleaseOption(String releaseOption) {
		this.releaseOption = releaseOption;
	}
	
	public String getCollectionToMerge() {
		return collectionToMerge;
	}

	public void setCollectionToMerge(String collectionToMerge) {
		this.collectionToMerge = collectionToMerge;
	}

	/**
	 * Procedure to release objects in a collection
	 */
	public boolean execute() throws Exception {
		
		if (releaseOption.equalsIgnoreCase(RELEASE_MERGE) && StringUtils.isBlank(collectionToMerge)){
			// Reject release for merging with no collection to merge to.
			exeResult = false;
			exeReport.append("Releasing merge but collection for merging is not provided.");
		}else{
			
			if (releaseOption.equalsIgnoreCase(RELEASE_COLLECTION)) {
				
				// Update the collection's visibility property
				Document doc = damsClient.getRecord(collectionId);
				Node visibilityNode = doc.selectSingleNode(
						"//*[contains(@rdf:about, '" + collectionId + "')]/*[local-name() = '" + PREDICATE_VISIBILITY + "']");
				if (visibilityNode != null) {
					((Element)visibilityNode).setText(releaseState);
				} else {
					// Add the visibility property when there no no such property found
					Node collectionNode = doc.selectSingleNode("//*[contains(@rdf:about, '" + collectionId + "')]");
					((Element)collectionNode).addElement(
							new QName(PREDICATE_VISIBILITY, new Namespace("dams", Constants.NS_PREFIX_MAP.get("dams")))).setText(releaseState);
					
				}

				// Update the collection record
				if(!damsClient.updateObject(collectionId, doc.asXML(), Constants.IMPORT_MODE_ALL)) {
					exeResult = false;
					failedCount++;
					tsUpdateFailed.append("\t" + collectionId);
					exeReport.append("Collection release process failed to update collection record " + collectionId + ".");
					return false;
				}
				
				// Update SOLR for the collection record
				if(!updateSOLR(collectionId)) {
					exeResult = false;
					failedCount++;
					solrUpdateFailed.append(collectionId);
					exeReport.append("Collection release process failed to update SOLR for collection record " + collectionId + ".");
					return false;
				}
			}

			// Release the objects in the collection
			for(int i=0; i<itemsCount && !interrupted; i++){
				count++;
				String subjectURI = items.get(i);
				
				try{
					if (releaseOption.equalsIgnoreCase(RELEASE_MERGE) || releaseOption.equalsIgnoreCase(RELEASE_ONE_OFFS)) {
						Document doc = damsClient.getRecord(subjectURI);
						String collType = damsClient.getCollectionType(collectionToMerge);
						if (releaseOption.equalsIgnoreCase(RELEASE_MERGE)) {
							// Update the collection link
							updateCollectionLink(doc, collType);
						} else 	if (releaseOption.equalsIgnoreCase(RELEASE_ONE_OFFS)) {
							// Remove the collection link
							removeCollectionLink(doc);					
						}
						
						if(!damsClient.updateObject(subjectURI, doc.asXML(), Constants.IMPORT_MODE_ALL)) {
							exeResult = false;
							failedCount++;
							tsUpdateFailed.append("\t" + subjectURI);
							exeReport.append("Failed to removed the collection link for record " + collectionId + ".");
						}
					}
					
					// Update solr index to release the object
					setStatus("Updating SOLR index for subject " + subjectURI  + " (" + (i+1) + " of " + itemsCount + ") ... " );
					if(!updateSOLR(subjectURI)) {
						exeResult = false;
						failedCount++;
						solrUpdateFailed.append(collectionId);
					}
					setProgressPercentage( ((i + 1) * 100) / itemsCount);
				} catch (Exception e){
					exeResult = false;
					failedCount++;
					tsUpdateFailed.append("\t" + subjectURI);
					log.error("Error releasing record " + subjectURI, e);
	    			logError("Error releasing record " + subjectURI + ": " + e.getMessage() + ". ");
				}

				try{
					Thread.sleep(10);
				} catch (InterruptedException e1) {
					exeResult = false;
					failedCount++;
					interrupted = true;
	    			logError("SOLR index canceled for subject " + subjectURI  + ". Error: " + e1.getMessage() + ". ");
					setStatus("Canceled");
					clearSession();
				}
			}

			if (exeResult) {
				// Update SOLR for the extent note in the collection with merging, and remove the stub collection for merge/one-offs release
				if (releaseOption.equalsIgnoreCase(RELEASE_MERGE) || releaseOption.equalsIgnoreCase(RELEASE_ONE_OFFS)) {
					if (releaseOption.equalsIgnoreCase(RELEASE_MERGE)) {
						if(!updateSOLR(collectionToMerge)) {
							failedCount++;
							solrUpdateFailed.append("\t" + collectionToMerge);
						}
					}
						
					damsClient.delete(collectionId, null, null);
					if(!solrDelete(collectionId)) {
						failedCount++;
						solrUpdateFailed.append("\t" + collectionId);
					}
				}
			}
		}
		return exeResult;
	}

	private void removeCollectionLink(Document doc) throws Exception {
		List<Node> resNodes = getCollectionResourceNodes (doc);
		for (Node resNode : resNodes) {
			String resNodeName = resNode.getName();
			if(resNodeName.equals("rdf:about")) {
				resNode.getParent().getParent().detach();
			} else {
				resNode.getParent().detach();
			}
		}
	}
	
	private void updateCollectionLink(Document doc, String collectionType) throws Exception {
		List<Node> resNodes = getCollectionResourceNodes (doc);
		for (Node resNode : resNodes) {
			String resNodeName = resNode.getName();
			int idx = collectionType.indexOf(":");
			String collPredicateName = "";
			
			// Setting specific new collection predicate for the collection to merge.
			if (idx > 0) { 
				collPredicateName = collectionType.substring(collectionType.indexOf(":") + 1);
				collPredicateName = collectionType.substring(0, collectionType.indexOf(":") + 1)
						+ collectionType.substring(0, 1).toLowerCase() + collectionType.substring(1);
				((Element)resNode.getParent()).setName(collPredicateName);
				resNode.setText(resNode.getStringValue().replace(collectionId, collectionToMerge));
			}else{
				collPredicateName = collectionType.substring(0, 1).toLowerCase() + collectionType.substring(1);
				Element elem = ((Element)resNode.getParent().getParent()).addElement(
						new QName(collPredicateName, new Namespace("dams", Constants.NS_PREFIX_MAP.get("dams"))));
				elem.addAttribute(new QName("resource", 
						new Namespace("rdf", Constants.NS_PREFIX_MAP.get("rdf"))), collectionToMerge);
				
				resNode.getParent().detach();
			}
		}
	}
	
	private List<Node> getCollectionResourceNodes (Document doc){
		List<Node> results = new ArrayList<Node>();
		// Lookup the collections link/Collection elements: rdf:resource reference or a Collection element
		List<Node> nodes = doc.selectNodes("//dams:Object/*[contains(local-name(), 'Collection') | local-name()='collection']");
		for (Node node : nodes) {
			Node resNode = node.selectSingleNode("@rdf:resource");
			if (resNode != null) {
				String url = resNode.getStringValue();
				if (url.endsWith(collectionId)) {
					results.add(resNode);
				}
			}
		}
		return results;
	}

	/**
	 * Execution result message
	 */
	public String getExeInfo() {
		if(exeResult){
			if (releaseOption.equals(RELEASE_MERGE)){
				exeReport.append("Successfully merged collection '" + collectionTitle + "': \n ");
			}else if (releaseOption.equals(RELEASE_ONE_OFFS)){
				exeReport.append("Successfully released collection '" + collectionTitle + "' as One-Offs: \n ");;
			}else
				exeReport.append("Successfully released collection '" + collectionTitle + "': \n ");
		}else
			exeReport.append("Object release failed (" + failedCount + " of " + count + " failed): \n ");	
		exeReport.append("Total items found " + itemsCount + ", number of items processed " + count + ".\n");
		if(tsUpdateFailed.length() > 0)
			exeReport.append("Release records failed: \n" + tsUpdateFailed.toString() + ".\n");
		if(solrUpdateFailed.length() > 0)
			exeReport.append("Release SOLR update failed: \n" + solrUpdateFailed.toString() + ".\n");
		String exeInfo = exeReport.toString();
		log("log", exeInfo);
		return exeInfo;
	}
}
