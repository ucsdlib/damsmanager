package edu.ucsd.library.xdre.tab;

import org.dom4j.Document;

/**
 * Simple Record implementation.
 *
 * @author escowles
 * @since 2014-09-17
**/
public class SimpleRecord implements Record
{
    private String id;
    private Document rdf;

    /**
     * Constructor.
    **/
    public SimpleRecord( String id, Document rdf )
    {
        this.id = id;
        this.rdf = rdf;
    }

    /**
     * Get the record's unique identifier.
    **/
    public String recordID()
    {
        return id;
    }

    /**
     * Convert the record to RDF/XML.
    **/
    public Document toRDFXML()
    {
        return rdf;
    }
}
