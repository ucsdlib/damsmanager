package edu.ucsd.library.xdre.ingest.assembler;

import java.util.ArrayList;
import java.util.List;

/**
 * Complex object ingesting
 * @author Longshou Situ
 */
public class ShareArkUploadTask extends UploadTask{
	
	public ShareArkUploadTask(){}
	public ShareArkUploadTask(List fileList) {
		this(fileList, null);
		//orderFiles();
	}
	
	public ShareArkUploadTask(List fileList, String[] fileOrderSuffixes) {
		super(fileList);
		this.fileOrderSuffixes = fileOrderSuffixes;
		orderFiles();
	}

	public List<Pair> generateTasks(){
		List<Pair> pairs = new ArrayList<Pair>();
		String tmp = null;
		int fileOrder = 1;
		for(int i=0; i<fileList.size(); i++){
			int matchFound = -1;
			tmp = (String) fileList.get(i);
			if(fileOrderSuffixes != null){
				
				for(int j=i; j<fileOrderSuffixes.length; j++){
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
				pairs.add(new Pair("0-"+fileOrder, tmp));
			else{
				if(i == 0 && fileOrder == 1)
					pairs.add(new Pair("", tmp));
				else
					pairs.add(new Pair(versionNo + "-" + fileOrder, tmp));
			}
		}
		return pairs;
	}
	
	protected void orderFiles(){
		if(fileOrderSuffixes != null && fileOrderSuffixes.length > 0){
			int groupSize = fileList.size();
			int extsSize = fileOrderSuffixes.length;
			String[] filesArr = new String[(groupSize>=extsSize?groupSize:extsSize)];
			
			for(int i=0; i<groupSize; i++){
				String tmp = (String) fileList.remove(0);
				String suffix = null;
				int suffixMatched = -1;
				for(int j=0; j< fileOrderSuffixes.length; j++){
					suffix = fileOrderSuffixes[j];
					if(suffix!= null && tmp.endsWith(suffix)){
						if(suffixMatched >= 0){
							if(fileOrderSuffixes[suffixMatched].length() < fileOrderSuffixes[j].length())
								suffixMatched = j;
						}else
							suffixMatched = j;
					}
				}
				if(suffixMatched >= 0){
					filesArr[suffixMatched] = tmp;						
				}else
					//Add to file list when there's no matches
					fileList.add(tmp);
			}
			
			for(int i=0; i<filesArr.length; i++){
				String fileMatched = (String)filesArr[i];
				if(fileMatched != null){
					if(i < fileList.size())
						fileList.add(i, fileMatched);
					else
						fileList.add(fileMatched);
				}
			}
		}
	}
}
