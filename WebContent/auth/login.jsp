<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<c:if test="${request.logout != null}">
	<%request.getSession().invalidate(); %>
</c:if>
<html>
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
<body onload="document.loginForm.j_username.focus();" style="background-color:#fff;">
<jsp:include flush="true" page="/jsp/libanner.jsp" />

<table align="center" cellspacing="0px" cellpadding="0px" class="bodyTable">
<tr><td>
<div style="padding-bottom:30px;">
		<div id="tdr_crumbs">
			<div id="tdr_crumbs_content">
				<ul>
					<li><a href="http://libraries.ucsd.edu/index.html">Home</a></li>
					<li><a href="/digital">Digital Library Collections</a></li>
					<li>XDRE Manager</li>
				</ul>
			</div><!-- /tdr_crumbs_content -->
			
			<!-- This div is for temporarily writing breadcrumbs to for processing purposes -->
				<div id="temporaryBreadcrumb" style="display: none">
			</div>
		</div><!-- /tdr_crumbs -->
		<div id="loginError" class="message">
			<c:if test="${param.error != null}">Invalid AD username/password! Please try again...</c:if>
		</div>
		<jsp:include flush="true" page="loginForm.jsp" />
</div>
</td>
</tr>
</table>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
</html>
