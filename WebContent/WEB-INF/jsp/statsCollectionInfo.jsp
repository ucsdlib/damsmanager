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
	<table border="0" cellspacing="0px" cellpadding="0px">
		<tr>
			<td align="left">
				<div class="location"><a href="/digital">Home&nbsp;&nbsp;&nbsp;</a><a href="/damsmanager/stats.do">DLC Statistics&nbsp;&nbsp;&nbsp;</a>Collection Status</div>
			</td>
			<td align="right">

			</td>
		</tr>
	</table>
</div>
<div id="main" class="gallery" align="center">
	<div class="main-title">UCSD Libraries Digital Asset Management System Statistics</div>
	<div class="tab-container">
		<div class="tab-title">${model.collectionName} Status Tracking</div>
		<table cellspacing=0 cellpadding=3 border=0 width=100%>
			<tbody id="stats-sum">
				<tr style="height:28px;background-color:#505050;color:#e8e8e8;font-size:12px;text-align:right;">
					<th style="border-right:2px solid #404040;"><span>Procedure Name</span></th>
					<th style="padding-right:10px"><span title="Process sumitted date">Initial Date</span></th>
					<th style="padding-right:10px"><span title="Latest submission date">Latest Update</span></th>
					<th style="padding-right:10px"><span title="Total number of items processed.">Processed</span></th>
					<th style="padding-right:10px"><span title="Number of items processed">Initial Count</span></th>
					<th style="padding-right:10px"><span title="Curator who submits the process">Submitted By</span></th>
					<th style="padding-right:10px"><span title="Number of items updated">Replaced Count</span></th>
					<th style="padding-right:10px"><span title="Curator who process the update">Replaced By</span></th>					
				</tr>
				<c:forEach var="statsItem" items="${model.processes}" varStatus="status">
						<tr style="text-align:right;">
							<td class="stats-sum-text" style="color:#e8e8e8;padding-right:5px;">${statsItem.processName}</td>
							<td>&nbsp;${statsItem.initialDate}</td>
							<td>&nbsp;${statsItem.updatedDate}</td>
							<td>&nbsp;<a href="statusItems.do?collectionId=${statsItem.collectionId}&processId=${statsItem.processId}&processArk=${statsItem.processArk}">${statsItem.numOfItemsProcessed + statsItem.numOfItemsUpdated}</a></td>							
							<td><fmt:formatNumber value="${statsItem.numOfItemsProcessed}" pattern="#,###" /></td>
							<td>&nbsp;${statsItem.processedBy}</td>
							<td><fmt:formatNumber value="${statsItem.numOfItemsUpdated}" pattern="#,###" /></td>
							<td>&nbsp;${statsItem.updatedBy}</td>
						</tr>
				</c:forEach>
			</tbody>
		</table>
		<!-- 
		<div class="export"><a href="/damsmanager/statsSummary.do?start=${model.start}&export"><img src="images/excel-icon.png" border="0" width="16px" /><span style="display:table-cell;vertical-align:top;font-size:11ps;font-weight:bold;">&nbsp;Export to Excel</span></a></div>
		 -->
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
