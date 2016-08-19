<%@ page language="java" contentType="text/html; charset=utf-8"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
  
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<head>
<meta content="text/html; charset=utf-8" http-equiv="Content-Type" />
<jsp:include flush="true" page="/jsp/libheader.jsp" />
<script src="//code.jquery.com/jquery-1.10.2.js"></script>
<script src="//code.jquery.com/ui/1.11.1/jquery-ui.js"></script>
<style>
	#fileList ul {list-style: none;}
	#fileList ul li {padding-bottom: 3px;margin-left: -15px; white-space: nowrap; }
	.fname {background-color:#eee;margin-right:5px;width:300px;}
	.fid {width:300px;}
	a {text-decoration:none;}
</style>
</head>
<body style="background-color:#fff;">
<script type="text/javascript">
	function confirmUpload(){
		var message = "";

		var fid = $("#arkFile").val().trim();
		var file = $("#filesPath").val().trim();
		if (fid.length == 0) {
			alert("Please enter an valid ark file name.");
			$("#arkFile").focus();
			return;
		}		
		if (file.length == 0) {
			alert("Please select source file.");
			$("#filesPath").focus();
			return;
		} else {
			var paths = file.split(";");
			if (paths.length > 2 || paths.length > 1 && paths[1] != null && paths[1].trim().length > 0) {
				message = "You've selected more than one files. Please select only one file."
				$("#filesPath").focus();
				return;
			} else {
				file = paths[0].trim();
				$("#filesPath").val(file);
			}
		}
    	var fileExt = file.substring(file.lastIndexOf('.'), file.length);
    	var fidExt = fid.substring(fid.lastIndexOf('.'), fid.length);
		if (fidExt != fileExt){
    		alert("File extensions for the ark file name and the selected source file are not matched.");
    		return;
    	}

	    var message = "Are you sure to replace file " + fid + " with file " + file + " from dams-staging? \n";
	    if(!confirm(message))
	    	return false;
    	
    	document.mainForm.submit();
    	$(".buttonDiv").html("<img src=\"/damsmanager/images/indicator.gif\" />");
	}

	var crumbs = [{"Home":"http://library.ucsd.edu"}, {"Digital Library Collections":"/dc"},{"DAMS Manager":"/damsmanager/"}, {"File Replace":""}];
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
<form id="mainForm" name="mainForm" method="post" action="/damsmanager/fileReplace.do" >
<div class="submenuText" style="text-align:left;padding:30px 0px 2px 130px;">${model.message}</div>
<div class="emBox_ark">
<div class="emBoxBanner">File Replace</div>
<div style="margin-top:10px;padding:30px 50px;height:120px" align="left">
	<table>
		<tr align="left">
			<td height="25px">
				<div class="submenuText" style="padding-left:25px;"><strong>ARK File: </strong></div>
			</td>
			<td  align="left">
				<div class="submenuText" style="margin-top:3px;padding:0px 10px 10px 0px;" title="Please enter an ARK file name or ARK file URL.">
					<input type="text" style="background-color:#eee;" id="arkFile" name="file" size="52" value="${model.file}">
				</div>
			</td>
		</tr>
		<tr align="left">
			<td height="25px">
				<div class="submenuText" style="padding-left:25px;"><strong>File location: </strong></div>
			</td>
			<td  align="left">
				<div class="submenuText" id="modsSpan">
					<div id="fileLocation"><input type="text" id="filesPath" name="filesPath" size="52" value="${model.filesPath}">&nbsp;<input type="button" onclick="showFilePicker('filesPath', event, null, 'files')" value="&nbsp;...&nbsp;"></div>
				</div>
			</td>
		</tr>
	</table>
</div>
<div class="buttonDiv">
	<input type="button" name="edit" value=" Replace " onClick="javascript: confirmUpload();"/>&nbsp;&nbsp;
	<input type="reset" name="clear" value=" Clear " onClick="document.mainForm.reset()"/>
</div>
</div>
</form>
</div>
	<jsp:include flush="true" page="/jsp/status.jsp" />
</td>
</tr>
</table>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
</html>
