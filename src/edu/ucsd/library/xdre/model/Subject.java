package edu.ucsd.library.xdre.model;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentFactory;
import org.dom4j.Element;
import org.dom4j.Namespace;
import org.dom4j.QName;

import edu.ucsd.library.xdre.tab.TabularRecordBasic;

public abstract class Subject extends DAMSResource {
    protected String type  = null;
    protected String elementName  = null;
    protected String term = null;
    protected String exactMatch = null;
    protected String closeMatch = null;

    public Subject(String id, String type, String term, String elementName, String exactMatch, String closeMatch) {
        super(id, type);
        this.term = term;
        this.elementName = elementName;
        this.exactMatch = exactMatch;
        this.closeMatch = closeMatch;
    }

    public String getElementName() {
        return elementName;
    }

    public void setElementName(String elementName) {
        this.elementName = elementName;
    }

    public String getTerm() {
        return term;
    }

    public void setTerm(String term) {
        this.term = term;
    }

    public String getExactMatch() {
        return exactMatch;
    }

    public void setExactMatch(String exactMatch) {
        this.exactMatch = exactMatch;
    }

    public String getCloseMatch() {
        return closeMatch;
    }

    public void setCloseMatch(String exactMatch) {
        this.closeMatch = exactMatch;
    }

    protected Element buildSubject(Namespace typeNS)
    {
        // setup subject
    	Document doc = new DocumentFactory().createDocument();
    	Element rdf = TabularRecordBasic.createRdfRoot (doc);

        Element subjectElem = rdf.addElement(new QName(getType(), typeNS));
        subjectElem.addAttribute(new QName("about", rdfNS), getId());
        subjectElem.addElement( new QName("authoritativeLabel", madsNS)).setText( getTerm() );
        if ( StringUtils.isNotBlank( getExactMatch() ) )
        {
            Element elem = subjectElem.addElement( new QName("hasExactExternalAuthority", madsNS) ) ;
            elem.addAttribute( new QName("resource", rdfNS), getExactMatch() );
        }
        if ( StringUtils.isNotBlank( getCloseMatch() ) )
        {
            Element elem = subjectElem.addElement( new QName("hasCloseExternalAuthority", madsNS) ) ;
            elem.addAttribute( new QName("resource", rdfNS), getCloseMatch() );
        }
        Element el = subjectElem.addElement( new QName("elementList", madsNS) );
        el.addAttribute( new QName("parseType", rdfNS), "Collection" );
        el.addElement(new QName(getElementName() + "Element", typeNS)).addElement(new QName("elementValue", madsNS)).setText(getTerm());

        return doc.getRootElement();
    }
}
