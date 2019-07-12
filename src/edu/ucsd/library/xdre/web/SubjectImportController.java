package edu.ucsd.library.xdre.web;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONArray;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.model.DAMSCollection;
import edu.ucsd.library.xdre.tab.ExcelSource;
import edu.ucsd.library.xdre.tab.RecordUtil;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;


 /**
 * Class SubjectImportController, the model for the SubjectImport view
 *
 * @author lsitu@ucsd.edu
 */
public class SubjectImportController implements Controller {

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String message = request.getParameter("message");

		HttpSession session = request.getSession();	
		Map dataMap = new HashMap();

		// initiate column name and control values for validation
		String validateTemplate = request.getServletContext().getRealPath("files/xls_standard_input_template.xlsx");
		try {
			ExcelSource.initControlValues(new File( validateTemplate));
		} catch (Exception e) {
			e.printStackTrace();
		}

		message = !StringUtils.isBlank(message) ? message : (String)session.getAttribute("message");
		session.removeAttribute("message");

		dataMap.put("message", message);
		
		return new ModelAndView("subjectImport", "model", dataMap);
	}
 }
