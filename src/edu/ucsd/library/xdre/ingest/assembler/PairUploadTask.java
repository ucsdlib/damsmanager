package edu.ucsd.library.xdre.ingest.assembler;

import java.util.ArrayList;
import java.util.List;

/**
 * Upload a master images file with its master-edited file
 * 
 * @author Longshou Situ
 */
public class PairUploadTask extends UploadTask{

	private String editedFilePostfix = "-edited";
	public PairUploadTask(List fileList) {
		super(fileList);
	}

	public List generateTasks(){
		List<Pair> pairs = new ArrayList<Pair>();
		String tmp = null;
		for(int i=0; i< fileList.size(); i++){
			tmp = (String) fileList.get(i);
			//Only have the image edited file for now
			if(tmp.indexOf(editedFilePostfix) > 0){
				if(damsRestImpl)
					pairs.add(new Pair("0-" + 4, tmp));
				else
					pairs.add(new Pair(versionNo + "-" + 4, tmp));
			}else{
				if(damsRestImpl)
					pairs.add(new Pair("0-1", tmp));
				else
					pairs.add(new Pair("", tmp));
			}
		}
		return pairs;
	}

	public String getEditedFilePostfix() {
		return editedFilePostfix;
	}

	public void setEditedFilePostfix(String editedFilePostfix) {
		this.editedFilePostfix = editedFilePostfix;
	}
	
	
}
