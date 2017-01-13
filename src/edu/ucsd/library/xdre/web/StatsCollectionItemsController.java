package edu.ucsd.library.xdre.web;

import java.io.IOException;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.statistic.analyzer.Statistics;
import edu.ucsd.library.xdre.statistic.beans.StatsCollectionItem;
import edu.ucsd.library.xdre.statistic.beans.StatsCollectionItemSummary;
import edu.ucsd.library.xdre.statistic.report.StatsUsage;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;


 /**
 * Class StatsCollectionItemsController to summarize object usage and file downloads in collection
 *
 * @author lsitu@ucsd.edu
 */
public class StatsCollectionItemsController implements Controller {
	public static final SimpleDateFormat DB_FORMAT = new SimpleDateFormat(Statistics.DATE_FORMAT);
	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DocumentException {
		
		Map<String, Object> model = new HashMap<String, Object>();
		
		String startDate = request.getParameter("start");
		String export = request.getParameter("export");
		String type = request.getParameter("type");       // stats type: hits or downloads
		String message = "";
		Connection con = null;
		Calendar sCal = Calendar.getInstance();
		Calendar eCal = Calendar.getInstance();
		Date sDate = null;
		StringBuilder strBuf = null;
		SimpleDateFormat dbFormat = null;

		DAMSClient damsClient = null;

		String statsTitle = "Object/Collection Hits by Month";
		if (StringUtils.isNotBlank(type) && type.equalsIgnoreCase("downloads"))
			statsTitle = "Object/Collection Downloads by Month";

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

			con = Constants.DAMS_DATA_SOURCE.getConnection();

			damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
			List<StatsCollectionItemSummary> statsItemSums = getStats(con, type, sCal.getTime(), eCal.getTime(), false, damsClient);

			if (export == null) {
				if (statsItemSums.size() > 0) {
					model.put("periodsList", statsItemSums.get(0).getPeriods());
					model.put("data", statsItemSums);
				} else {
					model.put("periodsList", new ArrayList<String>());
					model.put("data", new ArrayList<StatsCollectionItemSummary>());
				}
			} else {
				int count = 0;
				strBuf = new StringBuilder();
				for(StatsCollectionItemSummary statsItemSum : statsItemSums) {
					if (count == 0) {
						strBuf.append(statsTitle + "\n");
						strBuf.append("Collection,ARK");
						for (String period : statsItemSum.getPeriods()) {
							strBuf.append("," + Statistics.escapeCsv(period));
						}
						strBuf.append("\n");
					}
					List<Integer> stats = statsItemSum.getNumOfViews();
					strBuf.append(
							Statistics.escapeCsv(statsItemSum.getCollectionTitle()) + "," + 
							Statistics.escapeCsv(statsItemSum.getCollectionId())
							); 
					for (Integer stat : stats) {
						strBuf.append("," + stat );
					}
					strBuf.append( "\n" );
					count++;
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			message += "InternalError: " + e.getMessage();
		} catch (Exception e) {
			e.printStackTrace();
			message += "Error: " + e.getMessage();
		}finally{
			if (damsClient != null) {
				damsClient.close();
			}
			Statistics.close(con);
			con = null;
		}

		if(export == null) {
			model.put("message", message);
			model.put("statsTitle", statsTitle);
			model.put("type", (StringUtils.isBlank(type)?"hits":type));
			model.put("start", dbFormat.format(sCal.getTime()));

			return new ModelAndView("statsCollectionObjects", "model", model);
		}else{
			if(message != null && message.length() > 0)
				strBuf.append(message);

			String statsFileName = "collection_hits.csv";
			if (StringUtils.isNotBlank(type) && type.equalsIgnoreCase("downloads"))
				statsFileName = "collection_downloads.csv";
			OutputStream out = response.getOutputStream();
			response.setHeader("Content-Disposition", "inline; filename=" + statsFileName);
			response.setContentType("text/csv");
			out.write(strBuf.toString().getBytes());
			out.close();
			return null;
		}
	}

	private static List<StatsCollectionItemSummary> getStats(Connection con, String type, Date sDate, Date eDate, boolean isPrivate, DAMSClient damsClient) throws SQLException {

		List<StatsCollectionItemSummary> statsItemSums = new ArrayList<>();
		String query = StatsUsage.COLLECTION_OBJECT_POPULARITY_QUERY;
		if (StringUtils.isNotBlank(type) && type.equalsIgnoreCase("downloads"))
			query = StatsUsage.COLLECTION_FILE_DOWNLOAD_POPULARITY_QUERY;

		String period = null;
		String colid = null;
		int numOfViews = 0;
		List<StatsCollectionItem> statsItems = null;
		List<String> periods = new ArrayList<>();
		Map<String, List<StatsCollectionItem>> statsItemsMap = new HashMap<>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try{
			ps = con.prepareStatement(query.replace("PERIOD_PARAM", StatsUsage.MONTHLY_FORMAT));

			ps.setBoolean(1, isPrivate);
			ps.setString(2, DB_FORMAT.format(sDate));
			ps.setString(3, DB_FORMAT.format(eDate));
			rs = ps.executeQuery();
			while(rs.next()) {
				period = rs.getString("period");
				colid = rs.getString("col_id");
				numOfViews = rs.getInt("num_view");

				if(!periods.contains(period))
					periods.add(period);

				statsItems = statsItemsMap.get(colid);
				if(statsItems == null) {
					statsItems = new ArrayList<>();
					statsItemsMap.put(colid, statsItems);
				}
				statsItems.add (new StatsCollectionItem( colid, period, numOfViews ));
			}
		}finally{
			if(rs != null){
				try{
					rs.close();
					rs = null;
				}catch(SQLException e){}
			}
			if(ps != null){
				try{
					ps.close();
					ps = null;
				}catch(SQLException e){}
			}
		}

		// order the stats for each month
		Collections.sort(periods);
		for (String collectionId : statsItemsMap.keySet()) {
			Integer[] stats = new Integer[periods.size()];
			for (int j=0; j < stats.length; j++)
				stats[j] = new Integer(0);

			statsItems = statsItemsMap.get(collectionId);
			for(StatsCollectionItem statsItem : statsItems) {
				int idx = periods.indexOf(statsItem.getPeriod());
				if (idx >= 0)
					stats[idx] = statsItem.getNumOfViews();
			}

			String colTitle = "";
			try {
				Document doc = Statistics.cacheGet(collectionId);
				if (doc == null) {
					doc = Statistics.getRecordForStats(collectionId);
					Statistics.cacheAdd(collectionId, doc);
				}

				colTitle = Statistics.getTitleFromJson(Statistics.getTextValue (doc, "//doc/arr[@name='title_json_tesim']/str"));
				if (StringUtils.isNotBlank(colTitle)) {
					StatsCollectionItemSummary statsItemSum = new StatsCollectionItemSummary (colTitle, collectionId, periods, Arrays.asList(stats));
					statsItemSums.add(statsItemSum); 
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		Collections.sort(statsItemSums);
		return statsItemSums;
	}
}
