<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<c:set var="tsNameLen"> ${fn:length(model.triplestore)}</c:set>  
<c:set var="tsNameFl" scope="page">${fn:substring(model.triplestore, 0, 1)}</c:set> 
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<head>
<jsp:include flush="true" page="/jsp/libheader.jsp" />
<script src="//code.jquery.com/jquery-1.10.2.js"></script>
<script src="//code.jquery.com/ui/1.11.1/jquery-ui.js"></script>
<script type="text/javascript">
	function activateTab(obj){
			if(obj == null) {
				var $links = $('#tabs').find('a');
				obj = $links.filter('[href="' + document.location.hash + '"]')[0] || $links[0];
				//alert(obj);
			}
			
		var aStateTabs = $('.ui-tabs-active');
		var aStateTab = aStateTabs[0];
		// Hide the previous active content
		$(aStateTab).removeClass('ui-tabs-active');
		$(aStateTab).removeClass('ui-state-active');
		$($(aStateTab).children('a').attr('href')).hide();
		
		// Show the tab obj
		$(obj).parent().addClass('ui-tabs-active ui-state-active');
		var $content = $($(obj).attr('href'));
		$content.show();
	}

	function reloadPage(){
		document.location.href="/damsmanager/solrDump.do?ts=dams";
	}
	
	function selectSource(){
		var srcOptions = document.records.source.options;
		var sourceName = srcOptions[source.selectedIndex].value;
		for(var i=0; i<srcOptions.length; i++){
			if(srcOptions[i].value != sourceName)
				$("#" + srcOptions[i].value).hide();
		}
		
		$("#" + sourceName).show();
		if(sourceName == 'text')
			$("#sourceTitle").text("Subjects/ARKs");
		else
			$("#sourceTitle").text("Choose File");
	}
	
	function doSelect(obj){
		var html = $(obj).html();
		$("#collectionListDiv").html($("#collectionListDiv").html());
		if(html === 'Select All'){
			$('input[type="checkbox"]').each(function(){
				if($(this).val() == 'all')
					$(this).attr('checked', false);
				else {
			 		$(this).attr('checked', true);
				}
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
		if(obj.checked){
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
	
	function solrUpdate(formID){
		if (formID == 'records') {
			var srcOptions = document.records.source.options;
			var sourceName = srcOptions[source.selectedIndex].value;
			var inputVal = $("#" + sourceName + "Input").val();
			if (inputVal.trim().length == 0) {
				if (sourceName == 'file')
					alert ("Please choose a file containing the records for SOLR indexing.");
				else
					alert ("Please type in a record or records delimited by comma.");
				$("#" + sourceName + "Input").focus();
				return;
			}
			var formObj = document.getElementById(formID);
		    var exeConfirm = confirm('Are you sure to update the records in ' + inputVal + ' to SOLR? \n');
		    if(exeConfirm){
				formObj.action = "/damsmanager/operationHandler.do?ts=dams&solrRecordsDump&progress=0&formId=records&sid=" + getSid();
		    	displayMessage("message", "");
		    	getAssignment(formID);
				displayProgressBar(0);
			}
		} else {
			 var collections = '';
			 var collectionNames = '';
			 var count = 0;
			 $("#collectionListDiv").find('input[type="checkbox"]:checked').each(function(){
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
		    	document.mainForm.action = "/damsmanager/operationHandler.do?ts=dams&solrDump&progress=0&formId=mainForm&sid=" + getSid();
		    	displayMessage("message", "");
		    	getAssignment("mainForm");
				displayProgressBar(0);
			}
		}
	}
	
	var crumbs = [{"Home":"http://libraries.ucsd.edu"}, {"Digital Library Collections":"/curator"},{"DAMS Manager":"/damsmanager/"}, {"SOLR Dump":""}];
	drawBreadcrumbNMenu(crumbs, "tdr_crumbs_content", true);
	
</script>
<style>
	.ui-widget-content {
		border: 1px solid #ADBCC5;
	}
	.ui-widget-header {
		background: -moz-linear-gradient(center top , #F0FAFF 0%, #E1ECFF 3%, #BCD6E6 97%, #95AFC9 100%) repeat scroll 0% 0% transparent;
	    color: #222;
	    font-weight: bold;
	}

</style>
</head>
<body onload="activateTab()" style="background-color:#fff;">
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
  <div style="font-size:24px;font-weight:bold;color:#336699;margin-bottom:10px;margin-top:10px;">DAMS SOLR Index Utility</div>
  <div id="tabs" class="ui-tabs ui-widget ui-widget-content ui-corner-all">
	<ul class="ui-tabs-nav ui-helper-reset ui-helper-clearfix ui-widget-header ui-corner-all">
		<li class="ui-state-default ui-corner-top"><a href="#recordsTab" class="ui-tabs-anchor" onclick="activateTab(this)">Records</a></li>
		<li class="ui-state-default ui-corner-top ui-tabs-active ui-state-active"><a href="#colsTab" class="ui-tabs-anchor" onclick="activateTab(this)">Collections</a></li>
	</ul>

	<div id="recordsTab" class="ui-tabs-panel ui-widget-content ui-corner-bottom" style="display:none">
	  <form id="records" name="records" method="post" enctype="multipart/form-data">
		<table style="margin-bottom:10px;text-align:left;margin:30px;">
		  <tr>
			<td height="30px" width="150px" align="right">
				<span class="submenuText" style="font-weight:bold;font-size:14px;">Records Source: </span>&nbsp;&nbsp;
			</td>
			<td width="600px">
				<select id="source" name="source" class="inputText" onChange="selectSource(this);">
					<option value="text" selected>Text Input</option>
					<option value="file">File Attachment</option>
				</select>
		    </td>
		  </tr>
		  <tr>
			<td height="30px" align="right">
				<span class="submenuText">
					<span id="sourceTitle" style="font-weight:bold;font-size:14px;">Subjects/ARKs</span><b>: </b>&nbsp;&nbsp;
				</span>
			</td>
			<td  align="left">
				<div class="submenuText">
					<div id="text"><input type="text" id="textInput" name="textInput" size="50" value=""><span class="note"> (Delimiter comma <strong>,</strong>)</span></div>
					<div id="file" style="display:none"><input type="file" id="fileInput" name="fileInput" size="48"></div>
				</div>
			</td>
		  </tr>
	    </table>
	    <div class="buttonDiv">
		  <input type="button" name="buttonSubmit" value="Submit" onClick="solrUpdate('records');"/>
	    </div>
	  </form>
	</div>

	<div id="colsTab" class="ui-tabs-panel ui-widget-content ui-corner-bottom">
	<form id="mainForm" name="mainForm" method="post" action="/damsmanager/operationHandler.do?solrDump" >
	<div style="margin-bottom:10px;text-align:left;color:#000;">
	<div class="submenuText" style="padding-bottom:5px;">
		<a style="float:right;cursor:pointer;color:#336699;width:120px;font-weight:bold;" onClick="javascript:doSelect(this);" id="selectAll">Select All</a>
	</div>
	<div id="collectionListDiv">
	<table style="margin-bottom:10px;text-align:left;">
		<tr>
			<td valign="top" colspan="2">
				<div class="colList">
					<ul style="padding: 0;margin: 0;list-style: none;line-height: 1;">
						<li>
							<input type="checkbox" name="all" value="all" onclick="doSelectAllRecords(this)"><span id="${item.subject}" class="listTitle">All Records</span>
						</li>
					</ul>
				</div>
			</td>
		</tr>
		<tr>
			<td valign="top">
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
			<td valign="top">
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
    </div>
    <div class="buttonDiv">
	  <input type="hidden" id="category" name="category" value="" />
	  <input type="button" name="update" value="SOLR Update" onClick="solrUpdate('mainForm');"/>&nbsp;&nbsp;
    </div>
    </form>	
    </div>
  </div>
</div>
	<jsp:include flush="true" page="/jsp/status.jsp" />
	<div id="message" class="submenuText" style="text-align:left;">${model.message}</div>
</td>
</tr>
</table>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
</html>
