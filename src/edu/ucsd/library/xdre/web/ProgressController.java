package edu.ucsd.library.xdre.web;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.dom4j.Element;
import org.omg.PortableServer.POAManagerPackage.State;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.utils.DomUtil;
import edu.ucsd.library.xdre.utils.RequestOrganizer;
import edu.ucsd.library.xdre.utils.Submission;
import edu.ucsd.library.xdre.utils.Submission.Status;


 /**
 * Class StatusController handles assignments the status of a request
 *
 * @author lsitu@ucsd.edu
 */
public class ProgressController implements Controller {
	private Logger log = Logger.getLogger(ProgressController.class);
	
	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		HttpSession session = request.getSession();
		PrintWriter pw = response.getWriter();
		response.setContentType("text/xml");
		String progress = request.getParameter("progress");
		String formId = null;
		
		if (progress != null) {
			Element elem = DomUtil.createElement(null, "root", null, null);
			String status = (String) session.getAttribute("status");
			Thread servlet = RequestOrganizer.getReferenceServlet(session);
			if(progress.equals("0")){
              //New request
				formId = request.getParameter("formId");
				String progressId = request.getParameter("sid");
				DomUtil.createElement(elem, "progressId", null, progressId);
				DomUtil.createElement(elem, "formId", null, formId);
				if(servlet == null)
					status = "Done";
				else if(status == null || (status=status.trim()).length() == 0)
					status = "Processing request ...";
			}else{
				
				if(servlet == null || !servlet.isAlive()){
					status = "Done";
		        }else{
		    	   String result = RequestOrganizer.getResultMessage(session);
		    	   result = (result==null?"":result);
		    	   String errorLog = RequestOrganizer.getLogMessage(session);
		    	   errorLog = (errorLog==null?"":errorLog);
		    	   DomUtil.createElement(elem, "result", null,result);
		    	   DomUtil.createElement(elem, "log", null, errorLog);
		    	   DomUtil.createElement(elem, RequestOrganizer.PROGRESS_PERCENTAGE_ATTRIBUTE, null, RequestOrganizer.getProgressPercentage(session));
		       }
			}
			if(status == null)
				status = "";
			
			DomUtil.createElement(elem, "status", null, status);
	    	//System.out.println("Status --> " + elem.asXML());
			pw.write(elem.asXML());
			response.setStatus(200);
		}else if (request.getParameter("canceled") != null) {

			boolean interrupted = false;
			//String servletId = request.getParameter("progressId");
			Thread servlet = RequestOrganizer.getReferenceServlet(session);
			if(servlet != null && servlet.getName().endsWith(session.getId())){
				int count = 0;
				do{
			      servlet.interrupt();
			      interrupted = true;
                  try {
				     Thread.sleep(1000);
			       } catch (InterruptedException e) {
			    	   e.printStackTrace();
			       }
			       servlet = RequestOrganizer.getReferenceServlet(session);
			       if(servlet != null && servlet.getName().endsWith(session.getId()))
			    	   interrupted = false;
				}while(!interrupted && count++ < 5);
			}else{
				interrupted = true;
			}
			if(interrupted){
			     pw.write("<status>Canceled</status>");
			     response.setStatus(200);	
		    }else{
		    	 pw.write("<status>No Response</status>");
			     response.setStatus(200);
		    }
		 } else if (request.getParameter("kill") != null) {

				boolean interrupted = false;
				String processKey = request.getParameter("progress");
				String message = "";
				String status = "";
				Submission submission = RequestOrganizer.getSubmissionByKey(processKey);
				if(submission != null){
					SimpleDateFormat formater = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
					Thread worker = submission.getWorker();
					if(worker != null){
						int count = 0;
						do{
							worker.interrupt();
							try{
								Thread.sleep(1000);
							}catch (InterruptedException ie){}
						} while(count++ < 10 && worker.getState().equals(State.ACTIVE));
						if(worker.getState().equals(State.ACTIVE)){
							//Can't interrupted immediately
							message = "Unable to interrupt submission " + submission.getRequestInfo() + "(submitted by " + submission.getOwner() + " at +" + formater.format(submission.getStartDate()) + "). Please try again later.";
							log.info(message);
						}else{
							interrupted = true;
							message = "Request submitted by " + submission.getOwner() + " at +" + formater.format(submission.getStartDate()) + " was suspended.";
							submission.setStatus(Status.interrupted);
						}
						submission.setEndDate(Calendar.getInstance().getTime());
						submission.setMessage("Request interrupted by " + request.getRemoteUser() + " from " + request.getRemoteAddr() + ".");
					}else
						message = "Error: Submission " + processKey + " was ended.";
				}else
					message = "Error: Submission " + processKey + " doesn't exists.";

				if(interrupted)
					status = "Canceled";	
			    else
			    	status = "Error";
				
				Element elem = DomUtil.createElement(null, "result", null, null);
		    	DomUtil.createElement(elem, "status", null, status);
		    	DomUtil.createElement(elem, "message", null, message);
				response.setStatus(200);
			 }
			return null;
    	}  	
}