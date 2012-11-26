<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
<body onLoad="init();" style="background-color:#fff;">
<script type="text/javascript" src="/damsmanager/javascripts/directories.js"></script>
<script type="text/javascript" src="/damsmanager/javascripts/json2.js"></script>
<script type="text/javascript">	
	function confirmUpload(){
		var fileStore = $("#fileStore option:selected").val();
		var filesCount = 0
		var selectedFiles = [];
		var invalidFiles = "";
		
		var selFile = null;
		var dotIdx = -1;
		var fileIdx = -1;
		$("input:checked").each(function(){
			selFile = $(this).val();
			fileIdx = selFile.indexOf("20775-");
			dotIdx = selFile.lastIndexOf(".");
			if(fileIdx> 0 && dotIdx-fileIdx >= 16 && dotIdx >= selFile.length - 4){
				filesCount++;
				selectedFiles.push(encodeURIComponent(selFile));
			}else if(dotIdx > 0 && dotIdx >= selFile.length - 4)
				invalidFiles += selFile + "\t";
		});
		if(invalidFiles.length > 0){
			alert("Found invalid full filename(s): " + invalidFiles + ". Please remove them from the checked list.");
			return;
		}else if(selectedFiles.size <= 0){
			alert("Please check file(s) for ingestion.");
			return;
		}
	    var exeConfirm = confirm("Are you sure to move the selected " + filesCount + " files from development to production?");
	    if(!exeConfirm)
	    	return false;
	    
	    $("#files").val(JSON.stringify(selectedFiles));
    	document.mainForm.action = "/damsmanager/operationHandler.do?devUpload&progress=0&formId=mainForm&fileStore=" + fileStore + "&sid=" + getSid();
    	displayMessage("message", "");
    	getAssignment("mainForm");
		displayProgressBar(0);
	}
	
	function checkboxChange(inputObj){
		if($(inputObj).attr("checked")){
			$(inputObj).parent().parent().parent().find("input").each(function(i){
				if(!$(this).attr("checked"))
					$(this).attr("checked", true);
			});
		}else{
			$(inputObj).parent().parent().parent().find("input").each(function(i){
				if($(this).attr("checked"))
					$(this).attr("checked", false);
			});
		}
		$(inputObj).parent().trigger("click");
		return false;
	}
	
	function init(){
		var status = ${model.status};
		if(status == true){
			setDispStyle("statusDiv", "inline");
			setDispStyle("main", "none");
			httpcall(progressUrl + "?progress&sid=" + getSid(), progress);
			setTimeout("dispTree()", 1000);
		}else{
			dispTree();
		}
	}
	
	function dispTree(){
		var filestore = ${model.dirPaths};		
		var dbo = new Directories(filestore);
		var output = document.getElementById('dirTree');
		dbo.render(output);
	}
</script>
<jsp:include flush="true" page="/jsp/libanner.jsp" />
<table align="center" cellspacing="0px" cellpadding="0px" class="bodytable">
<tr><td>
	
<div id="tdr_crumbs">
	<div id="tdr_crumbs_content">
		<span class="location" style="float:right;"><a href="logout.do?">Log out</a></span><span style="float:right;"><jsp:include flush="true" page="/jsp/menus.jsp" /></span>
		<ul>
			<li><a href="http://libraries.ucsd.edu">Home</a></li>
			<li><a href="/curator">Digital Library Collections</a></li>
			<li><a href="/damsmanager/">XDRE Manager</a></li>
			<li>File Store Upload</li>
		</ul>
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
<div id="mainFormDiv" align="center">
<div class="adjustDiv">
<div style="width:650px;border:1px solid #BDBDBD;">
<div class="paneltitle">Development Upload</div>
<div id="collectionDiv" align="left">
	<span><span class="menuText">FileStore to use:&nbsp;</span>
		<select id="fileStore" name="fileStore" class="inputText">
			<option value=""> -- FileStore -- </option>
			<c:forEach var="entry" items="${model.filestores}">
				<option value="${entry.value}" <c:if test="${model.filestore == entry.value}">selected</c:if>>
                    			<c:out value="${entry.key}" /><c:if test="${model.filestoreDefault == entry.value}"> (default)</c:if>
                     	</option>
			</c:forEach>
		</select>
	</span>
</div>
<div class="emBox_upload">
<div style="margin-top:10px;padding-left:20px;" align="left">
<div class="submenuText" id="dirTree"></div>
</div>
</div>
</div>
</div>
</div>
<form id="mainForm" name="mainForm" method="post" action="/damsmanager/operationHandler.do?devUpload" >
	<input type="hidden" id="files" name="files" value="">
	<div style="text-align:center" >
		<input type="button" name="devUpload" value=" Upload " onClick="confirmUpload();"/>&nbsp;&nbsp;
		<input type="button" name="Cancel" value=" Cancel " onClick="document.location.href='/damsmanager/'"/>
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
<jsp:include flush="true" page="/jsp/popmenus.jsp" />
</html>
