package edu.ucsd.library.xdre.utils;

import java.util.Calendar;
import java.util.Map;
import java.util.TreeMap;

import javax.servlet.http.HttpSession;

import edu.ucsd.library.xdre.utils.Submission.Status;

/**
 * Class RequestOrganizer, a utility class for operation request
 *
 * @author lsitu@ucsd.edu
 */
public class RequestOrganizer {
	public static final int MAX_TASKS = 500;
	public static final String PROGRESS_PERCENTAGE_ATTRIBUTE = "progressPercentage";
	private static Map<String, Submission> TaskMap = new TreeMap<String, Submission>();
	
	public static Thread getReferenceServlet(HttpSession session){
		Thread worker = null;
		Submission submission = getSubmission(session);
		if(submission != null)
			worker = submission.getWorker();
		return worker;
	}
	
	public static Submission getSubmission(HttpSession session){
		synchronized(session){
			return (Submission) TaskMap.get(session.getId());
		}
	}
	
	public synchronized static Submission getSubmissionByKey(String processKey){
			return (Submission)TaskMap.get(processKey);
	}
	
	public static boolean setReferenceServlet(HttpSession session, String processId, Thread servlet) throws Exception{
		Thread currServlet = null;
		String sessionId = session.getId();
		String newServletName = processId + "_" + sessionId;
		synchronized(session){
			Submission submission = null;
			if(servlet != null){				
				submission = getSubmission(session);
				if(submission != null)
					currServlet = submission.getWorker();
				if(currServlet != null && currServlet.isAlive()){
					String servletName = currServlet.getName();
					
					int sessionIdx = servletName.indexOf(sessionId);
					if(servletName.equals(newServletName))
						return false;
					else if(sessionIdx > 0 && !currServlet.isInterrupted())
						throw new Exception("WARNING: Another process is running. Servlet Request aborted ....");
				}
				servlet.setName(newServletName);
			}
			if(servlet == null){
				if(TaskMap.size() > MAX_TASKS){
					String[] keys = (String[]) TaskMap.keySet().toArray();
					TaskMap.remove(keys[0]);
				}
				TaskMap.put(System.currentTimeMillis() + "_" + sessionId, submission);
				TaskMap.put(sessionId, null);
			}else{
				submission = new Submission(servlet, processId, null);
				submission.setSubmittedDate(Calendar.getInstance().getTime());
				submission.setStatus(Status.progressing);
				TaskMap.put(sessionId, submission);
			}
		}
		return true;
	}
	
	public static void clearSession(HttpSession session){
		Thread servlet = null;
		synchronized (session){
			Submission submission = getSubmission(session);
			if(submission != null)
				servlet = submission.getWorker();
			if(servlet != null){
				try {
					setReferenceServlet(session, "", null);
				} catch (Exception e) {}
			}
			try{
				if(session.getAttribute("status") != null)
			        session.removeAttribute("status");
			    if(session.getAttribute("result") != null)
			    	session.removeAttribute("result");
			    if(session.getAttribute("log") != null)
			    	session.removeAttribute("log");
			}catch(IllegalStateException e){
				//e.printStackTrace();
			}
		}
	}
	
	public static boolean addLogMessage (HttpSession session, String message){
		String sessionMessage = "";
		synchronized(session){
			try{
				sessionMessage = (String) session.getAttribute("log");
				sessionMessage = sessionMessage==null?"":sessionMessage;
	            session.setAttribute("log", sessionMessage + message);
		        return true;
			}catch(IllegalStateException e){
				//e.printStackTrace();
			}
		}
		return false;
	  }

	public static String getLogMessage (HttpSession session){
		String sessionMessage = "";
		synchronized(session){
			try{
				sessionMessage = (String) session.getAttribute("log");
	            session.setAttribute("log", "");
			}catch(IllegalStateException e){
				//e.printStackTrace();
			}
		}
		return sessionMessage;
	 }
	
	public static boolean addResultMessage (HttpSession session, String message){
		String sessionMessage = "";
		synchronized(session){
			try{
				sessionMessage = (String) session.getAttribute("result");
				if(sessionMessage != null && sessionMessage.length() > 0)
					sessionMessage += "\n" + message;
				session.setAttribute("result", message);
				Submission submission = getSubmission(session);
				if(submission != null){
					submission.setStatus(Status.done);
					submission.setMessage(message);
					submission.setEndDate(Calendar.getInstance().getTime());
				}
		        return true;
			}catch(IllegalStateException e){
				//e.printStackTrace();
			}
		}
		return false;
	 }

	public static String getResultMessage (HttpSession session){
		String sessionMessage = "";
		synchronized(session){
			try{
				sessionMessage = (String) session.getAttribute("result");
				session.removeAttribute("result");
		        return sessionMessage;
			}catch(IllegalStateException e){
				//e.printStackTrace();
			}
		}
		return sessionMessage;
	 }
	
	public static synchronized String getProgressPercentage(HttpSession session){
		String progressPercentage = "";
		synchronized(session){
			progressPercentage = (String) session.getAttribute(PROGRESS_PERCENTAGE_ATTRIBUTE);
	        return progressPercentage;
		}
	 }
	
	public static void setProgressPercentage(HttpSession session, int percent) {
		try{
			session.setAttribute(PROGRESS_PERCENTAGE_ATTRIBUTE, Integer.toString(percent));
		}catch(IllegalStateException e){
			//e.printStackTrace();
		}
	}
}
