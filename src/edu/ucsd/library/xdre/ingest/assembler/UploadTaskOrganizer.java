package edu.ucsd.library.xdre.ingest.assembler;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.commons.io.FilenameUtils;
import org.apache.log4j.Logger;

/**
 * UploadTaskIterator return a UploadTask for SRB loading each time when call for hasNext() and next() 
 * There are three types of Upload tasks: simple loading with one ark for one file, complex object loading
 * and pair images loading. File filters may be applied for file prefix and post-fix
 * 
 * @author Longshou Situ
 */
public class UploadTaskOrganizer{
	
	public static final int SIMPLE_LOADING = 0;
	public static final int COMPLEXOBJECT_LOADING = 1;
	public static final int MIX_LOADING = 2;
	public static final int PAIR_LOADING = 3;
	public static final int SHARE_ARK_LOADING = 4;
	public static final int MIX_CO_SHARE_ARK_LOADING = 5;
	
	public static enum PreferedOrder {PDFANDPDF, PDFANDXML, SUFFIX};
	
	private static Logger log = Logger.getLogger(UploadTaskOrganizer.class);
	
	private int versionNo = 1;
	private int currIndex = 0;
	//private String collectionId = null;
	private List<List> uploadList = null;
	private int uploadType = SIMPLE_LOADING;
	private String filePrefix = null;
	private String fileFilter = null;
	private String coDelimiter = null;
	private String singleKey = "singleList";
	private String message = "";
	private String[] fileOrderSuffixes = null;
	private int taskSize = 0;
	private PreferedOrder preferedOrder = null;
	
	public UploadTaskOrganizer(List<String> fileList, int uploadType){
		this(fileList, uploadType, null);
	}
	
	public UploadTaskOrganizer(List<String> fileList, int uploadType, String fileFilter){
		this(fileList, uploadType, fileFilter, null);
	}
	
	public UploadTaskOrganizer(List<String> fileList, int uploadType, String fileFilter, String[] fileOrderSuffixes){
		this(fileList, uploadType, fileFilter, fileOrderSuffixes, null);
	}
	
	public UploadTaskOrganizer(List<String> fileList, int uploadType, String fileFilter, String[] fileOrderSuffixes, String coDelimiter){
		this(fileList, uploadType, fileFilter, fileOrderSuffixes, coDelimiter, null);
	}
	
	public UploadTaskOrganizer(List<String> fileList, int uploadType, String fileFilter, String[] fileOrderSuffixes, String coDelimiter, PreferedOrder preferedOrder){
		//this.collectionId = collectionId;
		this.uploadType = uploadType;
		this.fileFilter = fileFilter;
		this.fileOrderSuffixes = fileOrderSuffixes;
		this.coDelimiter = coDelimiter;
		this.preferedOrder = preferedOrder;
		uploadList = initUploadList(fileList, preferedOrder);

		taskSize = uploadList.size();
	}

