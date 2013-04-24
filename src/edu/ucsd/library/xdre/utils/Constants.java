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

	public static final String IMPORT_MODE_ADD = "add";
	public static final String IMPORT_MODE_ALL = "all";
	public static final String IMPORT_MODE_SAMEPREDICATES = "samePredicates";
	public static final String IMPORT_MODE_DESCRIPTIVE = "descriptive";
	public static final String IMPORT_MODE_DELETE = "delete";

	public static final int TIMEOUT = 30000;

	public static String ARK_ORG = "";
	public static String DAMS_ARK_URL_BASE = "";
	
	public static String DAMS_STORAGE_URL = "";
	public static String DAMS_STORAGE_USER = "";
	public static String DAMS_STORAGE_PWD = "";
	
	public static String CLUSTER_HOST_NAME = "";
	public static String TMP_FILE_DIR = "";
	public static String DAMS_STAGING = "";
	public static String SOLR_URL_BASE = "";
	public static String DEFAULT_TRIPLESTORE = "";
	public static String DEFAULT_FILESTORE = "";
	public static String DEFAULT_DERIVATIVES = "";
	
	public static String MAILSENDER_DAMSSUPPORT = "";
	
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

			//DAMS repository REST API servlet URL
			DAMS_STORAGE_URL = props.getProperty("xdre.damsRepo");
			
			//DAMS repository user
			DAMS_STORAGE_USER = props.getProperty("xdre.damsRepo.user");
			
			//DAMS repository password
			DAMS_STORAGE_PWD = props.getProperty("xdre.damsRepo.pwd");
			
			//Cluster host URL
			CLUSTER_HOST_NAME = props.getProperty("xdre.clusterHostName");
			
			//DAMS staging area
			DAMS_STAGING = props.getProperty("xdre.staging");
			
			// Directory to write temp files
			TMP_FILE_DIR = props.getProperty("xdre.tmpFileDir");

			//SOLR UR
			DEFAULT_DERIVATIVES = props.getProperty("xdre.defaultDerivatives");
			
			//Default derivatives
			SOLR_URL_BASE = props.getProperty("xdre.solrBase");
			
			//ARK URL nase
			DAMS_ARK_URL_BASE = props.getProperty("xdre.ark.urlBase");
			
			//ARK orgCode
			ARK_ORG = props.getProperty("xdre.ark.orgCode");
			
			//Support mail
			MAILSENDER_DAMSSUPPORT = props.getProperty("mail.support");
			
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
