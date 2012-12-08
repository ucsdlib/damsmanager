package edu.ucsd.library.xdre.collection;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import javax.security.auth.login.LoginException;

import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.utils.DAMSClient;

/**
 * Class TripleStoreSyncHandler handles the synchronization between
 * the source triplestore and the target triplestore
 * 
 * @author lsitu@ucsd.edu
 */
public class DevUploadHandler extends CollectionHandler{
	private static Logger log = Logger.getLogger(DevUploadHandler.class);
	private StringBuilder faileds = null;
	private StringBuilder successfuls = null;
	private int failedsCount = 0;
	private boolean interrupted = false;
	
	public DevUploadHandler(List<String> files){
		super();
		this.items = files;
		faileds = new StringBuilder();
		successfuls = new StringBuilder();
	}
	
	public boolean execute() throws Exception{			
	    int size = items.size();
	    InputStream in = null;
	    String fileName = "";
	    String message = "";
	    String[] parts = null;
		for(int i=0; i<size && !interrupted; i++){
			fileName = (String) items.get(i);
			message = "Uploading file " + fileName + " to FileStore " + damsClient.getFileStore() + " (" + (i+1) + " of " + size + ")";
		    setStatus(message);
		    setProgressPercentage( (i * 100) / size);
		    log.info(message);
		    File file = new File(fileName);
			String fullName = fileName;
			if(file.exists()){
				int numTry = 0;
				while(numTry++ < maxTry && !interrupted){
					fullName = file.getName();
					parts = DAMSClient.toFileParts(fullName);
					try {
						damsClient.uploadFile(parts[1], parts[2], parts[3], in, file.length());
	
						successfuls.append(fileName + "\t");
						message = "Successfully uploaded " + fileName + " to FileStore " + damsClient.getFileStore() + ".";
						log.info(message);
						log("log", message);
						break;
					} catch (LoginException e){
						e.printStackTrace();
						if(numTry == maxTry){
							setExeResult(false);
							String eMessage = fullName + ". FileStoreAuthException: " + e.getMessage();
							String iMessagePrefix = "Upload failed with filestore " + damsClient.getFileStore() + " for ";
							setStatus(iMessagePrefix + eMessage);
							log("log", iMessagePrefix + eMessage );
							log.error(iMessagePrefix + eMessage, e);
						}
					} catch (Exception e) {
						e.printStackTrace();
						failedsCount++;
						exeResult = false;
						message = "Failed to upload " + fileName + " to Local Store: " + e.getMessage() + ".";
						setStatus(message);
						faileds.append(fileName + "\n");
						exeReport.append(message);
						log.info(message);
						log("log", message);
					} finally{
						if(in != null){
							try{
								in.close();
								in = null;
							}catch(Exception e){e.printStackTrace();}
						}
					}
				}

			}else{
				failedsCount++;
				exeResult = false;
				message = "Failed to upload " + fileName + " to Local Store: file is not found.";
				setStatus(message);
				faileds.append(fileName + "\n");
				log.info(message);
				log("log", message);
				exeReport.append(message);
			}
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				e.printStackTrace();
				exeResult = false;
				interrupted = true;
				message = "Failed to upload " + fileName + " to Local Store: user interrupted.";
				setStatus(message);
				faileds.append(fileName + "\n");
				log.info(message);
				log("log", message);
				exeReport.append(message);
			}
		}
		return exeResult;
	}

	public String getExeInfo() {
		String message = "";
		if(exeResult){
			message = items.size() + " files are uploaded to the Local Store successfully";
			log("log", message + ": \n" + successfuls.toString());
		}else {
			message = "Failed to upload " + failedsCount + " files Local Store: \n" + faileds.toString();
			message += exeReport.toString();
		}
		
		return message + ".";
	}
}
