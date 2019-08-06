package edu.ucsd.library.xdre.tab;

import java.text.DateFormat;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;

import edu.ucsd.library.xdre.collection.CollectionHandler;

/**
 * A bundle of tabular data, consisting of a key-value map for the record, and 0 or more
 * components (also key-value maps).
 * @author lsitu
 * @author escowles
 * @since 2014-06-05
**/
public class TabularRecord extends TabularRecordBasic
{
    public static final String COPYRIGHT_JURISDICTION = "copyrightJurisdiction";
    public static final String COPYRIGHT_STATUS = "copyrightStatus";
    public static final String COPYRIGHT_PURPOSE_NOTE = "copyrightPurposeNote";
    public static final String COPYRIGHT_NOTE = "copyrightNote";

    protected List<TabularRecord> cmp;
    protected int cmpCounter = 0;

    protected boolean titleProcessed = false;
    protected boolean cartographicsProcessed = false;
    protected boolean fileProcessed = false;
    protected boolean copyrightProcessed = false;

    // flag for watermarking
    private boolean watermarking = false;

    /**
     * Create an empty record.
    **/
    public TabularRecord()
    {
        this( null, null );
    }

    /**
     * Create a record with record-level data, but no component-level data.
    **/
    public TabularRecord( Map<String,String> data )
    {
        this( data, null );
    }

    /**
     * Create a record with both record-level and component-level data.
    **/
    public TabularRecord( Map<String,String> data, ArrayList<TabularRecord> cmp )
    {
        super(data);
        this.cmp = (cmp != null) ? cmp : new ArrayList<TabularRecord>();
    }

    /**
     * Get the watermarking flag
     */
    public boolean isWatermarking() {
        return watermarking;
    }

    /**
     * Set the watermarking flag
     * @param watermarking
     */
    public void setWatermarking(boolean watermarking) {
        this.watermarking = watermarking;
    }

    /**
     * Set the record-level key-value map.
    **/
    public void setData( Map<String,String> data )
    {
        this.data = data;
    }

    /**
     * Get the record-level key-value map.
    **/
    public Map<String,String> getData()
    {
        return data;
    }

    /**
     * Add component-level data.
    **/
    public void addComponent( TabularRecord component )
    {
        cmp.add(component);
    }
    
    /**
     * Set the component-level data.
    **/
    public void setComponents( List<TabularRecord> cmp )
    {
        this.cmp = cmp;
    }

    /**
     * Get the component-level data.
    **/
    public List<TabularRecord> getComponents()
    {
        return cmp;
    }

    /**
     * Convert the record to RDF/XML.
     * @throws Exception 
    **/
    public Document toRDFXML() throws Exception
    {
        // setup object
    	Document doc = new DocumentFactory().createDocument();
    	Element rdf = createRdfRoot (doc);

        Element root = addElement(rdf,"Object",damsNS);

        // get previously-assigned ark
        String ark = data.get("ark");

        if (StringUtils.isBlank(ark)) {
            ark = recordID();
        }

        // object metadata
        addFields( root, 0, ark );

        // component metadata
        serializeComponents (root, cmp, ark);

        return rdf.getDocument();
    }

    private void serializeComponents (Element parent, List<TabularRecord> cmps, String ark) throws Exception {
        for ( int i = 0; i < cmps.size(); i++ )
        {
        	TabularRecord component = cmps.get(i);
            Element e = addElement(parent, "hasComponent", damsNS, "Component", damsNS);
            addTextElement( e, "order", damsNS, "" + (i + 1));
            component.addFields(e, ++cmpCounter, ark); // 1-based component ids
            List<TabularRecord> subCmps = component.getComponents();
            // sub-component metadata
            serializeComponents (e, subCmps, ark);
        }
    }

