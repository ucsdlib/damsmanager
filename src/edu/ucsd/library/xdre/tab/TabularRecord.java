package edu.ucsd.library.xdre.tab;

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
public class TabularRecord
{
    public static final String OBJECT_ID = "Object Unique ID";
    public static final String DELIMITER = "|";

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
     * Convert the record to RDF/XML.
    **/
    public Document toRDFXML()
    {
        // setup object        
        Document doc = new DocumentFactory().createDocument();
        Element rdf = addElement(doc,"RDF",rdfNS);
        doc.setRootElement(rdf);
        rdf.add( damsNS );
        rdf.add( madsNS );
        rdf.add( rdfNS );
        rdf.add( rdfsNS );

        Element root = addElement(rdf,"Object",damsNS);

        // object metadata
        addFields( root, data, 0 );

        // component metadata
        for ( int i = 0; i < cmp.size(); i++ )
        {
            Element component = addElement( root, "hasComponent", damsNS, "Component",
                    damsNS );
            addFields( component, cmp.get(i), (i + 1) ); // 1-based component numbers
        }

        return doc;
    }

    /**
     * The meat of the metadata processing -- a long sequence of categories of metadata
     * fields with the key mapping between field names and dams4 structure
    **/
    private void addFields( Element e, Map<String,String> data, int cmp )
    {
        String id = (cmp > 0) ? "ARK/" + cmp : "ARK";
        addAttribute(e, "about", rdfNS, id);

        // typeOfResource ///////////////////////////////////////////////////////////////
        for ( String type : split(data.get("Type of Resource")) )
        {
            addTextElement( e, "typeOfResource", damsNS, type );
        }

        // collections //////////////////////////////////////////////////////////////////
        addCollection( e, data, "Assembled collection", "assembledCollection",
            "AssembledCollection" );
        addCollection( e, data, "Provenance collection", "provenanceCollection",
            "ProvenanceCollection" );
        addCollection( e, data, "Provenance collection part", "provenanceCollectionPart",
            "ProvenanceCollectionPart" );

        // title ////////////////////////////////////////////////////////////////////////
        String main  = data.get("Title");
        String sub   = data.get("Subtitle");
        String ptNam = data.get("Part name");
        String ptNum = data.get("Part number");
        String trans = data.get("Translation");
        String var   = data.get("Variant");
        addTitle( e, main, sub, ptNam, ptNum, trans, var );

        // date /////////////////////////////////////////////////////////////////////////
        // first create a date element to hold begin/end date if provided
        String date = data.get("Date");
        String begin = data.get("Begin date");
        String end   = data.get("End date");
        Element d = null;
        if ( pop(date) || pop(begin) || pop(end) )
        {
            d = addElement( e, "date", damsNS, "Date", damsNS );
            addTextElement( d, "value", rdfNS, date );
            addTextElement( d, "beginDate", damsNS, begin );
            addTextElement( d, "endDate", damsNS, end );
        }

        // next, find all qualified date types
        Map<String,String> dates = new HashMap<>();
        for ( Iterator<String> it = data.keySet().iterator(); it.hasNext(); )
        {
            String key = it.next();
            if ( key.startsWith("Date:") )
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
            if ( key.startsWith("Identifier:") )
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
            if ( key.startsWith("Note:") )
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
        addRelationship( data, e, "Person", "PersonalName", "personalName", "FullName" );
        addRelationship( data, e, "Corporate", "CorporateName", "corporateName", "Name" );

        // subjects /////////////////////////////////////////////////////////////////////
        // data, elem, header, class/ns, predicate/ns, element
        addSubject( data, e, "Subject:conference name", "ConferenceName", madsNS,
                "conferenceName", damsNS, "Name" );
        addSubject( data, e, "Subject:corporate name", "CorporateName", madsNS,
                "corporateName", damsNS, "Name" );
        addSubject( data, e, "Subject:family name", "FamilyName", madsNS,
                "familyName", damsNS, null );
        addSubject( data, e, "Subject:personal name", "PersonalName", madsNS,
                "personalName", damsNS, "FullName" );
        addSubject( data, e, "Subject:genre", "GenreForm", madsNS,
                "genreForm", damsNS, null );
        addSubject( data, e, "Subject:geographic", "Geographic", madsNS,
                "geographic", damsNS, null );
        addSubject( data, e, "Subject:occupation", "Occupation", madsNS,
                "occupation", damsNS, null );
        addSubject( data, e, "Subject:temporal", "Temporal", madsNS,
                "temporal", damsNS, null );
        addSubject( data, e, "Subject:topic", "Topic", madsNS, "topic", damsNS, null );

        // rights holder (special case of subject name) /////////////////////////////////
        addSubject( data, e, "Copyright holder personal name", "PersonalName", madsNS,
                "rightsHolderPersonal", damsNS, "FullName" );
        addSubject( data, e, "Copyright holder corporate name", "CorporateName", madsNS,
                "rightsHolderCorporate", damsNS, "Name" );

        // language /////////////////////////////////////////////////////////////////////
        for ( String lang : split(data.get("Language")) )
        {
            Element elem = addVocabElement(e,"language",damsNS,"Language",madsNS);
            addTextElement(elem,"code",madsNS,lang);
        }

        // cartographics ////////////////////////////////////////////////////////////////
        String line  = data.get("Geographic:line");
        String point = data.get("Geographic:point");
        String poly  = data.get("Geographic:polygon");
        String proj  = data.get("Geographic:projection");
        String ref   = data.get("Geographic:reference system");
        String scale = data.get("Geographic:scale");
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
        String relURI = data.get("Related resource:uri");
        String relDesc = data.get("Related resource:description");
        if ( pop(relURI) || pop(relDesc) )
        {
            Element rel = addElement(e,"relatedResource",damsNS,"RelatedResource",damsNS);
            if ( pop(relURI) ) { addElement(rel,"uri",damsNS).setText(relURI); }
            if ( pop(relDesc) ) { addElement(rel,"description",damsNS).setText(relDesc); }
        }

        // files ////////////////////////////////////////////////////////////////////////
        String fn = data.get("File name");
        String use = data.get("File use");
        if ( pop(fn) )
        {
            Element f = addElement(e,"hasFile",damsNS,"File",damsNS);
            String ext = (fn.indexOf(".") != -1) ? fn.substring(fn.lastIndexOf(".")) : "";
            addAttribute( f, "about", rdfNS, id + "/1" + ext );
            addElement(f,"sourceFileName",damsNS).setText(fn);
            if ( pop(use) )
            {
                addElement(f,"use",damsNS).setText(use);
            }
        }

        // copyright ////////////////////////////////////////////////////////////////////
        if ( pop(data.get("Rights status")) )
        {
            Element copy = addElement(e,"copyright",damsNS,"Copyright",damsNS);
            addTextElement(copy,"copyrightStatus",damsNS, data.get("Rights status"));
            addTextElement(copy,"copyrightJurisdiction",damsNS, data.get("Jurisdiction"));
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
}
