package edu.ucsd.library.xdre.web;

import java.io.PrintWriter;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.dom4j.Element;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;

import edu.ucsd.library.xdre.utils.Constants;
import edu.ucsd.library.xdre.utils.DAMSClient;
import edu.ucsd.library.xdre.utils.DomUtil;

public class JhoveIngestController implements Controller {

	@Override
	public ModelAndView handleRequest(HttpServletRequest request,
			HttpServletResponse response) throws Exception {

		String message = "";
		boolean successful = true;
		boolean loginRequired = true;
		Principal userPrincipal = request.getUserPrincipal();
		if (userPrincipal != null) {
			loginRequired = false;
			if (request.isUserInRole(Constants.CURATOR_ROLE))
				loginRequired = false;
		} else {
			String ip = request.getRemoteAddr();
			String ips = Constants.IPS_ALLOWED;
			if (ips.indexOf(ip) >= 0) {
				loginRequired = false;
				System.out.println("XDRE Manager access allowed from " + ip
						+ ". Operation Jhove Ingest - "
						+ request.getParameter("subject") + "/"
						+ request.getParameter("file"));
			}
		}

		if (!loginRequired) {
			String subject = request.getParameter("subject");
			String component = request.getParameter("component");
			String file = request.getParameter("file");
			String ds = request.getParameter("ds");
			String fs = request.getParameter("fs");
			String collectionId = request.getParameter("collectionId");



			if (subject == null || (subject = subject.trim()).length() == 0) {
				message = "Subject required.";
			}
			if (file == null || (file = file.trim()).length() == 0) {
				message = "Source filename required.";
			}

			String[] subjects = subject.split(",");
			String[] files = file.split(",");
			if (subjects.length != files.length) {
				message = "Subject length doesn't match the source file length: "
						+ subject + "->" + file;
			}
			if (collectionId != null)
				collectionId = collectionId.trim();
			if (message.length() == 0) {
				DAMSClient damsClient = new DAMSClient(Constants.DAMS_STORAGE_URL);
				damsClient.setTripleStore(ds);
				damsClient.setFileStore(fs);
				boolean success = false;

				try {
					for (int i = 0; i < subjects.length; i++) {
						damsClient.extractFileCharacterize(subject, file, null);
					}

					if (!success)
						successful = false;
				} catch (Exception e) {
					successful = false;
					message += e.getMessage();
				}
			}
			Element resultElem = DomUtil.createElement(null, "result", null,
					null);
			DomUtil.createElement(resultElem, "status", null,
					String.valueOf(successful));
			DomUtil.createElement(resultElem, "message", null, message);
			response.setContentType("text/xml");
			response.setCharacterEncoding("UTF-8");
			PrintWriter output = response.getWriter();
			output.write(resultElem.asXML());
			output.flush();
			output.close();
		} else {
			String queryString = "";
			Map pMap = request.getParameterMap();
			for (Iterator it = pMap.entrySet().iterator(); it.hasNext();) {
				Entry en = (Entry) it.next();
				String key = (String) en.getKey();
				queryString += (queryString.length() > 0 ? "&" : "") + key
						+ "=" + request.getParameter(key);
			}
			response.sendRedirect("loginPas.do?loginPage="
					+ URLEncoder.encode("jhove.do?" + queryString, "UTF-8"));
		}
		return null;
	}

}