    /**
     * The meat of the metadata processing -- a long sequence of categories of metadata
     * fields with the key mapping between field names and dams4 structure
     * @throws Exception 
    **/
    private void addFields( Element e, int cmp, String ark ) throws Exception
    {
        titleProcessed = false;
        cartographicsProcessed = false;
        fileProcessed = false;
        copyrightProcessed = false;

        String objectID = data.get("object unique id");
        if ( ark == null ) { ark = "ARK"; }
        String id = (cmp > 0) ? ark + "/" + cmp : ark;
        String fileID = id + "/1";
        addAttribute(e, "about", rdfNS, id);

        for (String key : data.keySet()) {
            // Handle common descriptive metadata
            addCommonDescriptiveData(e, objectID, key);

            // Headings fields that may be linking to other resources
            addHeadingsField(e, objectID, key);

            // Subjects: mads, dams /////////////////////////////////////////////////////////
            if (key.startsWith("subject:")) {
                // predicate for the subject
                String predicateName = getPredicateName(key.substring(key.lastIndexOf(":") + 1));
                String elemName = predicateName.substring(0, 1).toUpperCase() + predicateName.substring(1);
                for (String value : split(data.get(key))) {
                    handleSubject(e, predicateName, elemName, value);
                }
            }

            // files ////////////////////////////////////////////////////////////////////////
            if ( key.equalsIgnoreCase("file name") && pop(key) )
            {
                String fn = data.get("file name");
                String use = data.get("file use");
                String file1Id = fileID;
                if (watermarking && CollectionHandler.isDocument(fn, use)
                        && use.toLowerCase().contains("source")) {
                    // PDF source file that need watermarking will be stored as the second file
                    file1Id = getSecondFileID (fileID);
                }

                addFile (e, file1Id, fn, use);
            }

            if ( key.equalsIgnoreCase("file name 2") && pop(key) )
            {
                String fn = data.get("file name 2");
                String use = data.get("file use 2");
                String fileID2 = getSecondFileID (fileID);

                addFile (e, fileID2, fn, use);
            }
        }
    }

