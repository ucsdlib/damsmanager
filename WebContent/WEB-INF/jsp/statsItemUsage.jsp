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
				text: 'DAMS Item Monthly Usage'
			},
			legend: {
				enabled: true,										
				layout: 'vertical',
				align: 'left',
				verticalAlign: 'top',
				x: 150,
				y: 48,
				floating: true,
				borderWidth: 1,
				backgroundColor: '#FFFFFF'
			},
			xAxis: {
				categories:${model.periods}
				/*,
				plotBands: [{ // visualize
					from: 4.5,
					to: 6.5,
					color: 'rgba(68, 170, 213, .2)'
				}]*/
			},
			yAxis: {
				title: {
					text: ''
				}
			},
			tooltip: {
				formatter: function() {
		                return ''+ this.x + ',' + this.y +' times';
				}
			},
			plotOptions: {
				areaspline: {
					fillOpacity: 0.4
				}
			},
			series: [{
				name: 'Total Access',
				data: ${model.dlpData==null?model.pasData:model.dlpData}
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
			<li>Item Usage</li>
		</ul>
	</div><!-- /tdr_crumbs_content -->
	
	<!-- This div is for temporarily writing breadcrumbs to for processing purposes -->
		<div id="temporaryBreadcrumb" style="display: none">
	</div>
</div><!-- /tdr_crumbs -->
<div id="main" class="gallery" align="center">
	<div class="main-title">UCSD Libraries Digital Asset Management System Statistics</div>
	<div id="container"></div>
	<div class="tab-container">
		<div class="tab-title">DAMS Item Usage by Month</div>
		<table cellspacing=0 cellpadding=3 border=0 width=100%>
			<tbody id="stats-tab">
				<tr class="tab-banner">
					<th class="tab-banner-first">Name</th>
					<th class="tab-banner-first">Stats</th>
					<c:forEach var="monItem" items="${model.periodsList}">
						<th>${monItem}</th>
					</c:forEach>
				</tr>
				<tr style="background-color:#eee;">
					<td rowspan="2" class="stats-tab-text" style="border-bottom:2px solid #ccc;color:#e8e8e8;"><span title="Total count.">Total</span></td>
					<td class="stats-tab-text"><span title="Total number of times the unique items were accessed" style="color:#e8e8e8;">Access</span></td>
					<c:forEach var="cItem" items="${model.dlpAccessList==null?model.pasAccessList:model.dlpAccessList}">
						<td><fmt:formatNumber value="${cItem}" pattern="#,###" /></td>
					</c:forEach>
				</tr>
				<tr style="background-color:#f5fcfc;">
					<td class="stats-tab-text" style="border-bottom:2px solid #ccc;"><span title="Total number of times unique items were viewed" style="color:#e8e8e8;">Views</span></td>
					<c:forEach var="cItem" items="${model.dlpViewList==null?model.pasViewList:model.dlpViewList}">
						<td style="border-bottom:2px solid #ccc;"><fmt:formatNumber value="${cItem}" pattern="#,###" /></td>
					</c:forEach>
				</tr>
				<tr style="background-color:#eee;">
					<td rowspan="2" class="stats-tab-text" style="border-bottom:2px solid #999;color:#e8e8e8;"><span title="Count unique items">Object</span></td>
					<td class="stats-tab-text"><span title="Number of unique items were accessed" style="color:#e8e8e8;">Usage</span></td>
					<c:forEach var="cItem" items="${model.dlpObjectList==null?model.pasObjectList:model.dlpObjectList}">
						<td><fmt:formatNumber value="${cItem}" pattern="#,###" /></td>
					</c:forEach>
				</tr>
				<tr style="background-color:#f5fcfc;">
					<td class="stats-tab-text" style="border-bottom:2px solid #999;"><span title="Number of unique items were viewed" style="color:#e8e8e8;">Views</span></td>
					<c:forEach var="cItem" items="${model.dlpObjectViewList==null?model.pasObjectViewList:model.dlpObjectViewList}">
						<td style="border-bottom:2px solid #999;"><fmt:formatNumber value="${cItem}" pattern="#,###" /></td>
					</c:forEach>
				</tr>
			</tbody>
		</table>
	</div>
	<div id="message" align="left" class="errorBody">${model.message}</div>
</div>
</td>
</tr>
</table>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
</html>
