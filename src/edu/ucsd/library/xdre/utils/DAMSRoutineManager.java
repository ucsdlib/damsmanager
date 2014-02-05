package edu.ucsd.library.xdre.utils;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;

import org.apache.log4j.Logger;

import edu.ucsd.library.xdre.statistic.analyzer.Statistics;
import edu.ucsd.library.xdre.web.StatsCollectionsReportController;
import edu.ucsd.library.xdre.web.StatsQuantityAnalyzerController;
import edu.ucsd.library.xdre.web.StatsWeblogAnalyzerController;

public class DAMSRoutineManager{
	private static Logger logger = Logger.getLogger(DAMSRoutineManager.class);
	private static DAMSRoutineManager worker = null;
	private static int SCHEDULED_HOUR = 1;
	
	private DAMSRoutineManager(){}
	
	public void start(){
		Calendar calendar = Calendar.getInstance();
		calendar.add(Calendar.DATE, 1);
		calendar.set(Calendar.HOUR_OF_DAY, SCHEDULED_HOUR);
		calendar.set(Calendar.MINUTE, 50);
		calendar.set(Calendar.SECOND, 0);
		Date scheduledTime = calendar.getTime();
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new DamsRoutine(), scheduledTime, 1000*60*60*24);
		
		//Caching collection report
		calendar = Calendar.getInstance();
		calendar.add(Calendar.MINUTE, 2);
		timer = new Timer();
		timer.schedule(new StatisticsTask(), calendar.getTime());
		
	}
	
	class StatisticsTask extends TimerTask{

		public void run() {
			// DAMS collection reports
			try{
				logger.info("DAMS Mananger generating gollections report ... ");
				StatsCollectionsReportController.generateReport();
				logger.info("Collections report generated on " + Calendar.getInstance() + ".");
			}catch(Exception e){
				e.printStackTrace();
				logger.error("Failed to generate collections report on " + Calendar.getInstance() + ".");
			}
		}
		
	}
	
	class DamsRoutine extends TimerTask{
		
		public DamsRoutine(){}
		
		public void run(){

			int dateField = Calendar.HOUR_OF_DAY;
			long logAvailableTime = 30 * 24 * 60 * 60 * 10000;
			
			Calendar cal = Calendar.getInstance();
			cal.setTime(new Date());
			int hour = cal.get(dateField);
			logger.info("DAMS Manager scheduled task running at " + hour + " o'clock ... " );

			//Cleanup temp files
			File tmpDirFile = new File(Constants.TMP_FILE_DIR);
			if(tmpDirFile.exists()){
				File[] tmpFiles = tmpDirFile.listFiles();
				String fName = null;
				File tmp = null;
				for(int i=0; i<tmpFiles.length; i++){
					tmp = tmpFiles[i];
					if(tmp.isFile() && !tmp.isHidden()){
						fName = tmp.getName();
						if(fName.startsWith("tmpRdf_") || fName.startsWith("tmp_")){
							if((System.currentTimeMillis() - tmp.lastModified()) > logAvailableTime)
								tmp.delete();
						}else if(fName.startsWith("damslog-") || fName.startsWith("_tmp_damslog-") || fName.startsWith("_tmp_export-") || fName.startsWith("export-")){
							if((System.currentTimeMillis() - tmp.lastModified()) > logAvailableTime)
								tmp.delete();
						}
					}
				}
			}else
				logger.error("Error: directory " +  tmpDirFile.getAbsolutePath() + " doesn't exist.");
			
			Calendar sCal = Calendar.getInstance();
			sCal.add(Calendar.DATE, -1);
			Calendar eCal = Calendar.getInstance();
			SimpleDateFormat dFormat = Statistics.getDatabaseDateFormater();
			try{	
				StatsWeblogAnalyzerController.analyzeWeblog(sCal.getTime(), eCal.getTime(), false);
				logger.info("DAMS Statistics Weblog analyzed for period " + dFormat.format(sCal.getTime()) + " to " + dFormat.format(eCal.getTime()) + ".");
			}catch(Exception e){
				e.printStackTrace();
				logger.error(" DAMS Statistics failed to analyze Weblog for period " + dFormat.format(sCal.getTime()) + " to " + dFormat.format(eCal.getTime()) + ".");
			}
			
			// DAMS collection reports
			try{
				StatsCollectionsReportController.generateReport();
				logger.info("Collections report generated on " + Calendar.getInstance() + ".");
			}catch(Exception e){
				e.printStackTrace();
				logger.error("Failed to generate collections report on " + Calendar.getInstance() + ".");
			}
			
			// DAMS statistics quantity monthly analyzing
			if(cal.get(Calendar.DAY_OF_MONTH) == 1){
				try{
					cal.add(Calendar.DATE, -1);
					StatsQuantityAnalyzerController.statsAnalyze(cal.getTime());
					logger.info("DAMS quantity statistics generated on " +  Statistics.getDatabaseDateFormater().format(cal.getTime()) + ".");
				}catch(Exception e){
					e.printStackTrace();
					logger.error("Failed to generate DAMS quantity statistics on " +  Statistics.getDatabaseDateFormater().format(cal.getTime()) + ".");
				}
			}
		}
	}
	
	public static synchronized boolean startRoutine(){
		worker = new DAMSRoutineManager();
		worker.start();
		return true;
	}
}
