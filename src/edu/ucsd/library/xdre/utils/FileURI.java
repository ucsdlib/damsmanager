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
	
	public boolean isFileURI(){
		return object != null && object.length()>0 && fileName!=null && fileName.indexOf(".")>0;
	}
	
	public String toString(){
		return (object!=null&&object.length()>0?object+"/":"") + (component!=null&&component.length()>0?component+"/":"")+fileName;
	}
	
	/**
	 * Parse a file URL for the file ID parts
	 * @param fileURI
	 * @param object
	 * @return
	 * @throws Exception 
	 */
	public static FileURI toParts(String fileURI, String object) throws Exception{
		String component = null;
		String fileName = null;
		int idx = -1;
		if(object != null && object.length() > 0 && fileURI.indexOf(object) == 0 && fileURI.length() > object.length()){
			fileURI = fileURI.substring(object.length()+1);
			idx = fileURI.indexOf("/");
			if(idx > 0){
				component = fileURI.substring(0, idx);
				fileName = fileURI.substring(idx + 1);
			} else
				fileName = fileURI;
		} else if ((idx=fileURI.indexOf("/ark:/")) > 0){
			fileURI = fileURI.substring(idx+7);
			String[] tmp = fileURI.split("/");
			int len = tmp.length;
			if(len == 4){
				object = tmp[1];
				component = tmp[2];
				fileName = tmp[3];
			}else if(len == 3){
				object = tmp[1];
				fileName = tmp[2];
			}else if(len == 2){
				object = tmp[1];
			}else{
				object = fileURI.substring(0, fileURI.indexOf(tmp[len-1])-1);
			}
		}else
			throw new Exception("Unhandled object/file URL format: " + fileURI);
		return new FileURI(object, component, fileName);
	}
}
