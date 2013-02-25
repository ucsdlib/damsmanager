package edu.ucsd.library.xdre.ingest.assembler;

import java.util.ArrayList;
import java.util.List;

/**
 * Simple ingesting with one ark assigned to on image file
 * @author Longshou Situ
 */
public class SimpleUploadTask extends UploadTask{
	
	public SimpleUploadTask(List fileList){
		super(fileList);
	}

	public List<Pair> generateTasks(){
		List<Pair> pairs = new ArrayList<Pair>();
		String tmpFile = null;
		for(int i=0; i<fileList.size(); i++){
			tmpFile = (String) fileList.get(i);
			if(damsRestImpl)
				pairs.add(new Pair("0-1", tmpFile));
			else
				pairs.add(new Pair("", tmpFile));
		}
		return pairs;
	}
}
