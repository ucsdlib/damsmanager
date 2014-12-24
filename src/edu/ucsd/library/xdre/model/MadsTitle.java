package edu.ucsd.library.xdre.model;

import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.QName;

public class MadsTitle extends DAMSResource {
	private String type = null;
	private String title = null;

	public MadsTitle(String title){
		super(null, "Title");
		this.title= title;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public Element serialize(){
		Element titleElem = DocumentHelper.createElement(new QName("Title", madsNS));
		titleElem.addElement(new QName("authoritativeLabel", madsNS)).setText(title);
		Element elemList = titleElem.addElement("mads:elementList",madsURI);
		elemList.addAttribute(new QName("parseType",rdfNS), "Collection");
		elemList.addElement(new QName("MainTitleElement", madsNS)).addElement(new QName("elementValue", madsNS)).setText(title);
		 return titleElem;
	}
}
