<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
<body style="background-color:#fff;">
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
				text: 'DAMS Monthly Usage'
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
				name: 'DAMS',
				marker: {
					symbol: 'dot'
				},
				data: ${model.pasData}
		
			}]
		});
		
		
	});
</script>
<jsp:include flush="true" page="/jsp/libanner.jsp" />
<table align="center" cellspacing="0px" cellpadding="0px" class="bodytable">
<tr><td>
<div id="tdr_crumbs">
	<div id="tdr_crumbs_content">
		<ul>
			<li><a href="http://libraries.ucsd.edu">Library Home</a></li>
			<li><a href="${model.clusterHost}/dc">Digital Library Collections</a></li>
			<li><a href="/damsmanager/stats.do">DAMS Statistics</a></li>
			<li>DAMS Usage</li>
		</ul>
	</div><!-- /tdr_crumbs_content -->
	
	<!-- This div is for temporarily writing breadcrumbs to for processing purposes -->
		<div id="temporaryBreadcrumb" style="display: none">
	</div>
</div><!-- /tdr_crumbs -->
<div id="main" class="gallery" align="center">
	<div class="main-title">UCSD Library Digital Asset Management System Statistics</div>
	<div id="container"></div>
	<div class="tab-container">
		<div class="tab-title">DAMS Usage By Month</div>
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
					<td rowspan="5" class="stats-tab-text" style="border-bottom:2px solid #ccc;color:#e8e8e8;">PAS</td>
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
					<td class="stats-tab-text"><span title="Number of times DLC PAS home page were accessed" style="color:#e8e8e8;">Home Page</span></td>
					<c:forEach var="cItem" items="${model.pasHomePageList}">
						<td><fmt:formatNumber value="${cItem}" pattern="#,###" /></td>
					</c:forEach>
				</tr>
				<tr style="background-color:#f5fcfc;">
					<td class="stats-tab-text" style="border-bottom:2px solid #ccc;"><span title="Number of times DLC PAS collection page were accessed" style="color:#e8e8e8;">Collection Page</span></td>
					<c:forEach var="cItem" items="${model.pasColPageList}">
						<td style="border-bottom:2px solid #ccc;"><fmt:formatNumber value="${cItem}" pattern="#,###" /></td>
					</c:forEach>
				</tr>
			</tbody>
		</table>
	</div>
	<div id="message" align="left" class="errorBody">${model.message}</div>
</div>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</td>
</tr>
</table>
</body>
</html>
