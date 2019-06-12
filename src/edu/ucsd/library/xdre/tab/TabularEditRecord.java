package edu.ucsd.library.xdre.tab;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Element;
import org.dom4j.Node;

import edu.ucsd.library.xdre.utils.Constants;


/**
 * Tabular data consisting of a key-value map for the record, and 0 or more
 * components (also key-value maps) for editing.
 * @author lsitu
**/
public class TabularEditRecord extends TabularRecord
{
    public static String[] NONE_DESCRIPTIVE_NODES = {"hasFile", "hasComponent"};

    private Document document = null;

    /**
     * Create an empty record.
    **/
    public TabularEditRecord() {
        this( null );
    }

    /**
     * Create a record with record-level data, but no component-level data.
    **/
    public TabularEditRecord(Map<String,String> data) {
        super( data );
    }

    /**
     * Create a record with both record-level and component-level data.
     * @throws DocumentException 
     * @throws IOException 
     * @throws UnsupportedEncodingException 
    **/
    public TabularEditRecord( Map<String,String> data, ArrayList<TabularRecord> cmp, Document document )
            throws UnsupportedEncodingException, IOException, DocumentException
    {
        super( data, cmp );
        editDocument(document);
    }

    public void editDocument(Document document) throws UnsupportedEncodingException, IOException, DocumentException {
        this.document = document;
    }

    /**
     * Convert the record to RDF/XML.
     * @throws Exception 
    **/
    public Document toRDFXML() throws Exception
    {
        return editDocument();
    }

    /**
     * Edit the document with the excel data provided 
     * @return
     * @throws Exception 
     */
    protected Document editDocument() throws Exception {
        overlayElements(data);

        return document;
    }

    /**
     * Convert ark value to ARK URL. 
     * @param value
     * @return
     */
    public static String getArkUrl(String value) {
        String arkValue = value;

        if (arkValue.startsWith(Constants.DAMS_ARK_URL_BASE)) {
            return value;
        } else {
            // Convert ARK to dams4 ARK url
            return toArkUrl(arkValue);
        }
    }

    /*
     * Convert ark to dams4 ARK url
     * @param ark
     * @return
     */
    private static String toArkUrl(String ark) {
        if (!(ark.startsWith("http://") || ark.startsWith("https://"))) {
            String arkUrlBase = Constants.DAMS_ARK_URL_BASE;
            arkUrlBase += arkUrlBase.endsWith("/") ? "" : "/";
            return arkUrlBase + "20775/" + ark;
        }
        return ark;
    }

    /*
     * Edit: Overlay fields/elements
     * @param data
     */
    private void overlayElements(Map<String,String> data) throws Exception {
        // Object, CLR node to edit
        String objectID = data.get("object unique id");
        Element e = (Element)document.selectSingleNode("//*[@rdf:about='" + getArkUrl(objectID) + "']");

        boolean copyrightProcessed = false;
        boolean otherRightsProcessed = false;
        boolean licenseProcessed = false;

        // First Step: delete all descriptive metadata in the object/component
        deleteDescriptiveMetadata(e);

        // Second Step: add/rebuild descriptive metadata
        for (String key : data.keySet()) {
            // Handle common descriptive metadata
            addCommonDescriptiveData(e, objectID, key);

            // subjects, including FAST subjects ////////////////////////////////////////////
            // data, elem, header, class/ns, predicate/ns, element
            if (key.startsWith("subject:")) {

                // predicate: need to remove fast heading sufix from header
                String predicateName = getPredicateName(key.substring(key.lastIndexOf(":") + 1)).replace(" fast", "");
                String elemName = predicateName.substring(0, 1).toUpperCase() + predicateName.substring(1);
                for (String value : split(data.get(key))) {
                    handleSubject(e, predicateName, elemName, value);
                }
            }

            // Copyright ////////////////////////////////////////////////////////////////////
            if (key.startsWith("copyright") && !copyrightProcessed) {
                copyrightProcessed = true;

                addCopyright(e);
            }

            // RightsHolder ////////////////////////////////////////////////////////////////////////
            if (key.startsWith("rightsholder")) {
                for ( String value : split(data.get(key)) ) {
                    String copyrightStatus = key.endsWith("personal") ? RecordUtil.copyrightPerson
                            : key.endsWith("corporate") ? RecordUtil.copyrightCorporate : RecordUtil.copyrightOther;

                    RecordUtil.addRightsHolder( e, copyrightStatus, value);
                }
            }

            // OtherRights
            if (key.startsWith("otherrights") && !otherRightsProcessed) {
                otherRightsProcessed = true;

                String basis = data.get("otherrights:otherrightsbasis");
                String note = data.get("otherrights:otherrightsnote");
                String permission = data.get("otherrights:permission/type");
                String restriction = data.get("otherrights:restriction/type");

                RecordUtil.addOtherRights(e, basis, note, permission, restriction );
            }

            // License
            if (key.startsWith("license:") && !licenseProcessed) {
                licenseProcessed = true;

                String permission = data.get("license:permission/type");
                String restriction = data.get("license:restriction/type");
                String beginDate = data.get("license:begindate");
                String endDate = data.get("license:enddate");
                String note = data.get("license:licensenote");
                String licenseURI = data.get("license:licenseuri");

                RecordUtil.addLicense(e, note, permission, restriction, beginDate, endDate, licenseURI);
            }

            // collections //////////////////////////////////////////////////////////////////
            if (key.startsWith("collection(s)")) {
                for ( String value : split(data.get(key)) ) {
                    // add collection linkings
                    // Todo: correct predicate and collection name basing on collection type.
                    addCollectionElement(e, "collection", "Collection", value);
                }
            }

            // Handle collection specific headers ///////////////////////////////////////////
            // CLR Brief Description (ScopeAndContentNote) //////////////////////////////////
            if ( key.equalsIgnoreCase("brief description") ) {
                if ( pop(key) ) {
                    addScopeContentNote(e, data.get(key));
                }
            }

            // Collection image related resource ////////////////////////////////////////////
            if ( key.equalsIgnoreCase("clr image file name") ) {
                // add: dams:RelatedResource[dams:type='thumbnail']
                String clrImage = data.get("clr image file name");
                if ( pop(clrImage) ) {
                    addRelatedResource(e, "thumbnail", "@ " + clrImage);
                }
            }
        }
    }

    /**
     * Delete all descriptive metadata form object/component
     * @param e
     */
    private void deleteDescriptiveMetadata(Element e) {
        Iterator<Node> nodeIterdor = e.nodeIterator();
        List<String> nodesToKeep = Arrays.asList(NONE_DESCRIPTIVE_NODES);
        List<Node> nodesToDelete = new ArrayList<>();
        while (nodeIterdor.hasNext()) {
            Node node = nodeIterdor.next();
            if (!nodesToKeep.contains(node.getName())) {
                nodesToDelete.add(node);
            }
        }

        for (Node node : nodesToDelete) {
            node.detach();
        }
    }
}
