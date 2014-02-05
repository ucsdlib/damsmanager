<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

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
			<li>Popular Items</li>
		</ul>
	</div><!-- /tdr_crumbs_content -->
	
	<!-- This div is for temporarily writing breadcrumbs to for processing purposes -->
		<div id="temporaryBreadcrumb" style="display: none">
	</div>
</div><!-- /tdr_crumbs -->
<div id="main" class="gallery" align="center">
	<div class="main-title">UCSD Libraries Digital Asset Management System Statistics</div>
	<div class="tab-container">	
		<table cellspacing=0 cellpadding=3 border=0>
			<tbody style="color:#505050;font-size:12px;display:table-cell;text-align:left;">
				<tr>
					<td colspan="7" style="border-bottom:3px solid #B8B8B8;"><div class="tab-title">DAMS' Top 100 List:</div></td>
				</tr>
				<c:forEach var="item" items="${model.items}" varStatus="status">
					<c:set var="titleLen">${fn:length(item.title)}</c:set>
					<c:set var="colTitleLen"><c:choose><c:when test="${item.collectionTitle == null}" >0</c:when><c:otherwise>${fn:length(item.collectionTitle)}</c:otherwise></c:choose></c:set>
					<c:choose>
						<c:when test="${(status.count-1)%2==0}">
							<tr style="text-align:left;">
						</c:when>
						<c:otherwise>
							<td width="10" <c:if test="${status.count==fn:length(model.items)}">style="border-bottom:1px solid #ccc;"</c:if>>&nbsp;</td>
						</c:otherwise>
					</c:choose>
					<td style="vertical-align:top;<c:if test="${status.count==fn:length(model.items) || status.count+1==fn:length(model.items)}">border-bottom:1px solid #ccc;</c:if>">
						<div class="thumbIdx">${status.count}</div>
					</td>
					<td style="border-bottom:1px solid #ccc;width:71px;">
						<div class="thumbBox" title="Title: ${item.title}"><a href="${item.clusterHost}/dc/object/${item.subject}" target="dlp"><c:if test="${item.icon != null}"><img class="icon" src="${item.clusterHost}/dc/object/${item.subject}/${item.icon}" /></c:if></a></div>
					</td>
					<td style="vertical-align:top;border-bottom:1px solid #ccc;">
						<div class="thumbText" title="View item details"><a href="${item.clusterHost}/dc/object/${item.subject}"><c:choose><c:when test="${titleLen <= 74}">${item.title}</c:when><c:otherwise>${fn:substring(item.title, 0, 70)} ...</c:otherwise></c:choose></a></div>
						<div title="Collection Title" class="thumbColl"><c:choose><c:when test="${colTitleLen <= 74}">${item.collectionTitle}</c:when><c:otherwise>${fn:substring(item.collectionTitle, 0, 70)} ...</c:otherwise></c:choose></div>
						<div class="thumbView">[Views: ${item.numView}, Peeks: ${item.numAccess}]</div>
					</td>
					<c:if test="${(status.count-1)%2!=0}">
						</tr>
					</c:if>
				</c:forEach>
				<tr>
					<td colspan="7" style="border-top:2px solid #B8B8B8;">&nbsp;</td>
				</tr>
			</tbody>
		</table>
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
