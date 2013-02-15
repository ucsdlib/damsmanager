package edu.ucsd.library.xdre.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import javax.naming.InitialContext;

/**
 * Constants used by the DAMS Manager
 * 
 * @author Longshou Situ (lsitu@ucsd.edu)
 */
public class Constants {
	public static final String SERVICE = "service";
	public static final String SOURCE = "source";
	public static final String ALTERNATE = "alternate";
	public static final String IMAGE = "image";
	
	public static final String ARK_DAMS_TITLE = "bb3652744n"; //ARK for dams:title

	public static final String COLLECTION_ETD = "bb0956474h"; //ARK for ETD collection
	public static final String COLLECTION_SHOTSOFWAR = "bb3209056n"; //ARK for Shot of War collection
	public static final String COLLECTION_UNIVERSITYCOMMUNICATIONNSEWSRELEASE  = "bb33801227"; //ARK for University Communications collection
	
	public static String ARK_ORG = "20775";
	public static String DAMS_ARK_URL_BASE = "http://libraries.ucsd.edu/ark:/";

	public static String MAILSENDER_DAMSSUPPORT = "dams-support@ucsd.edu";

	public static final String IMPORT_MODE_ADD = "add";
	public static final String IMPORT_MODE_ALL = "all";
	public static final String IMPORT_MODE_SAMEPREDICATES = "samePredicates";
	public static final String IMPORT_MODE_DESCRIPTIVE = "descriptive";
	
	public static final int MEDIUM_RESOLUTION = 2;
	public static final int MEDIUM_RESOLUTION3A = 3;
	public static final int THUMBNAIL = 4;
	public static final int THUMBNAIL2A = 5;

	public static final int TIMEOUT = 30000;

	public static String CLUSTER_HOST_NAME = "";
	public static String DAMS_STORAGE_URL = "";
	public static String TMP_FILE_DIR = "";
	public static String CDL_MERRITT_URL = "";
	public static String DAMS_STAGING = "";
	public static String STATS_LOG_DIR = "";
	public static String CLUSTER_SHARE_PATH = "";
	public static int MERRITT_BATCH_SIZE = 0;
	public static String SOLR_URL_BASE = "";
	public static String JHOVE_ARK_LIST = "";
	public static String IPS_ALLOWED = "";
	public static String CURATOR_ROLE = "";
	public static String SOLR_PDF_FULLTEXT_COLLECTIONS = "";
	public static String JETL_JHOVE_CONF_FILE = "";
	public static String DEFAULT_TRIPLESTORE = "";
	public static String DEFAULT_FILESTORE = "";
	public static String DEFAULT_DERIVATIVES = "";

	static {
		InputStream in = null;
		try {
			// load config from JNDI
			InitialContext ctx = new InitialContext();

			String appHome = (String) ctx.lookup("java:comp/env/damsmanager/home");
			File f = new File(appHome, "damsmanager.properties" );
			in = new FileInputStream(f);
			Properties props = new Properties();
			props.load( in );

			//DAMS REST API servlet URL
			DAMS_STORAGE_URL = props.getProperty("xdre.damsStorageURL");
			
			//Cluster host URL
			CLUSTER_HOST_NAME = props.getProperty("xdre.clusterHostName");

			//Web log directory
			STATS_LOG_DIR = props.getProperty("xdre.weblogs");
			
			//DAMS staging area
			DAMS_STAGING = props.getProperty("xdre.staging");
			
			// Directory to write temp files
			TMP_FILE_DIR = props.getProperty("xdre.tmpFileDir");

			//Merritt feeder URL
			CDL_MERRITT_URL = props.getProperty("xdre.cdlMerrittUrl");

			//Merritt ingestion batch size
			MERRITT_BATCH_SIZE = Integer.parseInt(props.getProperty("xdre.merrittBatchSize"));
			
			//Sharing disk location 
			CLUSTER_SHARE_PATH = props.getProperty("xdre.clusterSharedPath");
			
			//Cluster IP addresses that allow direct access
			IPS_ALLOWED = props.getProperty("xdre.ipsAllowed");
			
			//Curator role 32
			CURATOR_ROLE = props.getProperty("xdre.curatorRole");

			//Jhove configuration file
			JETL_JHOVE_CONF_FILE = props.getProperty("xdre.jetlJhoveConfFile");
			
			//Collections need PDF full text extraction
			SOLR_PDF_FULLTEXT_COLLECTIONS = props.getProperty("xdre.pdfFulltextCollections");

			//SOLR UR
			DEFAULT_DERIVATIVES = props.getProperty("xdre.defaultDerivatives");
			
			//Default derivatives
			SOLR_URL_BASE = props.getProperty("xdre.solrBase");
			//Retrieve the default triplestore and filestore
			DAMSClient damsClient = new DAMSClient(DAMS_STORAGE_URL);		
			DEFAULT_FILESTORE = damsClient.defaultFilestore(); //Default filestore
			DEFAULT_TRIPLESTORE = damsClient.defaultTriplestore(); //Default triplestore
			
		} catch (Exception e) {
			e.printStackTrace();
		} finally{
			if(in != null){
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				in = null;
			}
		}
	}

	public static boolean startRoutine() {
		// DamsRoutineManager routineManager =
		// DamsRoutineManager.getInstance(DEFAULT_TRIPLESTORE, DAR_TRIPLESTORE,
		// DAR_TS_TO_SYNCHRONIZE);
		// routineManager.start();
		return true;
	}
}
