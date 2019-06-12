package edu.ucsd.library.xdre.tab;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.Node;
import org.dom4j.QName;

import edu.ucsd.library.xdre.collection.CollectionHandler;
import edu.ucsd.library.xdre.model.DAMSCollection;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.ImageMagick;

/**
 * Standard InputStreamRecord constructed from the Excel form, AT/Roger form etc.
 * @author lsitu
 * Since Feb. 4, 2015 
 */
public class InputStreamRecord implements Record {
    protected String id;
    protected Document rdf;

    protected DAMSClient damsClient = null;
    public InputStreamRecord() {}

	public InputStreamRecord(Record record, Map<String, String> collections, String unit, 
			String copyrightStatus, String copyrightJurisdiction, String[] copyrightOwner,
			String program, String access, String beginDate, String endDate) throws Exception {
		this.id = record.recordID();
		this.rdf = record.toRDFXML();
		
		//Assign component ID and file ID for symbols CID, FID 
		assignIDs();
		RecordUtil.addRights(rdf, unit, collections, copyrightStatus, copyrightJurisdiction, 
				copyrightOwner, program, access, beginDate, endDate);
	}

	public InputStreamRecord(Record record, DAMSClient damsClient) {
		this.id = record.recordID();
		this.damsClient = damsClient;
	}

	/**
	 * Constructor for Collection Input Stream with fields
	 */
	public InputStreamRecord(Record record, String id, String type, Map<String, String> collections, String unit, 
			String visibility) throws Exception {
		this.id = id;
		this.rdf = record.toRDFXML();
		Node node = rdf.selectSingleNode("//*[name()='dams:Object']");
		node.setName("dams:" + type);
		if (StringUtils.isNotBlank(id))
			node.selectSingleNode("@rdf:about").setText(id);
		
		addFields((Element)node, collections, unit, visibility);
	}

	@Override
	public String recordID() {
		return id;
	}

	@Override
	public Document toRDFXML() throws ParseException {
		return rdf;
	}
	
	/*
	 * Assign component ID and file ID.
	 */
	private void assignIDs() {
		int comSize = rdf.selectNodes("//dams:Component/@rdf:about").size();
		List<Node> nodes = rdf.selectNodes("//dams:Component/@rdf:about[contains(., '/CID')]");
		int cid = comSize - nodes.size() + 1;
		for (int j=0; j<nodes.size();j++) {
			Node rdfAbout = nodes.get(j);
			//Assigned CID for components: http://library.ucsd.edu/ark:/20775/OID/CID
			rdfAbout.setText(rdfAbout.getStringValue().replace("/CID", "/" + cid));
			Node cNode = rdfAbout.getParent();
			//Assigned FID for files
			List<Node> fNodes = cNode.selectNodes("dams:hasFile/dams:File/@rdf:about");
			for (int k=0; k<fNodes.size(); k++) {
				Node fidNode = fNodes.get(k);
				String fid = fidNode.getStringValue();
				if (fid.endsWith("/FID")) {
					String fileExt = "";
					Node fileNameNode = fidNode.getParent().selectSingleNode("dams:sourceFileName");
					if (fileNameNode != null) {
						String fileName = fileNameNode.getText().trim();
						if (fileName.indexOf(".") > 0) {
							fileExt = fileName.substring(fileName.indexOf("."));
						}
					}
					
					// $oid/CID/FID | $oid/FID
					if (fid.indexOf("/CID") > 0 && fid.indexOf("/FID") > 0) {
						fid = fid.replace("/CID", "/" + cid + "").replace("/FID", "/" + (k + 1) + fileExt);
					} else {
						fid = fid.replace("/FID", "/" + cid + "/" + (k + 1) + fileExt);
					}
					fidNode.setText(fid);
				}
			}

			cid += 1;
		}
		
		// assigned file id for simple files
		List<Node> fNodes = rdf.selectNodes("//dams:hasFile/dams:File/@rdf:about[contains(., '/FID')]");
		for (int k=0; k<fNodes.size(); k++) {
			Node fidNode = fNodes.get(k);
			String fid = fidNode.getStringValue();
			String fileExt = "";
			Node fileNameNode = fidNode.getParent().selectSingleNode("dams:sourceFileName");
			if (fileNameNode != null) {
				String fileName = fileNameNode.getText().trim();
				if (fileName.indexOf(".") > 0) {
					fileExt = fileName.substring(fileName.indexOf("."));
				}
			}
			fid = fid.replace("/FID", "/" + (k + 1) + fileExt);			
			fidNode.setText(fid);
		}
	}
	