	/**
	 * 
	 * @return List<List>, batches of files for uploading under diferent rules
	 */
	private List<List> initUploadList(List<String> fileList, PreferedOrder preferedOrder){
		List<List> uploadList = new ArrayList<List>();
		Map<String, Map> filesPoolMap = null;
		Iterator it = null;
		Iterator coIt = null;
		for(int i=0; i<fileList.size(); i++){					
			String tmpFileName = fileList.get(i);
			
			File tmpFile = new File(tmpFileName);
			if(!tmpFile.exists()){
				message += "File " + tmpFile.getAbsolutePath() + " doesn't exist.\n";
				log.error(message);
				System.out.println(message);
				continue;
			}
			switch (uploadType){
				case PAIR_LOADING:
					Map<String, List> filesPool = new HashMap<String, List>();
					
					appendPairFileToPool(filesPool, tmpFile);
					it = filesPool.values().iterator();
					while(it.hasNext()){
						uploadList.add((List) it.next());
					}
					break;
				case COMPLEXOBJECT_LOADING:
					//Files in the same directory are consider to be complex objects
					filesPoolMap = new HashMap<String, Map>();
					appendComplexObjectToPool(filesPoolMap, tmpFile, (fileOrderSuffixes != null&&(preferedOrder==null || !preferedOrder.equals(PreferedOrder.SUFFIX))));
					coIt = filesPoolMap.keySet().iterator();
					
					while(coIt.hasNext()){
						String path = (String) coIt.next();
						Map tmpMap = filesPoolMap.get(path);
						it = orderFiles(tmpMap, coDelimiter, preferedOrder);
						List<Object> cos = new ArrayList<Object>();
						uploadList.add(cos);
						while(it.hasNext())
							cos.add((Object) it.next());
					}
					
					break;
				case MIX_LOADING:
					//Mixing of single objects and complex object in the same directory with prefix+pxx+.tif
					//to indentify the order and the complex objects
					filesPoolMap = new HashMap<String, Map>();
					appendMixComplexObjectToPool(filesPoolMap, tmpFile);

					Map singleObjects = filesPoolMap.remove(singleKey);
					if(singleObjects != null && singleObjects.size() > 0){
						Iterator sIt = singleObjects.values().iterator();
						while(sIt.hasNext()){
							uploadList.add(getNewFileGroup((String) sIt.next()));
						}
					}
					coIt = filesPoolMap.keySet().iterator();	
					while(coIt.hasNext()){
						String path = (String) coIt.next();
						Map tmpMap = filesPoolMap.get(path);
						it = handleOrdering(tmpMap, coDelimiter);
						List<String> cos = new ArrayList<String>();
						uploadList.add(cos);
						while(it.hasNext())
							cos.add((String) it.next());
					}
					
					break;
				case SHARE_ARK_LOADING:
					//Mixing of single objects and objects with the same name and different extension 					
					//which should share one ark in the same directory with filename.xxx
					//to indentify the order and the derivatives
					filesPoolMap = new HashMap<String, Map>();
					appendShareArkObjectToPool(filesPoolMap, tmpFile);

					coIt = filesPoolMap.keySet().iterator();	
					while(coIt.hasNext()){
						List<String> sos = new ArrayList<String>();
						String key = (String) coIt.next();
						Map tmpMap = filesPoolMap.get(key);
						Iterator fIt = tmpMap.values().iterator();
						while(fIt.hasNext())
							sos.add((String)fIt.next());
						if(sos.size() > 0)
							uploadList.add(sos);						
					}					
					break;
				case MIX_CO_SHARE_ARK_LOADING:
					//Mixing of single objects and objects with the same name and different extension 					
					//which should share one ark in the same directory with filename.xxx
					//to indentify the order and the complex objects and their derivatives
					filesPoolMap = new HashMap<String, Map>();
					appendShareArkObjectToPool(filesPoolMap, tmpFile);
					coIt = filesPoolMap.keySet().iterator();
					Map<String, Map<String, List<String>>> coFilesMap = new HashMap<String, Map<String, List<String>>>();
					while(coIt.hasNext()){
						List<String> sos = new ArrayList<String>();
						String key = (String) coIt.next();
						Map tmpMap = filesPoolMap.get(key);
						Iterator fIt = tmpMap.values().iterator();
						while(fIt.hasNext())
							sos.add((String)fIt.next());
						if(sos.size() > 0){
							String coKey = key;
							int udIndex = coKey.lastIndexOf(coDelimiter);
							int delLength = coDelimiter.length();
							if(udIndex>0  && coKey.length()>udIndex+delLength){
								try{
									//Test for possible complex objects
									int order = Integer.parseInt(coKey.substring(udIndex + delLength));
									coKey = key.substring(0, udIndex);
								}catch(NumberFormatException e){}								
							}
							Map<String, List<String>> componentFilesMap = coFilesMap.get(coKey);
							if(componentFilesMap == null){
								componentFilesMap = new HashMap<String, List<String>>();
								coFilesMap.put(coKey, componentFilesMap);
							}
							componentFilesMap.put(key, sos);						
						}
					}
					
					//Handling complex object ordering
					coIt = coFilesMap.keySet().iterator();
					while(coIt.hasNext()){
						String path = (String) coIt.next();
						Map tmpMap = coFilesMap.get(path);
						it = handleOrdering(tmpMap, coDelimiter);
						List<List<String>> cos = new ArrayList<List<String>>();
						uploadList.add(cos);
						while(it.hasNext())
							cos.add((List<String>) it.next());
					}
					break;
			default:
				appendFiles(uploadList, tmpFile);
				break;
					
			}
		}
		return uploadList;
	}
		
