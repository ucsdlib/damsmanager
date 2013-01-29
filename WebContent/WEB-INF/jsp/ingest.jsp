<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<c:set var="tsNameLen"> ${fn:length(model.triplestore)}</c:set>  
<c:set var="tsNameFl" scope="page">${fn:substring(model.triplestore, 0, 1)}</c:set> 
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
<body onLoad="load('ingest')" style="background-color:#fff;">
<script type="text/javascript">
	function confirmIngest(){
		var dsIdx = document.mainForm.ts.selectedIndex;
		var ds = document.mainForm.ts.options[dsIdx].value;
		var collectionIndex = document.mainForm.category.selectedIndex;
		var repoIndex = document.mainForm.repo.selectedIndex;
	    if(repoIndex == 0){
	    	alert("Please select a repository.");
			return false;
	    }
	    
	    var stagingAreaPath = document.mainForm.filePath.value;
	    if(stagingAreaPath == null || stagingAreaPath.length == 0){
	    	alert("Please enter the file location in the Staging Area.");
			return false;
	    }
	    
	    var arkSetting = "0";
	    var asLength = document.mainForm.arkSetting.length;
	    var i = 0;
	    for(i=0; i<asLength; i++){
	    	if(document.mainForm.arkSetting[i].checked == true)
	    		arkSetting = document.mainForm.arkSetting[i].value;
	    }
	    if (arkSetting == "4"){
	    	var suffixes = trim(document.mainForm.fileSuffixes.value);
	    	if(suffixes.length == 0){
	    		alert("Please enter the file suffixes delimited by comma in order.");
				return false;
	    	}
	    }else if (arkSetting == "5"){
	    	var suffixes = trim(document.mainForm.fileSuffixes.value);
	    	if(suffixes.length == 0){
	    		alert("Please enter the file suffixes delimited by comma in order.");
				return false;
	    	}
	    	var codelimiter = trim(document.mainForm.coDelimiter.value);
	    	if(codelimiter.length == 0){
	    		alert("Please enter delimiter for complex object ordering.");
				return false;
	    	}
	    }
	    
	    var fileUses = trim(document.mainForm.fileUse.value);
	    if(fileUses.length > 0){
	    	var invalidFileUses = [];
	    	fileUses = fileUses.split(',');
	    	for(var i=0; i<fileUses.length; i++){
	    		var fileUse = fileUses[i];
	    		if(fileUse != null && fileUse.length > 0 && fileUseArr.indexOf(fileUse)==-1)
	    			invalidFileUses.push(fileUse);
	    	}
	    	if(invalidFileUses.length > 0){
	    		alert("Invalid file use: " + invalidFileUses.join(', ') + ". \nPlease enter the following values: " + fileUseArr.join(', '));
				return false;
	    	}
	    }
	    var message = "Are you sure to ingest files from the Staging Area " + stagingAreaPath + "?";
	    if(collectionIndex == 0){
	    	message = "No collection is selected for staging ingest! \nShall I just go ahead and ingest the files in " + stagingAreaPath + " anyway?";
	    }
	    var exeConfirm = confirm(message);
	    if(!exeConfirm)
	    	return false;
	    
    	document.mainForm.action = "/damsmanager/operationHandler.do?ds=" + ds + "&ingest&progress=0&formId=mainForm&sid=" + getSid();
    	displayMessage("message", "");
    	getAssignment("mainForm");
		displayProgressBar(0);
	}
	
	function selectCollection(selectObj){
		var dsIdx = document.mainForm.ts.selectedIndex;
		var ds = document.mainForm.ts.options[dsIdx].value;
		var selectedIndex = selectObj.selectedIndex;
		if (selectedIndex == 0) {
			document.mainForm.action = "/damsmanager/ingest.do?ts=" + ds;
			document.mainForm.submit();
		}
		else {
			var collectionId = selectObj.options[selectedIndex].value;
			document.mainForm.action = "/damsmanager/ingest.do?ts=" + ds + "&reset&category=" + collectionId;
			document.mainForm.submit();
		}
	}
	
	function setTriplestore(){
		document.getElementById("dsSpan").style.display = "none";
		document.getElementById("dsSelectSpan").style.display = "inline";
	}
	
	function resetTriplestore(){
		document.getElementById("dsSpan").style.display = "inline";
		document.getElementById("dsSelectSpan").style.display = "none";
	}
	
	function reloadPage(){
		var dsIdx = document.mainForm.ts.selectedIndex;
		var ds = document.mainForm.ts.options[dsIdx].value;
		document.location.href="/damsmanager/ingest.do?ts=" + ds;
	}
	
	var crumbs = [{"Home":"http://libraries.ucsd.edu"}, {"Digital Library Collections":"/curator"},{"DAMS Manager":"/damsmanager/"}, {"Staging Ingest":""}];
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
<form id="mainForm" name="mainForm" method="post" action="/damsmanager/operationHandler.do?ingest" >
<div class="emBox_ark">
<div class="emBoxBanner">Staging Ingest</div>
<div style="background:#DDDDDD;padding-top:8px;padding-bottom:8px;padding-left:25px;" align="left">
		<span id="dsSpan" class="menuText" Title="Double click to change the triplestore used for operation." ondblclick="setTriplestore();" onMouseOver="this.style.cursor='pointer'">${fn:toUpperCase(tsNameFl)}${fn:substring(model.triplestore, 1, tsNameLen)} </span>
			<span id="dsSelectSpan" ondblclick="resetTriplestore();" style="display:none" >
				<select name="ts" id="ts" onChange="reloadPage();"><option value=""> -- Triplestore -- </option>
						<c:forEach var="entry" items="${model.triplestores}">
							<option value="${entry}" <c:if test="${model.triplestore == entry}">selected</c:if>>
                       			<c:out value="${entry}" />
                        	</option>
						</c:forEach>
				</select>&nbsp;
			</span>
			<span class="menuText">Collection Chooser:&nbsp;</span>
			<span><select id="category" name="category" onChange="selectCollection(this);" class="inputText" >
						<option value=""> -- collections -- </option>
						<c:forEach var="entry" items="${model.categories}">
							<option value="${entry.value}" <c:if test="${model.category == entry.value}">selected</c:if>>
                       			<c:out value="${entry.key}" />
                        	</option>
						</c:forEach>
					</select>
			</span>
