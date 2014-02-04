package edu.ucsd.library.xdre.web;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.DocumentException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.statistic.analyzer.Statistics;
import edu.ucsd.library.xdre.statistic.report.DAMSCollectionsAccess;
import edu.ucsd.library.xdre.statistic.report.StatsUsage;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.ReportFormater;


 /**
 * Class StatsCollectionsAccessController
 *
 * @author lsitu@ucsd.edu
 */
public class StatsCollectionsAccessController implements Controller {
	public static final String[] APPS = {"dlp"};
	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DocumentException {
		
		Map<String, Object> model = new HashMap<String, Object>();
		
		String startDate = request.getParameter("start");
		String export = request.getParameter("export");
		String message = "";
		Connection con = null;
		Calendar sCal = Calendar.getInstance();
		Calendar eCal = Calendar.getInstance();
		Date sDate = null;
		StatsUsage statsUsage = null;
		StringBuilder strBuf = null;
		SimpleDateFormat dbFormat = null;
		String templete = "statsCollections";
		
		boolean isCas = false;
		String[] apps2sum = {"pas"};
		if(request.isUserInRole(Constants.CURATOR_ROLE)){
			isCas = true;
			apps2sum = APPS;
		}
		
		DAMSClient damsClient = null;
		Map<String, String> colMap = null;
		try {
			damsClient = DAMSClient.getInstance();
			dbFormat = new SimpleDateFormat(Statistics.DATE_FORMAT);	
			if(startDate != null && (startDate=startDate.trim()).length() > 0){
				sDate = dbFormat.parse(startDate);
				sCal.setTime(sDate);
			}else{
				//Default for one year back.
				sCal.add(Calendar.YEAR, -1);
				sCal.add(Calendar.MONTH, 1);
				sCal.set(Calendar.DATE, 1);
			}

			if(export != null)
				strBuf = new StringBuilder();
			
			colMap = collectionsMap(damsClient);
			con = Constants.DAMS_DATA_SOURCE.getConnection();
			
			for(int i=0; i<apps2sum.length; i++){
				statsUsage = new DAMSCollectionsAccess(apps2sum[i], sCal.getTime(), eCal.getTime(), con);
				statsUsage.setStatsMonthly();
				((DAMSCollectionsAccess)statsUsage).setCollectiotMap(colMap);
				
				//Collections access data
				model.putAll(statsUsage.getGraphData());
				List<String> periods = (List<String>)model.get("periodsList");
				periods.add(0, "Collection");
				if(export != null){
					if(i==0){
						strBuf.append("UCSD Library DAMS Usage Statistics " + ReportFormater.getCurrentTimestamp() + "\n");
						//strBuf.append("Month\tCollections\tTotal Items\tSize(MB)\tDAMS Usage\tQueries\tItem Hits\tItem Views\tObject Usage\tObject View" + "\n");
						periods = (List)model.get("periodsList");
						for(int j=0; j<periods.size();j++){
							strBuf.append( periods.get(j));
							if(j< periods.size()-1)
								strBuf.append("\t");
						}
						strBuf.append("\n");
					}
					
					int idx = -1;
					String colId = null;
					String colTitle = null;
					int[] colStatsArr = null;
					Map<String, int[]> colStatsData = (Map<String, int[]>)model.get("colStatsData");
					for(Iterator<String> it=colStatsData.keySet().iterator(); it.hasNext();){
						colId = (String)it.next();
						colTitle = colMap.get(colId);
						idx = colTitle.lastIndexOf("[");
						colStatsArr = (int[])colStatsData.get(colId);
						strBuf.append(colTitle.substring(0, (idx>0?idx:colTitle.length())) + "\t");
						for(int j=0; j<colStatsArr.length; j++){
							strBuf.append(colStatsArr[j]);
							if(j < colStatsArr.length - 1)
								strBuf.append("\t");
						}
						strBuf.append("\n");
					}
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
			if(damsClient != null){
				try{
					damsClient.close();
					damsClient = null;
				}catch(Exception e){}
			}
		}
		if(export == null){
			model.put("isCas", isCas);
			model.put("message", message);
			model.put("start", dbFormat.format(sCal.getTime()));
			return new ModelAndView(templete, "model", model);
		}else{
			if(message != null && message.length() > 0)
				strBuf.append(message);
			OutputStream out = response.getOutputStream();
			String excelFile = request.getSession().getServletContext().getRealPath("files/dams_collsAccess.xls");
			ReportFormater formater = new ReportFormater(strBuf.toString(), excelFile);
			response.setHeader("Content-Disposition", "inline; filename=dams_collections.xls");
			response.setContentType("application/vnd.ms-excel");
			formater.toExcel().write(out);
			return null;
		}
    }
	
	private Map<String, String> collectionsMap(DAMSClient damsClient) throws Exception{
		Map<String, String> collectionsMap = new HashMap<String, String>();
		Map<String, String> m = damsClient.listCollections();
		String key;
		String value;
		int idx = -1;
		for(Iterator<String> it=m.keySet().iterator(); it.hasNext();){
			key = it.next();
			value = m.get(key);
			if((idx=key.lastIndexOf("[")) >= 0)
				key = key.substring(0, idx);
			collectionsMap.put(value, key);
			
			idx = value.lastIndexOf("/");
			if((idx=value.lastIndexOf("/")) >= 0)
				collectionsMap.put(value.substring(idx+1), key);
		}
		return collectionsMap;
	}
}