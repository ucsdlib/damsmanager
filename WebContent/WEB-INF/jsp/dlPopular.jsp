<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/htmlheader.jsp" />
<body>
<table align="center" cellspacing="0px" cellpadding="0px" class="bodytable">
<tr><td>
<jsp:include flush="true" page="/jsp/banner.jsp" />
<div>
	<span class="location" style="float:right;display:cell;vertical-align:top;"><c:choose><c:when test="${model.isCas}"><a href="logout.do?loginPage=stats.do">Log out&nbsp;&nbsp;</a></c:when><c:otherwise><a href="loginPas.do?loginPage=stats.do">Log in&nbsp;&nbsp;</a></c:otherwise></c:choose></span>
	<table border="0" cellspacing="0px" cellpadding="0px">
		<tr>
			<td align="left">
				<div class="location"><a href="/digital">Home&nbsp;&nbsp;&nbsp;</a><a href="/damsmanager/stats.do">DLC Statistics&nbsp;&nbsp;&nbsp;</a>Popular Items</div>
			</td>
			<td align="right">

			</td>
		</tr>
	</table>
</div>
<div id="main" class="gallery" align="center">
	<div class="main-title">UCSD Libraries Digital Asset Management System Statistics</div>
	<div class="tab-container">	
		<table cellspacing=0 cellpadding=3 border=0>
			<tbody style="color:#505050;font-size:12px;display:table-cell;text-align:left;">
				<tr>
					<td colspan="7" style="border-bottom:3px solid #B8B8B8;"><div class="tab-title">DLC's Top 100 List:</div></td>
				</tr>
				<c:forEach var="item" items="${model.items}" varStatus="status">
					<c:set var="titleLen">${fn:length(item.title)}</c:set>
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
						<div class="thumbBox" title="Title: ${item.title}"><i></i><a href="/xdre/damsAccess?file=&subject=${item.subject}" target="dlp"><img style="border:0;margin-bottom:2px;" src="/xdre/damsAccess?subject=${item.subject}&file=${item.icon}" /></a><i></i></div>
					</td>
					<td style="vertical-align:top;border-bottom:1px solid #ccc;">
						<div class="thumbText" title="View item details"><a href="/xdre/damsAccess?file=&subject=${item.subject}"><c:choose><c:when test="${titleLen <= 72}">${item.title}</c:when><c:otherwise>${fn:substring(item.title, 0, 72)} ...</c:otherwise></c:choose></a></div>
						<div title="Collection Title" class="thumbColl">${item.collectionTitle}</div>
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
<jsp:include flush="true" page="/jsp/footer.jsp" />
</td>
</tr>
</table>
</body>
</html>
