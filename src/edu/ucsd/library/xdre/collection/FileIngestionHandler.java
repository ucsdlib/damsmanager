package edu.ucsd.library.xdre.collection;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import com.hp.hpl.jena.rdf.model.Statement;

import edu.ucsd.library.jetl.DAMSUploadTaskHandler;
import edu.ucsd.library.jetl.organizer.Pair;
import edu.ucsd.library.jetl.organizer.UploadTask;
import edu.ucsd.library.jetl.organizer.UploadTaskOrganizer;
import edu.ucsd.library.jetl.organizer.UploadTaskOrganizer.PreferedOrder;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
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

	private List<String> fileList = null;
	private String fileFilter = null;
	private String coDelimiter = null; // Delimiter for complex object ordering
	private String[] fileOrderSuffixes = null;
	private int uploadOption = 0;
	private PreferedOrder preferedOrder = null;
	private String masterContent = "-1";

	private int skipCount = 0;
	private int loadedCount = 0;
	private int failedCount = 0;
	private int objectsCount = 0;
	private final StringBuilder filesFailed = new StringBuilder();
	private HttpServletRequest request = null;
	private FileWriter fileStoreLog = null;
	private int uploadType = UploadTaskOrganizer.SIMPLE_LOADING;
	
	private Map<String, String> predicates = null;
	

	public FileIngestionHandler(List<String> fileList, int uploadType)
			throws Exception {
		super();
		this.fileList = fileList;
		this.uploadType = uploadType;
		initHandler();
	}

	public FileIngestionHandler(DAMSClient damsClient, List<String> fileList, int uploadType,
			String collectionId) throws Exception {
		super(damsClient, collectionId);
		this.fileList = fileList;
		this.uploadType = uploadType;
		initHandler();
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

	private void initHandler() throws Exception{
		predicates = damsClient.getPredicates();
	}
	
	public HttpServletRequest getHttpServletRequest() {
		return request;
	}

	public void setHttpServletRequest(HttpServletRequest request) {
		this.request = request;
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
		boolean successful = false;
		fileStoreLog = getFileStoreLog(collectionId);
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

					if (uploadOption == UploadTaskOrganizer.PAIR_LOADING) {
						if(i == 0 && batchSize > 1)
							 uploadHandler.setUse(fileUse(fileName, "source"));
						else
							uploadHandler.setUse(fileUse(fileName, "service"));

					} else if (uploadType == UploadTaskOrganizer.SHARE_ARK_LOADING) {
						if (batchSize == 1 && !contentId.endsWith(masterContent)) {
							successful = false;
							setExeResult(false);
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
								successful = false;
								setExeResult(false);
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

					int numTry = 0;
					successful = false;
					// Check for duplications and complex objects reloading
					while (!interrupted && !successful && numTry++ < maxTry) {
						try {
							subjectURI = uploadHandler.fileLoaded();
							successful = true;
							if (subjectURI != null && subjectURI.length() > 0) {
								subjectURI = CollectionHandler.getIDValue(subjectURI);
								String subId = DAMSClient.stripID(subjectURI);

								String tmpArk = subId.substring(0, 10);
								if (ark == null)
									ark = tmpArk;
								else if (!ark.equals(tmpArk)) {
									successful = false;
									setExeResult(false);
									eMessage = "Content files group with object " + uploadFile.getValue()
											+ " have being loaded in " + ark + " and " + subId.substring(0, 10);
									String iMessagePrefix = "JETL Loading failed. ";
									setStatus(iMessagePrefix + eMessage);
									log("log", iMessagePrefix + eMessage);
									log.error(iMessagePrefix + eMessage);
									break;
								}

								// Check for complex object ordering
								if (i > 0 && !(uploadType == UploadTaskOrganizer.PAIR_LOADING
												|| uploadType == UploadTaskOrganizer.SHARE_ARK_LOADING
												|| uploadType == UploadTaskOrganizer.COMPLEXOBJECT_LOADING || uploadType == UploadTaskOrganizer.MIX_CO_SHARE_ARK_LOADING)) {
									if (!subId.endsWith(String.valueOf(i + 1))) {
										successful = false;
										setExeResult(false);
										String iMessagePrefix = "File upload failed. ";
										eMessage = objBatch.get(i).getValue() + " had ingested with "
												+ subjectURI + " and has conflick order " + (i + 1);
										setStatus(iMessagePrefix + eMessage);
										log("log", iMessagePrefix
												+ eMessage);
										log.error(iMessagePrefix + eMessage);
										filesFailed.append(objBatch.get(i)
												.getValue() + "\n");
										failedCount++;
										break;
									}
								}
								String message = "File " + uploadFile.getValue()
										+ " was ingested into DAMS previously with URI " + subjectURI;
								setStatus(message);
								log("log", message);
								log.info(message);
								System.out.println(message);
								skipCount++;
							} else
								uploadTasks[i] = uploadHandler;
						} catch (Exception e) {
							e.printStackTrace();
							successful = false;
							setExeResult(false);
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
							successful = false;
							setExeResult(false);
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
				}

				// Ingest the batched content files in turn
				for (int i = 0; i < batchSize && !interrupted; i++) {
					successful = false;
					int numTry = 0;
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

						while (!interrupted && !successful && numTry++ < maxTry) {
							try {
								successful = uploadHandler.execute();
								if (successful) {
									String message = "Loaded " + damsClient.getRequestURL()
											+ " for file " + fileName + " successfully. \n";
									log("log", message);
									log.info(message);
									loadedCount++;
									logFile(uploadHandler);
								} else
									log.error("Failed to load " + damsClient.getRequestURL() + " for file " + fileName + ". \n");

							} catch (Exception e) {
								e.printStackTrace();
								successful = false;
								setExeResult(false);
								eMessage = subjectId + " (" + fileName + "). Error: " + e.getMessage();
								String iMessagePrefix = "File upload failed with ";
								setStatus(iMessagePrefix + eMessage);
								log("log", iMessagePrefix + eMessage);
								log.error(iMessagePrefix + eMessage, e);
							} catch (IllegalAccessError e) {
								e.printStackTrace();
								successful = false;
								setExeResult(false);
								eMessage = subjectId + " (" + fileName + "). Error: " + e.getMessage();
								String iMessagePrefix = "File upload failed with ";
								setStatus(iMessagePrefix + eMessage);
								log("log", iMessagePrefix + eMessage);
								log.error(iMessagePrefix + eMessage, e);
							}

						}
						if (!successful) {
							setExeResult(false);
							filesFailed.append(uploadHandler.getSourceFile() + "\n");

							if (failedCount++ > MAX_FAILED_ALLOWED) {
								String iMessage = "File upload failed: Maximum " + MAX_FAILED_ALLOWED + " failed reached.";
								log("log", iMessage);
								log.info(iMessage);
								interrupted = true;
							}
							if (i == 0) {
								// If the first component failed, then skit the
								// ingesting the complex object
								for (int j = 1; j < batchSize; j++) {
									if (uploadTasks[j] != null)
										filesFailed.append(uploadTasks[j].getSourceFile() + "\n");
								}
								break;
							}
						} else {

							if (i == 0 && ((objBatch.size() > 1 && uploadType == UploadTaskOrganizer.MIX_LOADING)
									|| (upLoadTask.getComponentsCount() > 1 && (uploadType == UploadTaskOrganizer.COMPLEXOBJECT_LOADING 
									|| uploadType == UploadTaskOrganizer.MIX_CO_SHARE_ARK_LOADING)))) {
								// Only process when duplicated files checking passed
								numTry = 0;
								successful = false;

								while (!successful && numTry++ < maxTry) {
									try {

										// XXX
										// Insert statement for complex object
										  
										RDFStore rdfStore = new RDFStore();
										List<Statement> stmts = new ArrayList<Statement>();
										stmts.add(rdfStore.createStatement(subjectId, predicates.get("dams:collection"), collectionId, false));
										damsClient.addMetadata(subjectId, stmts);
										successful = true;
									} catch (Exception e) {
										e.printStackTrace();
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
								boolean derivCreated = false;
								if((mimeType.startsWith("image") || mimeType.endsWith("pdf")) 
										&& ((use == null && fileId.startsWith("1.")) || use.endsWith("service"))){
									derivCreated = damsClient.createDerivatives(uploadHandler.getSubjectId(), uploadHandler.getCompId(), fileId, null);
									if(derivCreated)
										log.info("Created derivatives for " + damsClient.getRequestURL());
									else
										log.info("Failed to created derivatives for " + damsClient.getRequestURL());
								}
							} catch (Exception e) {
								e.printStackTrace();
								successful = false;
								setExeResult(false);
								eMessage = subjectId + " (" + fileName + "). Error: " + e.getMessage();
								String iMessagePrefix = "Failed to created derivatives for  ";
								setStatus(iMessagePrefix + eMessage);
								log("log", iMessagePrefix + eMessage);
								log.error(iMessagePrefix + eMessage, e);
							}
						}
					}
					
					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						interrupted = true;
						successful = false;
						setExeResult(false);
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
					successful = false;
					setExeResult(false);
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
		return successful;
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
				+ "damsmanager/downloadLog.do?log=ingest&category="
				+ collectionId + "\">Ingestion log</a>");
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
			fileStoreLog = fileStoreLogs.get(collectionId);
			if (fileStoreLog == null) {
				String logFileName = Constants.TMP_FILE_DIR + "/ingest-"
						+ collectionId + ".log";
				fileStoreLog = new FileWriter(logFileName, true);
				fileStoreLogs.put(collectionId, fileStoreLog);
			}
		}
	}
	
	public String fileUse (String filename, String useSuffix){
		String mimeType = DAMSClient.getMimeType(filename);
		if(mimeType.toLowerCase().endsWith("pdf"))
			return "document-" + useSuffix;
		return mimeType.substring(0, mimeType.indexOf("/"))+ "-" + useSuffix;
	}

	public int getUploadOption() {
		return uploadOption;
	}

	public void setUploadOption(int uploadOption) {
		this.uploadOption = uploadOption;
	}

	public String[] getFileOrderSuffixes() {
		return fileOrderSuffixes;
	}

	public void setFileOrderSuffixes(String[] fileOrderSuffixes) {
		this.fileOrderSuffixes = fileOrderSuffixes;
	}

	public static synchronized FileWriter getFileStoreLog(String collectionId)
			throws IOException {
		if (fileStoreLogs == null) {
			fileStoreLogs = new HashMap<String, FileWriter>();
		}
		FileWriter fw = fileStoreLogs.get(collectionId);
		if (fw == null) {
			String logFileName = Constants.TMP_FILE_DIR + "/ingest-"
					+ collectionId + ".log";
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
