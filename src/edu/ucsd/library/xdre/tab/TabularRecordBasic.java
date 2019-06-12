package edu.ucsd.library.xdre.tab;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringEscapeUtils;
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
    public static final Character[] RETAIN_CONTROL_CHARS = {10, 13};
    public static List<Character> RETAIN_CONTROL_CHAR_LIST = Arrays.asList(RETAIN_CONTROL_CHARS);
    public static String[] MADS_SUBJECTS = {"ConferenceName", "CorporateName", "FamilyName", "PersonalName",
        "GenreForm", "Geographic", "Occupation", "Temporal", "Topic"};

    public static final String OBJECT_ID = "object unique id";
    public static final String OBJECT_COMPONENT_TYPE = "level";
    public static final String COMPONENT = "component";
    public static final String SUBCOMPONENT = "sub-component";
    public static final char DELIMITER = '|';
    public static final char ESCAPE_CHAR = '\\';
    public static final String DELIMITER_CELL = "@";
    public static final String DELIMITER_LANG_ELEMENT = "-";
    public static final String[] ACCEPTED_DATE_FORMATS = {"yyyy-MM-dd", "yyyy-MM", "yyyy"};
    protected static DateFormat[] dateFormats = {new SimpleDateFormat(ACCEPTED_DATE_FORMATS[0]), 
        new SimpleDateFormat(ACCEPTED_DATE_FORMATS[1]), new SimpleDateFormat(ACCEPTED_DATE_FORMATS[2])};

    protected static int counter = 0;

    // namespaces
    public static final Namespace rdfNS  = new Namespace(
            "rdf", "http://www.w3.org/1999/02/22-rdf-syntax-ns#");
    public static final Namespace rdfsNS  = new Namespace(
            "rdfs", "http://www.w3.org/2000/01/rdf-schema#");
    public static final Namespace madsNS = new Namespace(
            "mads", "http://www.loc.gov/mads/rdf/v1#");
    public static final Namespace damsNS = new Namespace(
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
        this.data = (data != null) ? data : new HashMap<String,String>();
    }

    /**
     * Get the record's identifier.
    **/
    public String recordID()
    {
        return getData().get(OBJECT_ID);
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

    public static List<String> split( String s )
    {
        ArrayList<String> list = new ArrayList<>();
        if ( pop(s) )
        {
            StringBuilder sb = new StringBuilder();
            boolean escaped = false;
            for( char ch : s.toCharArray() )
            {
                if( escaped )
                {
                    handleEscapedCharacter( sb, ch, false );

                    escaped = false;
                }
                else if ( ch == ESCAPE_CHAR )
                {
                    escaped = true;
                }
                else if ( ch == DELIMITER )
                {
                    list.add( sb.toString().trim() );
                    sb.setLength( 0 );
                }
                else if ( !Character.isISOControl(ch) || RETAIN_CONTROL_CHAR_LIST.contains(ch) )
                {
                    sb.append( ch );
                }
            }

            if ( sb.length() > 0 )
            {
                list.add( sb.toString().trim() );
            }
        }
        return list;
    }

    /**
     * Handle escaped character. Control character could be ignored or replaced
     * @param sb StringBuilder the character to be attached
     * @param ch char the escaped Character
     * @param replaceChar flag to replace the Character
     * @return boolean flag indicating that the char is replaced or not
     */
    public static boolean handleEscapedCharacter(StringBuilder sb, char ch, boolean replaceChar)
    {
        boolean replaced = false;
        boolean isControlChar = Character.isISOControl(ESCAPE_CHAR + ch) || Character.isISOControl(ch);
        if ( ch == DELIMITER )
        {
            sb.append( ch );
        }
        else if ( !isControlChar || RETAIN_CONTROL_CHAR_LIST.contains(ch) )
        {
            sb.append( ESCAPE_CHAR + "" + ch );
        }
        else if ( replaceChar )
        {
            // replace control character with symbol [character name]
            replaced = true;
            if ( Character.isISOControl(ch) )
                sb.append( TabularRecord.ESCAPE_CHAR + "" + Character.getName(ch) );
            else
                sb.append( "[" + Character.getName( StringEscapeUtils.unescapeJava( TabularRecord.ESCAPE_CHAR + "" + ch ).charAt(0) ) + "]" );
        }
        return replaced;
    }

    /**
     * Convert string value to CamelCase
     * @param s
     * @return
     */
    public static String toCamelCase(String s)
    {
        if(StringUtils.isNotBlank( s ))
        {
            String[] tokens = s.split(" ");
            String camelCase = "";
            for (String token : tokens)
            {
                if (StringUtils.isNotBlank(token))
                    camelCase += token.substring(0, 1).toUpperCase() + (token.length() > 1 ? token.substring(1) : "");
             }
            return camelCase;
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
}
