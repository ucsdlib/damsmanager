package edu.ucsd.library.xdre.web;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.Controller;


public class LoginPasController implements Controller{

	public ModelAndView handleRequest(HttpServletRequest request, HttpServletResponse response) throws Exception {
		String loginPage = request.getParameter("loginPage");
		boolean cookieExisted = false;
		Cookie[] cookies = request.getCookies();
		if(cookies != null){
			for(int i=0; i<cookies.length; i++){
				if(cookies[i].getName().equals("cas") && cookies[i].getValue().equals("32")){
					cookieExisted = true;
					break;
				}
			}
		}
		if(!cookieExisted){
			Cookie cookie = new Cookie("cas", "32");
			response.addCookie(cookie);
		}
		response.sendRedirect(response.encodeURL(loginPage));
		return null;
	}	
}
