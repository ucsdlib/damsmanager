<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
  
<c:set var="tsNameFl" scope="page">${fn:substring(model.triplestore, 0, 1)}</c:set> 
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<head>
<jsp:include flush="true" page="/jsp/libheader.jsp" />
<script src="//code.jquery.com/jquery-1.10.2.js"></script>
<script src="//code.jquery.com/ui/1.11.1/jquery-ui.js"></script>
<style>
	#fileList ul {list-style: none;}
	#fileList ul li {padding-bottom: 3px;margin-left: -15px; white-space: nowrap; }
	.fname {background-color:#eee;margin-right:5px;width:300px;}
	.fid {width:300px;}
</style>
</head>
<body style="background-color:#fff;">
<script type="text/javascript">
	function confirmUpload(){
		var message = "";
		var fidObj = null;
		
        $(".fid").each (function() {
        	var fid = trim($(this).val());
        	var currId = $(this).attr('id');
        	var file = $("#" + currId.replace("fid-", "f-")).val();
        	var fileExt = file.substring(file.lastIndexOf('.'), file.length);
        	var fidExt = fid.substring(fid.lastIndexOf('.'), fid.length);
        	if (fid.length == 0 || !fid.indexOf('http://library.ucsd.edu/ark:/20775/') == 0) {
        		if (fidObj == null)
        			fidObj = this;
        		message += file + "\n";
        	} else if (fidExt != fileExt){
        		if (fidObj == null)
        			fidObj = this;
        		message += file + "(Wrong file extension " + fid + ") \n";
        	}
        });

        if (message.length > 0) {
        	alert("Please enter a valid file URL for the following file(s): \n" + message);
        	fidObj.focus();
        	return false;
        }
	    var message = "Are you sure to upload files from dams-staging? \n";
	    var exeConfirm = confirm(message);
	    if(!exeConfirm)
	    	return false;
    	
    	document.mainForm.action = "/damsmanager/operationHandler.do?fileUpload&progress=0&formId=mainForm&sid=" + getSid();
    	displayMessage("message", "");
    	getAssignment("mainForm");
		displayProgressBar(0);
	}

	function reloadPage(inputID){
		var filesPath = document.getElementById(inputID).value; 
		document.location.href = "/damsmanager/fileUpload.do?filesPath=" + encodeURIComponent(filesPath);
	}

	var crumbs = [{"Home":"http://library.ucsd.edu"}, {"Digital Library Collections":"/dc"},{"DAMS Manager":"/damsmanager/"}, {"File Upload":""}];
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
<div id="main" class="mainDiv">
<form id="mainForm" name="mainForm" method="post" action="/damsmanager/operationHandler.do?fileUpload" >
<div class="emBox_ark">
<div class="emBoxBanner">File Upload</div>
<div style="margin-top:10px;padding-left:20px;min-height:300px" align="left">
	<table width="600px">
		<tr align="left">
			<td height="25px">
				<div class="submenuText" style="padding-left:25px;"><strong>Files Location: </strong></div>
			</td>
			<td  align="left">
				<div class="submenuText" style="margin-top:3px;padding:0px 10px 10px 25px;" title="Open the file chooser on the right. From the popup, click on the folder to select/deselect a location. Multiple loations allowed.">
					<input type="text" style="background-color:#eee;" id="filesPath" name="filesPath" size="52" value="${model.filesPath}" readOnly="readOnly">&nbsp;<input type="button" onclick="showFilePicker('filesPath', event, reloadPage)" value="&nbsp;...&nbsp;">
				</div>
			</td>
		</tr>
		<tr align="left">
			<td height="25px" colspan="2">
			<div id="fileList">
				<ul>
					<c:forEach var="file" items="${model.files}" varStatus="status">
						<c:if test="${status.count == 1}">
							<li><input type="text" style="border:none;margin-right:5px;width:300px;background:#F8F8F8" readOnly="readOnly" value="File Path: "><input type="text" style="border:none;width:300px;background:#F8F8F8" readOnly="readOnly" value="File URI: "></li>
						</c:if>
						<li><input type="text" id="f-${status.count}" class="fname" name="f-${status.count}" value="${file}" readOnly="readOnly"><input type="text" class="fid" id="fid-${status.count}" name="fid-${status.count}" value="" ></li>
					</c:forEach>
				</ul>
			</div>
			</td>
		</tr>
	</table>
</div>
<div class="buttonDiv">
	<input type="button" name="fileUpload" value=" Upload " onClick="confirmUpload();"/>&nbsp;&nbsp;
	<input type="button" name="fileUpload" value=" Cancel " onClick="document.location.href='/damsmanager/fileUpload.do'"/>
</div>
</div>
</form>
</div>
	<jsp:include flush="true" page="/jsp/status.jsp" />
	<div id="message" class="submenuText" style="text-align:left;">${model.message}</div>
</td>
</tr>
</table>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
</html>
