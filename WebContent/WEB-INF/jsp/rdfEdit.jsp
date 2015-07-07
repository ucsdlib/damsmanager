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
</style>
</head>
<body style="background-color:#fff;">
<script type="text/javascript">
	function validate(){
		var message = "";
		var content = $("#data").val().trim();
		if (content.length == 0) {		
        	alert("No RDF content!");
        	$("#data").focus();
        	return false;
        } else {

			message = "Are you sure that you want to edit the record?";
		    var exeConfirm = confirm(message);
		    if(!exeConfirm)
		    	return false;
		    
		    document.getElementById("mainForm").submit();
        }
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
<form id="mainForm" name="mainForm" method="post" accept-charset="utf-8" action="/damsmanager/metadataImport.do" onSubmit="return validate()">
<div style="width:880px;padding-top:10px;color:red;text-align:left;"><c:out value="${model.message}" /></div>
<div class="menuText" style="text-align:left;padding:30px 0px 2px 40px;">Edit <c:out value="${model.title}" /></div>
<div class="emBox_edit">
<div style="margin-top:10px;padding-left:20px;min-height:300px" align="left">
	<table>
		<tr align="left">
			<td height="25px">
			    <label class="submenuText" for="dataFormat"><b>Format</b></label>
				<div class="submenuText" style="margin-top:3px;padding:0px 10px 10px 0px;">
					<select name="format" id="format"  style="width: 200px;">
						<c:forEach var="entry" items="${model.formats}">
							<option value="${entry.value}" <c:if test="${model.format == entry.value}">selected</c:if>>
                       			<c:out value="${entry.key}" />
                        	</option>
						</c:forEach>
					</select>
				</div>
			</td>
		</tr>
		<tr align="left">
			<td>
			    <label class="submenuText" for="data"><b>RDF XML</b></label>
				<div class="submenuText" style="margin-top:3px;padding:0px 10px 10px 0px;">
					<textarea rows="40" style="width:830px" name="data" id="data">
						<c:out value="${model.rdf}"/>
					</textarea>
				</div>
			</td>
		</tr>
	</table>
</div>
<div class="buttonDiv">
	<input type="hidden" name="ark" value="${model.ark}"/>
	<input type="hidden" name="importMode" value="${model.importMode}"/>
	<input type="submit" name="submit" value=" Submit "/>&nbsp;&nbsp;
	<input type="button" name="cancel" value=" Cancel " onClick="javascript: document.location.href='/damsmanager/rdfImport.do';"/>
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
