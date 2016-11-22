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
import org.dom4j.Node;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.statistic.analyzer.Statistics;
import edu.ucsd.library.xdre.statistic.beans.RdcpStatsDownload;
import edu.ucsd.library.xdre.statistic.beans.RdcpStatsDownloadSummary;
import edu.ucsd.library.xdre.statistic.report.StatsUsage;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;


 /**
 * Class StatsRdcpDownloadController to summarize RDCP file download for rdcp statistics
 *
 * @author lsitu@ucsd.edu
 */
public class StatsRdcpDownloadController implements Controller {
	public static final SimpleDateFormat DB_FORMAT = new SimpleDateFormat(Statistics.DATE_FORMAT);
	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, DocumentException {
		
		Map<String, Object> model = new HashMap<String, Object>();
		
		String startDate = request.getParameter("start");
		String export = request.getParameter("export");
		String message = "";
		Connection con = null;
		Calendar sCal = Calendar.getInstance();
		Calendar eCal = Calendar.getInstance();
		Date sDate = null;
		StringBuilder strBuf = null;
		SimpleDateFormat dbFormat = null;

		DAMSClient damsClient = null;
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
			List<RdcpStatsDownloadSummary> statsDownloadSums = getRdcpStats(con, sCal.getTime(), eCal.getTime(), false, damsClient);
	
			List<RdcpStatsDownloadSummary> curatorStatsDownloadSums = getRdcpStats(con, sCal.getTime(), eCal.getTime(), true, damsClient);
			// merge the non-curator ans curator stats 
			mergeRdcpStats(statsDownloadSums, curatorStatsDownloadSums);

			if (export == null) {
				if (statsDownloadSums.size() > 0) {
					model.put("periodsList", statsDownloadSums.get(0).getPeriods());
					model.put("data", statsDownloadSums);
				} else {
					model.put("periodsList", new ArrayList<String>());
					model.put("data", new ArrayList<RdcpStatsDownloadSummary>());
				}
			} else {
				int count = 0;
				strBuf = new StringBuilder();
				for(RdcpStatsDownloadSummary statsItemSum : statsDownloadSums) {
					if (count == 0) {
						strBuf.append("RDCP File Download Statistics By Month\n");
						strBuf.append("Collection,Object title,Component title,ARK");
						for (String period : statsItemSum.getPeriods()) {
							strBuf.append("," + Statistics.escapeCsv(period + "(public)"));
							strBuf.append("," + Statistics.escapeCsv(period + "(curator)"));
						}
						strBuf.append("\n");
					}
					List<Integer> stats = statsItemSum.getNumOfViews();
					strBuf.append(
							Statistics.escapeCsv(statsItemSum.getCollectionTitle()) + "," + 
							Statistics.escapeCsv(statsItemSum.getTitle()) + "," +
							Statistics.escapeCsv(statsItemSum.getComponentTitle()) + "," +
							Statistics.escapeCsv(statsItemSum.getSubjectId())
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

		if(export == null){
			model.put("message", message);
			model.put("start", dbFormat.format(sCal.getTime()));
			return new ModelAndView("statsRdcpDownload", "model", model);
		}else{
			if(message != null && message.length() > 0)
				strBuf.append(message);

			OutputStream out = response.getOutputStream();
			response.setHeader("Content-Disposition", "inline; filename=rdcp_downloads.csv");
			response.setContentType("text/csv");
			out.write(strBuf.toString().getBytes());
			out.close();
			return null;
		}
	}

	private static List<RdcpStatsDownloadSummary> getRdcpStats(Connection con, Date sDate, Date eDate, boolean isPrivate, DAMSClient damsClient) throws SQLException {
		//RDCP unique items usage
		List<RdcpStatsDownloadSummary> statsDownloadSums = new ArrayList<>();

		String period = null;
		String sid = null;
		String cid = null;
		String fid = null;
		int numOfViews = 0;
		List<RdcpStatsDownload> statsItems = null;
		List<String> periods = new ArrayList<>();
		List<String> filePaths = new ArrayList<>();
		Map<String, List<RdcpStatsDownload>> statsItemsMap = new HashMap<>();
		PreparedStatement ps = null;
		ResultSet rs = null;
		try{
			ps = con.prepareStatement(StatsUsage.RDCP_FILE_DOWNLOAD_POPULARITY_QUERY.replace("PERIOD_PARAM", StatsUsage.MONTHLY_FORMAT));
			
			ps.setBoolean(1, isPrivate);
			ps.setString(2, DB_FORMAT.format(sDate));
			ps.setString(3, DB_FORMAT.format(eDate));
			rs = ps.executeQuery();
			while(rs.next()) {
				period = rs.getString("period");
				sid = rs.getString("object_id");
				cid = rs.getString("comp_id");
				numOfViews = rs.getInt("num_view");

				String filePath = sid + "/" + (StringUtils.isBlank(cid) || cid.equals("0") ? "" : cid) + "/" + fid;
				if(!filePaths.contains(filePath))
					filePaths.add(filePath);
				if(!periods.contains(period))
					periods.add(period);

				statsItems = statsItemsMap.get(filePath);
				if(statsItems == null) {
					statsItems = new ArrayList<>();
					statsItemsMap.put(filePath, statsItems);
				}
				statsItems.add (new RdcpStatsDownload( period, sid, cid, fid, numOfViews ));
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
		for (String filePath : filePaths) {
			Integer[] stats = new Integer[periods.size()];
			for (int j=0; j < stats.length; j++)
				stats[j] = new Integer(0);

			statsItems = statsItemsMap.get(filePath);
			for(RdcpStatsDownload statsItem : statsItems) {
				int idx = periods.indexOf(statsItem.getPeriod());
				if (idx >= 0)
					stats[idx] = statsItem.getNumOfViews();
			}

			String colTitle = "";
			String objTitle = "";
			String compTitle = "";
			String[] paths = filePath.split("\\/");
			try {
				sid = paths[0];
				Document doc = Statistics.cacheGet(sid);
				if (doc == null) {
					doc = Statistics.getRecordForStats(sid);
					Statistics.cacheAdd(sid, doc);
				}
				colTitle = Statistics.getTextValue (doc, "//doc/arr[@name='collection_name_tesim']/str");
				objTitle = Statistics.getTitleFromJson(Statistics.getTextValue (doc, "//doc/arr[@name='title_json_tesim']/str"));
				if (StringUtils.isNotBlank(paths[1])) {
					String titleName = "component_" + paths[1] + "_title_json_tesim";
					compTitle = Statistics.getTitleFromJson(Statistics.getTextValue (doc, "//doc/arr[@name='" + titleName + "']/str"));
				}
				RdcpStatsDownloadSummary statsItemSum = new RdcpStatsDownloadSummary (colTitle, objTitle, compTitle, 
						paths[0], paths[1], paths[2], periods, Arrays.asList(stats));
				statsDownloadSums.add(statsItemSum); 
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return statsDownloadSums;
	}

	private static void mergeRdcpStats(List<RdcpStatsDownloadSummary> stats, List<RdcpStatsDownloadSummary> statsToBeMerged) {
		Map<String, RdcpStatsDownloadSummary> toBeMergedMap = new HashMap<>();
		for (RdcpStatsDownloadSummary toBeMerged :  statsToBeMerged) {
			toBeMergedMap.put(toBeMerged.getSubjectId(), toBeMerged);
		}

		// merged the non-curator and curator stats for displaying
		for (RdcpStatsDownloadSummary s : stats) {
			String subjectId = s.getSubjectId();
			List<String> periods = s.getPeriods();
			List<Integer> numOfViews = s.getNumOfViews();
			RdcpStatsDownloadSummary toBeMerged = toBeMergedMap.get(subjectId);
			List<String> periodsToBeMerged = toBeMerged != null ? toBeMerged.getPeriods() : new ArrayList<String>();
			List<Integer> numOfViewsToBeMerged = toBeMerged != null ? toBeMerged.getNumOfViews() : new ArrayList<Integer>();

			// stats result merged
			List<Integer> numOfViewsMerged = new ArrayList<>();
			s.setNumOfViews(numOfViewsMerged);
			for (int i=0 ; i< periods.size(); i++) {
				int numOfView = 0;
				if (toBeMerged != null) {
					int periodIndex = periodsToBeMerged.indexOf(periods.get(i));
					if (periodIndex >= 0 && numOfViewsToBeMerged.size() > periodIndex)
						numOfView = numOfViewsToBeMerged.get(periodIndex);
				}
				numOfViewsMerged.add(numOfViews.get(i));
				numOfViewsMerged.add(numOfView);
			}
		}
	}
}