    /**
     * Fields that are not linking to other resources.
     * @param e
     * @param objectID
     * @param key
     * @throws ParseException
     */
    protected void addCommonDescriptiveData(Element e, String objectID, String key)
            throws ParseException {
        // typeOfResource ///////////////////////////////////////////////////////////////
        if (key.equalsIgnoreCase("type of resource")) {
            for ( String value : split(data.get(key)) ) {
                if (pop(value)) {
                    addTextElement(e, "typeOfResource", damsNS, value);
                }
            }
        }

        // title ////////////////////////////////////////////////////////////////////////
        if ((key.equalsIgnoreCase("title")
                || key.startsWith("subtitle")
                || key.startsWith("part name")
                || key.startsWith("part number")
                || key.startsWith("translation")
                || key.startsWith("variant")) && !titleProcessed) {
            titleProcessed = true;

            String main  = data.get("title");
            String sub   = data.get("subtitle");
            String ptNam = data.get("part name");
            String ptNum = data.get("part number");
            String trans = data.get("translation");
            String var   = data.get("variant");
            addTitle( e, main, sub, ptNam, ptNum, trans, var );
        }

        // date /////////////////////////////////////////////////////////////////////////
        if (key.startsWith("date:")) {
            String type = key.substring(key.indexOf(":") + 1);
            String date = data.get(key);

            if (key.equalsIgnoreCase("date:creation")) {
                // date:creation with begin/end date if provided
                String begin = data.get("begin date");
                String end   = data.get("end date");
                testDateValue ( objectID, begin, "begin date" );
                testDateValue ( objectID, end, "end date" );

                addDate(e, type, "w3cdtf", date, begin, end);
            } else if (!key.equalsIgnoreCase("date:creation")) {
                // add: other qualified date types
                addDate(e, type, null, date, null, null);
            }
        }

        // identifier ///////////////////////////////////////////////////////////////////
        if ( key.startsWith("identifier:") ) {
            String label = key.substring(key.indexOf(":") + 1);
            for ( String value : split(data.get( key )) )
            {
                addNote(e, "identifier", label, value);
            }
        }

        // cartographics ////////////////////////////////////////////////////////////////
        if (key.startsWith("geographic:") && !cartographicsProcessed) {
            cartographicsProcessed = true;

            addCartographics(e);
        }

        // Copyright: Added for CIL mapping of copyright note ////////////////////////////////////////////////////////////////////
        if (key.startsWith("copyright") && !copyrightProcessed) {
            copyrightProcessed = true;

            addCopyright(e);
        }

        // collection related ///////////////////////////////////////////////////////////
        // CLR Brief Description (ScopeAndContentNote) //////////////////////////////////
        if (key.equals("brief description")) {
            String briefDescription = data.get(key);
            if ( pop(briefDescription) )
            {
                addScopeContentNote(e,briefDescription);
            }
        }

        // Collection image related resource ////////////////////////////////////////////
        if (key.equals("clr image file name")) {
            String clrImage = data.get(key);
            if ( pop(clrImage) )
            {
                addRelatedResource(e, "thumbnail", "@ " + clrImage);
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
                addRelationship(e, "PersonalName", role, predicateName, "FullName", value);
            }
        }

        // corporate name
        if (key.startsWith("corporate:")) {
            String role = key.substring(key.lastIndexOf(":") + 1);

            for ( String value : split(data.get(key)) ) {
                String predicateName = "corporateName";
                addRelationship(e, "CorporateName", role, predicateName, "Name", value);
            }
        }

        // language /////////////////////////////////////////////////////////////////////
        if (key.startsWith("language")) {
            for(String value : split(data.get(key))) {
                addLanguage(e, value);
            }
        }

        // related resource /////////////////////////////////////////////////////////////
        if (key.startsWith("related resource:")) {
            String type = key.substring(key.lastIndexOf(":") + 1);
            for (String value : split(data.get( key ))) {
                addRelatedResource(e, type, value);
            }
        }

        // notes /////////////////////////////////////////////////////////////////////////
        if ( key.startsWith("note:") ) {
            String type = key.substring( key.indexOf(":") + 1 );
            for ( String value : split(data.get( key )) )
            {
                if (key.endsWith("local attribution")) {
                    addNote(e, type, "digital object made available by", value);
                } else {
                    addNote(e, type, null, value);
                }
            }
        }
    }

    protected void addFile (Element parent, String fileID, String fileName, String use)
    {
        Element f = addElement(parent,"hasFile",damsNS,"File",damsNS);
        String ext = (fileName.indexOf(".") != -1) ? fileName.substring(fileName.lastIndexOf(".")) : "";
        addAttribute( f, "about", rdfNS, fileID + ext );
        addElement(f,"sourceFileName",damsNS).setText(fileName);
        if ( pop(use) )
        {
            addElement(f,"use",damsNS).setText(use);
        }
    }

    protected String getSecondFileID (String fileID) {
    	int f2Index = 2;
    	
    	String fileID2 = fileID;
    	if (fileID.lastIndexOf("/") > 0)
    		fileID2 = fileID.substring(0, fileID.lastIndexOf("/"));
        try {
        	f2Index = Integer.parseInt(fileID.substring(fileID.lastIndexOf("/") + 1)) + 1;
        } catch (NumberFormatException ne) {}
        return fileID2 + "/" + f2Index;
    }

    /*
     * Convert subject header to predicate name
     * @return
     */
    protected String getPredicateName(String label) {
        String localName = label;
        switch(label) {
            case "genre":
                localName = "genreForm";
                break;
            case "scientific name":
                localName = "scientificName";
                break;
            case "culturalcontext":
                localName = "culturalContext";
                break;
            default:
                String elemName = toCamelCase(label);
                localName = elemName.substring(0, 1).toLowerCase() + elemName.substring(1);
                break;
        }

        return localName;
    }

