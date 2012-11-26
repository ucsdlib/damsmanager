<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
<body style="background-color:#fff;">
<script type="text/javascript" src="/damsmanager/javascripts/highcharts.js"></script>
<jsp:include flush="true" page="/jsp/libanner.jsp" />
<table align="center" cellspacing="0px" cellpadding="0px" class="bodytable">
<tr><td>
<div id="tdr_crumbs">
	<div id="tdr_crumbs_content">
		<ul>
			<li><a href="http://libraries.ucsd.edu">Home</a></li>
			<li><a href="${model.isCas?"/curator":"/digital"}">Digital Library Collections</a></li>
			<li><a href="/damsmanager/stats.do">DLC Statistics</a></li>
			<li>Collections Access</li>
		</ul>
	</div><!-- /tdr_crumbs_content -->
	
	<!-- This div is for temporarily writing breadcrumbs to for processing purposes -->
		<div id="temporaryBreadcrumb" style="display: none">
	</div>
</div><!-- /tdr_crumbs -->
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
							<th style="border-right:2px solid #404040;text-align:right;padding-right:2px;">${period}</th>
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
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
</html>
