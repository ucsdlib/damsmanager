package edu.ucsd.library.xdre.web;

import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.utils.Constants;


 /**
 * Class AboutController, the controller for index page
 *
 * @author lsitu@ucsd.edu
 */
public class AboutController implements Controller {

    public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
        Map<String, Object> dataMap = new HashMap<>();

        dataMap.put("clusterHostName", Constants.CLUSTER_HOST_NAME);
        
        return new ModelAndView("about", "model", dataMap);
    }
 }