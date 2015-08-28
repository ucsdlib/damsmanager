package edu.ucsd.library.xdre.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.InitialContext;
import javax.sql.DataSource;

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

	public static String DEFAULT_ARK_NAME = "";
	public static String ARK_ORG = "";
	public static String DAMS_ARK_URL_BASE = "";
	public static String DAMS_CLR_URL_BASE = "";
	public static String DAMS_CLR_IMG_DIR = "";
	public static String DAMS_CLR_THUMBNAILS_DIR = "";
	public static String DAMS_CLR_SOURCE_DIR = "";
	
	public static String DAMS_STORAGE_URL = "";
	public static String DAMS_STORAGE_USER = "";
	public static String DAMS_STORAGE_PWD = "";
	
	public static String CLUSTER_HOST_NAME = "";
	public static String TMP_FILE_DIR = "";
	public static String DAMS_STAGING = "";
	public static String SOLR_URL_BASE = "";
	public static String DEFAULT_TRIPLESTORE = "";
	public static String DEFAULT_DERIVATIVES = "";
	
	public static String MAILSENDER_DAMSSUPPORT = "";
	
	public static String FILESTORE_DIR = "";
	public static String DERIVATIVES_LIST = "";
	public static String VIDEO_SIZE = "";
	public static String FFMPEG_COMMAND = "";
	public static String FFMPEG_VIDEO_PARAMS = "";
	public static String FFMPEG_AUDIO_PARAMS = "";
	public static String EXIFTOOL_COMMAND = "exiftool";
	public static String IMAGEMAGICK_COMMAND = "convert";
	
	/* begin stats declaration*/
	public static String CURATOR_ROLE ="";
	public static DataSource DAMS_DATA_SOURCE = null;
	public static String STATS_WEBLOG_DIR = "";
	/* end stats declaration*/
	
	//Namespace prefix
	public static String NS_PREFIX = "";
	public static Map<String, String> NS_PREFIX_MAP = null;
	
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
			
			// Collection URL base
			DAMS_CLR_URL_BASE = props.getProperty("xdre.clr.urlBase");

			//Collection image dir
			DAMS_CLR_IMG_DIR = props.getProperty("xdre.clr.imgDir");

			//Collection thumbnails dir
			DAMS_CLR_THUMBNAILS_DIR = props.getProperty("xdre.clr.thumbnailsDir");

			//Collection image source dir
			DAMS_CLR_SOURCE_DIR = props.getProperty("xdre.clr.sourceDir");
			
			//ARK name
			DEFAULT_ARK_NAME = props.getProperty("xdre.ark.name");
			//ARK orgCode
			ARK_ORG = props.getProperty("xdre.ark.orgCode");
			
			//Support mail
			MAILSENDER_DAMSSUPPORT = props.getProperty("mail.support");
			
			//Local filestore base dir for DAMS3 files rename to DAMS4 naming convention only
			FILESTORE_DIR = props.getProperty("fs.localStore.baseDir");
			
			//DAMS4/dams3 derivative list
			DERIVATIVES_LIST = props.getProperty("derivatives.list");
			
			// FFMPEG command
			FFMPEG_COMMAND = props.getProperty("xdre.ffmpeg");
			
			// FFMPEG MP4 video derivative creation parameters string
			FFMPEG_VIDEO_PARAMS = props.getProperty("ffmpeg.video.params");

			// FFMPEG MP3 audio derivative creation parameters string
			FFMPEG_AUDIO_PARAMS = props.getProperty("ffmpeg.audio.params");

			VIDEO_SIZE = props.getProperty("video.size");

			//ExifTool command
			EXIFTOOL_COMMAND = props.getProperty("exiftool.command");

			//ImageMagick command
			IMAGEMAGICK_COMMAND = props.getProperty("imageMagick.command");

			// Namespace prefix
			NS_PREFIX = props.getProperty("ns.prefix");
			NS_PREFIX_MAP = new HashMap<String, String>();
			if(NS_PREFIX != null){
				String[] nsPrefixes = NS_PREFIX.split(",");
				for(int i=0; i<nsPrefixes.length; i++){
					String nsPrefix = nsPrefixes[i].trim();
					NS_PREFIX_MAP.put(nsPrefix, props.getProperty("ns.prefix." + nsPrefix));
				}
			}
			
			// Retrieve the default triplestore
			DAMSClient damsClient = new DAMSClient(DAMS_STORAGE_URL);		
			DEFAULT_TRIPLESTORE = damsClient.defaultTriplestore(); //Default triplestore

			// Weblog location
			STATS_WEBLOG_DIR = props.getProperty("dams.weblog.dir");
			// DAMS super user role
			CURATOR_ROLE = props.getProperty("dams.curator.role");
			// DAMS stats datasource
			DAMS_DATA_SOURCE = (DataSource) ctx.lookup("java:comp/env/jdbc/dams");
			
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
}
