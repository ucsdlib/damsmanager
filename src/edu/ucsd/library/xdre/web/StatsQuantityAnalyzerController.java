package edu.ucsd.library.xdre.web;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.Date;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.statistic.analyzer.DAMSQuantityStats;
import edu.ucsd.library.xdre.statistic.analyzer.Statistics;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DomUtil;


 /**
 * Class StatsDAMSQuantityAnalyzerController collects quantity statistics for DAMS
 *
 * @author lsitu@ucsd.edu
 */
public class StatsQuantityAnalyzerController implements Controller {
	
	public Logger logger = Logger.getLogger(StatsQuantityAnalyzerController.class);
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
			
		boolean successful = false;
		String message = "";
		String numOfDaysDefered = request.getParameter("deferred");
		int daysDeferred = 1;

		Calendar sCal = Calendar.getInstance();
		try {
			if(numOfDaysDefered != null && (numOfDaysDefered=numOfDaysDefered.trim()).length() > 0)
				daysDeferred = Integer.parseInt(numOfDaysDefered);
			sCal.add(Calendar.DATE, -daysDeferred);
			
			statsAnalyze(sCal.getTime());
			successful = true;
		}catch(NumberFormatException ne){
			message += "Invalid number " + numOfDaysDefered + " for day defered: " + ne.getMessage();
		} catch (NamingException e) {
			e.printStackTrace();
			message += "InternalError: " + e.getMessage();
		} catch (SQLException e) {
			e.printStackTrace();
			message += "InternalError: " + e.getMessage();
		}catch (Exception e) {
			e.printStackTrace();
			message += "Error: " + e.getMessage();
		}
			
		if(successful)
			message = "Processed collection statistic on " +  Constants.CLUSTER_HOST_NAME + " successfully: " + Statistics.getDatabaseDateFormater().format(sCal.getTime()) + ". \n";
		else
			message = "Failed to calculate the object size for month ended at " + Statistics.getDatabaseDateFormater().format(sCal.getTime()) + " on " +  Constants.CLUSTER_HOST_NAME + " : \n" + message;
		
		OutputStream out = response.getOutputStream();
		logger.info(message);
		Element resultElem = DomUtil.createElement(null, "result", null, null);
		DomUtil.createElement(resultElem, "status", null, String.valueOf(successful));
		if((message != null && message.length() > 0))
			DomUtil.createElement(resultElem, "message", null, message);
		response.setContentType("text/xml");
		response.setCharacterEncoding("UTF-8");
		out.write(resultElem.asXML().getBytes());
		out.close();
		return null;
    }
	
	public static synchronized void statsAnalyze(Date statsDate) throws Exception{
		DAMSClient damsClient = null;
		Connection con = null;
		DAMSQuantityStats quanStats = null;
		Calendar sCal = Calendar.getInstance();
		sCal.setTime(statsDate);
		boolean successful = false;
		if(sCal.get(Calendar.MONTH) != Calendar.getInstance().get(Calendar.MONTH)){
			try {
				damsClient = DAMSClient.getInstance();
				con = Constants.DAMS_DATA_SOURCE.getConnection();
				
				quanStats = new DAMSQuantityStats(damsClient);
				quanStats.setCalendar(sCal);
				quanStats.doStatistics(con);
				quanStats.print();
				Statistics.close(con);
				con = null;
				con = Constants.DAMS_DATA_SOURCE.getConnection();
				con.setAutoCommit(false);
				quanStats.export(con);
				con.commit();
				successful = quanStats.getFailedItems().length()==0;
				if(!successful){
					String failedItems = quanStats.getFailedItems();
					throw new Exception("DAMS quantity statistics analyst failed on " + Statistics.getDatabaseDateFormater().format(sCal.getTime()) + ": " + failedItems.toString());
				}
			}finally{
				Statistics.close(con);
				con = null;
				if(damsClient != null){
					try {
						damsClient.close();
					} catch (Exception e) {
						e.printStackTrace();
					}
					damsClient = null;
				}
			}
		}else
			throw new Exception("Can't generate DAMS Quantity statistics for the current month: " + Statistics.getDatabaseDateFormater().format(sCal.getTime()));
	}
}