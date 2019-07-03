package edu.ucsd.library.xdre.tab;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
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
     * Edit the document with the excel data provided for object and component/sub-component metadata
     * @return
     * @throws Exception 
     */
    public Document editDocument() throws Exception {
        overlayElements();

        if (cmp.size() > 0) {
            for (TabularRecord c : cmp) {
                TabularEditRecord comonent = (TabularEditRecord)c;
                comonent.setDocument(document);
                comonent.editDocument();
            }
        }

        return document;
    }

    /*
     * Add rdf:recource attribute for resource reference.
     * @param e
     * @param propertyName
     * @param value
     * @return
     */
    private boolean addResourceReference(Element e, String propertyName, String value) {
        String arkUrl = getLinkedArkUrl(value);
        if (StringUtils.isNotBlank(arkUrl)) {
            Element el = addElement( e, propertyName, damsNS);
            addAttribute( el, "resource", rdfNS, arkUrl );
            return true;
        }
        return false;
    }

    /*
     * Add rdf:resource reference for relationship
     * @param e
     * @param propertyName
     * @param value
     * @param role
     * @return
     */
    protected boolean addRelationshipReference(Element e, String propertyName, String value, String role) {
        String arkUrl = getLinkedArkUrl(value);
        if (StringUtils.isNotBlank(arkUrl)) {
            Element rel = addElement( e, "relationship", damsNS, "Relationship", damsNS );

            // add role
            addRole(rel, role);
            // add resource reference
            Element el = addElement( rel, propertyName, damsNS);
            addAttribute( el, "resource", rdfNS, arkUrl );
            return true;
        }
        return false;
    }

    /**
     * Get object RDF/XML
     * @return
     */
    public Document getDocument() {
        return document;
    }

    /**
     * Set object RDF/XML
     * @param document
     */
    public void setDocument(Document document) {
        this.document = document;
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

    /**
     * Convert ark value to ARK URL. 
     * @param value
     * @return
     */
    public static String getLinkedArkUrl(String value) {
        String arkValue = value;

        if (arkValue.startsWith(Constants.DAMS_ARK_URL_BASE)) {
            return value;
        } else if (arkValue.indexOf("@") >= 0) {
            // Convert ARK to dams4 ARK url
            String[] pairs = arkValue.split("\\@");
            String ark = (pairs.length == 1 ? pairs[0] : pairs[pairs.length - 1]);
            if (ark.trim().length() == 10) {
                return toArkUrl(ark.trim());
            }
        }
        return null;
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
            return arkUrlBase + Constants.ARK_ORG + "/" + ark;
        }
        return ark;
    }

    /*
     * Edit: Overlay fields/elements
     * @param data
     */
    public void overlayElements() throws Exception {
        // Object, CLR node to edit
        String oid = data.get("object unique id");
        Element e = (Element)document.selectSingleNode("//*[@rdf:about='" + getArkUrl(oid) + "']");
        titleProcessed = false;
        cartographicsProcessed = false;
        fileProcessed = false;
        boolean copyrightProcessed = false;
        boolean otherRightsProcessed = false;
        boolean licenseProcessed = false;

        // First Step: delete all descriptive metadata in the object/component
        deleteDescriptiveMetadata(e);

        // Second Step: add/rebuild descriptive metadata
        for (String key : data.keySet()) {
            // descriptive metadata
            addCommonDescriptiveData(e, oid, key);

            // heading fields that may be linking to other resources
            addHeadingsField(e, oid, key);

            // subjects, including FAST subjects ////////////////////////////////////////////
            // data, elem, header, class/ns, predicate/ns, element
            if (key.startsWith("subject:")) {

                // predicate: need to remove fast heading sufix from header
                String predicateName = getPredicateName(key.substring(key.lastIndexOf(":") + 1)).replace(" fast", "");
                String elemName = predicateName.substring(0, 1).toUpperCase() + predicateName.substring(1);
                for (String value : split(data.get(key))) {
                    if (!addResourceReference(e, predicateName, value)) {
                        handleSubject(e, predicateName, elemName, value);
                    }
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

                    String predicateName = RecordUtil.getRightsHolderPredicate(copyrightStatus);
                    if (!addResourceReference(e, predicateName, value)) {
                        RecordUtil.addRightsHolder( e, copyrightStatus, value);
                    }
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
                    String predicateName = "collection";
                    if (!addResourceReference(e, predicateName, value)) {
                        addCollectionElement(e, predicateName, "Collection", value);
                    }
                }
            }

            // File: file use  //////////////////////////////////////////////////////////////
            if (key.equalsIgnoreCase("file use") || key.equalsIgnoreCase("file use 2")) {
                if (pop(key)) {
                    // Mapped file use to dams:use for master dams:File the alternate dams:File
                    String fileNamePattern = key.endsWith("2") ? "/2." : "/1.";
                    Node fileNode = e.selectSingleNode("dams:hasFile/dams:File[contains(@rdf:about, '" + fileNamePattern + "')]");
                    if (fileNode != null) {
                        // replace the file use value
                        Node elNode = fileNode.selectSingleNode("dams:use");
                        if (elNode != null)
                            elNode.setText(data.get(key));
                        else {
                            // add the file use element if missing
                            addElement((Element)fileNode, "use", damsNS).setText(data.get(key));
                        }
                    }
                }
            }
        }
    }

    /**
     * Fields that may be linking to other resources.
     * @param e
     * @param objectID
     * @param key
     * @throws ParseException
     */
    protected void addHeadingsField(Element e, String objectID, String key)
            throws ParseException {
        // relationships ////////////////////////////////////////////////////////////////
        // data, elem, header, class/ns, pred/ns, name element
        // personal name
        if (key.startsWith("person:")) {
            String role = key.substring(key.lastIndexOf(":") + 1);

            // add: Relationship by name and role
            for ( String value : split(data.get(key)) ) {
                String predicateName = "personalName";
                if (!addRelationshipReference(e, predicateName, value, role)) {
                    addRelationship(e, "PersonalName", role, predicateName, "FullName", value);
                }
            }
        }

        // corporate name
        if (key.startsWith("corporate:")) {
            String role = key.substring(key.lastIndexOf(":") + 1);

            for ( String value : split(data.get(key)) ) {
                String predicateName = "corporateName";
                if (!addRelationshipReference(e, predicateName, value, role)) {
                    addRelationship(e, "CorporateName", role, predicateName, "Name", value);
                }
            }
        }

        // language /////////////////////////////////////////////////////////////////////
        if (key.startsWith("language")) {
            for(String value : split(data.get(key))) {
                if (!addResourceReference(e, "language", value)) {
                    addLanguage(e, value);
                }
            }
        }

        // related resource /////////////////////////////////////////////////////////////
        if (key.startsWith("related resource:")) {
            String type = key.substring(key.lastIndexOf(":") + 1);
            for (String value : split(data.get( key ))) {
                if (!addResourceReference(e, "relatedResource", value)) {
                    addRelatedResource(e, type, value);
                }
            }
        }

        // notes /////////////////////////////////////////////////////////////////////////
        if ( key.startsWith("note:") ) {
            String type = key.substring( key.indexOf(":") + 1 );
            for ( String value : split(data.get( key )) )
            {
                // note could be linked resource reference
                if (!addResourceReference(e, "note", value)) {
                    if (key.endsWith("local attribution")) {
                        addNote(e, type, "digital object made available by", value);
                    } else {
                        addNote(e, type, null, value);
                    }
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