	public boolean hasNext() {		
		return currIndex < taskSize;
	}

	public UploadTask next() {
		UploadTask uploadTask = null;
		List taskList = uploadList.get(currIndex++);
		switch (uploadType){
			case PAIR_LOADING:
				uploadTask = new PairUploadTask(taskList);
				break;
			case COMPLEXOBJECT_LOADING:
				//taskList = uploadList.get(currIndex++);
				if(taskList.size() > 1){
					if(fileOrderSuffixes == null || (preferedOrder!=null && preferedOrder.equals(PreferedOrder.SUFFIX)))
						uploadTask = new ComplexObjectUploadTask(taskList);
					else
						uploadTask = new COShareArkUploadTask(taskList, fileOrderSuffixes);
				}else if(taskList.size() == 1){
					if(fileOrderSuffixes == null)
						uploadTask = new SimpleUploadTask(taskList);
					else
						uploadTask = new ShareArkUploadTask((List<String>)taskList.get(0), fileOrderSuffixes);
				}	
				
				break;
			case MIX_LOADING:
				//taskList = uploadList.get(currIndex++);
				if(taskList.size() > 1)
					uploadTask = new ComplexObjectUploadTask(taskList);
				else
					uploadTask = new SimpleUploadTask(taskList);
				break;
			case SHARE_ARK_LOADING:
				//taskList = uploadList.get(currIndex++);
				if(taskList.size() > 1)
					uploadTask = new ShareArkUploadTask(taskList, fileOrderSuffixes);
				else
					uploadTask = new SimpleUploadTask(taskList);
				
				//uploadTask.setFileOrderSuffixes(fileOrderSuffixes);
				break;
			case MIX_CO_SHARE_ARK_LOADING:
				//taskList = uploadList.get(currIndex++);
				
				if(taskList.size() > 1)
					uploadTask = new COShareArkUploadTask(taskList, fileOrderSuffixes);
				else if(taskList.size() == 1){
					uploadTask = new ShareArkUploadTask((List<String>)taskList.get(0), fileOrderSuffixes);
				}
				break;
			default:
				uploadTask = new SimpleUploadTask(taskList);
		}
		uploadTask.setPreferOrder(preferedOrder);
		return uploadTask;
	}
	
	private void appendFiles(List<List> uploadList, File file){
		if(file.isDirectory()){
			File[] fileList = file.listFiles();				
			for(int i=0; i<fileList.length; i++){
				File tmpFile = fileList[i];
				
				if(tmpFile.isDirectory()){
					appendFiles(uploadList, tmpFile);
				}else if(tmpFile.isFile() && !tmpFile.isHidden()){
					
					addFileGroup(tmpFile, uploadList);
				}
			}
		}else if(file.isFile() && !file.isHidden()){
			addFileGroup(file, uploadList);
		}
	}
	
	private void appendPairFileToPool(Map<String, List>filesPool, File tmpFile){
		if(tmpFile.isDirectory()){
			File[] tmpFiles = tmpFile.listFiles();
			File tmp = null;
			for(int i=0; i<tmpFiles.length; i++){
				tmp = tmpFiles[i];
				appendPairFileToPool(filesPool, tmp);
			}
		}else if(tmpFile.isFile() && !tmpFile.isHidden()){			
			addPairFileToPool(filesPool, tmpFile);
		}		
	}

