package edu.ucsd.library.xdre.web;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.sql.Connection;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.Node;
import org.dom4j.io.SAXReader;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.statistic.analyzer.Statistics;
import edu.ucsd.library.xdre.statistic.beans.DAMSItemSummary;
import edu.ucsd.library.xdre.statistic.beans.DAMSummary;
import edu.ucsd.library.xdre.statistic.beans.StatSummary;
import edu.ucsd.library.xdre.statistic.report.DAMSUsage;
import edu.ucsd.library.xdre.statistic.report.StatsUsage;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.ReportFormater;


 /**
 * Class StatsSummaryController to summarize out put for dams statistics
 *
 * @author lsitu@ucsd.edu
 */
public class StatsSummaryController implements Controller {
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
		NumberFormat numFormatter = new DecimalFormat("#,###");
		String solrParams = "q=category:bb36527497&fl=subject&rows=1000&wt=xml";
		String collectionFilter = "";
		String templete = "statsSummary";
		
		boolean isCas = false;
		String[] apps2sum = {"pas"};
		if(request.isUserInRole(Constants.CURATOR_ROLE)){
			isCas = true;
			//apps2sum = APPS;
		}else{
			solrParams += "q=has_model_ssim:\"info:fedora/afmodel:DamsProvenanceCollection\"+OR+has_model_ssim:\"info:fedora/afmodel:DamsProvenanceCollectionPart\"+OR+has_model_ssim:\"info:fedora/afmodel:DamsAssembledCollection\"+OR+has_model_ssim:\"info:fedora/afmodel:DamsCollection\"&fl=id&rows=500";
			SAXReader saxReader = new SAXReader();
			Document doc = saxReader.read(new URL(Constants.SOLR_URL_BASE + "/select?" + solrParams));
			List<Node> subjectNodes = doc.selectNodes("/response/result/doc/str[@name='id']");
			for(Iterator<Node> it=subjectNodes.iterator();it.hasNext();){
				collectionFilter += (collectionFilter.length()>0?",":"") + "'" + ((Node)it.next()).getText() + "'";
			}
		}
		
		try {
			
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
			con = Constants.DAMS_DATA_SOURCE.getConnection();
			Collection<StatSummary> rows = null;
			DAMSummary dlcSum = null;
			DAMSItemSummary dlcItemSum = null;

			for(int i=0; i<apps2sum.length; i++){
				statsUsage = new DAMSUsage(apps2sum[i], sCal.getTime(), eCal.getTime(), con);
				statsUsage.setStatsMonthly();
				if(!isCas){
					((DAMSUsage)statsUsage).setCollectionFilter(collectionFilter);
				}
				rows = statsUsage.getStatSummary();
				model.put(apps2sum[i], rows);
				
				if(export != null){
					if(i==0){
						strBuf.append("UCSD Library DAMS Statistics " + ReportFormater.getCurrentTimestamp() + "\n");
						//strBuf.append("Month\tCollections\tTotal Items\tSize(MB)\tDLC Usage\tQueries\tItem Hits\tItem Views\tObject Usage\tObject View" + "\n");
						strBuf.append("Month\tCollections\tTotal Items\tSize(MB)\tDAMS Usage\tQueries\tItem Hits\tItem Views" + "\n");
					}else{
						strBuf.append(" ");
						strBuf.append(apps2sum[i]);
					}
					for(Iterator<StatSummary> it=rows.iterator();it.hasNext();){
						dlcSum = (DAMSummary)it.next();
						dlcItemSum = dlcSum.getItemSummary(); 
						strBuf.append(dlcSum.getPeriodDisplay() + "\t" + dlcSum.getNumOfCollections() + "\t" + numFormatter.format(dlcSum.getNumOfItems()) + "\t" + numFormatter.format(dlcSum.getTotalSize()/1000000.0) + "\t" 
								+ numFormatter.format(dlcSum.getNumOfUsage()) + "\t" + numFormatter.format(dlcSum.getNumOfQueries()) + "\t" + numFormatter.format(dlcItemSum.getNumOfUsage())+ "\t" + numFormatter.format(dlcItemSum.getNumOfViews())/*+ "\t" + numFormatter.format(dlcItemSum.getNumOfItemAccessed()) + "\t" + numFormatter.format(dlcItemSum.getNumOfItemViewed())*/ + "\n");
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
			Statistics.close(con);
			con = null;
		}
		if(export == null){
			model.put("isCas", isCas);
			model.put("message", message);
			model.put("start", dbFormat.format(sCal.getTime()));
			model.put("clusterHost", "//" + Constants.CLUSTER_HOST_NAME + ".ucsd.edu");
			return new ModelAndView(templete, "model", model);
		}else{
			if(message != null && message.length() > 0)
				strBuf.append(message);
			OutputStream out = response.getOutputStream();
			String excelFile = request.getSession().getServletContext().getRealPath("files/dams_summary.xls");
			ReportFormater formater = new ReportFormater(strBuf.toString(), excelFile);
			response.setHeader("Content-Disposition", "inline; filename=dams_summary.xls");
			response.setContentType("application/vnd.ms-excel");
			formater.toExcel().write(out);
			return null;
		}
    }  	
}