package edu.ucsd.library.xdre.ingest.assembler;

/**
 * Properties for a uploading file
 * @author lsitu
 *
 */
public class Pair {
	private String key = null;
	private String value = null;
	
	public Pair(String key, String value){
		this.key = key;
		this.value = value;
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}
	
}
