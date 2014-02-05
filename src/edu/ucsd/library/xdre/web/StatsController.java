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
import edu.ucsd.library.xdre.statistic.report.StatsUsage;
import edu.ucsd.library.xdre.utils.Constants;


 /**
 * Class StatsController handles the stats requests
 *
 * @author lsitu@ucsd.edu
 */
public class StatsController implements Controller {

	public static final String[] APPS = {"pas", "cas"};
	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		Map<String, Object> model = new HashMap<String, Object>();
		
		String message = "";
		Connection con = null;
		Calendar sCal = Calendar.getInstance();
		Calendar eCal = Calendar.getInstance();
		sCal.add(Calendar.MONTH, -1);
		String templete = "stats";	
		
		boolean isCas = false;
		String[] apps2sum = {"pas"};
		if(request.isUserInRole(Constants.CURATOR_ROLE)){
			isCas = true;
			apps2sum = APPS;
		}

		try {
			
			con = Constants.DAMS_DATA_SOURCE.getConnection();
			for(int i=0; i<apps2sum.length; i++){
				StatsUsage statsUsage = null;

				statsUsage = new DAMSAppUsage(apps2sum[i], sCal.getTime(), eCal.getTime(), con);
				statsUsage.setStatsDaily();
				model.putAll(statsUsage.getGraphData());
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
		model.put("clusterHost", "//" + Constants.CLUSTER_HOST_NAME + ".ucsd.edu");
		
		return new ModelAndView(templete, "model", model);
    }
}