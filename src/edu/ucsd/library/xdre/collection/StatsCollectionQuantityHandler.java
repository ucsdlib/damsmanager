package edu.ucsd.library.xdre.collection;

import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DFile;

/**
 * Class CollectionQuantity calculate the disk size of a collection
 * 
 * @author lsitu@ucsd.edu
 */
public class StatsCollectionQuantityHandler extends CollectionHandler{
	
	private static Logger log = Logger.getLogger(StatsCollectionQuantityHandler.class);
	private long diskSize = 0;
	private int objectsCount = 0;
	
	public StatsCollectionQuantityHandler(DAMSClient damsClient, String collectionId) throws Exception{
		super(damsClient, collectionId);
	}
	
	public boolean execute() throws Exception{
		String subject = null;
		String fileUri = null;
		String use = null;
		for(Iterator<String> it=items.iterator();it.hasNext();){
			subject = (String)it.next();
			List<DFile> dFiles = damsClient.listObjectFiles(subject);
			String size = null;
			DFile dFile = null;
			try{
				if(dFiles.size() > 0){
					objectsCount++;
					for(Iterator<DFile> dit=dFiles.iterator(); dit.hasNext();){
						dFile = dit.next();
						fileUri = dFile.getId();
						use = dFile.getUse();
						//if(fileUri.indexOf("/1.") > 0 || fileUri.endsWith("/1") || (use != null && (use.endsWith("source") || use.endsWith("alternative")))){
						size = dFile.getSize();
						if(size != null)
							diskSize += Long.parseLong(size);
						else
							log.warn("Missing file size: " + dFile.getId());
						//}
					}
				}else{
					int idx = subject.lastIndexOf("/");
					String numFound = damsClient.solrLookup("q=id:" + (idx<0?subject:subject.substring(idx+1)) + "+AND+has_model_ssim:\"info:fedora/afmodel:DamsObject\"").selectSingleNode("//result/@numFound").getStringValue();
					if(!numFound.equals("0"))
						objectsCount++;
				}
			} catch (NumberFormatException e){
				log.warn("Invalid file size " + fileUri + ": " + size);
			}
		}
		return exeResult;
	}

	public long getDiskSize() {
		return diskSize;
	}

	public void setDiskSize(long diskSize) {
		this.diskSize = diskSize;
	}

	public int getObjectsCount() {
		return objectsCount;
	}

	public String getExeInfo() {
		return exeReport.toString();
	}
}
