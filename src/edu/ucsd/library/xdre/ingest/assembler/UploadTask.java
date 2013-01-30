package edu.ucsd.library.xdre.ingest.assembler;

import java.util.List;

import edu.ucsd.library.xdre.ingest.assembler.UploadTaskOrganizer.PreferedOrder;


/**
 * Abstract Class to perform the Ingesting task
 * @author Longshou Situ
 */
public abstract class UploadTask{

	protected int versionNo = 1;
	protected boolean damsRestImpl = true;
	protected List<String> fileList = null;
	protected PreferedOrder preferOrder = null;
	
	public UploadTask(){}
	public UploadTask(List<String> fileList){
		this.fileList = fileList;
	}
		
	/**
	 * Process the request
	 * @return
	 * @throws Exception
	 */
	public abstract List<Pair> generateTasks();
	
	public int getComponentsCount(){
		return 1;
	}

	public List<String> getFileList() {
		return fileList;
	}

	public void setFileList(List<String> fileList) {
		this.fileList = fileList;
	}

	public int getVersionNo() {
		return versionNo;
	}

	public void setVersionNo(int versionNo) {
		this.versionNo = versionNo;
	}
	
	public boolean isDamsRestImpl() {
		return damsRestImpl;
	}
	
	public void setDamsRestImpl(boolean damsRestImpl) {
		this.damsRestImpl = damsRestImpl;
	}

	public PreferedOrder getPreferOrder() {
		return preferOrder;
	}

	public void setPreferOrder(PreferedOrder preferOrder) {
		this.preferOrder = preferOrder;
	}
}
