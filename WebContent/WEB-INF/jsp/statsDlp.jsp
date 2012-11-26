<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/htmlheader.jsp" />
<body>
<script type="text/javascript" src="/damsmanager/javascripts/highcharts.js"></script>
<script type="text/javascript">	
	var chart;
	$(document).ready(function() {
		chart = new Highcharts.Chart({
			chart: {
				renderTo: 'container',
				defaultSeriesType: 'areaspline',
				backgroundColor: '#e8e8e8'
			},
			title: {
				text: ''
			},
			subtitle: {
				text: 'DLC PAS<c:if test="${model.isCas}">/CAS</c:if> Monthly Usage'
			},
			legend: {
				enabled: true,										
				layout: 'vertical',
				align: 'left',
				verticalAlign: 'top',
				x: 150,
				y: 50,
				floating: true,
				borderWidth: 1,
				backgroundColor: '#FFFFFF'
			},
			xAxis: {
				categories: ${model.periods}
			},
			yAxis: {
				title: {
					text: ''
				},
				labels: {
					formatter: function() {
						return this.value
					}
				}
			},
			tooltip: {
				crosshairs: true,
				shared: true
			},
			plotOptions: {
				areaspline: {
							fillOpacity: 0.4
						}
			},
			series: [{
				name: 'PAS',
				marker: {
					symbol: 'dot'
				},
				data: ${model.pasData}
		
			}<c:if test="${model.isCas}">, {
				name: 'CAS',
				marker: {
					symbol: 'diamond'
				},
				data: ${model.casData}
			}</c:if>]
		});
		
		
	});
</script>
<table align="center" cellspacing="0px" cellpadding="0px" class="bodytable">
<tr><td>
<jsp:include flush="true" page="/jsp/banner.jsp" />
<div>
	<span class="location" style="float:right;display:cell;vertical-align:top;"><c:choose><c:when test="${model.isCas}"><a href="logout.do?loginPage=stats.do">Log out&nbsp;&nbsp;</a></c:when><c:otherwise><a href="loginPas.do?loginPage=stats.do">Log in&nbsp;&nbsp;</a></c:otherwise></c:choose></span>
	<table border="0" cellspacing="0px" cellpadding="0px">
		<tr>
			<td align="left">
				<div class="location"><a href="/digital">Home&nbsp;&nbsp;&nbsp;</a><a href="/damsmanager/stats.do">DLC Statistics&nbsp;&nbsp;&nbsp;</a>PAS<c:if test="${model.isCas}">/CAS</c:if> Usage</div>
			</td>
			<td align="right">
			</td>
		</tr>
	</table>
</div>
<div id="main" class="gallery" align="center">
	<div class="main-title">UCSD Libraries Digital Asset Management System Statistics</div>
	<div id="container"></div>
	<div class="tab-container">
		<div class="tab-title">PAS<c:if test="${model.isCas}">/CAS</c:if> Usage By Month</div>
		<table cellspacing=0 cellpadding=3 border=0 width=100%>
			<tbody id="stats-tab">
				<tr class="tab-banner">
					<th class="tab-banner-first">Name</th>
					<th class="tab-banner-first">Stats</th>
					<c:forEach var="monItem" items="${model.periodsList}">
						<th>${monItem}</th>
					</c:forEach>
				</tr>
				<tr style="background-color:#f5fcfc;">
					<td rowspan="4" class="stats-tab-text" style="border-bottom:2px solid #ccc;color:#e8e8e8;">PAS</td>
					<td class="stats-tab-text"><span title="Number of times DLC PAS were accessed" style="color:#e8e8e8;">Usage</span></td>
					<c:forEach var="pItem" items="${model.pasUsageList}">
						<td><fmt:formatNumber value="${pItem}" pattern="#,###" /></td>
					</c:forEach>
				</tr>
				<tr style="background-color:#f5fcfc;">
					<td class="stats-tab-text"><span title="Number of queries conducted by searching in PAS" style="color:#e8e8e8;">Search</span></td>
					<c:forEach var="cItem" items="${model.pasSearchList}">
						<td><fmt:formatNumber value="${cItem}" pattern="#,###" /></td>
					</c:forEach>
				</tr>
				<tr style="background-color:#f5fcfc;">
					<td class="stats-tab-text"><span title="Number of queries conducted by browsing" style="color:#e8e8e8;">Browse</span></td>
					<c:forEach var="cItem" items="${model.pasBrowseList}">
						<td><fmt:formatNumber value="${cItem}" pattern="#,###" /></td>
					</c:forEach>
				</tr>
				<tr style="background-color:#f5fcfc;">
					<td class="stats-tab-text" style="border-bottom:2px solid #ccc;"><span title="Number of times DLC PAS collection page were accessed" style="color:#e8e8e8;">Collection Page</span></td>
					<c:forEach var="cItem" items="${model.pasColPageList}">
						<td><fmt:formatNumber value="${cItem}" pattern="#,###" /></td>
					</c:forEach>
				</tr>
				<c:if test="${model.isCas}">
				<tr style="background-color:#f8f8f8;">
					<td rowspan="4" class="stats-tab-text" style="border-bottom:2px solid #999;color:#e8e8e8;">CAS</td>
					<td class="stats-tab-text"><span title="Number of times DLC CAS were accessed" style="color:#e8e8e8;">Usage</span></td>
					<c:forEach var="cItem" items="${model.casUsageList}">
						<td style="border-top:1px solid #ccc;"><fmt:formatNumber value="${cItem}" pattern="#,###" /></td>
					</c:forEach>
				</tr>
				<tr style="background-color:#f8f8f8;">
					<td class="stats-tab-text"><span title="Number of queries conducted by searching in CAS" style="color:#e8e8e8;">Search</span></td>
					<c:forEach var="cItem" items="${model.casSearchList}">
						<td><fmt:formatNumber value="${cItem}" pattern="#,###" /></td>
					</c:forEach>
				</tr>
				<tr style="background-color:#f8f8f8;">
					<td class="stats-tab-text"><span title="Number of queries conducted by browsing" style="color:#e8e8e8;">Browse</span></td>
					<c:forEach var="cItem" items="${model.casBrowseList}">
						<td style="border-bottom:2px solid #999;"><fmt:formatNumber value="${cItem}" pattern="#,###" /></td>
					</c:forEach>
				</tr>
				<tr style="background-color:#f8f8f8;">
					<td class="stats-tab-text" style="border-bottom:2px solid #999;"><span title="Number of times DLC collection page were accessed" style="color:#e8e8e8;">Collection Page</span></td>
					<c:forEach var="cItem" items="${model.casColPageList}">
						<td><fmt:formatNumber value="${cItem}" pattern="#,###" /></td>
					</c:forEach>
				</tr>
				</c:if>
			</tbody>
		</table>
	</div>
	<div id="message" align="left" class="errorBody">${model.message}</div>
</div>
<jsp:include flush="true" page="/jsp/footer.jsp" />
</td>
</tr>
</table>
</body>
</html>
