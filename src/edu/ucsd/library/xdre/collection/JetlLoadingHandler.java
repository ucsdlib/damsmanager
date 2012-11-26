package edu.ucsd.library.xdre.collection;

import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;

import edu.ucsd.library.jetl.DAMSUploadTaskHandler;
import edu.ucsd.library.jetl.organizer.Pair;
import edu.ucsd.library.jetl.organizer.UploadTask;
import edu.ucsd.library.jetl.organizer.UploadTaskOrganizer;
import edu.ucsd.library.jetl.organizer.UploadTaskOrganizer.PreferedOrder;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;

public class JetlLoadingHandler extends CollectionHandler {
	private static Logger log = Logger.getLogger(JetlLoadingHandler.class);
	private static int MAX_FAILED_ALLOWED = 300;

	private static Map<String, FileWriter> fileStoreLogs = null;

	private List<String> fileList = null;
	private String filePrefix = null;
	private String fileFilter = null;
	private String coDelimiter = null; // Delimiter for complex object ordering
	private String[] fileOrderSuffixes = null;
	private int uploadOption = 0;
	private PreferedOrder preferedOrder = null;

	// private boolean interrupted = false;

	private int skipCount = 0;
	private int loadedCount = 0;
	private int failedCount = 0;
	private int objectsCount = 0;
	private final StringBuilder filesFailed = new StringBuilder();
	private HttpServletRequest request = null;
	private FileWriter localStoreLog = null;
	private int uploadType = UploadTaskOrganizer.SIMPLE_LOADING;

	public JetlLoadingHandler(List<String> fileList, int uploadType)
			throws Exception {
		super();
		this.fileList = fileList;
		this.uploadType = uploadType;
	}

	public JetlLoadingHandler(DAMSClient damsClient, List<String> fileList, int uploadType,
			String collectionId) throws Exception {
		super(damsClient, collectionId);
		this.fileList = fileList;
		this.uploadType = uploadType;
	}

	public JetlLoadingHandler(DAMSClient damsClient, List<String> fileList, int uploadType,
			String collectionId, String fileFilter, String filePrefix)
			throws Exception {
		this(damsClient, fileList, uploadType, collectionId);
		this.filePrefix = filePrefix;
		this.fileFilter = fileFilter;
	}