    /*
     * Add dams:Date element
     * @param e
     * @param type
     * @param encoding
     * @param value
     * @param begin
     * @param end
     */
    protected void addDate(Element e, String type, String encoding, String value,
            String begin, String end) {
        Element d = addElement( e, "date", damsNS, "Date", damsNS );
        addTextElement( d, "type", damsNS, type );

        if (StringUtils.isNotBlank(encoding))
            addTextElement( d, "encoding", damsNS, encoding );

        addTextElement( d, "value", rdfNS, value );

        if (StringUtils.isNotBlank(begin))
            addTextElement( d, "beginDate", damsNS, begin );

        if (StringUtils.isNotBlank(end))
            addTextElement( d, "endDate", damsNS, end );
    }

    /*
     * Add dams:Note elements
     * @param e
     * @param type
     * @param label
     * @param values
     */
    protected void addNote(Element e, String type, String label, String value) {
        if ( !pop(value) )
            return;

        Element e2 = addElement( e, "note", damsNS, "Note", damsNS );
        addTextElement( e2, "type", damsNS, type );
        addTextElement( e2, "value", rdfNS, value );

        if (StringUtils.isNotBlank(label)) {
            addElement(e2, "displayLabel", damsNS).setText(label);
        }
    }

    /*
     * Add dams:ScopeContentNote element
     * @param e
     * @param value
     */
    protected void addScopeContentNote(Element e, String value) {
        Element e2 = addElement( e, "scopeContentNote", damsNS, "ScopeContentNote", damsNS );
        addTextElement( e2, "value", rdfNS, value );
        addElement(e2, "type", damsNS).setText("scopeAndContent");
        addElement(e2, "displayLabel", damsNS).setText("Scope and Contents");
    }

    /*
     * Add dams:RelatedResource element
     * @param e
     * @param type
     * @param values
     */
    protected void addRelatedResource(Element e, String type, String value) {
        if ( !pop(value) )
            return;

        String[] parts = value.split("@");
        String description = parts[0];
        String uri = parts.length == 2 ? parts[1] : "";

        Element rel = addElement( e,"relatedResource", damsNS, "RelatedResource", damsNS );
        addElement( rel, "type", damsNS ).setText(type);

        if ( pop(description) ) { 
            addElement(rel, "description", damsNS).setText(description.trim());
        }

        if (StringUtils.isNotBlank(uri) && uri.trim().length() > 0) {
            Element uriElem = addElement( rel, "uri", damsNS );
            addAttribute( uriElem, "resource", rdfNS, uri.trim() );
        }
    }

    /*
     * Add dams:Language element
     * @param e
     * @param values
     */
    protected void addLanguage(Element e, String value) {
        if (!pop(value))
            return;

        String[] elemValues = value.split("\\" + DELIMITER_LANG_ELEMENT);
        Element elem = addVocabElement(e,"language",damsNS,"Language",madsNS);
        addTextElement(elem,"code",madsNS,elemValues[0].trim());
        if (elemValues.length == 2) {
            addTextElement(elem,"authoritativeLabel",madsNS,elemValues[1].trim());
        }
    }

    /*
     * Add Cartographics element
     * @param e
     */
    protected void addCartographics(Element e) {
        String line  = data.get("geographic:line");
        String point = data.get("geographic:point");
        String poly  = data.get("geographic:polygon");
        String proj  = data.get("geographic:projection");
        String ref   = data.get("geographic:reference system");
        String scale = data.get("geographic:scale");
        if ( pop(line) || pop(point) || pop(poly) || pop(proj) || pop(ref) || pop(scale) )
        {
            Element cart = addElement(e,"cartographics", damsNS,"Cartographics",damsNS);
            if ( pop(line) )  { addElement(cart,"line",damsNS).setText(line);           }
            if ( pop(point) ) { addElement(cart,"point",damsNS).setText(point);         }
            if ( pop(poly) )  { addElement(cart,"polygon",damsNS).setText(poly);        }
            if ( pop(proj) )  { addElement(cart,"projection",damsNS).setText(proj);     }
            if ( pop(ref) )   { addElement(cart,"referenceSystem",damsNS).setText(ref); }
            if ( pop(scale) ) { addElement(cart,"scale",damsNS).setText(scale);         }
        }
    }

