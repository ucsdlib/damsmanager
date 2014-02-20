package edu.ucsd.library.xdre.web;

import java.io.OutputStream;
import java.net.URLEncoder;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.dom4j.Document;
import org.dom4j.Element;
import org.dom4j.Node;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.statistic.analyzer.Statistics;
import edu.ucsd.library.xdre.statistic.report.StatsUsage;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DomUtil;
import edu.ucsd.library.xdre.utils.ReportFormater;

/**
 * Class StatsCollectionsReportController generate reports for DAMS collections
 * @author lsitu
 *
 */
public class StatsCollectionsReportController implements Controller {
	private static Logger log = Logger.getLogger(StatsCollectionsReportController.class);
	private static String STATUSREPORT = null;
	private static NumberFormat NUM_FORMATER = new DecimalFormat("#,###");
	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {

		String message = "";
		boolean successful = true;
		boolean current = request.getParameter("current")!=null;
		boolean cacheOnly = request.getParameter("cacheOnly")!=null;
		String today = ReportFormater.getCurrentTimestamp();
		today = today.substring(0, today.indexOf('T'));
		if(cacheOnly || STATUSREPORT == null || current || STATUSREPORT.indexOf(today) < 0){
			try{
				
				generateReport();
			}catch (Exception e){
				e.printStackTrace();
				successful = false;
				message += "Failed to generate report. Internal error: " + e.getMessage();
			}
		}
		OutputStream out = null;
		try{
			out = response.getOutputStream();
			if(cacheOnly || !successful){
				System.out.println(message);
				Element resultElem = DomUtil.createElement(null, "result", null, null);
				DomUtil.createElement(resultElem, "status", null, String.valueOf(successful));
				if((message != null && message.length() > 0))
					DomUtil.createElement(resultElem, "message", null, message);
				response.setContentType("text/xml");
				response.setCharacterEncoding("UTF-8");
				out.write(resultElem.asXML().getBytes());
			}else{
				String excelFile = request.getSession().getServletContext().getRealPath("files/collections_report.xls");
				ReportFormater formater = new ReportFormater(STATUSREPORT, excelFile);
				response.setHeader("Content-Disposition", "inline; filename=collections_report.xls");
				response.setContentType("application/vnd.ms-excel");
				formater.toExcel().write(out);
			}
		}catch (Exception e){
			e.printStackTrace();
		}finally {
			if(out != null)
				out.close();
		}
		return null;
	}

	public synchronized static void generateReport() throws Exception{
		DAMSClient damsClient = null;
		StringBuilder strBuf = new StringBuilder();
		strBuf.append("UCSD Library DAMS Collections Report - " + ReportFormater.getCurrentTimestamp() + "\n");
		strBuf.append("Collection\tType\tUnit\tSearchable Items\tItems Count\tSize(MB)\tView\tPublic\tUCSD\tCurator\tRestricted\tCulturally Sensitive\tNotes" + "\n");

		Map<String, String> colRows = new TreeMap<String, String>();

		try{
			String colId = null;
			String colTitle = null;
			List<String> values = null;
			String visibility = "";
			String unit = "";
			String colType = "";
			String rowVal = null;
			Document doc = null;
			damsClient = DAMSClient.getInstance();
			Map<String, String> colMap = damsClient.listCollections();
			for(Iterator<String> iter=colMap.keySet().iterator();iter.hasNext();){
				visibility = "";
				unit = "";
				colTitle = iter.next();
				colId = colMap.get(colTitle);
				colId = colId.substring(colId.lastIndexOf("/")+1);
				int idx = colTitle.lastIndexOf("[");
				if( idx > 0){
					colType = colTitle.substring(idx+1, colTitle.lastIndexOf("]"));
					colTitle = colTitle.substring(0, idx).trim();
				}
				
				doc = damsClient.solrLookup("q=" + URLEncoder.encode("id:" + colId, "UTF-8"));
				values = getValues(doc, "//*[@name='visibility_tesim']/str");
				for(Iterator<String> it=values.iterator(); it.hasNext();)
					visibility += (visibility.length()>0?" ":"") + it.next();
				values = getValues(doc, "//*[@name='unit_code_tesim']/str");
				for(Iterator<String> it=values.iterator(); it.hasNext();)
					unit += (unit.length()>0?" ":"") + it.next();
				rowVal = getRow(damsClient, colTitle, colId, colType, unit, visibility, colMap);
				if(rowVal != null && rowVal.length() > 0)
					colRows.put(colTitle, rowVal + "\n");
			}
			
			for(Iterator<String> iter=colRows.values().iterator();iter.hasNext();){
				strBuf.append(iter.next());
			}
			
			STATUSREPORT = strBuf.toString();
		}finally{
			if(damsClient != null){
				damsClient.close();
				damsClient = null;
			}
		}
	}
	
	public String getLiteralValue(Document doc, String xPath){
		String val = "";
		List<Node> nodes = doc.selectNodes(xPath);
		for(Iterator<Node> it=nodes.iterator(); it.hasNext();){
			val += (val.length()>0?"--":"") + it.next().getText();
		}
		return val;
	}
	
