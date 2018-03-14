package edu.ucsd.library.xdre.model;

import org.dom4j.Element;

public class MadsSubject extends Subject {
    public MadsSubject(String id, String type, String term, String elementName, String exactMatch, String closeMatch) {
        super(id, type, term, elementName, exactMatch, closeMatch);
    }

    public Element serialize(){
         return buildSubject( madsNS );
    }
}
