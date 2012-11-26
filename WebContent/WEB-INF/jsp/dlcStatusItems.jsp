<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/htmlheader.jsp" />
<body>
<script language="javaScript">
	function showPage(collectionId,processId,processArk,noAction,pageNo){
		var url = "/damsmanager/statusItems.do?collectionId=" + collectionId + "&processId=" + processId + "&processArk=" + processArk + (noAction==true?"&noAction":"")+ "&page=" + pageNo;
		document.location.href = url;
	}
</script>
<table align="center" cellspacing="0px" cellpadding="0px" class="bodytable">
<tr><td>
<jsp:include flush="true" page="/jsp/banner.jsp" />
<div>
	<table border="0" cellspacing="0px" cellpadding="0px">
		<tr>
			<td align="left">
				<div class="location"><a href="/digital">Home&nbsp;&nbsp;&nbsp;</a><a href="/damsmanager/stats.do">DLC Statistics&nbsp;&nbsp;&nbsp;</a>Items Tracking</div>
			</td>
			<td align="right">

			</td>
		</tr>
	</table>
</div>
<div id="main" class="gallery" align="center">
	<div class="main-title">UCSD Libraries Digital Asset Management System Statistics</div>
	<div class="tab-container" style="width:780px;">	
		<table cellspacing=0 cellpadding=3 border=0>
			<tbody style="color:#505050;font-size:12px;display:table-cell;text-align:left;">
				<tr>
					<td colspan="6"><div class="tab-title">DLC Event Tracking - ${model.event}: ${model.collectionTitle}</div></td>
				</tr>
				<tr>
					<td colspan="4" valign="bottom" style="border-bottom:3px solid #B8B8B8;"><div class="thumbText" style="color:#336699;">Total number of items <c:if test="${model.noAction}">not </c:if>done ${model.size} (	page ${model.page} of ${model.pages}): </div></td>
					<td colspan="2" valign="bottom" style="border-bottom:3px solid #B8B8B8;"><div id="keywordsPaging" style="text-align:right;"><span class="pagingAngle">&lt;</span> <span id="kPagingPre"><span <c:choose><c:when test="${model.page>1}">class="paging" onclick="showPage('${model.collectionId}','${model.processId}','${model.processArk}','${model.noAction}',${model.page-1})"</c:when><c:otherwise>class="pagingNone"</c:otherwise></c:choose>>Previous</span></span> &nbsp; <span id="kPagingNext"><span <c:choose><c:when test="${model.pages>model.page}">class="paging" onClick="showPage('${model.collectionId}','${model.processId}','${model.processArk}','${model.noAction}',${model.page+1})"</c:when><c:otherwise>class="pagingNone"</c:otherwise></c:choose>>Next</span></span> <span class="pagingAngle" style="margin-right:5px;">&gt;</span></div></td>
				</tr>
				<tr>
					<th style="vertical-align:top;border-bottom:1px solid #ccc;width:71px;text-align:center;">Icon</th>
					<th style="vertical-align:top;border-bottom:1px solid #ccc;text-align:center;">Identifiers</th>
					<!-- 
					<th style="vertical-align:top;border-bottom:1px solid #ccc;width:100px;text-align:center;">File</th>
					<th style="vertical-align:top;border-bottom:1px solid #ccc;width:200px;text-align:center;">Title</th>
					 -->
					<th style="vertical-align:top;border-bottom:1px solid #ccc;width:130px;text-align:center;" nowrap>Initial Date</th>
					<th style="vertical-align:top;border-bottom:1px solid #ccc;width:130px;text-align:center;" nowrap>Submitted Date</th>
					<th style="vertical-align:top;border-bottom:1px solid #ccc;width:80px;text-align:center;" nowrap>Submitted By</th>
					<th style="vertical-align:top;border-bottom:1px solid #ccc;width:200px;text-align:center;">Notes</th>
				</tr>
				<c:forEach var="item" items="${model.noAction?model.noActionItems:model.items}" varStatus="status">
					<c:set var="titleLen">${fn:length(item.title)}</c:set>
					<c:choose>
						<c:when test="${(status.count-1)%2==0}">
							<tr style="text-align:left;">
						</c:when>
						<c:otherwise>
							<tr style="text-align:left;background-color:#f0f0f0;">
						</c:otherwise>
					</c:choose>
					<td style="border-bottom:1px solid #ccc;width:71px;">
						<div class="thumbBox" title="Title - <c:choose><c:when test="${titleLen <= 72}">${item.title}</c:when><c:otherwise>${fn:substring(item.title, 0, 72)} ...</c:otherwise></c:choose>"><a href="/apps/public#ark:${item.subject}" target="dlp"><img border=0 src="/xdre/damsAccess?subject=${item.subject}&file=${item.icon}" /></a></div>
					</td>
					<td style="vertical-align:top;border-bottom:1px solid #ccc;">
						<div class="thumbText">${item.localId}</div>
					</td>
					<!-- 
					<td style="vertical-align:top;border-bottom:1px solid #ccc;">
						<div class="thumbText">${item.localId}</div>
					</td>
					<td style="vertical-align:top;border-bottom:1px solid #ccc;">
						<div class="thumbText"><c:choose><c:when test="${titleLen <= 72}">${item.title}</c:when><c:otherwise>${fn:substring(item.title, 0, 72)} ...</c:otherwise></c:choose></div>
					</td>
					-->
					<td style="vertical-align:top;border-bottom:1px solid #ccc;">
						<div class="thumbText">${item.initialDate}</div>
					</td>
					<td style="vertical-align:top;border-bottom:1px solid #ccc;">
						<div class="thumbText">${item.processedDate}</div>
					</td>
					<td style="vertical-align:top;border-bottom:1px solid #ccc;text-align:center;">
						<div class="thumbText">${item.processedBy}</div>
					</td>
					<td style="vertical-align:top;border-bottom:1px solid #ccc;;text-align:center;">
						<div class="thumbText">${item.notes}</div>
					</td>
					</tr>
				</c:forEach>
				<tr>
					<td colspan="7" style="border-top:2px solid #B8B8B8;">&nbsp;</td>
				</tr>
			</tbody>
		</table>
		<div class="tab-title" style="text-align:right;display:cell;vertical-align:top;"><c:choose><c:when test="${!model.noAction && (fn:length(model.noActionItems) > 0)}"><a href="statusItems.do?collectionId=${model.collectionId}&processId=${model.processId}&processArk=${model.processArk}&noAction">View items not done</a></c:when><c:when test="${model.noAction && fn:length(model.items)>0}"><a href="statusItems.do?collectionId=${model.collectionId}&processId=${model.processId}&processArk=${model.processArk}">View items done</a></c:when><c:otherwise></c:otherwise></c:choose></div>
		</div>
	</div>
	<div id="message" align="left" class="errorBody">${model.message}</div>
<jsp:include flush="true" page="/jsp/footer.jsp" />
</td>
</tr>
</table>
</body>
</html>

