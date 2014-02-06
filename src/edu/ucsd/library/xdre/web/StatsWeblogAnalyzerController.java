package edu.ucsd.library.xdre.web;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.statistic.analyzer.LogAnalyzer;
import edu.ucsd.library.xdre.statistic.analyzer.Statistics;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DomUtil;

/**
 * 
 * StatsWeblogAnalyzerController accepts request to process the web logs to collect data for DAMS statistics.
 * @author lsitu@ucsd.edu
 */
public class StatsWeblogAnalyzerController implements Controller {
	private static Logger logger = Logger.getLogger(StatsWeblogAnalyzerController.class);
	
	public ModelAndView handleRequest(HttpServletRequest request,
		HttpServletResponse response) throws Exception{
		String startDate = request.getParameter("start");
		String endDate = request.getParameter("end");
		boolean update = request.getParameter("update") != null;
		Date sDate = null;
		Date eDate = null;
		String message = "";
		Calendar cal = Calendar.getInstance();
		boolean successful = false;
		SimpleDateFormat dFormat = new SimpleDateFormat(Statistics.DATE_FORMAT);
		if(startDate != null && (startDate=startDate.trim()).length() > 0){
			try{
				sDate = dFormat.parse(startDate);
			}catch(Exception de){
				message = "Invalid start date. Please use format 'yyyy-MM-dd'. \n";
			}
		}else{
			cal.add(Calendar.DATE, -1);
			sDate = cal.getTime();
		}
		if(endDate != null && (endDate=endDate.trim()).length() > 0){
			try{
				eDate = dFormat.parse(endDate);
			}catch(Exception de){
				message += "Invalid end date. Please use format 'yyyy-MM-dd'. \n";
			}
		}else
			eDate = cal.getTime();
		
		if(message.length() == 0){
			try {
				analyzeWeblog(sDate, eDate, update);
				successful = true;
			} catch (Exception e) {
				e.printStackTrace();
				message += (message.length()>0?"; ":"") + e.getMessage();
			}finally{
				if(Constants.CLUSTER_HOST_NAME.indexOf("library") >= 0){
					String sender = Constants.MAILSENDER_DAMSSUPPORT;
					if(successful)
						message = "Processed weblog for DAMS statistics successfully: " + startDate + (startDate.equals(endDate)?"":" to " + endDate) + (update?" updated":" processed") + ". \n" + message;
					else
						message = "Failed to " + (update?"update":"processe") + " statistics data: " + startDate + (startDate.equals(endDate)?"":" to " + endDate) + ". \n" + message;
					logger.info(message);
					DAMSClient.sendMail(sender, new String[] {sender}, "DAMS Statistics Weblog Analyzer", message, "text/html", "smtp.ucsd.edu");
				}
			}
		}
		Element resultElem = DomUtil.createElement(null, "result", null, null);
		DomUtil.createElement(resultElem, "status", null, String.valueOf(successful));
		DomUtil.createElement(resultElem, "message", null, message);
		response.setContentType("text/xml");
		response.setCharacterEncoding("UTF-8");
		PrintWriter output = response.getWriter();
		output.write(resultElem.asXML());
		output.flush();
		output.close();
		return null;
	}

	public static void analyzeWeblog(Date startDate, Date endDate, boolean update) throws Exception{
		SimpleDateFormat dFormat = new SimpleDateFormat(Statistics.DATE_FORMAT);
		Calendar sCal = Calendar.getInstance();
		Calendar eCal = Calendar.getInstance();
		sCal.setTime(startDate);
		eCal.setTime(endDate);
		Connection con = null;
		String weblogDone = "";
		String weblogMissing = "";
		try {
			synchronized(logger){
				con = Constants.DAMS_DATA_SOURCE.getConnection();
				con.setAutoCommit(false);
				do{
					String dateString = dFormat.format(sCal.getTime());
					File logFile = getLogFile(dateString);
					if(!logFile.exists()){
						weblogMissing += (weblogMissing.length()>0?", ":"") + logFile.getAbsolutePath();
						logger.error("Weblog doesn't exist: " + logFile.getAbsolutePath());
						//throw new Exception(message);
					}else{
						LogAnalyzer analyzer = new LogAnalyzer();
						if(update || !analyzer.isRecordExist(con, dateString)){
							analyzer.setUpdate(update);
							analyzer.analyze(logFile);
							analyzer.export(con);
							con.commit();
						}else
							weblogDone += (weblogDone.length()>0?", ":"") + logFile.getName();
					}
					sCal.add(Calendar.DATE, 1);
				}while(sCal.before(eCal));
				
				String message = "";
				if(weblogDone.length() > 0)
					message = "Statistics records existed for weblog(s) " + weblogDone;
				if(weblogMissing.length() > 0)
					message += (message.length()>0?"; ":"") + "Missing weblog(s): " + weblogMissing;
				if(message.length() > 0)
					throw new Exception(message);
			}
		}finally{
			if(con != null){
				try{
					con.close();
				}catch(SQLException e){}
				con = null;
			}
		}
	}
	
	private static File getLogFile(String date){
		File logFile = null;
		logFile = new File(Constants.STATS_WEBLOG_DIR + "/messages." + date + ".gz");
		if(!logFile.exists())
			logFile = new File(Constants.STATS_WEBLOG_DIR + "/httpd." + date + ".gz");
		return logFile;
	}
}