	private void addPairFileToPool(Map<String, List> pairPool, File tmpFile){
		String fName = tmpFile.getName();
		//String tmpFileName = tmpFile.getAbsolutePath();
		String pairId = fName.substring(0, fName.lastIndexOf('.'));
		List<String> pairList = null;
		//int eIndex = pairId.endsWith("-edited");						 
		if(pairId.endsWith("-edited")){
			pairId = pairId.substring(0, pairId.indexOf("-edited"));
			pairList = pairPool.get(pairId);
			if(pairList == null){
				addPairGroup(pairId, tmpFile, pairPool);
			}else{
				addFile(tmpFile, pairList);
			}
		}else{
			//Master file
			pairList = pairPool.get(pairId);
			if(pairList == null){
				addPairGroup(pairId, tmpFile, pairPool);
			}else{
				addFile(tmpFile, pairList);
			}
		}
	}
	
	private void appendComplexObjectToPool(Map<String, Map> filesPoolMap, File tmpFile, boolean withDerivative){
		
		Map<String, Object> complexObjectMap = null;
		if(tmpFile.isDirectory()){
			File[] fileList = tmpFile.listFiles();
			for(int i=0; i<fileList.length; i++){
				File tmp = fileList[i];
				appendComplexObjectToPool(filesPoolMap, tmp, withDerivative);
			}
		}else if(tmpFile.isFile() && !tmpFile.isHidden()){
			boolean addFile = isFileValid(tmpFile, fileFilter);
			if (addFile) {			
				String key = null;
				String filePath = getFilePathValue(tmpFile);
				
				String fName = tmpFile.getName();			
				complexObjectMap = filesPoolMap.get(filePath);
				if(withDerivative){
					//int dIndex = fName.lastIndexOf('.');
					//key = fName.substring(0, (dIndex>0?dIndex:fName.length()));
					key = getGroupKey(fName);
					if(complexObjectMap == null){
						complexObjectMap = new TreeMap<String, Object>();
						filesPoolMap.put(filePath, complexObjectMap);
					}
						
					List<String> fList = (List<String>) complexObjectMap.get(key);
					if(fList == null){
						fList = new ArrayList<String>();
						complexObjectMap.put(key, fList);
					}
					fList.add(tmpFile.getAbsolutePath());	
				}else{
					key = fName;
					
					if(complexObjectMap == null){
						complexObjectMap = new TreeMap<String, Object>();
						filesPoolMap.put(filePath, complexObjectMap);
					}
					complexObjectMap.put(key, tmpFile.getAbsolutePath());							
				}
			}
		}
	}
	
	private void appendMixComplexObjectToPool(Map<String, Map> filesPoolMap, File tmpFile){
		
		Map<String, String> complexObjectMap = null;
		if(tmpFile.isDirectory()){
			File[] fileList = tmpFile.listFiles();
			for(int i=0; i<fileList.length; i++){
				File tmp = fileList[i];
				appendMixComplexObjectToPool(filesPoolMap, tmp);
			}
		}else if(tmpFile.isFile() && !tmpFile.isHidden()){
			boolean addFile = isFileValid(tmpFile, fileFilter);
			if (addFile) {	
				String fName = tmpFile.getName();
				int dIndex = fName.lastIndexOf('.');
				String key = fName.substring(0, (dIndex>0?dIndex:fName.length()));			
				int indexp = (key.toLowerCase()).lastIndexOf(coDelimiter);
				int delLength = coDelimiter.length();
				boolean isComplexObject = false;
				if(indexp > 0 && indexp != key.length() - delLength){
					String orderNum = key.substring(indexp + delLength, key.length());
					try{
						//Test for possible complex objects
						int order = Integer.parseInt(orderNum);
						isComplexObject = true;
						String filePath = getFilePathValue(tmpFile);
						complexObjectMap = filesPoolMap.get(filePath);
						if(complexObjectMap == null){
							complexObjectMap = new TreeMap<String, String>();
							filesPoolMap.put(filePath, complexObjectMap);
							complexObjectMap.put(key, tmpFile.getAbsolutePath());							
						}else{
							complexObjectMap.put(key, tmpFile.getAbsolutePath());
						}
					}catch (NumberFormatException ne){}
				}
				if(!isComplexObject){
					complexObjectMap = filesPoolMap.get(singleKey);
					if(complexObjectMap == null){
						complexObjectMap = new HashMap<String, String>();
						filesPoolMap.put(singleKey, complexObjectMap);
						complexObjectMap.put(tmpFile.getAbsolutePath(), tmpFile.getAbsolutePath());							
					}else{
						complexObjectMap.put(tmpFile.getAbsolutePath(), tmpFile.getAbsolutePath());
					}
				}
			}
		}
	}
	
