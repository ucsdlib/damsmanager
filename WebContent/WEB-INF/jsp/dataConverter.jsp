<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
<body style="background-color:#fff;">
<script type="text/javascript">
	function checkSourceFile(){
		var filePath = document.formSaemFile.saemFilePath.value;
		if(filePath == null || filePath.length == 0){
			alert("Please choose a source file. ");
			return;
		}
		var fileOptions = document.formSaemFile.fileType;
		var fileType = "excel";
		if(fileOptions[1].checked == true)
			fileType = "msclXml";

		//var formAction = document.formSaemFile.action;
		document.formSaemFile.action = "/damsmanager/sourceUpload.do?" + "fileType=" + fileType;
		document.formSaemFile.submit();
	}
</script>
<jsp:include flush="true" page="/jsp/libanner.jsp" />
<table align="center" cellspacing="0px" cellpadding="0px" class="bodytable">
<tr><td>
	
<div id="tdr_crumbs">
	<div id="tdr_crumbs_content">
		<span style="float:right;"><jsp:include flush="true" page="/jsp/menus.jsp" /></span>
		<ul>
			<li><a href="http://libraries.ucsd.edu">Home</a></li>
			<li><a href="/curator">Digital Library Collections</a></li>
			<li><a href="/damsmanager/">XDRE Manager</a></li>
			<li>Data Converter</li>
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
<div class="statusText">${data.message}</div>
<div id="saemFileDiv" align="center">
<form id="formSaemFile" name="formSaemFile" method="post" enctype="multipart/form-data" action="/damsmanager/sourceUpload.do">
<div class="adjustDiv"  align="center">
<div class="emBox">
<div class="emBoxBanner">Metadata Converter</div>
<div id="fileFormat"  class="emBoxLabelDiv">
		<span><strong>Please choose a source format: </strong></span><br/>
		<span class="submenuText"><input type="radio" name="fileType" value="excel" checked><a title="View sample format" href="/damsmanager/files/sampleExcel.xls">Excel</a></span>
		<span class="submenuText"><input type="radio" name="fileType" value="msclXml"><a title="View sample format" href="/damsmanager/files/sampleMscl.xml" target="_blank">MSCL XML</a></span>
		<span class="submenuText"><input type="radio" name="fileType" value="json" disabled>JSON</span><br>
</div>
<div class="emBoxLabelDiv"><strong>Please select the source file:</strong></div>
<div class="fileInputDiv"><input type="file" name="saemFilePath" size="40"></div>
<div class="buttonDiv" style="padding-top:30px">
	<input type="button" name="saemFileButton" value=" Continue " onClick="checkSourceFile();"/>
</div>
</div>
</div>
</form>
</div>
</div>
</td>
</tr>
</table>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
<jsp:include flush="true" page="/jsp/popmenus.jsp" />
</html>
