package edu.ucsd.library.xdre.model.Utility;

import org.json.simple.JSONObject;

/**
 * Triple attributes
 * 
 * @author lsitu
 * 
 */
public class Triple {
	private boolean isLiteral = true;
	private String subject = null;
	private String predicate = null;
	private String object = null;

	public Triple() {
	}

	public Triple(String subject, String predicate, String object) {
		this.subject = subject;
		this.predicate = predicate;
		this.object = object;
		if (object != null && object.startsWith("_:"))
			isLiteral = false;
	}

	public boolean isLiteral() {
		return isLiteral;
	}

	public void setLiteral(boolean isLiteral) {
		this.isLiteral = isLiteral;
	}

	public String getObject() {
		return object;
	}

	public void setObject(String object) {
		this.object = object;
		if (object != null && object.startsWith("_:")
				|| object.startsWith("http://")
				|| object.startsWith("https://"))
			isLiteral = false;
	}

	public void setLiteral(String object) {
		this.object = object;
		isLiteral = true;
	}

	public String getPredicate() {
		return predicate;
	}

	public void setPredicate(String predicate) {
		this.predicate = predicate;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}
	
	public String toJSONString(){
		JSONObject jsonObj = new JSONObject();
		jsonObj.put("subject", subject);
		jsonObj.put("predicate", predicate);
		jsonObj.put("object", object);
		return jsonObj.toJSONString();
	}
}
