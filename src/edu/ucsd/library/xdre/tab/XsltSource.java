package edu.ucsd.library.xdre.tab;

import java.io.File;
import java.io.IOException;

// dom4j
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.DocumentResult;
import org.dom4j.io.DocumentSource;
import org.dom4j.io.SAXReader;

// xsl
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamSource;

/**
 * RecordSource implementation that transforms XML files with XSLT.
 * @author escowles
 * @since 2014-09-17
**/
public class XsltSource implements RecordSource
{
    private Record record;
    boolean done = false;

    /**
     * Create an ExcelSource object from an Excel file on disk.
    **/
    public XsltSource( File xsl, File xml )
            throws IOException, DocumentException, TransformerException
    {
        // parse xml
        SAXReader reader = new SAXReader();
        Document document = reader.read(xml);

        // get id from METS header
        String id = document.getRootElement().attributeValue("OBJID");
        if ( id == null )
        {
            // fall back on filename
            id = xml.getName().replaceAll("\\..*","");
        }

        // parse xsl
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer( new StreamSource( xsl ) );

        // xslt
        DocumentSource source = new DocumentSource( document );
        DocumentResult result = new DocumentResult();
        transformer.transform( source, result );
        this.record = new SimpleRecord( id, result.getDocument() );
    }

    @Override
    public Record nextRecord()
    {
        if ( done )
        {
            return null;
        }
        else
        {
            done = true;
            return record;
        }
    }
}
