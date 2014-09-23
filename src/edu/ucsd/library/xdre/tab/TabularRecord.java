package edu.ucsd.library.xdre.tab;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

/**
 * A bundle of tabular data, consisting of a key-value map for the record, and 0 or more
 * components (also key-value maps).
 *
 * @author escowles
 * @since 2014-06-05
**/
public class TabularRecord implements Record
{
    public static final String OBJECT_ID = "object unique id";
    public static final String OBJECT_COMPONENT_TYPE = "object/component";
    public static final String COMPONENT = "component";
    public static final String DELIMITER = "|";
    public static final String[] ACCEPTED_DATE_FORMATS = {"yyyy-MM-dd", "yyyy-MM", "yyyy"};
    private static DateFormat[] dateFormats = {new SimpleDateFormat(ACCEPTED_DATE_FORMATS[0]), 
    	new SimpleDateFormat(ACCEPTED_DATE_FORMATS[1]), new SimpleDateFormat(ACCEPTED_DATE_FORMATS[2])};

    // namespaces
    private static final Namespace rdfNS  = new Namespace(
            "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    private static final Namespace rdfsNS  = new Namespace(
            "rdfs", "http://www.w3.org/2000/01/rdf-schema#");
    private static final Namespace madsNS = new Namespace(
            "mads", "http://www.loc.gov/mads/rdf/v1#");
    private static final Namespace damsNS = new Namespace(
            "dams", "http://library.ucsd.edu/ontology/dams#");

