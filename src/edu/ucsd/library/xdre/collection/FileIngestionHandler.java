package edu.ucsd.library.xdre.collection;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Statement;

import edu.ucsd.library.xdre.ingest.DAMSUploadTaskHandler;
import edu.ucsd.library.xdre.ingest.assembler.Pair;
import edu.ucsd.library.xdre.ingest.assembler.UploadTask;
import edu.ucsd.library.xdre.ingest.assembler.UploadTaskOrganizer;
import edu.ucsd.library.xdre.ingest.assembler.UploadTaskOrganizer.PreferedOrder;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DamsURI;
import edu.ucsd.library.xdre.utils.RDFStore;

/**
 * Upload files from staging area
 * @author lsitu
 *
 */
public class FileIngestionHandler extends CollectionHandler {
	private static Logger log = Logger.getLogger(FileIngestionHandler.class);
	private static int MAX_FAILED_ALLOWED = 300;

	private static Map<String, FileWriter> fileStoreLogs = null;

	private String unit = null;
	private List<String> fileList = null;
	private String fileFilter = null;
	private String coDelimiter = null; // Delimiter for complex object ordering
	private String[] fileOrderSuffixes = null;
	private String[] fileUses = null;
	private PreferedOrder preferedOrder = null;

	private int skipCount = 0;
	private int loadedCount = 0;
	private int failedCount = 0;
	private int objectsCount = 0;
	private final StringBuilder filesFailed = new StringBuilder();
	private FileWriter fileStoreLog = null;
	private int uploadType = UploadTaskOrganizer.SIMPLE_LOADING;
	

	public FileIngestionHandler(List<String> fileList, int uploadType)
			throws Exception {
		super();
		this.fileList = fileList;
		this.uploadType = uploadType;
	}

	public FileIngestionHandler(DAMSClient damsClient, List<String> fileList, int uploadType,
			String collectionId) throws Exception {
		super(damsClient, collectionId);
		this.fileList = fileList;
		this.uploadType = uploadType;
	}

	public FileIngestionHandler(DAMSClient damsClient, List<String> fileList, int uploadType,
			String collectionId, String fileFilter) throws Exception {
		this(damsClient, fileList, uploadType, collectionId);
		this.fileFilter = fileFilter;
	}

	public FileIngestionHandler(DAMSClient damsClient, List<String> fileList, int uploadType,
			String collectionId, String fileFilter, String coDelimiter) throws Exception {
		this(damsClient, fileList, uploadType, collectionId, fileFilter);
		this.coDelimiter = coDelimiter;
	}

	public void setPreferedOrder(String orderCode) {
		if (orderCode != null) {
			if (orderCode.equalsIgnoreCase("pdfAndPdf"))
				this.preferedOrder = PreferedOrder.PDFANDPDF;
			else if (orderCode.equalsIgnoreCase("pdfAndXml"))
				this.preferedOrder = PreferedOrder.PDFANDXML;
			else if (orderCode.equalsIgnoreCase("suffix"))
				this.preferedOrder = PreferedOrder.SUFFIX;
			else
				this.preferedOrder = null;
		} else
			this.preferedOrder = null;
	}
	
	public PreferedOrder getPreferedOrder(){
		return preferedOrder;
	}
	
	public void setFileUses(String[] fileUses){
		this.fileUses = fileUses;
	}

