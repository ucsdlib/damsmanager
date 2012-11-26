<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/htmlheader.jsp" />
<body>
<script type="text/javascript" src="/damsmanager/javascripts/highcharts.js"></script>
<script type="text/javascript" src="/damsmanager/javascripts/directories.js"></script>
<script type="text/javascript" src="/damsmanager/javascripts/json2.js"></script>
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
				text: 'DLC PAS<c:if test="${model.isCas}">/CAS</c:if> Usage in last 30 Days'
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
				<div class="location"><a href="/digital">Home&nbsp;&nbsp;&nbsp;</a>Statistics</div>
			</td>
			<td align="right">			</td>
		</tr>
	</table>
</div>
<div id="main" class="gallery" align="center">
	<div class="main-title">UCSD Libraries Digital Asset Management System Statistics</div>
	<div id="container" style="margin-bottom:20px;"></div>
	<div id="stats-links">
		<ul class="index-list">
			<li><div class="item"><a href="dlPopular.do">DLC's Top 100s</a></div></li>
			<li><div class="item"><a href="statsSummary.do?start=2010-03-01">DLC Summary by Month</a></div></li>
			<li><div class="item"><a href="statsDlp.do?type=dlp">DLC PAS<c:if test="${model.isCas}">/CAS</c:if> Usage by Month</a></div></li>
			<li><div class="item"><a href="statsDlp.do?type=dlpObject">DLC Items Usage by Month</a></div></li>
			<li><div class="item"><a href="statsDlpColls.do">DLC Collection Access by Month</a></div></li>
			<li><div class="item"><a href="statsKeywords.do">DLC Keywords/Phrases</a></div></li>
			<c:if test="${model.isCas}"><li><div class="item"><a href="collectionReport.do">DLC Collections</a></div></li></c:if>
		</ul>
	</div>
	<div id="message" align="left" class="errorBody">${model.message}</div>
</div>
<jsp:include flush="true" page="/jsp/footer.jsp" />
</td>
</tr>
</table>
</body>
</html>