</div>
<div style="margin-top:10px;padding-left:20px;" align="left">
	<table>
		<tr align ="left">
			<td height="25px">
				<span class="submenuText"><b>FileStore to use: </b></span>
				</td><td>
					<select id="fs" name="fs" class="inputText">
						<c:forEach var="entry" items="${model.filestores}">
							<option value="${entry}" <c:if test="${model.filestore == entry}">selected</c:if>>
                       			<c:out value="${entry}" /><c:if test="${model.filestoreDefault == entry}"> (default)</c:if>
                        	</option>
						</c:forEach>
					</select>
			</td>
		</tr>
		<tr align ="left">
			<td height="25px">
				<span class="submenuText"><b>Repository: </b></span>
				</td><td>
					<select id="repo" name="repo" class="inputText">
						<option value=""> -- repositories -- </option>
						<c:forEach var="entry" items="${model.repos}">
							<option value="${entry.value}" <c:if test="${model.repo == entry.value}">selected</c:if>>
                       			<c:out value="${entry.key}" />
                        	</option>
						</c:forEach>
					</select>
			</td>
		</tr>
		<tr align ="left">
			<td height="25px">
				<span class="submenuText"><b>Staging Area: </b></span>
				</td><td>
				<span class="submenuText">
					<input type="text" id="filePath" name="filePath" size="45" value="${model.filePath}">&nbsp;<input type="button" onclick="showFilePicker('filePath', event)" value="&nbsp;...&nbsp;">
				</span>
			</td>
		</tr>
		<tr align="left">
			<td height="25px">
				<span class="submenuText"><b>File Filter: </b></span>&nbsp;&nbsp;
			</td>
			<td  align="left">
				<span class="submenuText"><input type="text" name="fileFilter" size="20" value="${model.fileFilter}"></span>
			</td>
		</tr>
		<tr align="left">
			<td colspan="2">
	 <div class="submenuText"><b>ARK Setting: </b></div>
	 <div title="Check this checkbox to populate the TripleStore for the first round." class="specialmenuText">
				<input type="radio" name="arkSetting" value="0" class="pmcheckbox" <c:if test="${model.arkSetting == '0' or arkSetting == null}">checked</c:if>>
				<span class="text-special">One ARK to one file (Single Objects)</span>
	 </div>
	 <div title="Assign one ARK to files in the same directory" class="specialmenuText">
				<input type="radio" name="arkSetting" value="1" class="pmcheckbox" <c:if test="${model.arkSetting == '1'}">checked</c:if>>
				<span class="text-special">One ARK to files in the same directory</span>
				<fieldset class="groupbox_compOrder">
				 	<legend class="slegandText">Components Ordering</legend>
					<input type="radio" name="preferedOrder" value="cofDelimiter" <c:if test="${model.preferedOrder == null || fn:length(model.preferedOrder)== 0 || model.preferedOrder == 'cofDelimiter'}">checked</c:if>/><span>Alphabetic order. [Optional] Order delimited by: <input type="text" name="cofDelimiter" id="cofDelimiter" value="" size="10"></span><br />
					<input type="radio" name="preferedOrder" value="pdfAndPdf" <c:if test="${model.preferedOrder == 'pdfAndPdf'}">checked</c:if> /><span>A PDF following a low resolution PDF for access.</span><br />
					<input type="radio" name="preferedOrder" value="pdfAndXml" <c:if test="${model.preferedOrder == 'pdfAndXml'}">checked</c:if> /><span>A PDF following a proquest xml file.</span><br />
					<input type="radio" name="preferedOrder" value="suffix" <c:if test="${model.preferedOrder == 'suffix'}">checked</c:if> /><span>Components ordered by their suffixes provided.</span>
				</fieldset>
	 </div>
	 <div title="Assign one ARK to the files with the same prefix ending with letter 'p' and an order number" class="specialmenuText">
				<input type="radio" name="arkSetting" value="2" class="pmcheckbox" <c:if test="${model.arkSetting == '2'}">checked</c:if>>
				<span class="text-special">One ARK to files with a suffix ending with 'p' plus page number (Single &amp; Complex)</span>
	  </div>
	 <div title="Assign one ARK to a master file and its master-edited file which marked as '-edited'" class="specialmenuText">
				<input type="radio" name="arkSetting" value="3" class="pmcheckbox" <c:if test="${model.arkSetting == '3'}">checked</c:if>>
				<span class="text-special">One ARK to a file and its edited file (Master and Master-edited files)</span>
	  </div>
	  <div title="Assign one ARK to files with same file name but different extension." class="specialmenuText">
				<input type="radio" name="arkSetting" value="4" class="pmcheckbox" <c:if test="${model.arkSetting == '4'}">checked</c:if>>
				<span class="text-special">One ARK to files with different suffixes (Master file and derivatives)</span>
				<!-- <div style="margin-left:30px;">- File Suffixes delimited by comma in order: <input type="text" name="suffixes" id="suffixes" value="" size="18">
				</div>
				-->
	  </div>
	  <div title="Assign one ARK to complex objects with '_' delimiting ordering along with derivatives." class="specialmenuText"><div>
				<input type="radio" name="arkSetting" value="5" class="pmcheckbox" <c:if test="${model.arkSetting == '5'}">checked</c:if>>
				<span class="text-special">One ARK to complex objects, components order delimited by: <input type="text" name="coDelimiter" id="coDelimiter" value="" size="10"></span></div>
				<!-- <div style="margin-left:30px;">- File Suffixes delimited by comma in order: <input type="text" name="cosuffixes" id="cosuffixes" value="" size="18">
				</div>
				 -->
	  </div>
	  <div class="specialmenuText" style="margin-left:15px;">
		  <fieldset class="groupbox_jetlOptions"><legend class="slegandText"> Constraint Options </legend>
		  	<div class="specialmenuText"><div>Files ordered by suffixes (delimited by comma): <input type="text" name="fileSuffixes" id="fileSuffixes" value="${model.fileSuffixes}" size="30"></div></div>
		  	<div class="specialmenuText"><div style="margin-top:3px;">File Use properties applied (delimited by comma): <input type="text" name="fileUse" id="fileUse" value="${model.fileUse}" size="29"></div></div>
		  </fieldset>
	  </div>
	  </td>
	  </tr>
	</table>
</div>
<div class="buttonDiv">
	<input type="button" name="jetlIngest" value=" Ingest " onClick="confirmIngest();"/>&nbsp;&nbsp;
	<input type="button" name="jetlCancel" value=" Cancel " onClick="document.location.href='/damsmanager/'"/>
</div>
</div>
</form>
</div>
	<jsp:include flush="true" page="/jsp/status.jsp" />
	<div id="messageDiv">
		<div id="message" align="left">${model.message}</div>
	</div>
</td>
</tr>
</table>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
</html>
