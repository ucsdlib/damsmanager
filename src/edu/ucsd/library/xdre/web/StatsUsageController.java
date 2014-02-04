package edu.ucsd.library.xdre.web;

import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.statistic.report.DAMSAppUsage;
import edu.ucsd.library.xdre.statistic.report.DAMSItemUsage;
import edu.ucsd.library.xdre.statistic.report.StatsUsage;
import edu.ucsd.library.xdre.utils.Constants;


 /**
 * Class StatsUsageController summarizes the output for DAMS usage
 *
 * @author lsitu@ucsd.edu
 */
public class StatsUsageController implements Controller {
	public static final String[] APPS = {"pas", "cas"};
	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		Map<String, Object> model = new HashMap<String, Object>();
		String statsType = request.getParameter("type");
		String templete = request.getParameter("templete");
		
		String message = "";
		Connection con = null;
		Calendar sCal = Calendar.getInstance();
		Calendar eCal = Calendar.getInstance();
		sCal.add(Calendar.YEAR, -1);
		sCal.add(Calendar.MONTH, 1);
		sCal.set(Calendar.DATE, 1);
		
		boolean isCas = false;
		String[] apps2sum = {"pas"};
		if(request.isUserInRole(Constants.CURATOR_ROLE)){
			isCas = true;
		}
	
		try {
			if(isCas)
				apps2sum[0] = "dlp";
			con = Constants.DAMS_DATA_SOURCE.getConnection();
			StatsUsage statsUsage = null;

			if(statsType != null && statsType.equalsIgnoreCase("item")){
				templete = "statsItemUsage";
				for(int i=0; i<apps2sum.length; i++){
					statsUsage = new DAMSItemUsage(apps2sum[i], sCal.getTime(), eCal.getTime(), con);
					model.putAll(statsUsage.getGraphData());
				}
			}else{
				if(isCas)
					apps2sum = APPS;
				
				templete = "statsUsage";
				
				for(int i=0; i<apps2sum.length; i++){
					statsUsage = new DAMSAppUsage(apps2sum[i], sCal.getTime(), eCal.getTime(), con);
					model.putAll(statsUsage.getGraphData());
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			message += "InternalError: " + e.getMessage();
		} catch (Exception e) {
			e.printStackTrace();
			message += "Error: " + e.getMessage();
		}finally{
			if(con != null){
				try{
					con.close();
					con = null;
				}catch(SQLException e){}
			}
		}
		
		model.put("isCas", isCas);
		model.put("message", message);
		
		return new ModelAndView(templete, "model", model);
    }  	
}