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
</head>
<body onload="selectSource()" style="background-color:#fff;">
<script type="text/javascript">
	var letters = /^[A-Za-z]+$/;
	function confirmImport(){
		var dsIdx = document.mainForm.ts.selectedIndex;
		var ds = document.mainForm.ts.options[dsIdx].value;
		var collectionIndex = document.mainForm.category.selectedIndex;
	
        var sourceName = document.mainForm.source.options[source.selectedIndex].value;
        if (sourceName == 'bib') {
        	var bib = trim(document.mainForm.bibInput.value);
        	var message = ""; 
        	if (bib.length == 0) {
        		message = "Please type in valid bib number(s) delimited by comma (,).";
        	}else{
	       		var bibs = bib.split(",");
	       		var emptyCount = 0;
	       		for (var i=0; i<bibs.length; i++) {
	       			var bibVal = trim(bibs[i]);
	       			if (bibVal.length == 0)
	       				emptyCount += 1;
	       			else{
		       			if (bibVal.charAt(0) != 'b' || !(bibVal.length == 8 || bibVal.length == 9)){
		       				message = "Invalid bib number at index " + (i + 1) + ": " + bibVal + ". \nPlease type in valid bib number(s) delimited by comma (,).";
		       				break;
	       				}
	       			}
	       		}
	       		if(emptyCount == bibs.length)
	       			message = "Invalid bib number(s): " + bib + ". \nPlease type in valid bib number(s) delimited by comma (,).";

        	}
        	
       		if (message.length > 0) {
	   	    	alert(message);
	   	    	document.mainForm.bibInput.focus();
	   			return false;
        	}
        } else {
        	var file = document.mainForm.dataPath.value;
        	if (file == "") {
    	    	alert("Please choose metadata source file location.");
    	    	document.mainForm.dataPath.focus();
    			return false;
        	}
        	document.mainForm.enctype = "multipart/form-data";
        }
        
		var unitIndex = document.mainForm.unit.selectedIndex;  
	    if(unitIndex == 0){
	    	alert("Please select a unit.");
			return false;
	    }
	    
		var copyrightStatusIndex = document.mainForm.copyrightStatus.selectedIndex;
	    if(copyrightStatusIndex == 0){
	    	alert("Please select copyright status.");
	    	document.mainForm.copyrightStatus.focus();
			return false;
	    }
	    
	    var countryCode = document.mainForm.countryCode.value;    
        if(countryCode == null || countryCode.length != 2 || !countryCode.match(letters)){   
	        alert('Please enter a valid country code with two characters.'); 
	        document.mainForm.countryCode.focus();
	        return false;  
        }
        
		var programIndex = document.mainForm.program.selectedIndex;
	    if(programIndex == 0){
	    	alert("Please select a program.");
	    	document.mainForm.program.focus();
			return false;
	    }
        
	    var message = "Are you sure to import objects from METS/MODS metadata source? \n";
	    if(collectionIndex == 0){
	    	message = "No collections selected for DAMS staging ingest! \nAre you sure to continue?";
	    }
	    var exeConfirm = confirm(message);
	    if(!exeConfirm)
	    	return false;
	    
    	document.mainForm.action = "/damsmanager/operationHandler.do?ds=" + ds + "&metsModsImport&progress=0&formId=mainForm&sid=" + getSid();
    	displayMessage("message", "");
    	getAssignment("mainForm");
		displayProgressBar(0);
	}
	
	var fsDefault = "${model.filestoreDefault}";
	function selectCollection(selectObj){
		var dsIdx = document.mainForm.ts.selectedIndex;
		var ds = document.mainForm.ts.options[dsIdx].value;
		var selectedIndex = selectObj.selectedIndex;
		if (selectedIndex == 0) {
			document.mainForm.action = "/damsmanager/metsModsImport.do?ts=" + ds;
			document.mainForm.submit();
		}
		else {
			var collectionId = selectObj.options[selectedIndex].value;
			document.mainForm.action = "/damsmanager/metsModsImport.do?ts=" + ds + "&reset&category=" + collectionId;
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
		document.location.href="/damsmanager/atImport.do?ts=" + ds;
	}
	
	function selectUnit(unitOpt){
		var unitName = unitOpt.options[unitOpt.selectedIndex].text;
		var fsOpts = document.mainForm.fs.options;
		var fsSelected = fsDefault;
		if(unitName == "rci" || unitName == "RCI" || unitName.indexOf("Research Data Curation") == 0)
			fsSelected = "openStack";
		for(var i=0; i<fsOpts.length; i++){
			if(fsOpts[i].value == fsSelected){
				fsOpts[i].selected = true;
				break;
			}
		}
	}
	
	function selectSource(){
		var srcOptions = document.mainForm.source.options;
		var sourceName = srcOptions[source.selectedIndex].value;
		for(var i=0; i<srcOptions.length; i++){
			if(srcOptions[i].value != sourceName)
				$("#" + srcOptions[i].value).hide();
		}
		
		$("#" + sourceName).show();
		if(sourceName == 'bib')
			$("#sourceTitle").text("Bib number(s)");
		else
			$("#sourceTitle").text("Metadata Location");
	}
	  
	$(function() {
		 $( "#licenseEndDate" ).datepicker({dateFormat: "yy-mm-dd", appendText: "(yyyy-mm-dd)", buttonImage: "/damsmanager/images/calendar.jpg"});
	});
	
	var crumbs = [{"Home":"http://libraries.ucsd.edu"}, {"Digital Library Collections":"/curator"},{"DAMS Manager":"/damsmanager/"}, {"METS/MODS Import":""}];
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
<form id="mainForm" name="mainForm" method="post" action="/damsmanager/operationHandler.do?metsModsImport" >
<div class="emBox_ark">
<div class="emBoxBanner">METS/MODS Import</div>
<div style="background:#DDDDDD;padding-top:8px;padding-bottom:8px;padding-left:25px;" align="left">
	<span class="menuText">
		<span class="requiredLabel">*</span><span id="dsSpan" Title="Double click to change the triplestore used for operation." ondblclick="setTriplestore();" onMouseOver="this.style.cursor='pointer'">${fn:toUpperCase(tsNameFl)}${fn:substring(model.triplestore, 1, tsNameLen)} </span>
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
		</span>
		<span>
			<select id="category" name="category" onChange="selectCollection(this);" class="inputText" >
				<option value=""> -- collections -- </option>
				<c:forEach var="entry" items="${model.categories}">
					<c:set var="colNameLen"> ${fn:length(entry.key)}</c:set>
					<option value="${entry.value}" <c:if test="${model.category == entry.value}">selected</c:if>>
                     			<c:choose>
							<c:when test="${colNameLen > 75}"><c:out value="${fn:substring(entry.key, 0, 71)}" /> ...</c:when>
							<c:otherwise><c:out value="${entry.key}" /></c:otherwise>
						</c:choose>
                      	</option>
				</c:forEach>
			</select>
		</span>
</div>
<div style="margin-top:10px;padding-left:20px;" align="left">
	<table>
		<tr align ="left">
			<td height="25px">
				<span class="submenuText"><span class="requiredLabel">*</span><b>FileStore to use: </b></span>
			</td>
			<td>
				<select id="fs" name="fs" class="inputText">
					<c:forEach var="entry" items="${model.filestores}">
						<c:if test="${entry != 'isilon-nfs'}">
							<option value="${entry}" <c:if test="${model.filestore == entry}">selected</c:if>>
	                      			<c:out value="${entry}" /><c:if test="${model.filestoreDefault == entry}"> (default)</c:if>
	                       	</option>
                       	</c:if>
					</c:forEach>
				</select>
		    </td>
		</tr>
		<tr align="left">
			<td height="25px">
				<span class="submenuText"><span class="requiredLabel">*</span><b>Metadata Source: </b></span>&nbsp;&nbsp;
			</td>
			<td>
				<select id="source" name="source" class="inputText" onChange="selectSource(this);">
					<option value="mods" selected>METS/MODS</option>
					<option value="bib">Roger Record</option>
				</select>
		    </td>
		</tr>
		<tr align="left">
			<td height="25px">
				<span class="submenuText">
					<span class="requiredLabel">*</span><span id="sourceTitle" style="font-weight:bold;">Metadata Location</span><b>: </b>&nbsp;&nbsp;
				</span>
			</td>
			<td  align="left">
				<div class="submenuText" id="modsSpan">
					<div id="mods"><input type="text" id="dataPath" name="dataPath" size="48" value="">&nbsp;<input type="button" onclick="showFilePicker('dataPath', event)" value="&nbsp;...&nbsp;"></div>
					<div id="bib" style="display:none"><input type="text" id="bibInput" name="bibInput" size="50" value=""><span class="note"> (Records delimiter: <strong>,</strong> )</span></div>
				</div>
			</td>
		</tr>
		<tr align ="left">
			<td height="25px">
				<span class="submenuText"><span class="requiredLabel">*</span><b>Admin Unit: </b></span>
			</td>
			<td>
				<select id="unit" name="unit" class="inputText" onChange="selectUnit(this);">
					<option value=""> -- units -- </option>
					<c:forEach var="entry" items="${model.units}">
						<option value="${entry.value}" <c:if test="${model.unit == entry.value}">selected</c:if>>
                      			<c:out value="${entry.key}" />
                       	</option>
					</c:forEach>
				</select>
			</td>
		</tr>
		<tr align ="left">
			<td height="25px">
				<span class="submenuText"><span class="requiredLabel">*</span><b>Copyright Status: </b></span>
			</td>
			<td>
				<select id="copyrightStatus" name="copyrightStatus" class="inputText">
					<option value=""> -- copyright -- </option>
					<c:forEach var="val" items="${model.copyrightStatus}">
						<option value="${val}"><c:out value="${val}" /></option>
					</c:forEach>
				</select>
			</td>
		</tr>
		<tr align="left">
			<td height="25px">
				<span class="submenuText"><span class="requiredLabel">*</span><b>Copyright Jurisdiction: </b></span>&nbsp;&nbsp;
			</td>
			<td  align="left">
				<span class="submenuText"><input type="text" id="countryCode" name="countryCode" size="20"></span>
			</td>
		</tr>
		<tr align="left">
			<td height="25px">
				<span class="submenuText" style="padding-left: 6px"><b>Copyright Owner: </b></span>&nbsp;&nbsp;
			</td>
			<td  align="left">
				<span class="submenuText"><input type="text" name="fileFilter" size="20"></span>
			</td>
		</tr>
		<tr align ="left">
			<td height="25px">
				<span class="submenuText"><span class="requiredLabel">*</span><b>Program: </b></span>
			</td>
			<td>
				<select id="program" name="program" class="inputText">
					<option value=""> -- program -- </option>
					<c:forEach var="val" items="${model.program}">
						<option value="${val}"><c:out value="${val}" /></option>
					</c:forEach>
				</select>
			</td>
		</tr>
		<tr align ="left">
			<td height="25px">
				<span class="submenuText" style="padding-left: 6px"><b>Access Override: </b></span>
			</td>
			<td>
				<select id="accessOverride" name="accessOverride" class="inputText">
					<option value=""> -- access override -- </option>
					<c:forEach var="val" items="${model.accessOverride}">
						<option value="${val}"><c:out value="${val}" /></option>
					</c:forEach>
				</select>
			</td>
		</tr>
		<tr align="left">
			<td height="25px">
				<span class="submenuText" style="padding-left: 6px"><b>License End Date: </b></span>&nbsp;&nbsp;
			</td>
			<td  align="left">
				<span class="submenuText"><input type="text" id="licenseEndDate" name="licenseEndDate" size="20" value="${model.licenseEndDate}"></span>
			</td>
		</tr>
		<tr align="left">
			<td colspan="2">
				<div>
					 <fieldset class="groupbox_modsIngestOpts"><legend class="slegandText">Import Options</legend>
					    <div title="Check this checkbox for no ingest but pre-processing only." class="specialmenuText">
							<input checked type="radio" name="importOption" value="pre-processing" checked>
							<span class="text-special">Preview converted RDF/XML only, no ingest.</span>
						</div>
					 	<div title="Check this checkbox to import metadata and files." class="specialmenuText">
							<input type="radio" name="importOption" value="metadataAndFiles">
							<span class="text-special">Ingest metadata and files</span>
							<div class="specialmenuText" style="margin-top:3px;padding-left:22px;"  title="Enter a filter path for the location to speek up the search. From the popup, click on the folder to select/deselect a location. Multiple loations allowed.">Master Files location: 
								<input type="text" id="filesPath" name="filesPath" size="40" value="">&nbsp;<input type="button" onclick="showFilePicker('filesPath', event)" value="&nbsp;...&nbsp;">
							</div>
						 </div>
						 <div title="Check this checkbox to import metadata only." class="specialmenuText">
							<input type="radio" name="importOption" value="metadata">
							<span class="text-special">Ingest metadata only</span>
						 </div>
					  </fieldset>
				</div>
													  
			</td>
		</tr>
		<tr><td colspan="2" style="color:#333;size=12px;padding-left:6px;"><span class="requiredLabel">*</span> required field</td>
	</table>
</div>
<div class="buttonDiv">
	<input type="button" name="atImport" value=" Import " onClick="confirmImport();"/>&nbsp;&nbsp;
	<input type="button" name="atImportCancel" value=" Cancel " onClick="document.location.href='/damsmanager/'"/>
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
