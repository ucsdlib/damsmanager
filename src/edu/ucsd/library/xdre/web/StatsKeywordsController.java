package edu.ucsd.library.xdre.web;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.sql.Connection;
import java.sql.SQLException;
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

import org.json.simple.JSONObject;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.statistic.analyzer.Statistics;
import edu.ucsd.library.xdre.statistic.beans.DAMSKeywordsSummary;
import edu.ucsd.library.xdre.statistic.beans.StatSummary;
import edu.ucsd.library.xdre.statistic.report.DAMSKeywordsUsage;
import edu.ucsd.library.xdre.utils.Constants;


 /**
 * Class StatsKeywordsController handles the statics for keyword/phrase
 *
 * @author lsitu@ucsd.edu
 */
public class StatsKeywordsController implements Controller {
	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		
		Map<String, Object> model = new HashMap<String, Object>();
		
		String startDate = request.getParameter("start");
		String endDate = request.getParameter("end");
		String page = request.getParameter("page");
		String searchKeyword = request.getParameter("inputSearch");
		String type = request.getParameter("type");
		String message = "";
		Connection con = null;
		Calendar sCal = Calendar.getInstance();
		Calendar eCal = Calendar.getInstance();
		Date sDate = null;
		Date eDate = null;
		SimpleDateFormat dFormat = null;
		String templete = "statsKeywords";
		
		boolean isCas = false;
		String apps2sum = "pas";
		if(request.isUserInRole(Constants.CURATOR_ROLE)){
			isCas = true;
			apps2sum = "dlp";
		}
		
		boolean moreKeywords = false;
		boolean morePhrases = false;
		List<StatSummary> keywordSums = null;
		int pageNum = 1;
		if(page != null){
			pageNum =Integer.parseInt(page.trim());
		}

		dFormat = new SimpleDateFormat(Statistics.DATE_FORMAT.replace("-", ""));
		//String query = StatsUsage.DLC_KEYWORDS_QUERY;
		if(searchKeyword != null && (searchKeyword=searchKeyword.trim()).length() > 0){
			//query = StatsUsage.DLC_KEYWORDS_LIKE_QUERY;
			searchKeyword = URLDecoder.decode(searchKeyword, "UTF-8");
		}
		
		try {
			con = Constants.DAMS_DATA_SOURCE.getConnection();
			if(startDate != null && (startDate=startDate.trim()).length() > 0){
				sDate = dFormat.parse(startDate);
				sCal.setTime(sDate);
			}else{
				//Default from year 2009.
				sCal.set(Calendar.YEAR, 2009);
				sCal.set(Calendar.MONTH, 1);
				sCal.set(Calendar.DATE, 1);
			}
			
			if(endDate != null && (endDate=endDate.trim()).length() > 0){
				eDate = dFormat.parse(endDate);
				eCal.setTime(eDate);
			}
			
			DAMSKeywordsUsage keywordsUsage = null;
			if(type == null || type.equals("keyword")){
				keywordsUsage = new DAMSKeywordsUsage(apps2sum, sCal.getTime(), eCal.getTime(), con, "keyword", pageNum);
				keywordsUsage.setKeyword(searchKeyword);
				keywordSums = (List<StatSummary>)keywordsUsage.getStatSummary();
				if(keywordSums.size() > DAMSKeywordsUsage.KEYWORDS_PER_PAGE){
					moreKeywords = true;
					while(keywordSums.size() > DAMSKeywordsUsage.KEYWORDS_PER_PAGE)
						keywordSums.remove(DAMSKeywordsUsage.KEYWORDS_PER_PAGE);
				}
				model.put("keywords", keywordSums);
				model.put("keywordsTotal", keywordsUsage.getTotal());
				model.put("moreKeywords", moreKeywords);
			}
			if(type == null || type.equals("phrase")){
				keywordsUsage = new DAMSKeywordsUsage(apps2sum, sCal.getTime(), eCal.getTime(), con, "phrase", pageNum);
				keywordsUsage.setKeyword(searchKeyword);
				keywordSums = (List<StatSummary>)keywordsUsage.getStatSummary();
				if(keywordSums.size() > DAMSKeywordsUsage.KEYWORDS_PER_PAGE){
					morePhrases = true;
					while(keywordSums.size() > DAMSKeywordsUsage.KEYWORDS_PER_PAGE)
						keywordSums.remove(DAMSKeywordsUsage.KEYWORDS_PER_PAGE);
				}
				model.put("phrases", keywordSums);
				model.put("phrasesTotal", keywordsUsage.getTotal());
				model.put("morePhrases", morePhrases);
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

		if(startDate != null && startDate.length() > 0)
			model.put("startDate", dFormat.format(sCal.getTime()));
		if(endDate != null && endDate.length() > 0)
			model.put("endDate", dFormat.format(eCal.getTime()));
		model.put("message", message);
		model.put("page", pageNum);
		model.put("searchKeyword", searchKeyword);
		model.put("isCas", isCas);

		if(type == null)
			return new ModelAndView(templete, "model", model);
		else{
			JSONObject jsonObj = new JSONObject();
			if(startDate != null && startDate.length() > 0)
				jsonObj.put("startDate", model.get("startDate"));
			if(endDate != null && endDate.length() > 0)
				jsonObj.put("endDate", model.get("endDate"));
			jsonObj.put("page", model.get("page"));
			jsonObj.put("search", model.get("search"));

			keywordSums = (List<StatSummary>)model.get("keywords");
			if(keywordSums != null){
				jsonObj.put("keywords", toJson(keywordSums));
				jsonObj.put("moreKeywords", model.get("moreKeywords"));
				jsonObj.put("keywordsTotal", model.get("keywordsTotal"));
			}
			keywordSums = (List<StatSummary>)model.get("phrases");
			if(keywordSums != null){
				jsonObj.put("phrases", toJson(keywordSums));
				jsonObj.put("morePhrases", model.get("morePhrases"));
				jsonObj.put("phrasesTotal", model.get("phrasesTotal"));
			}
			response.setContentType("text/plain;charset=UTF-8");
			OutputStream out = response.getOutputStream();
			out.write(jsonObj.toString().getBytes());
			return null;
		}
    }
	
	public static JSONObject toJson(Collection<StatSummary> keywordSums){
		JSONObject keywords = new JSONObject();
		DAMSKeywordsSummary keywordSum = null;
		for(Iterator it=keywordSums.iterator();it.hasNext();){
			keywordSum = (DAMSKeywordsSummary)it.next();
			keywords.put(keywordSum.getKeyword(), keywordSum.getNumOfUsage());
		}
		return keywords;
	}
}