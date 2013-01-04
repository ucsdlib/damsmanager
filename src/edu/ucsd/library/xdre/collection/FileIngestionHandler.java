package edu.ucsd.library.xdre.collection;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Statement;

import edu.ucsd.library.xdre.ingest.DAMSUploadTaskHandler;
import edu.ucsd.library.xdre.ingest.assembler.Pair;
import edu.ucsd.library.xdre.ingest.assembler.UploadTask;
import edu.ucsd.library.xdre.ingest.assembler.UploadTaskOrganizer;
import edu.ucsd.library.xdre.ingest.assembler.UploadTaskOrganizer.PreferedOrder;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.FileURI;
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

	private String repository = null;
	private List<String> fileList = null;
	private String fileFilter = null;
	private String coDelimiter = null; // Delimiter for complex object ordering
	private String[] fileOrderSuffixes = null;
	private PreferedOrder preferedOrder = null;
	private String masterContent = "-1";

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

	@Override
	public boolean execute() throws Exception {

		String ark = null;
		DAMSUploadTaskHandler uploadHandler = null;
		UploadTaskOrganizer taskOrganizer = null;
		Pair uploadFile = null;
		UploadTask upLoadTask = null;

		fileStoreLog = getFileStoreLog(collectionId!=null?collectionId:repository!=null?repository:"dams");
		taskOrganizer = new UploadTaskOrganizer(fileList, uploadType,
				fileFilter, fileOrderSuffixes, coDelimiter, preferedOrder);

		objectsCount = taskOrganizer.getSize();

		String eMessage = taskOrganizer.getMessage();
		if (eMessage != null && eMessage.length() > 0) {
			exeReport.append(eMessage);
			log("log", eMessage + "\n");
		}
		int objCounter = 0;
		try {
			while (taskOrganizer.hasNext() && !interrupted) {
				// reset ark
				ark = null;
				upLoadTask = taskOrganizer.next();
				List<Pair> objBatch = upLoadTask.generateTasks();
				String subjectURI = null;

				int batchSize = objBatch.size();
				// Is file loaded
				DAMSUploadTaskHandler[] uploadTasks = new DAMSUploadTaskHandler[batchSize];
				String subjectId = null;
				String contentId = null;
				String fileName = null;
				for (int i = 0; i < batchSize && !interrupted; i++) {
					uploadFile = objBatch.get(i);
					fileName = uploadFile.getValue();
					contentId = uploadFile.getKey();
					setStatus("Processing file " + fileName + " (" + (objCounter + 1) + " of "
							+ taskOrganizer.getSize() + " objects for " + collectionTitle + ")");

					System.out.println("Batch size: " + batchSize + " -> " + fileName + " " + contentId);
					uploadHandler = new DAMSUploadTaskHandler(contentId,
							fileName, collectionId, damsClient);
					uploadHandler.setRepositoryId(repository);

					if (uploadType == UploadTaskOrganizer.PAIR_LOADING) {
						if(i == 0 && batchSize > 1)
							 uploadHandler.setUse(fileUse(fileName, "source"));
						else
							uploadHandler.setUse(fileUse(fileName, "service"));

					} else if (uploadType == UploadTaskOrganizer.SHARE_ARK_LOADING) {
						if (batchSize == 1 && !contentId.endsWith(masterContent)) {
							exeResult = false;
							eMessage = "Content files group with object "
									+ uploadFile.getValue()
									+ " have no master file.";
							String iMessagePrefix = "File Loading failed. ";
							setStatus(iMessagePrefix + eMessage);
							log("log", iMessagePrefix + eMessage);
							log.error(iMessagePrefix + eMessage);
							continue;
						} 
						if (i == 0 && batchSize > 1)
							uploadHandler.setUse(fileUse(fileName, "source"));

							
					} else if (uploadType == UploadTaskOrganizer.MIX_CO_SHARE_ARK_LOADING
							|| uploadType == UploadTaskOrganizer.COMPLEXOBJECT_LOADING) {
						// Master file with derivatives
						if (upLoadTask.getComponentsCount() == 1) {
							if (batchSize == 1 && !contentId.endsWith(masterContent)) {
								exeResult = false;
								eMessage = "Content files group with object "
										+ uploadFile.getValue()
										+ " have no master file.";
								String iMessagePrefix = "File Loading failed. ";
								setStatus(iMessagePrefix + eMessage);
								log("log", iMessagePrefix + eMessage);
								log.error(iMessagePrefix + eMessage);
								continue;
							}
						}
						PreferedOrder preferedOrder = upLoadTask.getPreferOrder();
						if(preferedOrder != null && preferedOrder.equals(PreferedOrder.PDFANDPDF)){
							if(i == 0 && batchSize > 1)
								uploadHandler.setUse(fileUse(fileName, "source"));
							else if (i == 1 && fileName.endsWith(".pdf"))
								uploadHandler.setUse(fileUse(fileName, "service"));
								
						}
					}

					// Check for duplications and complex objects reloading
					try {

						List<FileURI> filesLoaded = fileLoaded(uploadFile.getValue());
						if(filesLoaded.size() > 1){
							exeResult = false;
							eMessage = "File " + uploadFile.getValue()
									+ " was loaded in dams with ";
							for(Iterator<FileURI> it=filesLoaded.iterator();it.hasNext();){
								eMessage += it.next().toString() + " ";
							}
							String iMessagePrefix = "File upload failed. ";
							setStatus(iMessagePrefix + eMessage);
							log("log", iMessagePrefix + eMessage);
							log.error(iMessagePrefix + eMessage);
							System.out.println(iMessagePrefix + eMessage);
							continue;
						}
						
						if (filesLoaded.size() > 0) {
							FileURI fileURI = filesLoaded.get(0);
							subjectURI = fileURI.getObject();

							String tmpArk = subjectURI;
							if (ark == null)
								ark = tmpArk;
							else if (!tmpArk.endsWith(ark)) {
								exeResult = false;
								eMessage = "Content files group with object " + uploadFile.getValue()
										+ " have being loaded in " + ark + " and " + subjectURI;
								String iMessagePrefix = "File upload failed. ";
								setStatus(iMessagePrefix + eMessage);
								log("log", iMessagePrefix + eMessage);
								log.error(iMessagePrefix + eMessage);
							}

							if(exeResult){
								String message = "File " + uploadFile.getValue()
										+ " was ingested into DAMS previously with URI " + subjectURI;
								setStatus(message);
								log("log", message);
								log.info(message);
								System.out.println(message);
								skipCount++;
							}
						} else
							uploadTasks[i] = uploadHandler;
					} catch (Exception e) {
						e.printStackTrace();
						exeResult = false;
						eMessage = "Error processing " + fileName + ": "
								+ e.getMessage();
						String iMessagePrefix = "File upload failed with ";
						setStatus(iMessagePrefix + eMessage);
						log("log", iMessagePrefix + eMessage);
						log.error(iMessagePrefix + eMessage, e);
					}

					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						interrupted = true;
						exeResult = false;
						eMessage = "Error processing " + fileName + ": "
								+ e.getMessage();
						String iMessagePrefix = "File upload interrupted with ";
						System.out.println(iMessagePrefix + eMessage);
						setStatus("Canceled");
						clearSession();
						log("log", iMessagePrefix + eMessage);
						log.info(iMessagePrefix + eMessage, e);
					}
				}

				// Ingest the batched content files in turn
				for (int i = 0; i < batchSize && !interrupted; i++) {
					uploadHandler = uploadTasks[i];

					if (uploadHandler != null) {
						fileName = uploadHandler.getSourceFile();
						if (ark == null) {
							ark = damsClient.mintArk(null);
							subjectId = ark;
							String message = "Assigning ark " + ark
									+ " for file " + fileName + " in collection " + collectionTitle
									+ " (" + (objCounter + 1) + " of " + taskOrganizer.getSize() + ")";
							log.info(message);
							System.out.println(message);
						}

						// Set the subjecct ID for the file to be loaded
						uploadHandler.setSubjectId( subjectId );
						setStatus("Loading " + uploadHandler.getSubjectId()
								+ " for file " + fileName + " (" + (objCounter + 1) + " of "
								+ taskOrganizer.getSize() + " objects for " + collectionTitle + ")");
						try {
							boolean successful = uploadHandler.execute();
							if (successful) {
								String message = "Loaded " + damsClient.getRequestURL()
										+ " for file " + fileName + " successfully. \n";
								log("log", message);
								log.info(message);
								loadedCount++;
								logFile(uploadHandler);
								
								if ( i == 0 ) {
									RDFStore rdfStore = new RDFStore();
									List<Statement> stmts = new ArrayList<Statement>();
									if(collectionId != null && collectionId.length() > 0)
										stmts.add(rdfStore.createStatement(subjectId, "dams:collection", collectionId, true));
									if(repository != null && repository.length() > 0)
										stmts.add(rdfStore.createStatement(subjectId, "dams:repository", repository, true));
									if(stmts.size() > 0){
										try {
											damsClient.addMetadata(subjectId, stmts);
										} catch (Exception e) {
											e.printStackTrace();
												exeResult = false;
												eMessage = subjectId + " (" + fileName + ").";
												String iMessagePrefix = "Failed to add repository/collection links for  ";
												setStatus(iMessagePrefix + eMessage);
												log("log", iMessagePrefix + eMessage);
												log.error(iMessagePrefix + eMessage);
										}
									}
								}

								log.info("Uploaded " + successful + ": "
										+ uploadHandler.getSubjectId() + " -> " + uploadHandler.getSourceFile());
								//Create derivatives for images and PDFs
								try{
									String fileId = uploadHandler.getFileId();
									String mimeType = DAMSClient.getMimeType(fileId);
									String use = uploadHandler.getUse();
									if((mimeType.startsWith("image") || mimeType.endsWith("pdf")) 
											&& ((use == null && fileId.startsWith("1.")) || use.endsWith("service"))){
										successful = damsClient.createDerivatives(uploadHandler.getSubjectId(), uploadHandler.getCompId(), fileId, null);
										if(successful){
											String iMessage = "Created derivatives for " + damsClient.getRequestURL();
											setStatus(iMessage);
											log("log", iMessage);
										} else {
											exeResult = false;
											eMessage = subjectId + " (" + fileName + ").";
											String iMessagePrefix = "Failed to created derivatives for  ";
											setStatus(iMessagePrefix + eMessage);
											log("log", iMessagePrefix + eMessage);
											log.error(iMessagePrefix + eMessage);
											System.out.println(iMessagePrefix + eMessage);
										}
									}
								} catch (Exception e) {
									e.printStackTrace();
									exeResult = false;
									eMessage = subjectId + " (" + fileName + "). Error: " + e.getMessage();
									String iMessagePrefix = "Failed to created derivatives for  ";
									setStatus(iMessagePrefix + eMessage);
									log("log", iMessagePrefix + eMessage);
									log.error(iMessagePrefix + eMessage, e);
								}
							} else {
								log.error("Failed to load " + damsClient.getRequestURL() + " for file " + fileName + ". \n");

								exeResult = false;
								eMessage = subjectId + " (" + fileName + "). ";
								String iMessagePrefix = "File upload failed with ";
								setStatus(iMessagePrefix + eMessage);
								log("log", iMessagePrefix + eMessage);
								log.error(iMessagePrefix + eMessage);
								
								filesFailed.append(uploadHandler.getSourceFile() + "\n");

								if (failedCount++ > MAX_FAILED_ALLOWED) {
									String iMessage = "File upload failed: Maximum " + MAX_FAILED_ALLOWED + " failed reached.";
									log("log", iMessage);
									log.info(iMessage);
									interrupted = true;
								}
								if (i == 0) {
									// If the first component failed, then skip ingesting the complex object
									for (int j = 1; j < batchSize; j++) {
										if (uploadTasks[j] != null)
											filesFailed.append(uploadTasks[j].getSourceFile() + "\n");
									}
									break;
								}
							}

						} catch (Exception e) {
							e.printStackTrace();
							exeResult = false;
							eMessage = subjectId + " (" + fileName + "). Error: " + e.getMessage();
							String iMessagePrefix = "File upload failed with ";
							setStatus(iMessagePrefix + eMessage);
							log("log", iMessagePrefix + eMessage);
							log.error(iMessagePrefix + eMessage, e);
						} catch (IllegalAccessError e) {
							e.printStackTrace();
							exeResult = false;
							eMessage = subjectId + " (" + fileName + "). Error: " + e.getMessage();
							String iMessagePrefix = "File upload failed with ";
							setStatus(iMessagePrefix + eMessage);
							log("log", iMessagePrefix + eMessage);
							log.error(iMessagePrefix + eMessage, e);
						}
					}
					
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						interrupted = true;
						exeResult = false;
						eMessage = subjectId + " (" + fileName + "). Error: "
								+ e.getMessage();
						String iMessagePrefix = "File upload interrupted with ";
						System.out.println(iMessagePrefix + eMessage);
						setStatus("Canceled");
						clearSession();
						log("log", iMessagePrefix + eMessage);
						log.info(iMessagePrefix + eMessage, e);
					}
				}
				setProgressPercentage(((objCounter + 1) * 100)
						/ taskOrganizer.getSize());

				objCounter++;

				try {
					Thread.sleep(10);
				} catch (InterruptedException e) {
					interrupted = true;
					exeResult = false;
					eMessage = subjectId + " (" + fileName + "). Error: "
							+ e.getMessage();
					String iMessagePrefix = "File upload interrupted with ";
					System.out.println(iMessagePrefix + eMessage);
					setStatus("Canceled");
					clearSession();
					log("log", iMessagePrefix + eMessage);
					log.info(iMessagePrefix + eMessage, e);
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
			exeReport.append("Files failed to be loaded: "
					+ filesFailed.toString() + "\n");
		}
		exeReport.append("For records, please download the <a href=\""
				+ Constants.CLUSTER_HOST_NAME
				+ "/damsmanager/downloadLog.do?log=ingest&category="
				+ DAMSClient.stripID(collectionId!=null?collectionId:repository!=null?repository:"dams") + "\">Ingestion log</a>");
		String exeInfo = exeReport.toString();
		log("log", exeInfo);
		return exeInfo;
	}

	private synchronized void logFile(DAMSUploadTaskHandler uploadTask)
			throws IOException {
		try {
			fileStoreLog.write(uploadTask.getSubjectId() + "\t"
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
		String mimeType = DAMSClient.getMimeType(filename);
		if(mimeType.toLowerCase().endsWith("pdf"))
			return "document-" + useSuffix;
		return mimeType.substring(0, mimeType.indexOf("/"))+ "-" + useSuffix;
	}

	public String getRepository() {
		return repository;
	}

	public void setRepository(String repository) {
		this.repository = repository;
	}

	public String[] getFileOrderSuffixes() {
		return fileOrderSuffixes;
	}

	public void setFileOrderSuffixes(String[] fileOrderSuffixes) {
		this.fileOrderSuffixes = fileOrderSuffixes;
	}
	
	public List<FileURI> fileLoaded(String sourceFile) throws Exception{
		File srcFile = new File(sourceFile);
		return damsClient.retrieveFileURI(srcFile.getName(), srcFile.getParent(), collectionId, repository);
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
