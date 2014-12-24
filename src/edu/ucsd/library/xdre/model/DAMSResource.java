package edu.ucsd.library.xdre.model;

import org.dom4j.Element;
import org.dom4j.Namespace;
import edu.ucsd.library.xdre.utils.Constants;

/**
 * Abstract class for DAMSResource
 * @author lsitu
 *
 */
public abstract class DAMSResource {
	protected String damsURI = null;
	protected String madsURI = null;
	protected String rdfURI = null;
	protected Namespace madsNS = null;
	protected Namespace damsNS = null;
	protected Namespace rdfNS = null;

	protected String id = null;
	protected String type = null;

	public abstract Element serialize() throws Exception;

	public DAMSResource (String id, String type) {
		this.id = id;
		this.type = type;
	    madsURI = Constants.NS_PREFIX_MAP.get("mads");
	    damsURI = Constants.NS_PREFIX_MAP.get("dams");
	    rdfURI = Constants.NS_PREFIX_MAP.get("rdf");
	    madsNS = new Namespace("mads", madsURI);
	    damsNS = new Namespace("dams", damsURI);
	    rdfNS = new Namespace("rdf", rdfURI);
	}
	
	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
