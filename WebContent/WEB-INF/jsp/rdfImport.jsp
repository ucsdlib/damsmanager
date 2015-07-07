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
	function validate(){
		var message = "";
		var ark = $("#ark").val().trim();
		if (ark.length == 0)
			message = "Please enter an ARK.";
		else {
			var idx = ark.lastIndexOf("/");
			if (idx >= 0)
				ark = ark.substring (idx + 1);
			if (ark.length != 10)
				message = "Please enter a valid ark or ark URL."
		}

		if (message.length > 0) {
        	alert("Please enter a valid file URL for the following file(s): \n" + message);
        	$("#ark").focus();
        	return false;
        }

		document.mainForm.submit();
	}

	var crumbs = [{"Home":"http://library.ucsd.edu"}, {"Digital Library Collections":"/dc"},{"DAMS Manager":"/damsmanager/"}, {"RDF Import":""}];
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
<form id="mainForm" name="mainForm" method="post" accept-charset="utf-8" action="/damsmanager/rdfImport.do" >
<div class="submenuText" style="text-align:left;padding:30px 0px 2px 130px;">${model.message}</div>
<div class="emBox_ark">
<div class="emBoxBanner">RDF Import</div>
<div style="margin-top:10px;padding:50px;height:200px" align="left">
	<table>
		<tr align="left">
			<td height="25px">
				<div class="submenuText" style="padding-left:25px;"><strong>ARK: </strong></div>
			</td>
			<td  align="left">
				<div class="submenuText" style="margin-top:3px;padding:0px 10px 10px 0px;" title="Please enter an ARK or ARK URL.">
					<input type="text" style="background-color:#eee;" id="ark" name="ark" size="52" value="">
				</div>
			</td>
		</tr>
	</table>
</div>
<div class="buttonDiv">
	<input type="button" name="edit" value=" Edit " onClick="javascript: validate();"/>&nbsp;&nbsp;
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
