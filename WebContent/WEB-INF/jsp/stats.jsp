<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

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
				defaultSeriesType: 'spline',
				backgroundColor: '#e8e8e8'
			},
			title: {
				text: ''
			},
			subtitle: {
				text: 'DAMS Usage in last 30 Days'
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
				spline: {
					marker: {
						radius: 4,
						lineColor: '#666666',
						lineWidth: 1
					}
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
		<span class="location" style="float:right;display:cell;vertical-align:top;"><c:choose><c:when test="${model.isCas}"><a href="logout.do?loginPage=stats.do">Log out&nbsp;&nbsp;</a></c:when><c:otherwise><a href="loginPas.do?loginPage=stats.do">Log in&nbsp;&nbsp;</a></c:otherwise></c:choose></span>
		<ul>
			<li><a href="http://libraries.ucsd.edu">Library Home</a></li>
			<li><a href="${model.clusterHost}/dc">Digital Library Collections</a></li>
			<li>DAMS Statistics</li>
		</ul>
	</div><!-- /tdr_crumbs_content -->
	
	<!-- This div is for temporarily writing breadcrumbs to for processing purposes -->
		<div id="temporaryBreadcrumb" style="display: none">
	</div>
</div><!-- /tdr_crumbs -->
<div id="main" class="gallery" align="center">
	<div class="main-title">UCSD Libraries Digital Asset Management System Statistics</div>
	<div id="container" style="margin-bottom:10px;"></div>
	<div id="stats-links">
		<ul class="index-list">
			<li><div class="item"><a href="statsPopular.do">DAMS's Top 100s</a></div></li>
			<li><div class="item"><a href="statsSummary.do?start=2010-03-01">DAMS Summary by Month</a></div></li>
			<li><div class="item"><a href="statsUsage.do?type=dams">DAMS Usage by Month</a></div></li>
			<li><div class="item"><a href="statsUsage.do?type=item">DAMS Items Usage by Month</a></div></li>
			<li><div class="item"><a href="statsCollections.do">DAMS Collections Access by Month</a></div></li>
			<li><div class="item"><a href="statsKeywords.do">DAMS Keywords/Phrases</a></div></li>
			<c:if test="${model.isCas}"><li><div class="item"><a href="collectionsReport.do">DAMS Collections</a></div></li></c:if>
		</ul>
	</div>
	<div id="message" align="left" class="errorBody">${model.message}</div>
</div>
</td>
</tr>
</table>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
</html>
