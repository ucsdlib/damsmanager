<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
<body style="background-color:#fff;">
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
	<div class="main-title">UCSD Library Digital Asset Management System Statistics</div>
	<div id="container"></div>
	<div class="tab-container">
		<div class="tab-title">${model.statsTitle}</div>
		<table cellspacing=0 cellpadding=3 border=0 width=100%>
			<tbody id="stats-tab">
				<tr class="tab-banner">
					<th class="tab-banner-first" style="width:280px;">Collection</th>
					<th class="tab-banner-first" style="width:80px;">ARK</th>
					<c:forEach var="monItem" items="${model.periodsList}">
						<th style="border-right: 1px solid #444;width:80px;">${monItem}</th>
					</c:forEach>
				</tr>
				<c:forEach var="sItem" items="${model.data}">
					<tr style="background-color:#f5fcfc;">
						<td style="text-align:left;paddings:0ps 2px;">${sItem.collectionTitle}</td>
						<td style="text-align:left;paddings:0ps 2px;">${sItem.collectionId}</td>
						<c:forEach var="v" items="${sItem.numOfViews}">
							<td><fmt:formatNumber value="${v}" pattern="#,###" /></td>
						</c:forEach>
					</tr>
				</c:forEach>
			</tbody>
		</table>
		<div class="export">
			<a href="/damsmanager/statsCollectionObjects.do?type=${model.type}&start=${model.start}&export">
				<img src="images/excel-icon.png" border="0" width="16px" />
				<span style="display:table-cell;vertical-align:top;font-size:11ps;font-weight:bold;">&nbsp;Export CSV</span></a>
		</div>
	</div>
	<div id="message" align="left" class="errorBody">${model.message}</div>
</div>
</td>
</tr>
</table>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
</html>