    /**
     * Add dams:Copyright
     * @param data
     * @param e
     */
    protected void addCopyright(Element e) {
        Element elem = addElement(e, "copyright", damsNS, "Copyright", damsNS);

        // add copyright elements: copyrightJurisdiction, copyrightStatus, copyrightPurposeNote, copyrightNote
        String copyrightJurisdiction = data.get(COPYRIGHT_JURISDICTION.toLowerCase());
        if (pop(copyrightJurisdiction)) {
            addTextElement(elem, COPYRIGHT_JURISDICTION, damsNS, copyrightJurisdiction);
        }

        String copyrightStatus = data.get(COPYRIGHT_STATUS.toLowerCase());
        if (pop(copyrightStatus)) {
            addTextElement(elem, COPYRIGHT_STATUS, damsNS, copyrightStatus);
        }

        String copyrightPurposeNote = data.get(COPYRIGHT_PURPOSE_NOTE.toLowerCase());
        if (pop(copyrightPurposeNote)) {
            addTextElement(elem, COPYRIGHT_PURPOSE_NOTE, damsNS, copyrightPurposeNote);
        }

        String copyrightNote = data.get(COPYRIGHT_NOTE.toLowerCase());
        if (pop(copyrightNote)) {
        addTextElement(elem, COPYRIGHT_NOTE, damsNS, copyrightNote);
        }
    }

    /*
     * Add collection linking element
     * @param e
     * @param predicate
     * @param collectionType
     * @param collectionName
     */
    protected void addCollectionElement(Element e, String predicate, String collectionType, String collectionName) {
        Element elem = addVocabElement(e, predicate, damsNS, collectionType, damsNS);
        Element el = addElement(elem, "title", damsNS, "Title", madsNS);
        addElement(el,"authoritativeLabel", madsNS).setText(collectionName);
    }

    /*
     * Add dams:Relationship element
     * @param e
     * @param type
     * @param role
     * @param pred
     * @param nameElem
     * @param value
     */
    protected void addRelationship(Element e, String type, String role, String pred, String nameElem, String value) {
        // name
        Element rel = addElement( e, "relationship", damsNS,
            "Relationship", damsNS );
        Element name = addVocabElement( rel, pred, damsNS, type, madsNS );
        addTextElement( name, "authoritativeLabel", madsNS, value );
        Element el = addElement( name, "elementList", madsNS );
        addAttribute( el, "parseType", rdfNS, "Collection" );
        addMadsElement( el, nameElem, value );

        // role
        addRole(rel, role);
    }

    /*
     * Add Role (dams:Authority) element
     * @param rel
     * @param role
     */
    protected void addRole(Element rel, String role) {
        Element r = addVocabElement(rel, "role", damsNS, "Authority", madsNS);
        addTextElement( r, "authoritativeLabel", madsNS, role );
        Element scheme = addVocabElement(r, "isMemberOfMADSScheme", madsNS,
            "MADSScheme", madsNS );
        addTextElement( scheme, "label", rdfsNS, "MARC Relator Codes" );
    }

    /*
     * Add Subject (mads or dams)
     * @param e
     * @param predicateName
     * @param elemName
     * @param value
     */
    protected void handleSubject(Element e, String predicateName, String elemName, String value) {
        if (Arrays.asList(MADS_SUBJECTS).indexOf(elemName) >= 0) {
            String nameElement = elemName.endsWith("PersonalName") ? "FullName" : elemName.endsWith("Name") ? "Name" : null;
            addMadsSubject( e, elemName, madsNS, predicateName, damsNS, nameElement, value );
        } else {
            addDamsSubject( e, elemName, damsNS, predicateName, damsNS, null, value );
        }
    }