    private Map<String,String> data;
    private List<Map<String,String>> cmp;
    private int counter = 0;

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
    public TabularRecord( Map<String,String> data, ArrayList<Map<String,String>> cmp )
    {
        this.data = (data != null) ? data : new HashMap<String,String>();
        this.cmp = (cmp != null) ? cmp : new ArrayList<Map<String,String>>();
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
     * Set the component-level data.
    **/
    public void setComponents( List<Map<String,String>> cmp )
    {
        this.cmp = cmp;
    }

    /**
     * Get the component-level data.
    **/
    public List<Map<String,String>> getComponents()
    {
        return cmp;
    }

    /**
     * Get the record's identifier.
    **/
    public String recordID()
    {
        return getData().get(OBJECT_ID);
    }

    /**
     * Convert the record to RDF/XML.
     * @throws ParseException 
    **/
    public Document toRDFXML() throws ParseException
    {
        // setup object
    	Document doc = new DocumentFactory().createDocument();
    	Element rdf = createRdfRoot (doc);

        Element root = addElement(rdf,"Object",damsNS);

        // get previously-assigned ark
        String ark = data.get("ark");

        // object metadata
        addFields( root, data, 0, ark );

        // component metadata
        for ( int i = 0; i < cmp.size(); i++ )
        {
            Element e = addElement(root, "hasComponent", damsNS, "Component", damsNS);
            addFields(e, cmp.get(i), (i + 1), ark); // 1-based component ids
        }

        return rdf.getDocument();
    }

    /**
     * The meat of the metadata processing -- a long sequence of categories of metadata
     * fields with the key mapping between field names and dams4 structure
    **/
    private void addFields( Element e, Map<String,String> data, int cmp, String ark ) throws ParseException
    {
        if ( ark == null ) { ark = "ARK"; }
        String id = (cmp > 0) ? ark + "/" + cmp : ark;
        String fileID = ark + "/" + cmp + "/1";
        addAttribute(e, "about", rdfNS, id);

        // typeOfResource ///////////////////////////////////////////////////////////////
        for ( String type : split(data.get("type of resource")) )
        {
            addTextElement( e, "typeOfResource", damsNS, type );
        }

        // unit /////////////////////////////////////////////////////////////////////////
        if ( cmp == 0 && pop(data.get("unit")) )
        {
            Element unit = addElement( e, "unit", damsNS );
            addAttribute( unit, "resource", rdfNS, data.get("unit") );
        }

        // collections //////////////////////////////////////////////////////////////////
        addCollection( e, data, "assembled collection", "assembledCollection",
            "AssembledCollection" );
        addCollection( e, data, "provenance collection", "provenanceCollection",
            "ProvenanceCollection" );
        addCollection( e, data, "provenance collection part", "provenanceCollectionPart",
            "ProvenanceCollectionPart" );

        // title ////////////////////////////////////////////////////////////////////////
        String main  = data.get("title");
        String sub   = data.get("subtitle");
        String ptNam = data.get("part name");
        String ptNum = data.get("part number");
        String trans = data.get("translation");
        String var   = data.get("variant");
        addTitle( e, main, sub, ptNam, ptNum, trans, var );

        // date /////////////////////////////////////////////////////////////////////////
        // first create a date element to hold begin/end date if provided
        String objectID = data.get("object unique id");
        String date = data.get("date");
        String begin = data.get("begin date");
        String end   = data.get("end date");
        Element d = null;
        if ( pop(date) || pop(begin) || pop(end) )
        {
        	testDateValue ( objectID, begin, "begin date" );
        	testDateValue ( objectID, end, "end date" );
        	
            d = addElement( e, "date", damsNS, "Date", damsNS );
            addTextElement( d, "encoding", damsNS, "w3cdtf" );
            addTextElement( d, "value", rdfNS, date );
            addTextElement( d, "beginDate", damsNS, begin );
            addTextElement( d, "endDate", damsNS, end );
        }

        // next, find all qualified date types
        Map<String,String> dates = new HashMap<>();
        for ( Iterator<String> it = data.keySet().iterator(); it.hasNext(); )
        {
            String key = it.next();
            if ( key.startsWith("date:") )
            {
                String value = data.get( key );
                if ( pop(value) )
                {
                    String type = key.substring( key.indexOf(":") + 1 );
                    dates.put( type, value );
                }
            }
        }

        // if there's only one qualified date, add to the structured date above,
        // otherwise, create a date element for each qualified date
        if ( d != null && dates.size() == 1 )
        {
            // only one date
            String type = dates.keySet().iterator().next();
            String value = dates.get(type);
            addTextElement( d, "type", damsNS, type );
            addTextElement( d, "value", rdfNS, value );
        }
        else
        {
            // multiple qualified dates
            for ( Iterator<String> it = dates.keySet().iterator(); it.hasNext(); )
            {
                String type = it.next();
                String value = dates.get( type );
                Element e2 = addElement( e, "date", damsNS, "Date", damsNS );
                addTextElement( e2, "type", damsNS, type );
                addTextElement( e2, "value", rdfNS, value );
            }
        }

        // identifier ///////////////////////////////////////////////////////////////////
        for ( Iterator<String> it = data.keySet().iterator(); it.hasNext(); )
        {
            String key = it.next();
            if ( key.startsWith("identifier:") )
            {
                String value = data.get( key );
                if ( pop(value) )
                {
                    Element e2 = addElement( e, "note", damsNS, "Note", damsNS );
                    addTextElement( e2, "type", damsNS, "identifier" );
                    addTextElement( e2, "value", rdfNS, value );

                    String label = key.substring( key.indexOf(":") + 1 );
                    addElement(e2, "displayLabel", damsNS).setText(label);
                }
            }
        }

        // note /////////////////////////////////////////////////////////////////////////
        for ( Iterator<String> it = data.keySet().iterator(); it.hasNext(); )
        {
            String key = it.next();
            if ( key.startsWith("note:") )
            {
                String values = data.get( key );
                for ( String value : split(values) )
                {
                    Element e2 = addElement( e, "note", damsNS, "Note", damsNS );
                    addTextElement( e2, "value", rdfNS, value );

                    String type = key.substring( key.indexOf(":") + 1 );
                    addElement(e2, "type", damsNS).setText(type);
                }
            }
        }

        // relationships ////////////////////////////////////////////////////////////////
        // data, elem, header, class/ns, pred/ns, element
        addRelationship( data, e, "person", "PersonalName", "personalName", "FullName" );
        addRelationship( data, e, "corporate", "CorporateName", "corporateName", "Name" );

        // subjects /////////////////////////////////////////////////////////////////////
        // data, elem, header, class/ns, predicate/ns, element
        addSubject( data, e, "subject:conference name", "ConferenceName", madsNS,
                "conferenceName", damsNS, "Name" );
        addSubject( data, e, "subject:corporate name", "CorporateName", madsNS,
                "corporateName", damsNS, "Name" );
        addSubject( data, e, "subject:family name", "FamilyName", madsNS,
                "familyName", damsNS, "Name" );
        addSubject( data, e, "subject:personal name", "PersonalName", madsNS,
                "personalName", damsNS, "FullName" );
        addSubject( data, e, "subject:genre", "GenreForm", madsNS,
                "genreForm", damsNS, null );
        addSubject( data, e, "subject:geographic", "Geographic", madsNS,
                "geographic", damsNS, null );
        addSubject( data, e, "subject:occupation", "Occupation", madsNS,
                "occupation", damsNS, null );
        addSubject( data, e, "subject:temporal", "Temporal", madsNS,
                "temporal", damsNS, null );
        addSubject( data, e, "subject:topic", "Topic", madsNS, "topic", damsNS, null );

        // rights holder (special case of subject name) /////////////////////////////////
        addSubject( data, e, "copyright holder conference name", "ConferenceName", madsNS,
                "rightsHolderConference", damsNS, "Name" );
        addSubject( data, e, "copyright holder corporate name", "CorporateName", madsNS,
                "rightsHolderCorporate", damsNS, "Name" );
        addSubject( data, e, "copyright holder family name", "FamilyName", madsNS,
                "rightsHolderFamily", damsNS, "Name" );
        addSubject( data, e, "copyright holder personal name", "PersonalName", madsNS,
                "rightsHolderPersonal", damsNS, "FullName" );

        // language /////////////////////////////////////////////////////////////////////
        for ( String lang : split(data.get("language")) )
        {
            Element elem = addVocabElement(e,"language",damsNS,"Language",madsNS);
            addTextElement(elem,"code",madsNS,lang);
        }

        // cartographics ////////////////////////////////////////////////////////////////
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

        // related resource /////////////////////////////////////////////////////////////
        String relType = data.get("related resource:type");
        String relURI = data.get("related resource:uri");
        String relDesc = data.get("related resource:description");
        if ( pop(relURI) || pop(relDesc) )
        {
            Element rel = addElement(e,"relatedResource",damsNS,"RelatedResource",damsNS);
            if ( pop(relType) ) { addElement(rel,"type",damsNS).setText(relType); }
            if ( pop(relURI) ) { addElement(rel,"uri",damsNS).setText(relURI); }
            if ( pop(relDesc) ) { addElement(rel,"description",damsNS).setText(relDesc); }
        }

        // files ////////////////////////////////////////////////////////////////////////
        String fn = data.get("file name");
        String use = data.get("file use");
        if ( pop(fn) )
        {
            Element f = addElement(e,"hasFile",damsNS,"File",damsNS);
            String ext = (fn.indexOf(".") != -1) ? fn.substring(fn.lastIndexOf(".")) : "";
            addAttribute( f, "about", rdfNS, fileID + ext );
            addElement(f,"sourceFileName",damsNS).setText(fn);
            if ( pop(use) )
            {
                addElement(f,"use",damsNS).setText(use);
            }
        }

        // copyright ////////////////////////////////////////////////////////////////////
        if ( pop(data.get("rights status")) )
        {
            Element copy = addElement(e,"copyright",damsNS,"Copyright",damsNS);
            addTextElement(copy,"copyrightStatus",damsNS, data.get("rights status"));
            addTextElement(copy,"copyrightJurisdiction",damsNS, data.get("jurisdiction"));
        }

        // other rights /////////////////////////////////////////////////////////////////
        if ( pop(data.get("other rights permission")) )
        {
            Element other = addElement(e, "otherRights", damsNS, "OtherRights", damsNS);
            addTextElement( other, "otherRightsBasis",damsNS,
                data.get("other rights permission basis") );
            Element perm = addElement(other, "permission", damsNS, "Permission", damsNS);
            addTextElement( perm, "type", damsNS, data.get("other rights permission") );
        }

    }

    /////////////////////////////////////////////////////////////////////////////////////
    // metadata utilities ////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    private void addCollection( Element e, Map<String,String> data, String header,
        String pred, String elem )
    {
        for ( String colName : split(data.get(header)) )
        {
            Element coll = addVocabElement( e, pred, damsNS, elem, damsNS );
            addTitle( coll, colName, null, null, null, null, null );
        }
    }
    private void addRelationship( Map<String,String> data, Element e, String header,
        String type, String pred, String element )
    {
        for ( Iterator<String> it = data.keySet().iterator(); it.hasNext(); )
        {
            String key = it.next();
            if ( key.startsWith(header + ":") )
            {
                for ( String value : split(data.get(key)) )
                {
                    // name
                    Element rel = addElement( e, "relationship", damsNS,
                        "Relationship", damsNS );
                    Element name = addVocabElement( rel, pred, damsNS, type, madsNS );
                    addTextElement( name, "authoritativeLabel", madsNS, value );
                    Element el = addElement( name, "elementList", madsNS );
                    addAttribute( el, "parseType", rdfNS, "Collection" );
                    addMadsElement( el, element, value );

                    // role
                    String role = key.substring( key.indexOf(":") + 1 );
                    Element r = addVocabElement(rel, "role", damsNS, "Authority", madsNS);
                    addTextElement( r, "authoritativeLabel", madsNS, role );
                    Element scheme = addVocabElement(r, "isMemberOfMADSScheme", madsNS,
                        "MADSScheme", madsNS );
                    addTextElement( scheme, "label", rdfsNS, "MARC Relator Codes" );
                }
            }
        }
    }
    private void addSubject( Map<String,String> data, Element e, String header,
        String type, Namespace typeNS, String pred, Namespace predNS, String element )
    {
        for ( String value : split(data.get(header)) )
        {
            Element sub = addVocabElement( e, pred, predNS, type, typeNS );
            addTextElement( sub, "authoritativeLabel", madsNS, value );
            Element el = addElement( sub, "elementList", madsNS );
            addAttribute( el, "parseType", rdfNS, "Collection" );
            if ( element == null ) { element = type; }
            addMadsElement( el, element, value );
        }
    }
    private static void addTitle( Element e, String mainTitle, String subTitle,
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
    private static void addMadsElement( Element list, String name, String value )
    {
        addTextElement(list,name + "Element", madsNS,"elementValue",madsNS, value);
    }

    /////////////////////////////////////////////////////////////////////////////////////
    // xml utilities ////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    private static void addTextElement( Element e, String name, Namespace ns,
        String value )
    {
        if ( pop(value) )
        {
            addElement( e, name, ns ).setText( value );
        }
    }
    private static void addTextElement( Element e, String name1, Namespace ns1,
        String name2, Namespace ns2, String value )
    {
        if ( pop(value) )
        {
            addElement( e, name1, ns1, name2, ns2 ).setText( value );
        }
    }
    private static void addAttribute( Element e, String name, Namespace ns, String value )
    {
        e.addAttribute( new QName(name,ns), value );
    }

