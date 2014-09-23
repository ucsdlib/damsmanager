package edu.ucsd.library.xdre.tab;

import java.io.ByteArrayInputStream;
import java.io.FilterInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.util.Iterator;
import org.dom4j.Document;
import org.dom4j.Element;

/**
 * InputStream implementation that reads a TabularSource, converts it to
 * RDF/XML and provides the result as a stream.
 * @author escowles
 * @since 2014-06-10
**/
public class TabularInputStream extends FilterInputStream
{
    /**
     * Convert all records from a TabularSource and build a single RDF/XML
     * document.
     * @throws ParseException 
    **/
    public TabularInputStream( ExcelSource source ) throws ParseException
    {
        super( toStream(source) );
    }

	/**
	 * Convert a TabularSource to RDF/XML and return the result as a stream.
	 * @throws ParseException 
	**/
    public static InputStream toStream( ExcelSource source ) throws ParseException
    {
        return new ByteArrayInputStream( toString(source).getBytes() );
    }

	/**
	 * Convert a TabularSource to RDF/XML and return the result as a string.
	 * Convert a ExcelSource to RDF/XML and return the result as a string.
	**/
    public static String toString( ExcelSource source ) throws ParseException
    {
        Document doc = null;
        Element root = null;
        for ( TabularRecord rec = null; (rec = source.nextRecord()) != null; )
        {
            String id = rec.recordID();
            Document tmp = rec.toRDFXML();
            if ( doc == null )
            {
                // use the first document
                doc = tmp;
                root = doc.getRootElement();
            }
            else
            {
                // add this document to the bundle
                Iterator it = tmp.getRootElement().elementIterator();
                if ( it.hasNext() )
                {
                    Element e = (Element)it.next();
                    root.add( e.detach() );
                }
            }
        }
        return doc.asXML();
    }
}
