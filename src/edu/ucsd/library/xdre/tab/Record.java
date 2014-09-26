package edu.ucsd.library.xdre.tab;

import java.text.ParseException;
import org.dom4j.Document;

/**
 * A metadata record.
 *
 * @author escowles
 * @since 2014-09-17
**/
public interface Record
{
    /**
     * Get the record's unique identifier.
    **/
    public String recordID();

    /**
     * Convert the record to RDF/XML.
    **/
    public Document toRDFXML() throws ParseException;
}
