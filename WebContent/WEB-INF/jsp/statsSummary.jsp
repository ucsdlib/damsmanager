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
		<tr>
			<td align="left">
				<div class="location"><a href="/digital">Home&nbsp;&nbsp;&nbsp;</a><a href="/damsmanager/stats.do">DLC Statistics&nbsp;&nbsp;&nbsp;</a>Summary</div>
			</td>
			<td align="right">

			</td>
		</tr>
	</table>
</div>
<div id="main" class="gallery" align="center">
	<div class="main-title">UCSD Libraries Digital Asset Management System Statistics</div>
	<div class="tab-container">
		<div class="tab-title">DLC Statistics Summary By Month</div>
		<table cellspacing=0 cellpadding=3 border=0 width=100%>
			<tbody id="stats-sum">
				<tr style="height:28px;background-color:#505050;color:#e8e8e8;font-size:12px;text-align:center;">
					<th style="border-right:2px solid #404040;"><span>Month</span></th>
					<th><span title="Number of Collections in DAMS">Collections</span></th>
					<th><span title="Number of unique items in DAMS">Total Items</span></th>
					<th><span title="Total size in megabytes">Size (MB)</span></th>
					<th><span title="Number of times PAS<c:if test="${model.isCas}">/CAS</c:if> were accessed">DLC Usage</span></th>
					<th><span title="Number of queries conducted in a month">Queries</span></th>
					<th><span title="Number of times unique items were accessed (functionality calls on object)">Item Hits</span></th>
					<th><span title="Number of times the unique items were viewed">Item Views</span></th>
					<!-- 
					<th><span title="Number of unique items were accessed">Object Usage</span></th>
					<th><span title="Number of unique items were viewed">Object View</span></th>
					 -->
				</tr>
				<c:forEach var="statsItem" items="${(model.dlp==null?model.pas:model.dlp)}" varStatus="status">
					<tr>
						<td class="stats-sum-text" style="color:#e8e8e8;	padding-right:5px;">${statsItem.periodDisplay}</td>
						<td><fmt:formatNumber value="${statsItem.numOfCollections}" pattern="#,###" /></td>
						<td><fmt:formatNumber value="${statsItem.numOfItems}" pattern="#,###" /></td>
						<td><fmt:formatNumber value="${statsItem.totalSize/1000000.0}" pattern="#,###" /></td>
						<td><fmt:formatNumber value="${statsItem.numOfUsage}" pattern="#,###" /></td>
						<td><fmt:formatNumber value="${statsItem.numOfQueries}" pattern="#,###" /></td>
						<td><fmt:formatNumber value="${statsItem.itemSummary.numOfUsage}" pattern="#,###" /></td>
						<td><fmt:formatNumber value="${statsItem.itemSummary.numOfViews}" pattern="#,###" /></td>
						<!-- 
						<td><fmt:formatNumber value="${statsItem.itemSummary.numOfItemAccessed}" pattern="#,###" /></td>
						<td><fmt:formatNumber value="${statsItem.itemSummary.numOfItemViewed}" pattern="#,###" /></td>
						 -->
					</tr>
				</c:forEach>
			</tbody>
		</table>
		<div class="export"><a href="/damsmanager/statsSummary.do?start=${model.start}&export"><img src="images/excel-icon.png" border="0" width="16px" /><span style="display:table-cell;vertical-align:top;font-size:11ps;font-weight:bold;">&nbsp;Export to Excel</span></a></div>
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
