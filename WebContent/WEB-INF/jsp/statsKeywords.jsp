<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
<body onLoad="initCalendarForm()" style="background-color:#fff;">
<script type="text/javascript" src="/damsmanager/javascripts/ui.datepicker.js"></script>
<script language="javaScript">
	function showPage(type, page){
		var startDate = $("#start").val();
		var endDate = $("#end").val();
		var parameters = "page=" + page + "&type=" + type + "&inputSearch=" + encodeURIComponent($("#preSearch").val()) + "&start=" + startDate + "&end=" + endDate;
		var url = "/damsmanager/statsKeywords.do";
		$.ajax({
			url: url,
			dataType: "text/plain",
			data: parameters,
			success: function(cdata){
				var jsondata = eval('(' + cdata + ')');
				$("#message").html(jsondata["message"]);
				var keywordsHtml = "";
				var dispVal = "";
					
				if(type == 'keyword'){
					$("#kPagingPre").unbind();
					$("#kPagingNext").unbind();
					$("#kPagingPre").empty().html("Previous");
					$("#kPagingNext").empty().html("Next");
					$("#kPagingPre").removeClass("paging pagingNone");
					$("#kPagingNext").removeClass("paging pagingNone");
					var keywords = jsondata["keywords"];
					var page = jsondata["page"];
					var pageNum = parseInt(page);
					var moreKeywords = jsondata["moreKeywords"];
					if(page > 1){
					 	$("#kPagingPre").click(function(){showPage('keyword',pageNum-1);});
					 	$("#kPagingPre").addClass("paging");
					 }else
					 	$("#kPagingPre").addClass("pagingNone");
					if(moreKeywords != null && moreKeywords == true){
					 	$("#kPagingNext").click(function(){showPage('keyword',pageNum+1);});
					 	$("#kPagingNext").addClass("paging");
					 }else
					 	$("#kPagingNext").addClass("pagingNone");
					
					var count = 0;
					for (var propName in keywords) {
						count++;
						dispVal = wrapWords(propName, 35);
						keywordsHtml += "<li>" + dispVal + " (" + keywords[propName] + ")</li>";
					}
					var resulrText = "<div class=\"resultText\">" + ((page-1)*20+1) + " to " + ((page-1)*20+count) + " of " + jsondata["keywordsTotal"] + ": </div>";		
					keywordsHtml = resulrText + "<ul>" + keywordsHtml + "</ul>";
					$("#keywordsBlock").html(keywordsHtml);
				}
				
				if(type == 'phrase'){
					$("#pPagingPre").unbind();
					$("#pPagingNext").unbind();
					$("#pPagingPre").empty().html("Previous");
					$("#pPagingNext").empty().html("Next");
					$("#pPagingPre").removeClass("paging pagingNone");
					$("#pPagingNext").removeClass("paging pagingNone");
					var keywords = jsondata["phrases"];
					var page = jsondata["page"];
					var pageNum = parseInt(page);
					var moreKeywords = jsondata["morePhrases"];
					if(page > 1){
					 	$("#pPagingPre").click(function(){showPage('phrase',pageNum-1);});
						$("#pPagingPre").addClass("paging");
					 }else
					 	$("#pPagingPre").addClass("pagingNone");
					if(moreKeywords != null && moreKeywords == true){
					 	$("#pPagingNext").click(function(){showPage('phrase',pageNum+1);});
						$("#pPagingNext").addClass("paging");
					 }else
					 	$("#pPagingNext").addClass("pagingNone");
					
					var count = 0;
					for (var propName in keywords) {
						count++;
						dispVal = wrapWords(propName, 35);
						keywordsHtml += "<li>" + dispVal + " (" + keywords[propName] + ")</li>";
					}
					var resulrText = "<div class=\"resultText\">" + ((page-1)*20+1) + " to " + ((page-1)*20+count) + " of " + jsondata["phrasesTotal"] + ": </div>";	
					keywordsHtml =  resulrText + "<ul>" + keywordsHtml + "</ul>";
					$("#phrasesBlock").html(keywordsHtml);
				}
			},
			error: function(xhr, desc, exceptionobj){
				if(exceptionobj){
					throw exceptionobj;
				}else if(desc){
					throw desc;
				}else{
					throw "unknown error occurred during ajax request to URL: "+url+" with parameters"+parameters;
				}
			}
		});
	}
	
	function searchKeywords(){
		var keywords = trim($("#inputSearch").val());
		if(keywords.length == 0)
			keywords = "";
		else
			keywords = "inputSearch=" + encodeURIComponent(eval('('+JSON.stringify(keywords)+')'));
		var url = "/damsmanager/statsKeywords.do?" + keywords;
		document.location.href = url;
	}
	
	function wrapWords(val, maxCount){
		var dispVal = "";
		var idx0 = -1;
		var idx1 = -1;
		val = eval('('+JSON.stringify(val)+')');
		var len = val.length;
		while(len>maxCount){
			idx0 = val.indexOf(" ");
			idx1 = val.indexOf("/");
			if((idx0 < 0 || idx0 > maxCount) && (idx1 < 0 || idx1 > maxCount)){
				dispVal += val.substring(0,maxCount) + " ";
				val = val.substring(maxCount);
			}else{
				if(idx0 > idx1){
					if(idx1 >= 0){
						dispVal += val.substring(0,idx1+1);
						val = val.substring(idx1+1);
					}else{
						dispVal += val.substring(0,idx0+1);
						val = val.substring(idx0+1);
					}
				}else{
					if(idx0 > 0){
						dispVal += val.substring(0,idx0 + 1);
						val = val.substring(idx0 + 1);
					}else{
						dispVal += val.substring(0,idx1 + 1);
						val = val.substring(idx1 + 1);
					}
				}
			}
			len = val.length;
		}
		dispVal += val;
		return dispVal;
	}
	
	function initCalendarForm(){
		$("#start,#end").datepicker({
			beforeShow: customRange,
			dateFormat: 'yymmdd',
			showOn: "both",
			buttonImage: "/damsmanager/images/calendar.jpg",
			buttonImageOnly: true
		});
		//current range req: max date is the current date
		function customRange(input) { 
			return {minDate: (input.id == "end" ? $("#start").datepicker("getDate") : null), 
				maxDate: new Date()}; 
		}
	}
	
	function clearForm(formId){
		var form = document.getElementById?document.getElementById(formId):document.forms[formId];
		var elements, elm;
		if(document.getElementsByTagName){
			elements = form.getElementsByTagName("input");
		}else{
			elements = form.elements();
		}
		for( i=0, elm; elm=elements[i++]; ){
			if (elm.type == "text")
			{
				elm.value ='';
			}
		}
	}
