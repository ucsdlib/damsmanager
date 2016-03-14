package edu.ucsd.library.xdre.tab;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;


/**
 * class SubjectTabularRecord: A bundle of tabular data, key-value map for the subject
 * @author lsitu
**/
public class SubjectTabularRecord extends TabularRecordBasic
{
    public static final String SUBJECT_TYPE = "subject type";
    public static final String SUBJECT_TERM = "subject term";
    public static final String EXACT_MATCH = "exactMatch";
	public static final String[] ALL_FIELDS_FOR_SUBJECTS = { "ARK", SUBJECT_TYPE, SUBJECT_TERM, EXACT_MATCH };

	public SubjectTabularRecord ( Map<String,String> data )
    {
    	super( data );
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

        // get previously-assigned ark
        String ark = data.get("ark");

        // object metadata
        createSubject( rdf, data, ark );

        return rdf.getDocument();
    }

 
    /**
     * fields with the key mapping for the subject structure
     * @throws Exception 
    **/
    private void createSubject( Element e, Map<String,String> data, String ark ) throws Exception
    {
        // subject: data, elem, header, class/ns, predicate/ns, element
        String elemName = null;
        String subjectType = data.get(SUBJECT_TYPE.toLowerCase());
        String subjectName = toCarmelCase(subjectType);
        if ( subjectType.equalsIgnoreCase("genre") )
        	subjectName = "GenreForm";
        if ( subjectType.equalsIgnoreCase("personal name") )
    		elemName = "FullName";

        String exactMatch = (String)data.get(EXACT_MATCH.toLowerCase());
        String subjectTerm = (String)data.get(SUBJECT_TERM.toLowerCase());
        switch (subjectType)
        {
    		case "conference name":
    			elemName = "Name";
        	case "corporate name":
        		elemName = "Name";
        	case "family name":
        		elemName = "Name";
        	case "personal name":
    		case "genre":
        	case "geographic":
        	case "occupation":
        	case "temporal":
        	case "topic":
                // mats subject: elem, header, class/ns, predicate/ns, element, header exactMatch
        		madsSubject( ark, e, subjectName, madsNS, subjectTerm, elemName, exactMatch );
        		break;
        	default:
        		// dams subject: scientific name, common name, culturalContext, lithology, series, cruise etc.
        		damsSubject( ark, e, subjectName, damsNS, subjectTerm, null, exactMatch );
        		break;
        }
    }

    protected void madsSubject( String id, Element e, String type, Namespace typeNS, String label, String element, String exactMatch )
    {
        if ( element == null ) { element = type; }
        Element el = buildSubject( id, e, type, typeNS, label, exactMatch );
        addMadsElement( el, element, label );
    }

    protected void damsSubject( String id, Element e, String type, Namespace typeNS, String label, String element, String exactMatch )
    {
        if ( element == null ) { element = type; }
        Element el = buildSubject( id, e, type, typeNS, label, exactMatch );
        addDamsElement( el, element, label );
    }

    protected Element buildSubject( String id, Element e, String type, Namespace typeNS, String value, String exactMatch ) {
        Element root = addElement( e, type, typeNS );
        addAttribute( root, "about", rdfNS, StringUtils.isBlank(id) ? "ARK" + counter++ : id );
        addTextElement( root, "authoritativeLabel", madsNS, value );
        if ( StringUtils.isNotBlank( exactMatch ) )
        {
            Element elem = addElement( root,"hasExactExternalAuthority", madsNS ) ;
            addAttribute( elem, "resource", rdfNS, exactMatch );
        }
        Element el = addElement( root, "elementList", madsNS );
        addAttribute( el, "parseType", rdfNS, "Collection" );
        return el;
    }
}