    /*
     * Add MADS subject
     * @param e
     * @param type
     * @param typeNS
     * @param pred
     * @param predNS
     * @param element
     * @param value
     */
    protected void addMadsSubject( Element e, String type, Namespace typeNS, String pred,
            Namespace predNS, String element, String value )
    {
        addMadsSubjectElement( e, type, typeNS, pred, predNS, element, value );
    }

    protected void addMadsSubjectElement( Element e, String type, Namespace typeNS, String pred,
            Namespace predNS, String element, String value )
    {
        if ( element == null ) { element = type; }
        Element el = createSubject(e, type, typeNS, pred, predNS, value);
        addMadsElement( el, element, value );
    }

    /*
     * Add DAMS subject
     * @param e
     * @param type
     * @param typeNS
     * @param pred
     * @param predNS
     * @param element
     * @param value
     */
    protected void addDamsSubject( Element e, String type, Namespace typeNS, String pred,
            Namespace predNS, String element, String value )
    {
        addDamsSubjectElement( e, type, typeNS, pred, predNS, element, value );
    }

    protected void addDamsSubjectElement( Element e, String type, Namespace typeNS, String pred,
            Namespace predNS, String element, String value )
    {
        if ( element == null ) { element = type; }
        Element el = createSubject(e, type, typeNS, pred, predNS, value);
        addDamsElement( el, element, value );
    }

    protected Element createSubject( Element e, String type, Namespace typeNS, String pred, Namespace predNS, String value ) {
        Element sub = addVocabElement( e, pred, predNS, type, typeNS );
        addTextElement( sub, "authoritativeLabel", madsNS, value );
        Element el = addElement( sub, "elementList", madsNS );
        addAttribute( el, "parseType", rdfNS, "Collection" );
        return el;
    }

    public static void addTitle( Element e, String mainTitle, String subTitle,
        String partName, String partNumber, String translation, String variant )
    {
        String label = mainTitle;
        if ( pop(subTitle) )   { label += "--" + subTitle;   }
        if ( pop(partName) )   { label += "--" + partName;   }
        if ( pop(partNumber) ) { label += "--" + partNumber; }
        if ( !pop(label) )
        {
            // if there's no title provided, don't add the structure
            return;
        }
        Element t = addElement(e,"title",damsNS,"Title",madsNS);
        addElement(t,"authoritativeLabel",madsNS).setText(label);
        Element el = addElement(t,"elementList",madsNS);
        addAttribute( el, "parseType", rdfNS, "Collection" );
        addMadsElement( el, "MainTitle",  mainTitle  );
        addMadsElement( el, "SubTitle",   subTitle   );
        addMadsElement( el, "PartName",   partName   );
        addMadsElement( el, "PartNumber", partNumber );

        // variants
        for ( String var : split(variant) )
        {
            Element varElem = addElement( t, "hasVariant", madsNS, "Variant", madsNS );
            addTextElement( varElem, "variantLabel", madsNS, var );
        }
        for ( String trans : split(translation) )
        {
            Element varElem = addElement( t, "hasTranslationVariant", madsNS,
                "Variant", madsNS );
            addTextElement( varElem, "variantLabel", madsNS, trans );
        }
    }

    protected static void testDateValue ( String objectID, String dateValue, String dateType ) throws ParseException 
    {
    	if(pop( dateValue )) {
    		int len = dateValue.length();
    		if(len == 4 || len == 7 || len == 10) {
	    		for(DateFormat dateFormat : dateFormats) {
		    		try{
		    			dateFormat.parse(dateValue);
		    			return;
		    		}catch(ParseException ex){
		    		}
	    		}
    		}
    		String dateFormatString = "";
    		for (String format : ACCEPTED_DATE_FORMATS) {
    			dateFormatString += (dateFormatString.length() > 0 ? ", " : "") + format.toLowerCase();
    		}
    		throw new ParseException( "Invalid " + dateType + " " + dateValue + " in record " + objectID
    				+ ". Formats accepted: " + dateFormatString, 0);
    	}
    }
}
