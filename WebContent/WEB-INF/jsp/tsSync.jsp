<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
<body onLoad="load('tsSync');" style="background-color:#fff;">
<script type="text/javascript">	
	function confirmOperation(){
		var message = "";
		var tsSrc = mainForm.ds;
		var tsDest = mainForm.dsDest;
		if(tsSrc.selectedIndex == 0)
			message = "Source tripleStore required. \n";
		if(tsDest.selectedIndex == 0)
			message += "Destination tripleStore required. \n";
		if(message.length > 0){
			alert(message);
			return false;
		}else if (tsSrc.value == tsDest.value){
			alert("Please choose different Source Triplestore and Destination Triplestore.");
			return false;
		}
		var OpOptions = mainForm.tsSyncOption;
		if(OpOptions[1].checked)
			message = "rebuild triplestore " + tsDest.value + " with metadata in " + tsSrc.value;
		else
			message = "sync metadata in triplestore " + tsSrc.value + " to triplestore " + tsDest.value;
	    var exeConfirm = confirm("Are you sure to " + message + "?");
	    if(!exeConfirm)
	    	return false;
	    
    	document.mainForm.action = url + "?tsSyn&progress=0&formId=mainForm&sid=" + getSid();
    	displayMessage("message", "");
    	getAssignment("mainForm");
		displayProgressBar(0);
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
			<li>TripleStore Sync</li>
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
<form id="mainForm" name="mainForm" method="post" action="/damsmanager/operationHandler.do?tsSync" >
<div class="adjustDiv"  align="center">
<div class="emBox">
<div class="emBoxBanner">TripleStore Synchronizer</div>

<div style="margin-top:20px;margin-left:50px;" align="left">
	<div class="label"><strong>Please choose source/destination TripleStores:</strong></div>
	 <div class="label" style="margin-top:10px;margin-bottom:5px;margin-left:25px;">
	 	<span class="menuText">Source: </span>
	 	<select name="ds" id="ds"><option value=""> -- TripleStore -- </option>
			<c:forEach var="entry" items="${model.triplestores}">
				<option value="${entry.value}" <c:if test="${model.triplestore == entry.key}">selected</c:if>>
                	<c:out value="${entry.key}" />
                </option>
			</c:forEach>
	 	</select>
	 	<span class="menuText" style="margin-left:20px;">Destination: </span>
	 	<select name="dsDest" id="dsDest"><option value=""> -- TripleStore -- </option>
			<c:forEach var="entry" items="${model.triplestores}">
				<c:if test="${fn:indexOf(entry.key,'ts/') == 0}">
					<option value="${entry.value}">
	                    <c:out value="${entry.key}" />
	                </option>
                </c:if>
			</c:forEach>
	 	</select>
	 </div>
	 <fieldset class="groupbox_ingestOpts"><legend class="slegandText"><strong>TripleStore Operation:</strong></legend>
	 <div class="menuText">
		 <input type="radio" name="tsSyncOption" value="tsSync" class="pmcheckbox" checked>
		 <span class="text-special" title="Sync metadata in the source triplestore to the destination triplestore">Sync TripleStore From Source to Destination.</span>
	 </div>
	 <div class="menuText">
		 <input type="radio" name="tsSyncOption" value="tsRebuild" class="pmcheckbox">
		 <span class="text-special" title="Rebuild the destination triplestore from the source triplestore">Rebuild the Destination TripleStore from Source.</span>
	 </div>
	 </fieldset>
</div>
<div class="buttonDiv">
	<input type="button" name="saemFileCancelButton" value="Cancel" onClick="document.location.href='/damsmanager/'"/>&nbsp;&nbsp;
	<input type="button" name="saemFileButton" value=" Operate " onClick="confirmOperation();"/>
</div>
</div>
</div>
</form>
</div>
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
