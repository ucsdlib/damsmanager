package edu.ucsd.library.xdre.tab;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.Element;

import edu.ucsd.library.xdre.model.DamsSubject;
import edu.ucsd.library.xdre.model.MadsSubject;
import edu.ucsd.library.xdre.model.Subject;


/**
 * class SubjectTabularRecord: A bundle of tabular data, key-value map for the subject
 * @author lsitu
**/
public class SubjectTabularRecord extends TabularRecordBasic
{
    public static final String SUBJECT_TYPE = "subject type";
    public static final String SUBJECT_TERM = "subject term";
    public static final String EXACT_MATCH = "exactMatch";
    public static final String CLOSE_MATCH = "closeMatch";
	public static final String[] ALL_FIELDS_FOR_SUBJECTS = { "ARK", SUBJECT_TYPE, SUBJECT_TERM, EXACT_MATCH, CLOSE_MATCH };

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
        // get previously-assigned ark
        String ark = data.get("ark");

        // object metadata
        Element subjectElem = createSubject( data, ark );

        return subjectElem.getDocument();
    }

 
    /**
     * fields with the key mapping for the subject structure
     * @throws Exception 
    **/
    private Element createSubject( Map<String,String> data, String ark ) throws Exception
    {
        // subject: data, elem, header, class/ns, predicate/ns, element
        String elemName = null;
        String subjectType = data.get(SUBJECT_TYPE.toLowerCase());
        subjectType = subjectType.substring(subjectType.indexOf(":") + 1);
        String subjectName = toCamelCase(subjectType);
        if ( subjectType.equalsIgnoreCase("genre") )
        	subjectName = "GenreForm";
        if ( subjectType.equalsIgnoreCase("personal name") )
    		elemName = "FullName";

        String exactMatch = (String)data.get(EXACT_MATCH.toLowerCase());
        String closeMatch = (String)data.get(CLOSE_MATCH.toLowerCase());
        String subjectTerm = (String)data.get(SUBJECT_TERM.toLowerCase());

        Subject subject = null;
        String id = StringUtils.isBlank(ark) ? "ARK_" + counter++ : ark;
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
                elemName = StringUtils.isBlank(elemName) ? subjectName : elemName;
        		subject = new MadsSubject(id, subjectName, subjectTerm, elemName, exactMatch, closeMatch);
        		break;
        	default:
        		// dams subject: scientific name, common name, culturalContext, lithology, series, cruise etc.
        		elemName = StringUtils.isBlank(elemName) ? subjectName : elemName;
        		subject = new DamsSubject(id, subjectName, subjectTerm, elemName, exactMatch, closeMatch);
        		break;
        }

        return subject.serialize();
    }
}
