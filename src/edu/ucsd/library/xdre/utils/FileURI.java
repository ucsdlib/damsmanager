package edu.ucsd.library.xdre.utils;

/**
 * Parts of a file ID
 * @author lsitu
 *
 */
public class FileURI {
	private String object = null;
	private String component = null;
	private String fileName = null;
	
	public FileURI(String object, String component, String fileName){
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
	
	public boolean isComponentFile(){
		return component != null && component.length()>0;
	}
	
	public String toString(){
		return (object!=null&&object.length()>0?object+"/":"") + (component!=null&&component.length()>0?component+"/":"")+fileName;
	}
	
	/**
	 * Parse a file URL for the file ID parts
	 * @param fileURI
	 * @param object
	 * @return
	 */
	public static FileURI toParts(String fileURI, String object){
		String component = null;
		String fileName = null;
		if(object != null && object.length() > 0 && fileURI.indexOf(object) == 0){
			fileURI = fileURI.substring(object.length()+1);
			int idx = fileURI.indexOf("/");
			if(idx > 0){
				component = fileURI.substring(0, idx);
				fileName = fileURI.substring(idx + 1);
			} else
				fileName = fileURI;
		} else {
			String[] tmp = fileURI.split(fileURI);
			int len = tmp.length;
			try {
				Integer.parseInt(tmp[len - 2]);
				object = fileURI.substring(0, fileURI.indexOf(tmp[len-2])-1);
				component = tmp[len-2];
				fileName = tmp[len-1];
			}catch(NumberFormatException ne){
				object = fileURI.substring(0, fileURI.indexOf(tmp[len-1])-1);
				fileName = tmp[len-1];
			}
		}
		return new FileURI(object, component, fileName);
	}
}