	private void appendShareArkObjectToPool(Map<String, Map> filesPoolMap, File tmpFile){
		
		Map<String, String> shareArkObjectMap = null;
		if(tmpFile.isDirectory()){
			File[] fileList = tmpFile.listFiles();
			for(int i=0; i<fileList.length; i++){
				File tmp = fileList[i];
				appendShareArkObjectToPool(filesPoolMap, tmp);
			}
		}else if(tmpFile.isFile() && !tmpFile.isHidden()){
			boolean addFile = isFileValid(tmpFile, fileFilter);
			if (addFile) {	
				String key = getGroupKey(tmpFile.getName());
				
				String filePath = tmpFile.getAbsolutePath();
				shareArkObjectMap = filesPoolMap.get(key);
				if(shareArkObjectMap == null){					
					shareArkObjectMap = new HashMap<String, String>();
					filesPoolMap.put(key, shareArkObjectMap);												
				}
				shareArkObjectMap.put(filePath, filePath);
			}
		}
	}
	
	private String getGroupKey(String fileName){
		int dIndex = -1;
		int matchIndex = -1;
		if(fileOrderSuffixes != null && fileOrderSuffixes.length > 0){
			String tmp = null;
			
			for(int i=0; i< fileOrderSuffixes.length; i++){
				tmp = fileOrderSuffixes[i];
				if((fileName.endsWith(tmp))){
					//Handle the matching like xxx.tif, .tif. with longest matching
					if(	matchIndex >= 0 ){
						if(tmp.length() > fileOrderSuffixes[matchIndex].length())
							matchIndex = i;							
					}else
						matchIndex = i;
				}
			}
		}
		if(matchIndex >= 0){
			dIndex = fileName.lastIndexOf(fileOrderSuffixes[matchIndex]);
		}else
			dIndex = fileName.lastIndexOf(".");
		return fileName.substring(0, (dIndex>0?dIndex:fileName.length()));
	}
	
	private Iterator handleOrdering(Map tmpMap, String delimiter){
		
		Map<String, Object> orderMap = new TreeMap<String, Object>();
		
		if(delimiter != null && delimiter.length() > 0){
			Object[] fileList = (Object[]) tmpMap.keySet().toArray();
			
			String largestKey = (String) fileList[0];
			int keyLength = largestKey.length();
			for (int i = 0; i < fileList.length; i++) {
				String filename = (String) fileList[i];
				if (filename.length() >= keyLength) {
					keyLength = filename.length();
					largestKey = filename;
				}
			}
	
			String tmpKey = null;
			Object tmpFile = null;
			for(int i=0; i<fileList.length; i++){
				tmpKey = (String) fileList[i];
				tmpFile = (Object) tmpMap.remove(tmpKey);
				int tmpKeyLength = tmpKey.length();
				
				if(tmpKey.length() < keyLength){
					//Page ordering
					int indexp = tmpKey.lastIndexOf(delimiter);
					if (indexp > 0){
						String tmp = tmpKey.substring(indexp + delimiter.length());
						try{
							int order = Integer.parseInt(tmp);
							for(int j=0; j<(keyLength-tmpKeyLength); j++){
								tmp = "0" + tmp;
							}
							tmpKey = tmpKey.substring(0, indexp + delimiter.length()) + tmp;
						}catch(NumberFormatException ne){
							ne.printStackTrace();
						}
					}
				}
				orderMap.put(tmpKey, tmpFile);
			}
		}else{
			orderMap.putAll(tmpMap);
		}
		return orderMap.values().iterator();
	}
	