	/**
	 * Adding master file(s) for bib/Roger records: a PDF, a TIFF, or a PDF + a ZIP file 
	 * @param cid
	 * @param files
	 * @param fileUseMap
	 */
	public void addFiles(int cid, List<File> files, Map<String, String> fileUseMap) {
		Element o = (Element) rdf.selectSingleNode("//dams:Object");
		String objUri = o.selectSingleNode("@rdf:about").getStringValue();
		for (int i = 0; i< files.size(); i++) {
			String fileName = files.get(i).getName();
			String fileUse = fileUseMap.get(fileName);
			int idx = fileName.indexOf(".");
			String fileUri = objUri + "/" + (cid != 0 ? cid + "/" : "") + (i + 1) + (idx > 0 ? fileName.substring(fileName.indexOf(".")) : "");
			Element f = o.addElement("dams:hasFile").addElement("dams:File");
			f.addAttribute(new QName("about",  rdf.getRootElement().getNamespaceForPrefix("rdf")), fileUri);
			f.addElement("dams:sourceFileName").setText(fileName);
			if (!StringUtils.isBlank(fileUse)) {
				f.addElement("dams:use").setText(fileUse);
			}
		}
	}

	private void addFields(Element p, Map<String, String> collections, 
			String unitUri, String visibility)
	{
		Namespace rdfNS = rdf.getRootElement().getNamespaceForPrefix("rdf");
		// unit
		p.addElement("dams:unit").addAttribute(new QName("resource",  rdfNS), unitUri);
		// visibility
		p.addElement("dams:visibility").setText(visibility);
        // parent collections
        if (collections != null && collections.size() > 0) {
	        for ( Iterator<String> it = collections.keySet().iterator(); it.hasNext(); )
	        {
	            String uri = it.next();
	            String collType = collections.get(uri);
	            String collPredicate = StringUtils.isNotBlank(collType) ? collType.substring(0, 1).toLowerCase() + collType.substring(1) : "collection";
	            p.addElement("dams:" + collPredicate).addAttribute(new QName("resource",  rdfNS), uri);
	        }
        }
	}

    private void addTitle( Element e, String title)
    {
    	Namespace rdfNS = rdf.getRootElement().getNamespaceForPrefix("rdf");
		Element titleElem = e.addElement("dams:title").addElement("mads:Title");
		titleElem.addElement("mads:authoritativeLabel").setText(title);
		Element elemList = titleElem.addElement("mads:elementList");
		elemList.addAttribute(new QName("parseType",  rdfNS), "Collection");
		elemList.addElement("mads:MainTitleElement").addElement("mads:elementValue").setText(title);
    }

    /**
	 * Ingest the collection image
	 * @param files
	 * @throws Exception
	 */
	public void ingestCollectionImage(String[] filesPaths) throws Exception {
		Map<String, File> filesMap = new HashMap<>();
		if(filesPaths != null){
			File file = null;
			// List the source files
			for(int i=0; i<filesPaths.length; i++){
				file = new File(filesPaths[i]);
				if(file.exists()){
					CollectionHandler.listFile(filesMap, file);
				}
			}
		}
		String collectionArk = id.substring(id.lastIndexOf("/"), id.length());
		// collection thumbnail file in relatedResource
		Node thumbUriNode = rdf.selectSingleNode("//dams:RelatedResource[dams:type='thumbnail']/dams:uri/@rdf:resource");
		if (thumbUriNode != null) {

			String thumbnailUri = thumbUriNode.getStringValue();

			File srcFile = filesMap.get(thumbnailUri);

			if (srcFile != null) {
				File storedSrcFile = new File (Constants.DAMS_CLR_SOURCE_DIR, 
						collectionArk + (thumbnailUri.lastIndexOf(".") > 0 ? thumbnailUri.substring(thumbnailUri.lastIndexOf(".")) : ""));
				File thumbnailFile = new File (Constants.DAMS_CLR_THUMBNAILS_DIR, collectionArk + ".jpg");
				File imgFile = new File (Constants.DAMS_CLR_IMG_DIR, collectionArk + ".jpg");

				if (!storedSrcFile.getParentFile().exists())
					 storedSrcFile.getParentFile().mkdirs();
				if (!thumbnailFile.getParentFile().exists())
					thumbnailFile.getParentFile().mkdirs();
				if (!imgFile.getParentFile().exists())
					imgFile.getParentFile().mkdirs();

				// cope the image source file for local storage
				copyFile (srcFile,  storedSrcFile);

				// create derivatives
				boolean thumbSuccessful = false;
				boolean imgSuccessful = false;
				ImageMagick imageMagick = new ImageMagick(Constants.IMAGEMAGICK_COMMAND);
				try {
					String[] devrSize = Constants.COLLECTION_THUMBNAILS_SIZE.split("x");
					thumbSuccessful = imageMagick.makeDerivative(storedSrcFile, thumbnailFile, 
							Integer.parseInt(devrSize[0].trim()), Integer.parseInt(devrSize[1].trim()), -1, Constants.IMAGEMAGICK_PARAMS);
					devrSize = Constants.COLLECTION_IMAGE_SIZE.split("x");
					imgSuccessful = imageMagick.makeDerivative(storedSrcFile, imgFile, 
							Integer.parseInt(devrSize[0].trim()), Integer.parseInt(devrSize[1].trim()), -1, Constants.IMAGEMAGICK_PARAMS);
				} catch (Exception e) {
					throw new Exception(e.getMessage());
				}
				if (thumbSuccessful && imgSuccessful) {
					//Update the thumbnail url
					thumbUriNode.setText(Constants.DAMS_CLR_URL_BASE + "/" + thumbnailFile.getName());
				} else
					throw new Exception ("Failed to create derivative for the collection image.");
			} else if (thumbnailUri.startsWith("http")) {
				//rewrite it to thumbnail ark url
				String ark = thumbnailUri.substring(thumbnailUri.lastIndexOf("/") + 1);
				if (ark.indexOf(".") < 0) {
					if (ark.length() == 10) {
						thumbUriNode.setText(Constants.DAMS_ARK_URL_BASE + "/" + Constants.ARK_ORG + "/"  + ark + "/4.jpg");
					} else
						throw new Exception("Invalid object url: " + thumbnailUri);
				}
			} else
				throw new Exception ("Collection image is not found: " + thumbnailUri);
		}
	}

