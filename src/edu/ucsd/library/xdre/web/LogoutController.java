package edu.ucsd.library.xdre.web;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;


public class LogoutController implements Controller{

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		HttpSession session = request.getSession(false);
		if(session != null && !session.isNew())
			session.invalidate();
		String loginPage = request.getParameter("loginPage");
		if(loginPage == null)
			loginPage = "/damsmanager/";
		Cookie[] cookies = request.getCookies();
		if(cookies != null){
			for(int i=0; i<cookies.length; i++){
				if(cookies[i].getName().equals("cas") && cookies[i].getValue().equals("32")){
					cookies[i].setMaxAge(0);
					break;
				}
			}
		}
		response.sendRedirect(loginPage);
		return null;
	}
	
}