</script>
<jsp:include flush="true" page="/jsp/libanner.jsp" />
<table align="center" cellspacing="0px" cellpadding="0px" class="bodytable">
<tr><td>
	
<div id="tdr_crumbs">
	<div id="tdr_crumbs_content">
		<ul>
			<li><a href="http://libraries.ucsd.edu">Home</a></li>
			<li><a href="/dc">Digital Library Collections</a></li>
			<li><a href="/damsmanager/stats.do">DAMS Statistics</a></li>
			<li>Keywords</li>
		</ul>
	</div><!-- /tdr_crumbs_content -->
	
	<!-- This div is for temporarily writing breadcrumbs to for processing purposes -->
		<div id="temporaryBreadcrumb" style="display: none">
	</div>
</div><!-- /tdr_crumbs -->
<div id="main" class="gallery" align="center">
	<div class="main-title">UCSD Libraries Digital Asset Management System Statistics</div>
	<div class="tab-container">
		<div class="tab-title">DAMS Statistics Keywords/Phrases</div>
		<table cellspacing=0 cellpadding=3 border=0 width=100%>
			<tbody style="background-color:#f0f0f0;">
				<tr style="padding:10px;">
					<td style="text-align:left;height:40px;border-bottom:2px solid #fff;" colspan="2">
						<form name="mainForm" id="mainForm">
							<div style="padding-top:20px;padding-bottom:2px;margin-left:10px;" title="Search the usage of keyword(s) or phase(s) in the input box. Leave the input box empty to browse for all keywords/phases."><input type="text" size="40" id="inputSearch" name="inputSearch" value="${model.searchKeyword}" />&nbsp;<button id="searchButton" onclick="searchKeywords();">&nbsp;</button><input type="hidden" size="40" id="preSearch" name="preSearch" value="${model.searchKeyword}" /></div>
							<div style="padding-bottom:10px;margin-left:10px;"><span class="resultText">Period: </span><input type="text" size="10" value='<c:if test="${model.startDate != null}">${model.startDate}</c:if>' id="start" name="start"/> to <input type="text" size="10" value='<c:if test="${model.endDate != null}">${model.endDate}</c:if>' id="end" name="end"/>&nbsp;<input type="button" value="Reset" class="resetButton" onclick="clearForm('mainForm');"/></div>
						</form>
					</td>
				</tr>
				<tr style="padding:10px;">
					<td width="50%" valign="top">
						<div style="margin-left:10px;margin-bottom:15px;">
							<div id="keywordsPaging" style="text-align:right;"><span class="pagingAngle">&lt;</span> <span id="kPagingPre"><span <c:choose><c:when test="${model.page>1}">class="paging" onclick="showPage('keyword', ${model.page-1})"</c:when><c:otherwise>class="pagingNone"</c:otherwise></c:choose>>Previous</span></span> &nbsp; <span id="kPagingNext"><span <c:choose><c:when test="${model.moreKeywords==true}">class="paging" onClick="showPage('keyword', ${model.page+1})"</c:when><c:otherwise>class="pagingNone"</c:otherwise></c:choose>>Next</span></span> <span class="pagingAngle" style="margin-right:5px;">&gt;</span></div>
							<fieldset><legend class="legend">Keywords</legend>
								<div style="height:380px" id="keywordsBlock">
									<c:set var="keywordSize">${fn:length(model.keywords)}</c:set>
									<c:choose>
										<c:when test="${model.keywords == null || keywordSize==0}">
											<div class="resultText">No results found.</div>
										</c:when>
										<c:otherwise>
										<div class="resultText">${(model.page-1)*20+1} to ${(model.page-1)*20+keywordSize} of ${model.keywordsTotal} results:</div>
										<ul>
											<c:forEach var="item" items="${model.keywords}">
												<c:set var="keyLen">${fn:length(item.keyword)}</c:set>
												<c:set var="spaceIndex">${fn:indexOf(item.keyword, ' ')}</c:set>
												<li><c:choose><c:when test="$keyLen&gt;30 && ($spaceIndex&lt;0 || $spaceIndex&gt;30)">${fn:substring(item.keyword, 0, 30)}&nbsp;${fn:substring(item.keyword, 30, keyLen)}</c:when><c:otherwise>${item.keyword}</c:otherwise></c:choose> (${item.numOfUsage})</li>
											</c:forEach>
										</ul>
										</c:otherwise>
									</c:choose>
								</div>
							</fieldset>
						</div>
					</td>
					<td width="50%" valign="top">
						<div style="margin-right:10px;margin-bottom:15px;">
							<div id="phrasePaging" style="text-align:right;"><span class="pagingAngle">&lt;</span> <span id="pPagingPre"><span <c:choose><c:when test="${model.page>1}">class="paging" onclick="showPage('phrase', ${model.page-1})"</c:when><c:otherwise>class="pagingNone"</c:otherwise></c:choose>>Previous</span></span> &nbsp; <span id="pPagingNext"><span <c:choose><c:when test="${model.morePhrases==true}">class="paging" onClick="showPage('phrase', ${model.page+1})"</c:when><c:otherwise>class="pagingNone"</c:otherwise></c:choose>>Next</span></span> <span class="pagingAngle" style="margin-right:5px;">&gt;</span></div>
							<fieldset><legend class="legend">Phrase</legend>
								<div style="height:380px" id="phrasesBlock">
									<c:set var="phraseSize">${fn:length(model.phrases)}</c:set>
									<c:choose>
										<c:when test="${model.phrases == null || phraseSize == 0}">
											<div class="resultText">No results found.</div>
										</c:when>
										<c:otherwise>
										<div class="resultText">${(model.page-1)*20+1} to ${(model.page-1)*20+phraseSize} of ${model.phrasesTotal} results:</div>
										<ul>
											<c:forEach var="item" items="${model.phrases}">
												<c:set var="keyLen">${fn:length(item.keyword)}</c:set>
												<c:set var="spaceIndex">${fn:indexOf(item.keyword,' ')}</c:set>
												<li><c:choose><c:when test="$keyLen&gt;30 && ($spaceIndex&lt;0 || $spaceIndex&gt;30)">${fn:substring(item.keyword, 0, 30)}&nbsp;${fn:substring(item.keyword, 30, keyLen)}</c:when><c:otherwise>${item.keyword}</c:otherwise></c:choose> (${item.numOfUsage})</li>
											</c:forEach>
										</ul>
										</c:otherwise>
									</c:choose>
								</div>
							</fieldset>
						</div>
					</td>
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
