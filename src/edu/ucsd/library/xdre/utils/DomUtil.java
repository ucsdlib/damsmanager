package edu.ucsd.library.xdre.utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.XMLWriter;
import org.jdom.input.DOMBuilder;
import org.xml.sax.SAXException;

/**
 * Class DomUtil, an utility class to create document and elements
 *
 * @author lsitu@ucsd.edu
 */
public class DomUtil {
	public static Document createDocument() {
        return DocumentHelper.createDocument();
    }
	
	public static Element createElement(Element parent, String tagName,
			      List attrs, String text){
		Element elem = null;
		if(parent == null)
			elem = DocumentHelper.createElement(tagName);
		else
		    elem = parent.addElement(tagName);
		if(attrs != null)
			elem.setAttributes(attrs);
		if(text != null)
			elem.addText(text);
		return elem;
	}

	public static void outputXml(PrintWriter out, Document doc) throws IOException{
        OutputFormat format = OutputFormat.createPrettyPrint();
        format = OutputFormat.createCompactFormat();
        XMLWriter writer = new XMLWriter( out, format );
        writer.write( doc );
	}
	
	public static org.jdom.Document parseXml(String xmlData) throws ParserConfigurationException, SAXException, IOException{
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		//factory.setValidating(true);
		factory.setNamespaceAware(true);
		DocumentBuilder builder = factory.newDocumentBuilder();
		InputStream in = new ByteArrayInputStream(xmlData.getBytes());
		org.w3c.dom.Document w3cDoc = builder.parse(in);
		DOMBuilder domBuilder = new DOMBuilder();
		//builder.setValidation(false);
		return domBuilder.build(w3cDoc);
	}
}