	private Iterator orderFiles(Map tmpMap, String delimiter, PreferedOrder preferedOrder){
		
		Map<String, Object> orderMap = new TreeMap<String, Object>();
		
		if(delimiter != null && delimiter.length() > 0){
			int delLength = delimiter.length();
			Object[] fileList = (Object[]) tmpMap.keySet().toArray();
			
			String orderKey = null;
			int largestOrderLen = 0;
			int indexp = -1;
			for (int i = 0; i < fileList.length; i++) {
				orderKey = (String) fileList[i];
				int idx = orderKey.lastIndexOf('.');
				if(idx > 0)
					orderKey = orderKey.substring(0, idx);
				indexp = orderKey.lastIndexOf(delimiter);
				if(indexp >= 0 && orderKey.length() > (indexp + delLength)){
					orderKey = orderKey.substring(indexp + delLength);

					if (orderKey.length() >= largestOrderLen) {
						try{
							Integer.parseInt(orderKey);
							largestOrderLen = orderKey.length();
						}catch (NumberFormatException ne){}
					}
				}
			}
	
			String tmpKey = null;
			Object tmpFile = null;
			for(int i=0; i<fileList.length; i++){
				tmpKey = (String) fileList[i];
				tmpFile = (Object) tmpMap.remove(tmpKey);
				
				orderKey = "";
				indexp = tmpKey.lastIndexOf(delimiter);
				if(indexp >= 0 && tmpKey.length() > (indexp + delLength)){
					orderKey = tmpKey.substring(indexp + delLength);
					int idx = orderKey.lastIndexOf('.');
					if(idx > 0)
						orderKey = orderKey.substring(0, idx);
					
					int orderKeyLen = orderKey.length();
						//Page ordering with numbers like 1, 2, ..., 10, 11, ..., etc.	
					try{
						Integer.parseInt(orderKey);	
						String tmp = tmpKey.substring(indexp + delimiter.length());
						if(orderKeyLen < largestOrderLen){							
							for(int j=0; j<(largestOrderLen-orderKeyLen); j++)
								tmp = "0" + tmp;
						}
						tmpKey = tmp + delimiter + tmpKey;
					}catch (NumberFormatException ne){}
				}
				orderMap.put(tmpKey, tmpFile);
			}
		}else{
			orderMap.putAll(tmpMap);
		}
		orderComponents(orderMap, preferedOrder); 
		return orderMap.values().iterator();
	}

