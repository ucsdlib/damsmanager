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
	public static final String ARK_DAMS_TITLE = "bb3652744n"; //ARK for ETD collection

	public static final String COLLECTION_ETD = "bb0956474h"; //ARK for ETD collection
	public static final String COLLECTION_SHOTSOFWAR = "bb3209056n"; //ARK for Shot of War collection
	public static final String COLLECTION_UNIVERSITYCOMMUNICATIONNSEWSRELEASE  = "bb33801227"; //ARK for University Communications collection
	
	public static String ARK_ORG = "20775";
	public static String DAMS_ARK_URL_BASE = "http://libraries.ucsd.edu/ark:/";

	public static String MAILSENDER_DAMSSUPPORT = "dams-support@ucsd.edu";

	public static final int JETL_UPLOAD_ALL = 0;
	public static final int JETL_UPLOAD_FILE_ONLY = 1;
	public static final int JETL_UPLOAD_JHOVE_ONLY = 2;

	public static final String PATHS_DELIMITER = ";";
	public static final String XPATH_ESCAPE_SEQUENCE = "|||";
	public static final String SUBJECT_XPATH = "/RDF/Description";
	public static final int METADAT_NEW = 0;
	public static final int METADAT_RENEW = 1;
	public static final int METADAT_REPOPULATE_ALL = 2;
	public static final int METADAT_REPOPULATE_ONLY = 3;
	public static final int METADAT_SAME_PREDICATE_REPLACEMENT = 4;

	public static final int THUMBNAIL = 1;
	public static final int MEDIUM_RESOLUTION = 2;
	public static final int MEDIUM_RESOLUTION3A = 3;
	public static final int THUMBNAIL2A = 4;
	public static final String THUMBNAIL_SIZE = "150x150";
	public static final String THUMBNAIL2A_SIZE = "65x65";
	public static final String MEDIUM_RESOLUTION_SIZE = "768x768";
	public static final String MEDIUM_RESOLUTION3A_SIZE = "450x450";
	public static final String THUMBNAIL_NAME_EXT = "-2.jpg";
	public static final String MEDIUM_RESOLUTION_NAME_EXT = "-3.jpg";
	public static final String MEDIUM_RESOLUTION3A_NAME_EXT = "-3a.jpg";
	public static final String THUMBNAIL2A_NAME_EXT = "-2a.jpg";
	public static final String[] DERIVATIVE_NAMES = { "2a", "2", "3a", "3" };
	public static final String[] DERIVATIVE_SIZES = { THUMBNAIL2A_SIZE,
			THUMBNAIL_SIZE, MEDIUM_RESOLUTION3A_SIZE, MEDIUM_RESOLUTION_SIZE };

	public static final int TIMEOUT = 30000;
	
	public static final String NAMESPACE_RDF = "rdf";
	public static final String ELEM_ABOUT = "about";
	public static final String ELEM_NODEID = "nodeId";
	public static final String ELEM_PARSETYPE = "parseType";
	public static final String RESOURCE = "Resource";


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