    private static Element addElement( Branch b, String name, Namespace ns )
    {
        return b.addElement( new QName(name,ns) );
    }
    private static Element addElement( Branch b, String name1, Namespace ns1,
        String name2, Namespace ns2 )
    {
        return b.addElement( new QName(name1,ns1) ).addElement( new QName(name2,ns2) );
    }
    private Element addVocabElement( Branch b, String name1, Namespace ns1,
        String name2, Namespace ns2 )
    {
        Element e = addElement( b, name1, ns1, name2, ns2 );
        addAttribute( e, "about", rdfNS, "id_" + counter++ );
        return e;
    }

    /////////////////////////////////////////////////////////////////////////////////////
    // text processing //////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    private static boolean pop( String s )
    {
        return ( s != null && !s.trim().equals("") );
    }
    private static List<String> split( String s )
    {
        ArrayList<String> list = new ArrayList<>();
        if ( pop(s) )
        {
            if ( s.indexOf(DELIMITER) != -1 )
            {
                String[] parts = s.split("\\" + DELIMITER);
                for ( int i = 0; i < parts.length; i++ )
                {
                    if ( pop(parts[i]) ) { list.add( parts[i].trim() ); }
                }
            }
            else
            {
                list.add( s.trim() );
            }
        }
        return list;
    }
    
    private static void testDateValue ( String objectID, String dateValue, String dateType ) throws ParseException 
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
    
    /**
     * Create the RDF root element
     * @param doc
     * @return
     */
    public static Element createRdfRoot (Document doc)
    {
        Element rdf = addElement(doc,"RDF",rdfNS);
        doc.setRootElement(rdf);
        rdf.add( damsNS );
        rdf.add( madsNS );
        rdf.add( rdfNS );
        rdf.add( rdfsNS );
        return rdf;
    }
}
