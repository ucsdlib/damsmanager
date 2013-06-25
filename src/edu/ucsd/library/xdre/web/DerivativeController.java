package edu.ucsd.library.xdre.web;

import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.simple.JSONValue;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.collection.DerivativeHandler;
import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DomUtil;

/**
 * Servlet to create derivative for an item or batch of items.
 * @author lsitu
 *
 */
public class DerivativeController implements Controller{

	@Override
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		DAMSClient damsClient = null;
		DerivativeHandler handler = null;
		boolean replace = (request.getParameter("replace")!=null || request.getParameter("derReplace")!=null);
		 
		String reqSize = request.getParameter("size");
		String[] subjects = request.getParameterValues("subject");
		String frameNo = request.getParameter("frame");
		boolean successful = false;
		String message = "";
		int status = 200;
		if(subjects == null){
			message = "Subject required.";
		}else{
			if(subjects[0].indexOf(',') > 0)
				subjects = subjects[0].trim().split(",");
		}
		String[] sizes = null;
		if(reqSize != null && reqSize.length() > 0)
			sizes = reqSize.split(",");
		
		try{
			damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
			handler = new DerivativeHandler(damsClient, null, sizes, replace);
			List<String> items = Arrays.asList(subjects);
			handler.setItems(items);
			handler.setFrameNo(frameNo);
			successful = handler.execute();
			if(successful)
				message = handler.getExeInfo();
			else{
				status = 500;
				message = "Failed to create derivatives.";
			}
		}catch (Exception e){
			status = 500;
			message = "Error: " + e.getMessage();
		}finally{
			if(handler != null)
				handler.release();
			if(damsClient != null)
				damsClient.close();
			response.setContentType("text/xml");
			
			Map <String, String> result = new HashMap<String, String>();

			result.put("statusCode", String.valueOf(status));
			result.put("status", successful?"Succeeded":"Failed");
			result.put("message", message.replace("\n", ""));
			String content = DomUtil.toXml("result", result);
			OutputStream out = response.getOutputStream();
			out.write(content.getBytes("UTF-8"));
			out.flush();
			out.close();
		}
		return null;
	}
}