	/**
	 * Special handling for the second pdf for access in AV Dissertations and XML for ETD
	 * @param components
	 */
	public void orderComponents(Map<String, Object> components, PreferedOrder preferedOrder){

		if(preferedOrder != null){
			if(preferedOrder.equals(PreferedOrder.PDFANDPDF) || preferedOrder.equals(PreferedOrder.PDFANDXML)){
				int order = 0;
				int fPosition = -1;
				String fileExt = ".pdf";
				Object[] keys = (Object[])components.keySet().toArray();
				String tmpKey = (String)keys[order];
				//The first order for the high resolution rdf
				if(!tmpKey.toLowerCase().endsWith(fileExt)){
					for(int i=0; i<keys.length; i++){
						tmpKey = (String)keys[i];
						if(tmpKey.toLowerCase().endsWith(fileExt)){
							fPosition = i;
							components.put("#0" + tmpKey, components.remove(tmpKey));
							break;
						}
					}
				}else{
					fPosition = 0;
					components.put("#0" + tmpKey, components.remove(tmpKey));
				}
				
				//The second order for access rdf or proquest xml
				order = 1;
				if(preferedOrder.equals(PreferedOrder.PDFANDXML))
					fileExt = ".xml";
				tmpKey = (String)keys[order];
				if(fPosition == order || !tmpKey.toLowerCase().endsWith(fileExt)){
					for(int i=0; i<keys.length; i++){
						if(i!=fPosition){
							tmpKey = (String)keys[i];
							if(tmpKey.toLowerCase().endsWith(fileExt)){
								components.put("#1" + tmpKey, components.remove(tmpKey));
								break;
							}
						}
					}
				}
			}else if(preferedOrder.equals(PreferedOrder.SUFFIX) && fileOrderSuffixes != null){
				String fileExt = null;
				String tmpKey = null;
				
				Object[] keys = (Object[])components.keySet().toArray();
				int filesLength = keys.length;
				int filesOrderLength = fileOrderSuffixes.length;
				for (int n=0; n<filesOrderLength; n++){
					int order = n;
					fileExt = fileOrderSuffixes[order];
					
					tmpKey = (String)keys[order];
					if(tmpKey.toLowerCase().endsWith(fileExt)){
						components.put("#" + n + ":" + tmpKey, components.remove(tmpKey));
					}else{
						for(int i=0; i<filesLength; i++){
							tmpKey = (String)keys[i];
							if(tmpKey.toLowerCase().endsWith(fileExt)){
								components.put("#" + n + ":" + tmpKey, components.remove(tmpKey));
								break;
							}
						}
					}
				}
				
				//Remove other files that haven't included in the suffix list. 
				//Files removed from the ingest list include the manifest file and the validation file etc. 
				if(filesLength > filesOrderLength){
					keys = (Object[])components.keySet().toArray();
					for(int n=0; n<filesLength; n++){
						tmpKey = (String)keys[n];
						if(n >= filesOrderLength || !tmpKey.endsWith(fileOrderSuffixes[n])){
							// Check for files that are not in the suffixes list for removal
							boolean found = false;
							for(int m=0; m<fileOrderSuffixes.length; m++){
								if(tmpKey.endsWith(fileOrderSuffixes[m])){
									found = true;
									break;
								}
							}
							if(!found)
								components.remove(tmpKey);
						}
					}
				}
	
			}
		}
	}
	
	public String getFileFilter() {
		return fileFilter;
	}

	public void setFileFilter(String fileFilter) {
		this.fileFilter = fileFilter;
	}

	public void addFileGroup(File file, List<List> poolList){
		boolean addFile = isFileValid(file, fileFilter);
		if (addFile) {
			List<String> files = getNewFileGroup(file.getAbsolutePath());
			poolList.add(files);
		}
	}
	
	private List<String> getNewFileGroup(String fileName){
		List<String> files = new ArrayList<String>();
		files.add(fileName);
		return files;
	}
	
	public void addFile(File file, List<String> fileGroup){
		boolean addFile = isFileValid(file, fileFilter);
		if (addFile) {	
			fileGroup.add(file.getAbsolutePath());
		}
	}
	
	public void addPairGroup(String key, File file, Map<String, List> poolList){
		boolean addFile = isFileValid(file, fileFilter);
		if (addFile) {
			List<String> files = new ArrayList<String>();
			files.add(file.getAbsolutePath());
			poolList.put(key, files);
		}
	}

	public String[] getFileOrderSuffixes() {
		return fileOrderSuffixes;
	}

	public void setFileOrderSuffixes(String[] fileOrderSuffixes) {
		this.fileOrderSuffixes = fileOrderSuffixes;
	}

	public int getSize(){
		return uploadList.size();
	}
	
	public String getMessage(){
		return message;
	}
	
	public String getCoDelimiter() {
		return coDelimiter;
	}

	public static boolean isFileValid(File file, String fileFilter){
		if (file.getName().startsWith("."))
				return false; 
		if (fileFilter != null && fileFilter.length() > 0)
			return FilenameUtils.wildcardMatchOnSystem(file.getAbsolutePath(), fileFilter);
		else
			return true;
	}
	
	public static String getFilePathValue(File file){
		String pathValue = file.getAbsolutePath().replace("\\", "/");
		return pathValue.substring(0, pathValue.lastIndexOf("/") + 1);
	}
}
