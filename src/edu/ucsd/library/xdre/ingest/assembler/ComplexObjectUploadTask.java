package edu.ucsd.library.xdre.ingest.assembler;

import java.util.ArrayList;
import java.util.List;

/**
 * Complex object ingesting
 * @author Longshou Situ
 */
public class ComplexObjectUploadTask extends UploadTask{

	public ComplexObjectUploadTask(List fileList) {
		this(fileList, null);
	}
	
	public ComplexObjectUploadTask(List fileList, String[] fileOrderSuffixes){
		super(fileList);
		this.fileOrderSuffixes = fileOrderSuffixes;
	}

	public List<Pair> generateTasks(){
		List<Pair> pairs = new ArrayList<Pair>();
		String tmp = null;

		for(int i=0; i<fileList.size(); i++){
			int compOrder = i + 1;
			int matchFound = -1;
			tmp = (String) fileList.get(i);
			
			if(fileOrderSuffixes != null){
				
				for(int j=0; j<fileOrderSuffixes.length; j++){
					if(fileOrderSuffixes[j] != null && tmp.endsWith(fileOrderSuffixes[j])){						
						//Handle the matching like xxx.tif, .tif
						if(	matchFound >= 0 ){
							if(fileOrderSuffixes[j].length() > fileOrderSuffixes[matchFound].length())
								matchFound = j;							
						}else
							matchFound = j;
					}
				}
			}
			
			if(matchFound >= 0)
				compOrder = matchFound + 1;

			if(damsRestImpl)
				pairs.add(new Pair(compOrder + "-1", tmp));
			else
				pairs.add(new Pair(versionNo + "-" + compOrder, tmp));
		}
		return pairs;
	}
	
	public int getComponentsCount(){
		return fileList.size();
	}
}
