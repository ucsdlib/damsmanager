package edu.ucsd.library.xdre.ingest.assembler;

import java.util.ArrayList;
import java.util.List;

/**
 * Complex object ingesting
 * @author Longshou Situ
 */
public class ComplexObjectUploadTask extends UploadTask{

	public ComplexObjectUploadTask(List fileList) {
		super(fileList);
	}

	public List<Pair> generateTasks(){
		List<Pair> pairs = new ArrayList<Pair>();
		String tmp = null;
		for(int i=0; i<fileList.size(); i++){
			tmp = (String) fileList.get(i);
			if(damsRestImpl)
				pairs.add(new Pair((i+1) + "-1", tmp));
			else
				pairs.add(new Pair(versionNo + "-" + (i + 1), tmp));
		}
		return pairs;
	}
	
	public int getComponentsCount(){
		return fileList.size();
	}
}