	public static String getRow(DAMSClient damsClient, String colTitle, String colId, String colType, String unit, String visibility, Map<String, String> colMap) throws Exception{
		String rowVal = null;
		long itemsCount = damsClient.countObjects(colId);;
		
		String solrBase = "start=0&rows=1&";
		String[] views = {"discover_access_group_ssim:public", "discover_access_group_ssim:local", "discover_access_group_ssim:dams-manager-admin AND NOT(discover_access_group_ssim:public OR discover_access_group_ssim:local)"};
		String solrQuery = solrBase + "q=" + URLEncoder.encode("collections_tesim:" + colId + " OR collection_sim:\""+colTitle+ "\"", "UTF-8") + "&fq=" + URLEncoder.encode("has_model_ssim:\"info:fedora/afmodel:DamsObject\"", "UTF-8");
		Document doc = damsClient.solrLookup(solrQuery);
		String numFound = doc.selectSingleNode("/response/result/@numFound").getStringValue();
		long size = getDiskSize(colId);
		
		if(size == 0 || itemsCount != Integer.parseInt(numFound)){
			//Collection counted
			List<String> items = damsClient.listObjects(colId);
			int recordSize = items.size();
			String item = null;
			itemsCount = 0;
			for (int i=0; i<recordSize; i++){
				item = items.get(i);
				if(!colMap.containsValue(item)){
					itemsCount++;
				}
			}
		}
		rowVal = colTitle + "\t" + colType + "\t" + unit + "\t" + numFound + "\t" + itemsCount + "\t" + NUM_FORMATER.format(size/1000000.0) + "\t" + visibility;
		for(int j=0;j<views.length;j++){
			solrQuery = solrBase + "q=" + URLEncoder.encode("collections_tesim:" + colId + " OR collection_sim:\""+colTitle+ "\"", "UTF-8") + "&fq=" + URLEncoder.encode(views[j], "UTF-8");
			doc = damsClient.solrLookup(solrQuery);;
			numFound = doc.selectSingleNode("/response/result/@numFound").getStringValue();
			rowVal += "\t" + (numFound.equals("0")?" ":numFound);
		}
		//System.out.println("Embargo: " + tsUtils.getTripleStoreName() + " " + colName);
		//List<RightsAction> embargos = null;
		List<String> restrictedItems = null;
		List<String> sensitiveItems = null;
		
		restrictedItems = getRestrictedItems(damsClient, colId);
		sensitiveItems = getCulturallySensitiveItems(damsClient, colId);
		
		if(restrictedItems != null && restrictedItems.size() > 0){
			rowVal += "\t" + restrictedItems.size();
		}else
			rowVal += "\t ";
		
		if(sensitiveItems != null && sensitiveItems.size() > 0){
			rowVal += "\t" + sensitiveItems.size();
		}else
			rowVal += "\t ";
	
		rowVal += "\t ";			
		if(restrictedItems.size() > 0){
			String restricted = "";
			for(Iterator<String> it1=restrictedItems.iterator();it1.hasNext();){
				restricted += (restricted.length()>0?", ":"") + it1.next();
			}
			rowVal += "Restricted items: [" + restricted + "]";
		}
		
		if(sensitiveItems.size() > 0){
			String sensitive = "";
			for(Iterator<String> it1=sensitiveItems.iterator();it1.hasNext();){
				sensitive += (sensitive.length()>0?", ":"") + it1.next(); 
			}
			rowVal += " Culturally sensitive items: [" + sensitive + "]";
		}
		return rowVal;
	}
		
	public static List<String> getRestrictedItems(DAMSClient damsClient, String collectionId) throws Exception{
		String field = "id";
		String solrQuery = "fl=" + field + "&q=" + URLEncoder.encode("\"Display currently prohibited\"", "UTF-8") + "&qf=license_tesim&fq=" + URLEncoder.encode("collections_tesim:" + collectionId, "UTF-8");
		return getSOLRResults(damsClient, solrQuery, field);
	}
	
	public static List<String> getCulturallySensitiveItems(DAMSClient damsClient, String collectionId) throws Exception{
		String field = "id";
		String solrQuery = "fl=" + field + "&q=" + URLEncoder.encode("\"cultural sensitivity\"", "UTF-8") + "&qf=otherRights_tesim&fq=" + URLEncoder.encode("collections_tesim:" + collectionId, "UTF-8");
		return getSOLRResults(damsClient, solrQuery, field);
	}
	
	public static List<String> getSOLRResults(DAMSClient damsClient, String solrQuery, String field) throws Exception{
		List<String> results = new ArrayList<String>();	
		String xPath = "//*[@name='" + field + "']";
		
		int numFound = 0;
		int rows = 100;
		Document doc = null;
		if(solrQuery.indexOf("wt=xml") < 0)
			solrQuery += "&wt=xml";
		if(solrQuery.indexOf("rows=") < 0){
			doc = damsClient.solrLookup(solrQuery + "&rows=" + rows);
		}
			
		numFound = Integer.parseInt(doc.selectSingleNode("/response/result/@numFound").getStringValue());			
		if(numFound > 0)	
			results.addAll(getValues(doc, xPath));
		
	    if(solrQuery.indexOf("rows=") < 0){
	    	solrQuery += "&rows=" + rows;
			int idx = rows;
			while (idx < numFound){
				doc = damsClient.solrLookup(solrQuery + "&start=" + idx);
				results.addAll(getValues(doc, xPath));
				idx += rows;
			}
	    }
	    return results;
	}
	
	public static List<String> getValues(Document doc, String xPath){
		List<String> values = new ArrayList<String>();
		List<Node> nodes = doc.selectNodes(xPath);
		for(Iterator<Node> it=nodes.iterator(); it.hasNext();)
			values.add(it.next().getText());
		return values;
	}
	
	public static long getDiskSize(String collectionId) throws SQLException{
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet rs = null;
		long size = 0;
		try{
			con = Constants.DAMS_DATA_SOURCE.getConnection();
			ps = con.prepareStatement(StatsUsage.DLP_COLLECTION_RECORD_QUERY);
			ps.setString(1, collectionId);
			rs = ps.executeQuery();
			if(rs.next()){
				size = rs.getLong("SIZE_BYTES");
			}
		}finally{
			Statistics.close(rs);
			Statistics.close(ps);
			Statistics.close(con);
			rs = null;
			ps = null;
			con = null;
		}
		return size;
	}
}
