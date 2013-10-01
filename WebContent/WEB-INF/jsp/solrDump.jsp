<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<c:set var="tsNameLen"> ${fn:length(model.triplestore)}</c:set>  
<c:set var="tsNameFl" scope="page">${fn:substring(model.triplestore, 0, 1)}</c:set> 
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
<body style="background-color:#fff;">
<script type="text/javascript">

	function reloadPage(){
		var dsIdx = document.mainForm.ts.selectedIndex;
		var ds = document.mainForm.ts.options[dsIdx].value;
		document.location.href="/damsmanager/solrDump.do?ts=" + ds;
	}
	
	function doSelect(obj){
		var html = $(obj).html();
		if(html === 'Select All'){
			$('input[type="checkbox"]').each(function(){
				if($(this).val() == 'all')
					$(this).attr('checked', false);
				else
			 		$(this).attr('checked', true);
				$(this).attr('disabled', false);
			 });
			 $(obj).html('Clear All');
		}else{
			$('input[type="checkbox"]:checked').each(function(){
				$(this).attr('checked', false);
			 });
			 $(obj).html('Select All');
		}
	}
	
	function doSelectAllRecords(obj){
		if($(obj).attr('checked')==true){
			$('input[type="checkbox"]').each(function(){
				if($(this).val() != 'all'){
			 		$(this).attr('checked', false);
					$(this).attr('disabled', true);
				}
			 });
			$('#selectAll').html('Select All');
		}else{
			$('input[type="checkbox"]').each(function(){
				$(this).attr('disabled', false);
			 });
		}
	}
	
	function solrUpdate(){
		 var collections = '';
		 var collectionNames = '';
		 var count = 0;
		 $('input[type="checkbox"]:checked').each(function(){
		 	count++;
		 	collections += $(this).val() + ',';
		 	collectionNames += $(this).next().html() + '\n';
		 });
		if(collections.length===0){
			alert('Please check a collection for SOLR update!');
			return false;
		}
	    var exeConfirm = confirm('Are you sure to update SOLR for the following ' + count + ' collections? \n' + collectionNames);
	    if(exeConfirm){
		    mainForm.category.value = collections;
	    	document.mainForm.action = "/damsmanager/operationHandler.do?solrDump&progress=0&formId=mainForm&sid=" + getSid();
	    	displayMessage("message", "");
	    	getAssignment("mainForm");
			displayProgressBar(0);
		}
	}
	
	var crumbs = [{"Home":"http://libraries.ucsd.edu"}, {"Digital Library Collections":"/curator"},{"DAMS Manager":"/damsmanager/"}, {"SOLR Dump":""}];
	drawBreadcrumbNMenu(crumbs, "tdr_crumbs_content", true);
	
</script>
<jsp:include flush="true" page="/jsp/libanner.jsp" />
<table align="center" cellspacing="0px" cellpadding="0px" class="bodytable">
<tr><td>
	
<div id="tdr_crumbs">
	<div id="tdr_crumbs_content">
	</div><!-- /tdr_crumbs_content -->
	
	<!-- This div is for temporarily writing breadcrumbs to for processing purposes -->
		<div id="temporaryBreadcrumb" style="display: none">
	</div>
</div><!-- /tdr_crumbs -->
</td>
</tr>
<tr>
<td align="center">
<div id="main" class="gallery" align="center">
	<form id="mainForm" name="mainForm" method="post" action="/damsmanager/operationHandler.do?solrDump" >
	<div style="font-size:24px;font-weight:bold;color:#336699;margin-bottom:10px;margin-top:10px;">DLP Collections SOLR Dump Utility</div>
	<div style="margin-bottom:10px;text-align:left;width:900px;color:#000;">
	<div class="submenuText" style="padding-bottom:5px;">
		<span>Please choose a Triplestore: </span>
		<span id="dsSelectSpan" >
			<select name="ts" id="ts" onChange="reloadPage();"><option value=""> -- Triplestore -- </option>
				<c:forEach var="entry" items="${model.triplestores}">
					<option value="${entry}" <c:if test="${model.triplestore == entry}">selected</c:if>>
                     			<c:out value="${entry}" />
                      	</option>
				</c:forEach>
			</select>&nbsp;
		</span>
		<a style="float:right;cursor:pointer;" onClick="javascript:doSelect(this);" id="selectAll">Select All</a>
	</div>
	<div id="collectionListDiv">
	<table style="margin-bottom:10px;text-align:left;">
		<tr>
			<td valign="top" colspan="2">
				<div class="colList">
					<ul style="padding: 0;margin: 0;list-style: none;line-height: 1;">
						<li>
							<input type="checkbox" onClick="doSelectAllRecords(this)" name="all" value="all"/><span id="${item.subject}" class="listTitle">All Records</span>
						</li>
					</ul>
				</div>
			</td>
		</tr>
		<tr>
			<td valign="top" width="450px">
				<div class="colList">
					<ul style="padding: 0;margin: 0;list-style: none;line-height: 1.2;">
						<c:forEach var="item" items="${model.collections}" varStatus="status">
							<c:if test="${(status.count-1)%2==0}">
								<li><input type="checkbox" name="collectionId" value="${item.subject}"/><span id="${item.subject}" class="listTitle">${item.title}</span>
									<c:if test="${item.count!='0'}"><span title="Records found in SOLR: ${item.count}" style="cursor:pointer;color:gray;"><span class="listTitle">(</span><span class="count">${item.count}</span><span class="listTitle">)</span></span></c:if>
								</li>
							</c:if>
						</c:forEach>
					</ul>
				</div>
			</td>
			<td valign="top" width="450px">
				<div class="colList">
					<ul style="padding: 0;margin: 0;list-style: none;line-height: 1.2;">
						<c:forEach var="item" items="${model.collections}" varStatus="status">
							<c:if test="${(status.count-1)%2!=0}">
								<li><input type="checkbox" name="collectionId" value="${item.subject}"/><span id="${item.subject}" class="listTitle">${item.title}</span>
									<c:if test="${item.count!='0'}"><span title="Records found in SOLR: ${item.count}" style="cursor:pointer;color:gray;"><span class="listTitle">(</span><span class="count">${item.count}</span><span class="listTitle">)</span></span></c:if>
								</li>
							</c:if>
						</c:forEach>
					</ul>
				</div>
			</td>
		</tr>
	</table>
	</div>
<div class="buttonDiv">
	<input type="hidden" id="category" name="category" value="" />
	<input type="button" name="update" value="SOLR Update" onClick="solrUpdate();"/>&nbsp;&nbsp;
</div>
</div>
</form>
</div>
	<jsp:include flush="true" page="/jsp/status.jsp" />
	<div id="messageDiv">
		<div id="message" align="left" class="errorBody">${model.message}</div>
	</div>
</td>
</tr>
</table>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
</html>
