<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page isErrorPage="true" %>
<%@ page import="edu.ucsd.library.xdre.utils.RequestOrganizer" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%
   RequestOrganizer.clearSession(session);
%>
<html>
<head>
<c:set var="context" value="${pageContext.servletContext}"/>
<c:set var="servletNames" value="${pageContext.servletContext.servlets}"/>
 <title>${context.servletContextName} Application Error</title>
 <link href="<%= request.getContextPath() %>/base.css"
rel="stylesheet" type="text/css">
</head>
<body>
<table>
<tr align=center>
<td colspan=2>
<p class="errort_text"><b>${context.servletContextName} Application Error</b>
</p>
</td>
</tr>
<tr>
<td width=100>&nbsp;</td>
<td>

<p class="error_text">
<br>
<c:set var="err" scope="session" value="${pageContext.errorData.throwable}"/>
<c:set var="sTrace" scope="pageContext.exception.stackTrace"/>
<c:if test="${!empty err}">
  ${err.message}
 </c:if>  
<br>
        [Occurred while accessing: <%=request.getAttribute("javax.servlet.error.request_uri") %> 
      </p>
<p>
<c:if test="${!empty err}">
   <c:set var="sTrace" value="${err.stackTrace}"/>
   <c:set var="count" value="0"/>
   <c:if test="${!empty sTrace}">
   Server threw exceptions<br>
   <c:forEach var="eMessage" items="${sTrace}" begin="${count}" step="1" end="2">
       &nbsp;&nbsp;&nbsp;&nbsp;at ${eMessage}<br>
    </c:forEach>       
   </c:if>
</c:if>
       &nbsp;&nbsp;&nbsp;&nbsp;...
</p>
</td>
</tr>
<tr>
<td colspan=2>
<p class="menun_text" align=center>
---------------------------------------------------------------<br>
Please <a href="/damsmanager/downloadLog.do?sessionId=<%=session.getId() %>"> view log</a> for details.<br>
<a> If this situation occurred continuously, please contact us</a>.
 </p>
 </td>
</tr>
</table>
</body>
</html>