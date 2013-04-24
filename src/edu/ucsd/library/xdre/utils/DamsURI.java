package edu.ucsd.library.xdre.utils;

/**
 * Parts of a file ID
 * @author lsitu
 *
 */
public class DamsURI {
	public static final String FILE = "File";
	public static final String COMPONENT = "Component";
	public static final String OBJECT = "Object";
	private String object = null;
	private String component = null;
	private String fileName = null;
	
	public DamsURI(String object, String component, String fileName){
		this.object = object;
		this.component = component;
		this.fileName = fileName;
	}

	public String getObject() {
		return object;
	}

	public void setObject(String object) {
		this.object = object;
	}

	public String getComponent() {
		return component;
	}

	public void setComponent(String component) {
		this.component = component;
	}

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
	public boolean isComponentURI(){
		if(!isFileURI() && component != null && component.length()>0){
			try{
				Integer.parseInt(component);
				return true;
			}catch(NumberFormatException ne){
				if(component.toLowerCase().startsWith("cid"))
					return true;
			}
		}
		return false;
	}
	
	public boolean isFileURI(){
		return object != null && object.length()>0 && fileName!=null;
	}
	
	public String toString(){
		return (object!=null&&object.length()>0?object:"") + (component!=null&&component.length()>0?"/"+component:"") + (fileName!=null?"/" + fileName:"");
	}
	
	/**
	 * Parse a file URL for the file ID parts
	 * @param damsUri
	 * @param object: object id
	 * @return
	 * @throws Exception 
	 */
	public static DamsURI toParts(String damsUri, String object) throws Exception{
		return toParts(damsUri, object, null);
	}
	
	/**
	 * Parse a file URL for the file ID parts with record type
	 * @param damsUri
	 * @param object: object id
	 * @param type: File, Component, Object
	 * @return
	 * @throws Exception 
	 */
	public static DamsURI toParts(String damsUri, String object, String type) throws Exception{
		String component = null;
		String fileName = null;
		String idString = null;
		int idx = -1;
		String[] tmp = null;
		if(object != null && object.length() > 0 && damsUri.indexOf(object) == 0 && damsUri.length() > object.length()){
			// object/cid/fid
			idString = damsUri.substring(object.length()+1);
			tmp = idString.split("/");
			if (tmp.length == 2) {
				component = tmp[0];
				fileName = tmp[1];
			} else if(tmp.length == 1) {			
				if(isFileId(tmp[0], type))
					fileName = tmp[0];
				else
					component = tmp[0];	
			} else
				throw new Exception("Unknown object/file URL format: " + damsUri);
		} else if ((idx=damsUri.indexOf("ark:/")) >= 0){
			// /ark:/20775/cid/fid
			idString = damsUri.substring(idx+6);
			tmp = idString.split("/");
			int len = tmp.length;
			object = damsUri.substring(0, damsUri.indexOf(tmp[1]))+tmp[1];
			if(len == 4){
				component = tmp[2];
				fileName = tmp[3];
			}else if(len == 3){
				if(isFileId(tmp[2], type) && !tmp[2].toLowerCase().startsWith("cid"))
					fileName = tmp[2];
				else
					component = tmp[2];
			}else if(len != 2)
				throw new Exception("Unknown object/file URL format: " + damsUri);
		} else {
			// oid/cid/fid
			tmp = damsUri.split("/");
			int len = tmp.length;
			if(tmp.length > 3)
				throw new Exception("Unhandled object/file URL format: " + damsUri);
			object = damsUri.substring(0, damsUri.indexOf(tmp[0]))+tmp[0];
			if(len == 3){
				component = tmp[1];
				fileName = tmp[2];
			} else if(len == 2){
				if(isFileId(tmp[1], type) && !tmp[1].toLowerCase().startsWith("cid"))
					fileName = tmp[1];
				else
					component = tmp[1];
			}
		}
		return new DamsURI(object, component, fileName);
	}
	
	/**
	 * Determine a file id
	 * @param value
	 * @param type
	 * @return
	 */
	private static boolean isFileId(String value, String type){
		boolean isFid = false;
		if(type != null && type.endsWith(FILE)){
			isFid = true;
		} else {
			// Guess it as a component when there is no file extension
			try{
				Integer.parseInt(value);
			}catch (NumberFormatException ne){
				isFid = true;
			}
		}
		return isFid;
	}
}
