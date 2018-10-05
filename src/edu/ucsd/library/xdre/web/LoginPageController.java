package edu.ucsd.library.xdre.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.utils.Constants;


 /**
 * Class LoginPageController, the controller for login page
 *
 * @author lsitu@ucsd.edu
 */
public class LoginPageController implements Controller {

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> dataMap = new HashMap<>();

        String environment = Constants.CLUSTER_HOST_NAME.equals("library") ? "Production" : Constants.CLUSTER_HOST_NAME.contains("test")
                ? "Staging" : Constants.CLUSTER_HOST_NAME.contains("qa") ? "QA" : Constants.CLUSTER_HOST_NAME;
        dataMap.put("environment", environment);
        dataMap.put("clusterHostName", Constants.CLUSTER_HOST_NAME);
        
        return new ModelAndView("loginPage", "model", dataMap);
    }
 }