	public JetlLoadingHandler(DAMSClient damsClient, List<String> fileList, int uploadType,
			String collectionId, String fileFilter, String filePrefix,
			String coDelimiter) throws Exception {
		this(damsClient, fileList, uploadType, collectionId, fileFilter, filePrefix);
		this.coDelimiter = coDelimiter;
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
		localStoreLog = getFileStoreLog(collectionId);
		taskOrganizer = new UploadTaskOrganizer(fileList, uploadType,
				fileFilter, fileOrderSuffixes, coDelimiter, preferedOrder);
		// taskOrganizer.setFileOrderSuffixes(fileOrderSuffixes);
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
					setStatus("Processing file " + fileName + " ("
							+ (objCounter + 1) + " of "
							+ taskOrganizer.getSize() + " objects for "
							+ collectionTitle + ")");

					System.out.println("Batch size: " + batchSize + " -> "
							+ fileName + " " + contentId);
					uploadHandler = new DAMSUploadTaskHandler(contentId,
							fileName, collectionId, damsClient);

					if (uploadOption == Constants.JETL_UPLOAD_FILE_ONLY) {
						uploadHandler.setFileUploadOnly(true);
					} else if (uploadOption == UploadTaskOrganizer.PAIR_LOADING) {
						if (contentId != null && contentId.length() > 0)
							uploadHandler.setChildComponent(true);
					} else if (uploadType == UploadTaskOrganizer.SHARE_ARK_LOADING) {
						if (i > 0)
							uploadHandler.setFileUploadOnly(true);
						else if (batchSize == 1
								&& (contentId != null && contentId.length() > 0)) {
							successful = false;
							setExeResult(false);
							eMessage = "Content files group with object "
									+ uploadFile.getValue()
									+ " have no master file.";
							String iMessagePrefix = "JETL Loading failed. ";
							setStatus(iMessagePrefix + eMessage);
							log("log", iMessagePrefix + eMessage);
							log.error(iMessagePrefix + eMessage);
							continue;
						}
					} else if (uploadType == UploadTaskOrganizer.MIX_CO_SHARE_ARK_LOADING
							|| uploadType == UploadTaskOrganizer.COMPLEXOBJECT_LOADING) {
						// Master file with derivatives
						if (upLoadTask.getComponentsCount() == 1) {
							if (i > 0)
								uploadHandler.setFileUploadOnly(true);
							else if (batchSize == 1
									&& (contentId != null && contentId.length() > 0)) {
								successful = false;
								setExeResult(false);
								eMessage = "Content files group with object "
										+ uploadFile.getValue()
										+ " have no master file.";
								String iMessagePrefix = "JETL Loading failed. ";
								// exeReport.append(iMessagePrefix + eMessage +
								// "<br />");
								setStatus(iMessagePrefix + eMessage);
								log("log", iMessagePrefix + eMessage);
								log.error(iMessagePrefix + eMessage);
								continue;
							}
						} else if (upLoadTask.getComponentsCount() > 1) {
							// Complex objects with derivatives
							if (contentId != null
									&& contentId.lastIndexOf('-') > 1) {
								uploadHandler.setFileUploadOnly(true);
							}
						}
					}

					int numTry = 0;
					successful = false;
					// Check for duplications and complex objects reloading
					while (!interrupted && !successful && numTry++ < maxTry) {
						try {
							if (!uploadHandler.isFileUploadOnly()) {
								subjectURI = uploadHandler.isFileLoaded();
								successful = true;
								if (subjectURI != null
										&& subjectURI.length() > 0) {
									subjectURI = CollectionHandler.getIDValue(subjectURI);
									String subId = DAMSClient.stripID(subjectURI)
											.replace(DAMSClient.DAMS_ARK_URL_BASE, "")
											.replaceFirst(uploadHandler.getArkOrg()+ "/", "");
									String tmpArk = subId.substring(0, 10);
									if (ark == null)
										ark = tmpArk;
									else if (!ark.equals(tmpArk)) {
										successful = false;
										setExeResult(false);
										eMessage = "Content files group with object "
												+ uploadFile.getValue()
												+ " have being loaded in "
												+ ark
												+ " and "
												+ subId.substring(0, 10);
										String iMessagePrefix = "JETL Loading failed. ";
										setStatus(iMessagePrefix + eMessage);
										log("log", iMessagePrefix + eMessage);
										log.error(iMessagePrefix + eMessage);
										break;
									}

									// Check for complex object ordering
									if (i > 0
											&& !(uploadType == UploadTaskOrganizer.PAIR_LOADING
													|| uploadType == UploadTaskOrganizer.SHARE_ARK_LOADING
													|| uploadType == UploadTaskOrganizer.COMPLEXOBJECT_LOADING || uploadType == UploadTaskOrganizer.MIX_CO_SHARE_ARK_LOADING)) {
										if (!subId.endsWith(String
												.valueOf(i + 1))) {
											successful = false;
											setExeResult(false);
											String iMessagePrefix = "JETL Loading failed. ";
											eMessage = objBatch.get(i)
													.getValue()
													+ " had ingested with "
													+ subjectURI
													+ " and has conflick order "
													+ (i + 1);
											// exeReport.append(iMessagePrefix +
											// eMessage + "<br />");
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
									String message = "File "
											+ uploadFile.getValue()
											+ " was ingested into DAMS previously with URI "
											+ subjectURI;
									setStatus(message);
									log("log", message);
									log.info(message);
									System.out.println(message);
									skipCount++;
								} else
									uploadTasks[i] = uploadHandler;
							} else
								uploadTasks[i] = uploadHandler;
						} catch (Exception e) {
							e.printStackTrace();
							successful = false;
							setExeResult(false);
							eMessage = "Error processing " + fileName + ": "
									+ e.getMessage();
							String iMessagePrefix = "JETL Loading failed with ";
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
							String iMessagePrefix = "JETL Loading interrupted with ";
							System.out.println(iMessagePrefix + eMessage);
							setStatus("Canceled");
							clearSession();
							log("log", iMessagePrefix + eMessage);
							log.info(iMessagePrefix + eMessage, e);
						}
					}

					try {
						Thread.sleep(10);
					} catch (InterruptedException e) {
						interrupted = true;
						successful = false;
						setExeResult(false);
						eMessage = "Error processing " + fileName + ": "
								+ e.getMessage();
						String iMessagePrefix = "JETL Loading interrupted with ";
						System.out.println(iMessagePrefix + eMessage);
						setStatus("Canceled");
						clearSession();
						log("log", iMessagePrefix + eMessage);
						log.info(iMessagePrefix + eMessage, e);
					}
				}

				// Ingest the batched content files in turn
				for (int i = 0; i < batchSize && !interrupted; i++) {
					successful = false;
					int numTry = 0;
					uploadHandler = uploadTasks[i];

					if (uploadHandler != null) {
						fileName = uploadHandler.getFileName();
						if (ark == null) {
							ark = damsClient.mintArk(null);
							String message = "Assigning ark " + ark
									+ " for file " + fileName
									+ " in collection " + collectionTitle
									+ " (" + (objCounter + 1) + " of "
									+ taskOrganizer.getSize() + ")";
							log.info(message);
							// setStatus(message);
						}

						// Set the subjecct ID for the file to be loaded
						subjectId = uploadHandler.setSubjectId(ark);
						setStatus("Loading " + uploadHandler.getSubjectId()
								+ " for file " + fileName + " ("
								+ (objCounter + 1) + " of "
								+ taskOrganizer.getSize() + " objects for "
								+ collectionTitle + ")");

						while (!interrupted && !successful && numTry++ < maxTry) {
							try {
								successful = uploadHandler.execute();
								if (successful) {
									String message = "Loaded "
											+ damsClient.getRequestURL()
											+ " for file " + fileName
											+ " successfully. \n";
									log("log", message);
									log.info(message);
									loadedCount++;
									logFile(uploadHandler);
								} else
									log.error("Failed to load "
											+ damsClient.getRequestURL()
											+ " for file " + fileName + ": "
											+ uploadHandler.getMessage() + "\n");

							} catch (Exception e) {
								e.printStackTrace();
								successful = false;
								setExeResult(false);
								eMessage = subjectId + " (" + fileName
										+ "). Error: " + e.getMessage();
								String iMessagePrefix = "JETL Loading failed with ";
								setStatus(iMessagePrefix + eMessage);
								log("log", iMessagePrefix + eMessage);
								log.error(iMessagePrefix + eMessage, e);
							} catch (IllegalAccessError e) {
								e.printStackTrace();
								successful = false;
								setExeResult(false);
								eMessage = subjectId + " (" + fileName
										+ "). Error: " + e.getMessage();
								String iMessagePrefix = "JETL Loading failed with ";
								setStatus(iMessagePrefix + eMessage);
								log("log", iMessagePrefix + eMessage);
								log.error(iMessagePrefix + eMessage, e);
							}

						}
						if (!successful) {
							setExeResult(false);
							filesFailed.append(uploadHandler.getFileName()
									+ "\n");

							if (failedCount++ > MAX_FAILED_ALLOWED) {
								String iMessage = "JETL Loading failed: Maximum "
										+ MAX_FAILED_ALLOWED
										+ " failed reached.";
								log("log", iMessage);
								log.info(iMessage);
								interrupted = true;
							}
							if (i == 0) {
								// If the first component failed, then skit the
								// ingesting the complex object
								for (int j = 1; j < batchSize; j++) {
									if (uploadTasks[j] != null)
										filesFailed.append(uploadTasks[j]
												.getFileName() + "\n");
								}
								break;
							}
						} else {

							if (i == 0
									&& ((objBatch.size() > 1 && uploadType == UploadTaskOrganizer.MIX_LOADING)
											|| (upLoadTask.getComponentsCount() > 1 && uploadType == UploadTaskOrganizer.COMPLEXOBJECT_LOADING) || (upLoadTask
											.getComponentsCount() > 1 && uploadType == UploadTaskOrganizer.MIX_CO_SHARE_ARK_LOADING))) {
								// Only process when duplicated files checking
								// passed
								numTry = 0;
								successful = false;

								while (!successful && numTry++ < maxTry) {
									try {

										// XXX
										// Insert statement for complex object

										successful = true;
									} catch (Exception e) {
										e.printStackTrace();
									}
								}
							}

							log.info("Uploaded " + successful + ": "
									+ uploadHandler.getSubjectId() + " -> "
									+ uploadHandler.getFileName());
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
						String iMessagePrefix = "JETL Loading interrupted with ";
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
					String iMessagePrefix = "JETL Loading interrupted with ";
					System.out.println(iMessagePrefix + eMessage);
					setStatus("Canceled");
					clearSession();
					log("log", iMessagePrefix + eMessage);
					log.info(iMessagePrefix + eMessage, e);
				}
			}
		} finally {
			if (localStoreLog != null) {
				closeFileStoreLog(collectionId, localStoreLog);
				localStoreLog = null;
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
				+ "damsmanager/downloadLog.do?log=srb&collection="
				+ collectionId + "\">File Store log</a>");
		String exeInfo = exeReport.toString();
		log("log", exeInfo);
		return exeInfo;
	}

	private synchronized void logFile(DAMSUploadTaskHandler uploadTask)
			throws IOException {
		try {
			localStoreLog.write(uploadTask.getSubjectId() + "\t"
					+ uploadTask.getFileName() + "\t"
					+ damsClient.getRequestURL() + "\n");
		} catch (IOException e) {
			// e.printStackTrace();
			if (localStoreLog != null) {
				try {
					localStoreLog.close();
				} catch (IOException e1) {
				}
			}
			localStoreLog = fileStoreLogs.get(collectionId);
			if (localStoreLog == null) {
				String logFileName = Constants.TMP_FILE_DIR + "srblog-"
						+ collectionId + ".log";
				localStoreLog = new FileWriter(logFileName, true);
				fileStoreLogs.put(collectionId, localStoreLog);
			}
		}
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
			String logFileName = Constants.TMP_FILE_DIR + "srblog-"
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