	public static void updateCollectionHierarchy (DAMSClient damsClient, DAMSCollection oCollection, DAMSCollection collection) throws Exception {
		// update parent linking
		String parentId = collection == null ? null : collection.getParent();
		String oParentId = oCollection == null ? null : oCollection.getParent();
		if (collection != null && !collection.isEquals(parentId, oParentId) ) {
        	
        	// remove the linkings related the old parent record
        	if ( StringUtils.isNotBlank(oParentId)) {
	        	Node oParentRecord = damsClient.getRecord(oParentId)
	        			.selectSingleNode("//*[contains(@rdf:about, '" + oParentId + "')]");
	        	// remove the linking from the original parent record
		    	List<Node> resNodes = oParentRecord.selectNodes("//*[contains(@rdf:resource, '" + oCollection.getId() + "')]");
		    	for (Node resNode : resNodes) {
		    		resNode.detach();
		    	}
		    	// Removed the extent note when saving the collection record
		    	removeExtentNote(oParentRecord.getDocument());
		    	if (!damsClient.updateObject(oParentId, oParentRecord.getDocument().asXML(), Constants.IMPORT_MODE_ALL))
		    		throw new Exception("Failed to update parent collection for linking: " + oParentId);
		    		
        	}
			
        	// add linkings related to the new parent
        	if ( StringUtils.isNotBlank(parentId) ) { 
	        	Document docParent = damsClient.getRecord(parentId);
	        	// child linking in the new parent collection 
	        	Node collNode = docParent.selectSingleNode("//*[contains(@rdf:about, '" + parentId + "')]");
	        	((Element)collNode).addElement("dams:has" + collection.getType().replace("ProvenanceCollectionPart", "Part"))
	        			.addAttribute(new QName("resource", docParent.getRootElement().getNamespaceForPrefix("rdf")), collection.getId());

	        	// Removed the extent note when saving the collection record
	        	removeExtentNote(docParent);
		    	if (!damsClient.updateObject(parentId, docParent.asXML(), Constants.IMPORT_MODE_ALL))
		    		throw new Exception("Failed to update parent collection for linking: " + parentId);
        	}
        }
        
	}

	private static void removeExtentNote(Document doc){
    	List<Node> nodes = doc.selectNodes("//dams:note/dams:Note[dams:type='extent' and (substring-after(rdf:value, ' ')='digital objects.' or rdf:value='1 digital object.')]");
    	for (Node node : nodes)
    		node.getParent().detach();
	}
	/**
	 * Copy file from source to destination
	 * @param source
	 * @param dest
	 * @throws IOException 
	 */
	public static void copyFile (File source, File dest) throws IOException {
		FileInputStream in = null;
		FileOutputStream out = null;
		
		try {
			in = new FileInputStream(source);
			out = new FileOutputStream(dest);
			
			copy (in, out);
		} finally {
			close (in);
			close (out);
		}
	}

	/**
	 * Copy file from source to destination
	 * @param source
	 * @param dest
	 * @throws IOException 
	 */
	public static void copy (InputStream in, OutputStream out) throws IOException {
		int ch;
		while ((ch=in.read()) != -1) {
			out.write(ch);
		}
	}

	/**
	 * Utility function to close a stream
	 * @param closeable
	 */
	public static void close (Closeable closeable) {
		if (closeable != null) {
			try{
				closeable.close();
			}catch (Exception e) {
			}
		}
	}
}
