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
	
	public boolean isComponentURI(){
		if(component != null && component.length()>0){
			try{
				Integer.parseInt(component);
				return true;
			}catch(NumberFormatException ne){}
		}
		return false;
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
		String idString = null;
		int idx = -1;
		if(object != null && object.length() > 0 && fileURI.indexOf(object) == 0 && fileURI.length() > object.length()){
			idString = fileURI.substring(object.length()+1);
			idx = idString.indexOf("/");
			if(idx > 0){
				component = idString.substring(0, idx);
				fileName = idString.substring(idx + 1);
			} else {
				try{
					Integer.parseInt(idString);
					component = idString;
				}catch (NumberFormatException ne){
					fileName = idString;
				}
			}
		} else if ((idx=fileURI.indexOf("/ark:/")) > 0){
			idString = fileURI.substring(idx+7);
			String[] tmp = idString.split("/");
			int len = tmp.length;
			object = fileURI.substring(0, fileURI.indexOf(tmp[1]))+tmp[1];
			if(len == 4){
				component = tmp[2];
				fileName = tmp[3];
			}else if(len == 3){
				try{
					Integer.parseInt(tmp[2]);
					component = tmp[2];
				}catch (NumberFormatException ne){
					fileName = tmp[2];
				}
			}else if(len != 2)
				throw new Exception("Unknown object/file URL format: " + fileURI);
		}else
			throw new Exception("Unhandled object/file URL format: " + fileURI);
		return new FileURI(object, component, fileName);
	}
}