	@Override
	public boolean execute() throws Exception {

		String ark = null;
		DAMSUploadTaskHandler uploadHandler = null;
		UploadTaskOrganizer taskOrganizer = null;
		Pair uploadFile = null;
		UploadTask upLoadTask = null;

		fileStoreLog = getFileStoreLog(collectionId!=null?collectionId:unit!=null?unit:"dams");
		taskOrganizer = new UploadTaskOrganizer(fileList, uploadType,
				fileFilter, fileOrderSuffixes, coDelimiter, preferedOrder);

		objectsCount = taskOrganizer.getSize();

		String message = taskOrganizer.getMessage();
		if (message != null && message.length() > 0) {
			exeReport.append(message);
			log("log", message + "\n");
		}
		int objCounter = 0;
		try {
			while (taskOrganizer.hasNext() && !interrupted) {
				// reset ark
				ark = null;
				upLoadTask = taskOrganizer.next();
				preferedOrder = upLoadTask.getPreferOrder();
				List<Pair> objBatch = upLoadTask.generateTasks();
				String subjectURI = null;

				int batchSize = objBatch.size();
				// Is file loaded
				DAMSUploadTaskHandler[] uploadTasks = new DAMSUploadTaskHandler[batchSize];
				String subjectId = null;
				String contentId = null;
				String fileName = null;
				String fileUse = null;
				boolean derivatives = true;
				for (int i = 0; i < batchSize && !interrupted; i++) {
					derivatives = true;
					uploadFile = objBatch.get(i);
					fileName = uploadFile.getValue();
					contentId = uploadFile.getKey();
					setStatus("Processing file " + fileName + " (" + (objCounter + 1) + " of "
							+ taskOrganizer.getSize() + " objects for " + collectionTitle + ")");

					System.out.println("Batch size: " + batchSize + " -> " + fileName + " " + contentId);
					uploadHandler = new DAMSUploadTaskHandler(contentId,
							fileName, collectionId, damsClient);
					uploadHandler.setUnitId(unit);
						
					if (uploadType == UploadTaskOrganizer.PAIR_LOADING) {
						// Default file use properties for master/master-edited pair file upload
						if(i == 0){
							 fileUse = fileUse(fileName, "source");
							 // Use the alternate file for derivative creation if there's one exists.
							 if(batchSize > 1)
								 derivatives = false;
						}else
							fileUse = fileUse(fileName, "alternate");

					} else if (uploadType == UploadTaskOrganizer.SHARE_ARK_LOADING
							|| uploadType == UploadTaskOrganizer.MIX_CO_SHARE_ARK_LOADING
							|| uploadType == UploadTaskOrganizer.COMPLEXOBJECT_LOADING) {
						// Reject ingest the file group if the first file is not in place.
						if(i == 0){
							String cId = uploadHandler.getCompId();
							String fileId = uploadHandler.getFileId();
							if((cId != null && cId.length()>0) && cId.compareTo("1") > 0 || !(fileId.equals("1") || fileId.startsWith("1."))){
								logError("File Loading failed. The first file for files group with object "
										+ fileName + " is missing. Fist content file order is " + contentId + ".");
								break;
							}
						}
						
						if(preferedOrder != null && preferedOrder.equals(PreferedOrder.PDFANDPDF)){
							// Default file use for high resolution PDF and low resolution PDF upload 
							if(i == 0){
								fileUse = fileUse(fileName, "source");
							}else if (i == 1 && fileName.endsWith(".pdf")){
								fileUse = fileUse(fileName, "service");
								// Skip derivative creation
								derivatives = false;
							}
						}
					}
					
					// Apply user submitted file use properties
					if(fileUses != null && fileUses.length > 0){
						int fUseSize = fileUses.length;
						String[] fileParts = contentId.split("-");
						int compId = Integer.parseInt(fileParts[0]);
						int fn = Integer.parseInt(fileParts.length==1?fileParts[0]:fileParts[1]);
						
						// Complex Object: component files and their derivatives or files for other uses
						if(uploadType == UploadTaskOrganizer.COMPLEXOBJECT_LOADING
								|| uploadType == UploadTaskOrganizer.MIX_CO_SHARE_ARK_LOADING){
							
							if(preferedOrder != null && fUseSize >= compId){
								// Set file use properties for components files
								fileUse = fileUses[compId - 1];
							}else {
								// Component file and its derivatives or files for other uses
								if (fn == 1){
									// Master components files
									fileUse = fileUses[0];
								}else if (fn > 1 && fn <= fUseSize){
									// Derivatives or files for other uses
									fileUse = fileUses[fn - 1];
								}
							}
								
						}else if (fUseSize > i){
							// Simple object: master file and its derivatives or files for other uses
							fileUse = fileUses[i];
						}
					}
					uploadHandler.setUse(fileUse);

					// Check for duplications and complex objects reloading
					try {
						
						List<DamsURI> filesLoaded = fileLoaded(uploadFile.getValue());
						if(filesLoaded.size() > 1){
							message = "File " + uploadFile.getValue() + " was loaded in dams with ";
							for(Iterator<DamsURI> it=filesLoaded.iterator();it.hasNext();){
								message += it.next().toString() + " ";
							}
							logError("File upload failed. " + message);
							continue;
						}
						
						if (filesLoaded.size() > 0) {
							DamsURI damsURI = filesLoaded.get(0);
							subjectURI = damsURI.getObject();

							String tmpArk = subjectURI;
							if (ark == null)
								ark = tmpArk;
							else if (!tmpArk.endsWith(ark)) {
								logError("File upload failed. Content files group with object " + uploadFile.getValue()
										+ " have being loaded in " + ark + " and " + subjectURI);
							}

							if(exeResult){
								message = "File " + uploadFile.getValue() + " was ingested into DAMS previously with URI " + subjectURI;
								logMessage(message);
								log.info(message);
								skipCount++;
							}
						} else
							uploadTasks[i] = uploadHandler;
					} catch (Exception e) {
						e.printStackTrace();
						logError("File upload failed. Error processing " + fileName + ": " + e.getMessage());
					}

					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						interrupted = true;
						logError("File upload interrupted. Error processing " + fileName + ": " + e.getMessage());
						setStatus("Canceled");
						clearSession();
					}
				}

				// Ingest the batched content files in turn
				boolean updateSOLR = false;
				for (int i = 0; i < batchSize && !interrupted; i++) {
					uploadHandler = uploadTasks[i];

					if (uploadHandler != null) {
						fileName = uploadHandler.getSourceFile();
						if (ark == null) {
							ark = damsClient.mintArk(null);
							message = "Assigning ark " + ark
									+ " for file " + fileName + " in collection " + collectionTitle
									+ " (" + (objCounter + 1) + " of " + taskOrganizer.getSize() + ")";
							log.info(message);
							System.out.println(message);
						}

						subjectId = ark;
						// Set the subject ID for the file to be loaded
						uploadHandler.setSubjectId( subjectId );
						setStatus("Loading " + uploadHandler.getSubjectId()
								+ " for file " + fileName + " (" + (objCounter + 1) + " of "
								+ taskOrganizer.getSize() + " objects for " + collectionTitle + ")");
						boolean successful = false;
						try {
							successful = uploadHandler.execute();
							if (successful) {
								updateSOLR = true;
								message = "Loaded " + damsClient.getRequestURL() + " for file " + fileName + " successfully. \n";
								log("log", message);
								log.info(message);
								loadedCount++;
								logFile(uploadHandler);
								
								if ( i == 0 ) {
									RDFStore rdfStore = new RDFStore();
									List<Statement> stmts = new ArrayList<Statement>();
									if(collectionId != null && collectionId.length() > 0){
										// Collection linking predicate
										String colType = getCollectionType(collectionId);
										String colPredicate = "dams:collection";
										if(colType != null && colType.length() > 0)
											colPredicate = "dams:" + colType.substring(0,1).toLowerCase() +  colType.substring(1);
										stmts.add(rdfStore.createStatement(subjectId, colPredicate, collectionId, true));
									}
									if(unit != null && unit.length() > 0)
										stmts.add(rdfStore.createStatement(subjectId, "dams:unit", unit, true));
									if(stmts.size() > 0){
										try {
											damsClient.addMetadata(subjectId, stmts);
										} catch (Exception e) {
											e.printStackTrace();
											logError("Failed to add unit/collection links for " + subjectId + " (" + fileName + ").");
										}
									}
								}

								log.info("Uploaded " + successful + ": "
										+ uploadHandler.getSubjectId() + " -> " + uploadHandler.getSourceFile());
								// Create derivatives for images and PDFs
								try{
									String fileId = uploadHandler.getFileId();
									String use = uploadHandler.getUse();
									if( isDerivativesRequired(fileId, use) ){
										
										successful = damsClient.createDerivatives(uploadHandler.getSubjectId(), uploadHandler.getCompId(), fileId, null);
										if(successful){
											logMessage( "Created derivatives for " + damsClient.getRequestURL());
										} else {
											logError("Failed to created derivatives " + damsClient.getRequestURL() + "(" + fileName + "). ");
										}
									}
								} catch (Exception e) {
									e.printStackTrace();
									logError("Failed to created derivatives  " + damsClient.getRequestURL() + "(" + fileName + "). Error: " + e.getMessage() + ". ");
								}
							} else {
								logError("File upload failed: " + damsClient.getRequestURL() + " (" + fileName + "). ");
							}

						} catch (Exception e) {
							e.printStackTrace();
							logError("File upload failed with " + subjectId + " (" + fileName + "). Error: " + e.getMessage());
						} catch (IllegalAccessError e) {
							e.printStackTrace();
							logError("File upload failed with " + subjectId + " (" + fileName + "). Error: " + e.getMessage());
						}
						
						// Abort ingesting the batch when failing to ingest the first file.
						if( !successful){
							filesFailed.append(uploadHandler.getSourceFile() + "\n");

							if (failedCount++ > MAX_FAILED_ALLOWED) {
								logError("File upload failed: Maximum " + MAX_FAILED_ALLOWED + " failed reached.");
								interrupted = true;
							}
							if (i == 0) {
								String compId = null;
								// If the first component failed, then skip ingesting the whole file group
								for (int j = 1; j < batchSize; j++) {
									if (uploadTasks[j] != null){
										failedCount++;
										filesFailed.append(uploadTasks[j].getSourceFile() + "\n");
										compId = uploadTasks[j].getCompId();
										logError("File upload aborted for " + subjectId + "/" + (compId!=null&&compId.length()>0?compId+"/":"") + uploadTasks[j].getFileId() + " (" + uploadTasks[j].getSourceFile() + "). ");
									}
								}
								break;
							}
						}
					}

					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						interrupted = true;
						logError("File upload canceled on " + subjectId + " (" + fileName + ").");
						setStatus("Canceled");
						clearSession();
					}
				}
				
				// Updated SOLR
				if(updateSOLR && !updateSOLR(subjectId))
					failedCount++;

				setProgressPercentage(((objCounter + 1) * 100)/ taskOrganizer.getSize());

				objCounter++;

				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					interrupted = true;
					logError("File upload canceled on " + subjectId + " (" + fileName + ").");
					setStatus("Canceled");
					clearSession();
				}
			}
		} finally {
			if (fileStoreLog != null) {
				closeFileStoreLog(collectionId, fileStoreLog);
				fileStoreLog = null;
			}
		}
		return exeResult;
	}

	@Override
	public String getExeInfo() {
		exeReport.append("Total objects found: " + objectsCount + "\n");
		exeReport.append("Number of files loaded sucessful: " + loadedCount
				+ "\n");
		if (skipCount > 0)
			exeReport.append("Number of files skip: " + skipCount + "\n");
		if (failedCount > 0) {
			exeReport.append("Number of files failed: " + failedCount + "\n");
			if(filesFailed.length() > 0)
				exeReport.append("Files aborted or failed to be loaded: " + filesFailed.toString() + "\n");
		}
		// Add solr report message
		exeReport.append(getSOLRReport());
		exeReport.append("For records, please download the <a href=\""
				+ "https://" + (Constants.CLUSTER_HOST_NAME.indexOf("localhost")>=0?Constants.CLUSTER_HOST_NAME:Constants.CLUSTER_HOST_NAME+".ucsd.edu:8443")
				+ "/damsmanager/downloadLog.do?log=ingest&category="
				+ DAMSClient.stripID(collectionId!=null?collectionId:unit!=null?unit:"dams") + "\">Ingest log</a>");
		String exeInfo = exeReport.toString();
		log("log", exeInfo);
		return exeInfo;
	}

	private synchronized void logFile(DAMSUploadTaskHandler uploadTask)
			throws IOException {
		try {
			fileStoreLog.write(DAMSClient.stripID(uploadTask.getSubjectId()) + "\t"
					+ uploadTask.getSourceFile() + "\t"
					+ damsClient.getRequestURL() + "\n");
		} catch (IOException e) {
			// e.printStackTrace();
			if (fileStoreLog != null) {
				try {
					fileStoreLog.close();
				} catch (IOException e1) {
				}
			}
			fileStoreLog = getFileStoreLog(collectionId);
		}
	}
	
	public String fileUse (String filename, String useSuffix){
		String fileUse = null;
		String fname = filename.toLowerCase();
		if(fname.endsWith(".tif") || fname.endsWith(".jpg")){
			fileUse = "visual-" + useSuffix;
		}else{
			String mimeType = DAMSClient.getMimeType(filename);
			if(mimeType.toLowerCase().endsWith("pdf"))
				fileUse = "document-" + useSuffix;
		}
		return fileUse;
	}

	public String getUnit() {
		return unit;
	}

	public void setUnit(String unit) {
		this.unit = unit;
	}

	public String[] getFileOrderSuffixes() {
		return fileOrderSuffixes;
	}

	public void setFileOrderSuffixes(String[] fileOrderSuffixes) {
		this.fileOrderSuffixes = fileOrderSuffixes;
	}
	
	public List<DamsURI> fileLoaded(String sourceFile) throws Exception{
		File srcFile = new File(sourceFile);
		return damsClient.retrieveFileURI(srcFile.getName(), srcFile.getParent(), collectionId, unit);
	}
	
	public String getCollectionType(String cid){
		String colTitle = collectionsMap.get(cid);
		return colTitle.charAt(colTitle.length()-1)==']'?colTitle.substring(colTitle.lastIndexOf("[")+1, colTitle.length()-1):null;
	}

	public static synchronized FileWriter getFileStoreLog(String collectionId)
			throws IOException {
		if (fileStoreLogs == null) {
			fileStoreLogs = new HashMap<String, FileWriter>();
		}
		FileWriter fw = fileStoreLogs.get(collectionId);
		if (fw == null) {
			String logFileName = (Constants.TMP_FILE_DIR==null||Constants.TMP_FILE_DIR.length()==0?"":Constants.TMP_FILE_DIR+"/") + "ingest-"
					+ DAMSClient.stripID(collectionId) + ".log";
			System.out.println("Ingestion log:" + new File(logFileName).getAbsolutePath());
			fw = new FileWriter(logFileName, true);
			fileStoreLogs.put(collectionId, fw);
		}
		return fw;
	}

	public static synchronized void closeFileStoreLog(String collectionId,
			FileWriter localStoreLog) {
		fileStoreLogs.remove(collectionId);
		try {
			localStoreLog.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
