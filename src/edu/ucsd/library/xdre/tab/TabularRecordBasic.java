package edu.ucsd.library.xdre.tab;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Branch;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;


/**
 * A bundle of tabular data, consisting of a key-value map for the record
 * @author lsitu
 * @author escowles
 * @since 2014-06-05
**/
public abstract class TabularRecordBasic implements Record
{
    public static final String DELIMITER = "|";
    protected static int counter = 0;

    // namespaces
    protected static final Namespace rdfNS  = new Namespace(
            "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    protected static final Namespace rdfsNS  = new Namespace(
            "rdfs", "http://www.w3.org/2000/01/rdf-schema#");
    protected static final Namespace madsNS = new Namespace(
            "mads", "http://www.loc.gov/mads/rdf/v1#");
    protected static final Namespace damsNS = new Namespace(
            "dams", "http://library.ucsd.edu/ontology/dams#");

    protected Map<String,String> data;

    /**
     * Create an empty record.
    **/
    public TabularRecordBasic()
    {
        this( null );
    }

    /**
     * Create a record with record-level data, but no component-level data.
    **/
    public TabularRecordBasic( Map<String,String> data )
    {
        this.data = data;
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

    protected static void addMadsElement( Element list, String name, String value )
    {
        addTextElement(list,name + "Element", madsNS,"elementValue",madsNS, value);
    }

    protected static void addDamsElement( Element list, String name, String value )
    {
        addTextElement(list, name + "Element", damsNS, "elementValue", madsNS, value);
    }
    /////////////////////////////////////////////////////////////////////////////////////
    // xml utilities ////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    protected static void addTextElement( Element e, String name, Namespace ns,
        String value )
    {
        if ( pop(value) )
        {
            addElement( e, name, ns ).setText( value );
        }
    }
    protected static void addTextElement( Element e, String name1, Namespace ns1,
        String name2, Namespace ns2, String value )
    {
        if ( pop(value) )
        {
            addElement( e, name1, ns1, name2, ns2 ).setText( value );
        }
    }
    protected static void addAttribute( Element e, String name, Namespace ns, String value )
    {
        e.addAttribute( new QName(name,ns), value );
    }

    protected static Element addElement( Branch b, String name, Namespace ns )
    {
        return b.addElement( new QName(name,ns) );
    }
    protected static Element addElement( Branch b, String name1, Namespace ns1,
        String name2, Namespace ns2 )
    {
        return b.addElement( new QName(name1,ns1) ).addElement( new QName(name2,ns2) );
    }
    protected Element addVocabElement( Branch b, String name1, Namespace ns1,
        String name2, Namespace ns2 )
    {
    	if ( StringUtils.isBlank(name1) && StringUtils.isBlank(name2) )
    		return (Element)b;
        Element e = addElement( b, name1, ns1, name2, ns2 );
        addAttribute( e, "about", rdfNS, "id_" + counter++ );
        return e;
    }

    /////////////////////////////////////////////////////////////////////////////////////
    // text processing //////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////
    protected static boolean pop( String s )
    {
        return ( s != null && !s.trim().equals("") );
    }
    protected static List<String> split( String s )
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

    public static String toCarmelCase(String s)
    {
        if(StringUtils.isNotBlank( s ))
        {
            String[] tokens = s.split(" ");
            String carmelCase = "";
            for (String token : tokens)
            {
                if (StringUtils.isNotBlank(token))
                    carmelCase += token.substring(0, 1).toUpperCase() + (token.length() > 1 ? token.substring(1) : "");
             }
            return carmelCase;
        }
            return s;
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

	@Override
	public String recordID() {
		// TODO Auto-generated method stub
		return null;
	}
}
