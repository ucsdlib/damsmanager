package edu.ucsd.library.xdre.model;

import org.dom4j.Element;

public class DamsSubject extends Subject {
    public DamsSubject(String id, String type, String term, String elementName, String exactMatch, String closeMatch) {
        super(id, type, term, elementName, exactMatch, closeMatch);
    }

    public Element serialize(){
        return buildSubject( damsNS );
    }
}
