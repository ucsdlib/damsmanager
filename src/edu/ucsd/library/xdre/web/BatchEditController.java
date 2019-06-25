package edu.ucsd.library.xdre.web;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.commons.lang3.StringUtils;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.tab.BatchEditExcelSource;


 /**
 * Class BatchEditController, the model for the BatchEdit view
 *
 * @author lsitu@ucsd.edu
 */
public class BatchEditController implements Controller {

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        String message = request.getParameter("message");

        // initiate column name and control values for validation
        String validateTemplate = request.getServletContext().getRealPath("files/xls_standard_input_template.xlsx");
        try {
            BatchEditExcelSource.initControlValues(new File( validateTemplate), true);
        } catch (Exception e) {
            e.printStackTrace();
        }

        HttpSession session = request.getSession();	
        Map dataMap = new HashMap();

        message = !StringUtils.isBlank(message) ? message : (String)session.getAttribute("message");
        session.removeAttribute("message");

        dataMap.put("message", message);

        return new ModelAndView("batchEdit", "model", dataMap);
    }
}
