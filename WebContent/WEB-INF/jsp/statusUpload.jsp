<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
    <%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
 
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
<body style="background-color:#fff;">
<script type="text/javascript">
	function submitStatusFile(){

	    var formFileObj = document.mainForm;
	   
	    var fileName = formFileObj.filePath.value;
	    if(fileName == null || trim(fileName).length == 0){
	       alert("Please choose a file.");
	       return false;      
	    }/*else{
	    	var idx = fileName.indexOf('.csv');
	    	if(fileName.indexOf('.') > 0 && (idx <=0 || idx + 5 != fileName.length)){
	    		alert("Please select the original Merritt status csv file.");
	       		return false;
	    	}
	    }*/
	    formFileObj.submit();	    
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
			<li>Merritt</li>
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
<div id="saemFileDiv" align="center">
<form id="mainForm" name="mainForm" method="post" enctype="multipart/form-data" action="/damsmanager/statusUpload.do">
<div class="adjustDiv"  align="center">
<div class="emBox">
<div class="emBoxBanner">Merritt Status Upload</div>
<div class="emBoxLabelDiv">Please select the status csv file:</div>
<div class="fileInputDiv"><input type="file" name="filePath" size="40"></div>
<div class="buttonDiv">
	<input type="button" name="fileButton" value=" Submit " onClick="submitStatusFile();"/>
</div>
</div>
</div>
</form>
</div>
	<div id="messageDiv">
		<div id="message" align="left" class="errorBody">${model.message}</div>
	</div>
</td>
</tr>
</table>
</div>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
<jsp:include flush="true" page="/jsp/popmenus.jsp" />
</html>