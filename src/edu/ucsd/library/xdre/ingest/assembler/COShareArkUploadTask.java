package edu.ucsd.library.xdre.ingest.assembler;

import java.util.ArrayList;
import java.util.List;

/**
 * Complex object with derivatives ingesting
 * @author Longshou Situ
 */
public class COShareArkUploadTask extends ShareArkUploadTask{
	private List<List<String>> coList = null;
	private int numOfComponents = 0;
	public COShareArkUploadTask(List<List<String>> coList) {
		this(coList, null);
	}
	
	public COShareArkUploadTask(List<List<String>> coList, String[] fileOrderSuffixes) {
		super();
		this.coList = coList;
		this.fileOrderSuffixes = fileOrderSuffixes;
	}

	public List<Pair> generateTasks(){
		List<Pair> pairs = new ArrayList<Pair>();
		String tmp = null;
		int fileOrder = 1;
		for(int n=0; n<coList.size(); n++){
			fileList = coList.get(n);
			orderFiles();
			for(int i=0; i<fileList.size(); i++){
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
					fileOrder = matchFound + 1;
				else
					fileOrder = 1;
				
				if(damsRestImpl)
					pairs.add(new Pair((n+1) + "-" + fileOrder, tmp));
				else{
					if(fileOrder == 1){
						pairs.add(new Pair(versionNo + "-" + (n+1), tmp));
						numOfComponents++;
					}else
						pairs.add(new Pair(versionNo  + "-" + (n+1) + "-" + fileOrder, tmp));
				}
			}
		}
		return pairs;
	}
	
	public int getComponentsCount(){
		if(numOfComponents == 0)
			generateTasks();
		return numOfComponents;
	}
}
