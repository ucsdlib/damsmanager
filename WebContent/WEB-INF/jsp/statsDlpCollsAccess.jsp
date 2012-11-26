<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/htmlheader.jsp" />
<body>
<script type="text/javascript" src="/damsmanager/javascripts/highcharts.js"></script>
<table align="center" cellspacing="0px" cellpadding="0px" class="bodytable">
<tr><td>
<jsp:include flush="true" page="/jsp/banner.jsp" />
<div>
	<span class="location" style="float:right;display:cell;vertical-align:top;"><c:choose><c:when test="${model.isCas}"><a href="logout.do?loginPage=stats.do">Log out&nbsp;&nbsp;</a></c:when><c:otherwise><a href="loginPas.do?loginPage=stats.do">Log in&nbsp;&nbsp;</a></c:otherwise></c:choose></span>
	<table border="0" cellspacing="0px" cellpadding="0px">
		<tr">
			<td align="left">
				<div class="location"><a href="/digital">Home&nbsp;&nbsp;&nbsp;</a><a href="/damsmanager/stats.do">DLC Statistics&nbsp;&nbsp;&nbsp;</a>Collections Access</div>
			</td>
			<td align="right">

			</td>
		</tr>
	</table>
</div>
<div id="main" class="gallery" align="center">
	<div class="main-title">UCSD Libraries Digital Asset Management System Statistics</div>
	<div class="tab-container">
		<div class="tab-title">DLC Collections Access By Month</div>
		<table cellspacing=0 cellpadding=3 border=0 width=100%>
			<tbody id="stats-sum">
				<tr style="height:28px;background-color:#505050;color:#e8e8e8;font-size:12px;">
					<c:forEach var="period" items="${model.periodsList}" varStatus="status">
					<c:choose>
						<c:when test="${status.count==1}">
							<th style="text-align:center;padding-left:5px;padding-right:2px;border-right:2px solid #404040;">${period}</th>
						</c:when>
						<c:otherwise>
							<th style="padding-right:2px;text-align:right;border-right:2px solid #404040;">${period}</th>
						</c:otherwise>
					</c:choose>
					</c:forEach>
				</tr>
				<c:forEach var="entry" items="${model.colStatsData}" varStatus="status">
					<tr>
						<td class="stats-sum-text" style="padding-left:5px;color:#e8e8e8;padding-right:2px;text-align:left;">${entry.key}</td>
						<c:forEach var="statsData" items="${entry.value}">
							<td style="text-align:right;padding-right:4px;"><fmt:formatNumber value="${statsData}" pattern="#,###" /></td>
						</c:forEach>
					</tr>
				</c:forEach>
			</tbody>
		</table>
		<div class="export"><a href="/damsmanager/statsDlpColls.do?start=${model.start}&export"><img src="images/excel-icon.png" border="0" width="16px" /><span style="display:table-cell;vertical-align:top;font-size:11ps;font-weight:bold;">&nbsp;Export to Excel</span></a></div>
	</div>
	<div id="message" align="left" class="errorBody">${model.message}</div>
</div>
<jsp:include flush="true" page="/jsp/footer.jsp" />
</td>
</tr>
</table>
</body>
<jsp:include flush="true" page="/jsp/popmenus.jsp" />
</html>
