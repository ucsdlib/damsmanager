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
<style>
.i_group { width: 320px; }
</style>
</head>
<body onload="selectSource()" style="background-color:#fff;">
<script type="text/javascript">
	var letters = /^[A-Za-z]+$/;
	function confirmImport(){
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
        
	    var isIngest = document.getElementById("importOption").checked;
	    var preingestOption = $('input[name="preingestOption"]:checked').val();

       	if (isIngest || preingestOption == 'pre-processing' || preingestOption == 'pre-processing-csv') {
		var unitIndex = document.mainForm.unit.selectedIndex;  
	    if(unitIndex == 0){
	    	alert("Please select a unit.");
			return false;
	    }
	    
		var programIndex = document.mainForm.program.selectedIndex;
	    if(programIndex == 0){
	    	alert("Please select a program.");
	    	document.mainForm.program.focus();
			return false;
	    } else {
	    	var unitText = document.mainForm.unit.options[unitIndex].text;
	    	var programText = document.mainForm.program.options[programIndex].text;
	    	if (unitText.indexOf("Research Data") >= 0 || programText.indexOf("Research Data") >= 0) {
	    		 if (!(unitText.indexOf("Research Data") >= 0 && programText.indexOf("Research Data") >= 0)) {
		    		alert("The Unit selected doesn't match the Program. Please select a program that match unit \"" + unitText + "\"!");
		    		document.mainForm.program.focus();
					return false;
	    		 }
	    	}
	    }
	    
		var copyrightStatusIndex = document.mainForm.copyrightStatus.selectedIndex;
		var csSelectedValue = document.mainForm.copyrightStatus.options[copyrightStatusIndex].value;
	    if(copyrightStatusIndex == 0){
	    	alert("Please select copyright status.");
	    	document.mainForm.copyrightStatus.focus();
			return false;
	    }
	    
	    if (csSelectedValue == 'Copyright UC Regents' || csSelectedValue == 'Copyrighted (Person)' || csSelectedValue == 'Copyrighted (Corporate)' || csSelectedValue == 'Copyrighted (Other)') {
		    var countryCode = document.mainForm.countryCode.value.trim(); 
		    var accessOverrideVal = document.mainForm.accessOverride.options[accessOverride.selectedIndex].value;
		    if(accessOverrideVal.indexOf ("Creative Commons") < 0){
		       	if(countryCode.length != 2 || !countryCode.match(letters)){
			        alert('Please enter a valid country code with two characters.'); 
			        document.mainForm.countryCode.focus();
			        return false; 
		       	}
	       	}

		    var copyrightOwner = document.mainForm.copyrightOwner.value.trim(); 
	       	if(copyrightOwner.length == 0){
		        alert('Copyright owner field is required.'); 
		        document.mainForm.copyrightOwner.focus();
		        return false; 
	       	}
	       	
	       	// License begin/end date validation
	       	var licenseBeginDate = document.mainForm.licenseBeginDate.value.trim();
	       	var licenseEndDate = document.mainForm.licenseEndDate.value.trim();
	       	var beginDate = null;
	       	var endDate = null;
	       	if (licenseBeginDate.length > 0) {
	       		beginDate = parseDate(licenseBeginDate);
	       		if (beginDate == null) {
	       			alert ("Please enter a valid license begin date in format yyyy-mm-dd.");
	       			document.mainForm.licenseBeginDate.focus();
	       			return false;
	       		}
	       	}
	       	
	       	if (licenseEndDate.length > 0) {
		       	endDate = parseDate(licenseEndDate);
	       		if (endDate == null) {
	       			alert ("Please enter a valid license end date in format yyyy-mm-dd.");
	       			document.mainForm.licenseEndDate.focus();
	       			return false;
	       		}
	       	}
	       	
	       	if (beginDate != null && endDate != null) {
	       		if (beginDate > endDate) {
	       			alert ("Invalid date range: license end date can't be earilier than the license begin date!");
	       			document.mainForm.licenseEndDate.focus();
	       			return false;
		       	}
	       	}
		    }
	    } else {
	    	var filesCheckPath = document.mainForm.filesCheckPath.value.trim();
	    	if (filesCheckPath.length == 0) {
	    		alert ("Please choose a file location from the staging area!");
       			document.mainForm.filesCheckPath.focus();
       			return false;
	    	}
	    }
        
	    var message = "Are you sure to you want to do pre-ingest validation for files check? \n";;

	    if (isIngest) {
	    	message = "This will create new objects in DAMS production. \nHave all pre-ingest validations been completed? \n";
	    	var filesPath = document.mainForm.filesPath.value.trim();
	    	if (filesPath.length == 0) {
	    		alert("No Master Files location selected for dams ingest! Please select a Master Files location.\n");
	    		return false;
	    	}
	    } else if (preingestOption == 'pre-processing') {
		    message = "Are you sure you want to preview the converted RDF/XML? \n";
	    } else if (preingestOption == 'pre-processing-csv')
		    message = "Are you sure you want to preview the converted source in CSV format? \n";

	    if(collectionIndex == 0){
	    	message = "No collections selected! \n" + message;
	    }

	    var exeConfirm = confirm(message);
	    if(!exeConfirm)
	    	return false;
	    
    	document.mainForm.action = "/damsmanager/operationHandler.do?marcModsImport&progress=0&formId=mainForm&sid=" + getSid();
    	displayMessage("message", "");
    	getAssignment("mainForm");
		displayProgressBar(0);
	}
	
	function onIngestSelectionChange(obj) {
		$('input[name="preingestOption"]').each(function(){
			if ($(obj).prop('checked'))
				$(this).attr('disabled', true);
			else
				$(this).attr('disabled', false);
		});
	}

	function selectCollection(selectObj){
		var dsIdx = document.mainForm.ts.selectedIndex;
		var ds = document.mainForm.ts.options[dsIdx].value;
		var selectedIndex = selectObj.selectedIndex;
		if (selectedIndex == 0) {
			document.mainForm.action = "/damsmanager/marcModsImport.do?ts=" + ds;
			document.mainForm.submit();
		}
		else {
			var collectionId = selectObj.options[selectedIndex].value;
			document.mainForm.action = "/damsmanager/marcModsImport.do?ts=" + ds + "&reset&category=" + collectionId;
			document.mainForm.submit();
		}
	}
	
	function reloadPage(){
		var dsIdx = document.mainForm.ts.selectedIndex;
		var ds = document.mainForm.ts.options[dsIdx].value;
		document.location.href="/damsmanager/atImport.do?ts=" + ds;
	}
	
	function selectUnit(unitOpt){
		var unitName = unitOpt.options[unitOpt.selectedIndex].text;
		var unitID = unitOpt.options[unitOpt.selectedIndex].value;

		if(unitName == "UCSD Research Data Collections" || unitName.indexOf("Research Data Curation") >= 0 || unitID.indexOf("bb6827300d") >= 0) {

			var proOpts = document.mainForm.program.options;
			for (var i = 0; i < proOpts.length; i++) { 
				if (proOpts[i].text.indexOf("Research Data") >=0)
					proOpts[i].selected = true;
			}
		} else {
			var programText = document.mainForm.program.options[mainForm.program.selectedIndex].text;
			if (programText.indexOf("Research Data") >=0)
				document.mainForm.program.selectedIndex = 0;
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

	var accessOverrideOptions = ${model.accessOverride};
	function copyrightStatusChanged() {
		var csSelectedIdx = document.mainForm.copyrightStatus.selectedIndex;
		var csOptions = document.mainForm.copyrightStatus.options;
		var csSelectedValue = csOptions[csSelectedIdx].value;
		var copyrightOwnerField = $("#copyrightOwnerField");
		var accessOverrideField = $("#accessOverrideField");
		var licenseBeginDateField = $("#licenseBeginDateField");
		var licenseEndDateField = $("#licenseEndDateField");

		if (csSelectedValue == '' || csSelectedValue == 'Public domain') {
			$(copyrightOwnerField).hide();
			$(accessOverrideField).hide();
			$(licenseBeginDateField).hide();
			$(licenseEndDateField).hide();
		} else if (csSelectedValue == 'Copyright UC Regents' || csSelectedValue == 'Copyrighted (Person)' || csSelectedValue == 'Copyrighted (Corporate)' || csSelectedValue == 'Copyrighted (Other)') {
			$(copyrightOwnerField).show();

            if (csSelectedValue == 'Copyright UC Regents')
                $("#copyrightOwner").val("UC Regents").prop('disabled', true);
            else
                $("#copyrightOwner").val("").prop('disabled', false);

			// access override
			$(accessOverrideField).show();
			addAccessOverrideOptions(accessOverrideOptions);
			
			$(licenseBeginDateField).show();
			$(licenseEndDateField).show();
		} else if (csSelectedValue == 'Unknown') {
			$(copyrightOwnerField).hide();

			// access override
			$(accessOverrideField).show();
			var aoOptions = ["Public - open fair use", "UCSD - educational fair use", "Curator", "Click through - cultural sensitivity", "Restricted - cultural sensitivity"];
			addAccessOverrideOptions(aoOptions);

			$(licenseBeginDateField).hide();
			$(licenseEndDateField).hide();
		}
	}

	function addAccessOverrideOptions(options) {
		$("#accessOverride").find("option").remove();
		$('#accessOverride').append('<option value=""> -- access override -- </option>');
		$.each(options, function(key, val) {
			$('#accessOverride').append('<option value="' + val + '">' + val + '</option>');
		});
	}

	// parse a date in yyyy-mm-dd format
	function parseDate(input) {
	  var parts = input.split('-');
	  if (parts.length != 3) {
		  return null;
	  }
	  // new Date(year, month [, day [, hours[, minutes[, seconds[, ms]]]]])
	  return new Date(parts[0], parts[1]-1, parts[2]); // Note: months are 0-based
	}

	$(function() {
		var beginCal = $( "#licenseBeginDate" );
		var endCal = $( "#licenseEndDate" );
		$(beginCal).datepicker({
			dateFormat: "yy-mm-dd", 
			appendText: "(yyyy-mm-dd)", 
			buttonImage: "/damsmanager/images/calendar.jpg",
			onSelect: function() {
				$(this).datepicker( "option", "maxDate", $(endCal).datepicker('getDate'));
			}
		});
		$(endCal).datepicker({
			dateFormat: "yy-mm-dd", 
			appendText: "(yyyy-mm-dd)", 
			buttonImage: "/damsmanager/images/calendar.jpg",
			onSelect: function() {
				$(this).datepicker( "option", "minDate", $(beginCal).datepicker('getDate'));
			}
		 });
	});
	
	var crumbs = [{"Home":"http://library.ucsd.edu"}, {"Digital Library Collections":"/dc"},{"DAMS Manager":"/damsmanager/"}, {"MARC/MODS Import":""}];
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
<form id="mainForm" name="mainForm" method="post" action="/damsmanager/operationHandler.do?marcModsImport" >
<div class="emBox_ark">
<div class="emBoxBanner">MARC/MODS Import</div>
<div style="background:#DDDDDD;padding-top:8px;padding-bottom:8px;padding-left:25px;" align="left">
		<span class="submenuText"><span class="requiredLabel">*</span><b>Collection Selection:&nbsp;</b></span>
		<span style="padding:0px 5px 0px 7px;">
			<select id="category" name="category" class="inputText" >
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
		<tr align="left" style="width:160px">
			<td height="25px">
				<span class="submenuText"><span class="requiredLabel">*</span><b>Metadata Source: </b></span>
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
					<span class="requiredLabel">*</span><span id="sourceTitle" style="font-weight:bold;">Metadata Location</span><b>: </b>
				</span>
			</td>
			<td  align="left">
				<div class="submenuText" id="modsSpan">
					<div id="mods"><input type="text" id="dataPath" name="dataPath" size="52" value="">&nbsp;<input type="button" onclick="showFilePicker('dataPath', event)" value="&nbsp;...&nbsp;"></div>
					<div id="bib" style="display:none"><input type="text" id="bibInput" name="bibInput" size="56" value=""><span class="note"> (Records delimiter: <strong>,</strong> )</span></div>
				</div>
			</td>
		</tr>
		<tr align ="left">
			<td height="25px">
				<span class="submenuText"><span class="requiredLabel">*</span><b>Admin Unit: </b></span>
			</td>
			<td>
				<select id="unit" name="unit" class="inputText i_group" onChange="selectUnit(this);">
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
				<span class="submenuText"><span class="requiredLabel">*</span><b>Program: </b></span>
			</td>
			<td>
				<select id="program" name="program" class="inputText i_group">
					<option value=""> -- program -- </option>
					<c:forEach var="val" items="${model.program}">
						<option value="${val}"><c:out value="${val}" /></option>
					</c:forEach>
				</select>
			</td>
		</tr>
		<tr align ="left">
			<td height="25px">
				<span class="submenuText"><span class="requiredLabel">*</span><b>Copyright Status: </b></span>
			</td>
			<td>
				<select id="copyrightStatus" name="copyrightStatus" class="inputText i_group" onChange="copyrightStatusChanged();">
					<option value=""> -- copyright -- </option>
					<c:forEach var="val" items="${model.copyrightStatus}">
						<option value="${val}"><c:out value="${val}" /></option>
					</c:forEach>
				</select>
			</td>
		</tr>
		<tr align="left" id="copyrightJurisdictionField">
			<td height="25px" style="width:160px nowrap">
				<span class="submenuText"><span class="requiredLabel" id="copyrightJurisdiction_required">*</span><b>Copyright Jurisdiction: </b></span>
			</td>
			<td  align="left">
				<span class="submenuText">
					<select id="countryCode" name="countryCode" class="inputText i_group">
						<c:forEach var="entry" items="${model.countryCodes}">
							<option value="${entry.value}" <c:if test="${entry.value == 'US'}">selected</c:if>>
	                      			<c:out value="${entry.key}" />
	                       	</option>
						</c:forEach>
					</select>
				</span>
			</td>
		</tr>
		<tr align="left" id="copyrightOwnerField" style="display:none;">
			<td height="25px">
				<span class="submenuText"><span class="requiredLabel" id="copyrightOwner_required">*</span><b>Copyright Owner: </b></span>
			</td>
			<td  align="left">
				<span class="submenuText"><input type="text" id="copyrightOwner" name="copyrightOwner" class="i_group"></span>
			</td>
		</tr>
		<tr align ="left" id="accessOverrideField" style="display:none;">
			<td height="25px">
				<span class="submenuText"><span class="requiredLabel" id="accessOverride_required" style="padding-left:6px;"></span><b>Access Override: </b></span>
			</td>
			<td>
				<select id="accessOverride" name="accessOverride" class="inputText i_group">
					<option value=""> -- access override -- </option>
					<c:forEach var="val" items="${model.accessOverride}">
						<option value="${val}"><c:out value="${val}" /></option>
					</c:forEach>
				</select>
			</td>
		</tr>
		<tr align="left" id="licenseBeginDateField" style="display:none;">
			<td height="25px">
				<span class="submenuText"><span class="requiredLabel" id="licenseBeginDate_required" style="padding-left:6px;"></span><b>License Begin Date: </b></span>
			</td>
			<td  align="left">
				<span class="submenuText"><input type="text" id="licenseBeginDate" name="licenseBeginDate" size="25" style="cursor:pointer;margin-right:2px;" value="${model.licenseBeginDate}"></span>
			</td>
		</tr>
		<tr align="left"  id="licenseEndDateField" style="display:none;">
			<td height="25px">
				<span class="submenuText"><span class="requiredLabel" id="licenseEndDate_required" style="padding-left:6px;"></span><b>License End Date: </b></span>
			</td>
			<td  align="left">
				<span class="submenuText"><input type="text" id="licenseEndDate" name="licenseEndDate" size="25" style="cursor:pointer;margin-right:2px;" value="${model.licenseEndDate}"></span>
			</td>
		</tr>
		<tr align="left">
			<td colspan="2">
				<div title="Ingest metadata and files" class="menuText" style="display:none;">
					<input class="pcheckbox" type="checkbox" name="importOption" id="importOption" onchange="onIngestSelectionChange(this);">
					<span class="submenuText" style="vertical-align:2px;"><b>Ingest metadata and files</b></span>
				</div>
			    <div class="submenuText" style="margin-top:3px;padding-left:25px;display:none;"  title="Enter a filter path for the location to speek up the search. From the popup, click on the folder to select/deselect a location. Multiple loations allowed.">Master Files location: 
					<input type="text" id="filesPath" name="filesPath" size="48" value="">&nbsp;<input type="button" onclick="showFilePicker('filesPath', event)" value="&nbsp;...&nbsp;">
				</div>
			</td>
		</tr>
		<tr align="left">
			<td colspan="2">
				<div>
					<fieldset class="groupbox_modsIngestOpts"><legend class="slegandText">Pre-ingest validation</legend>
					    <div title="Check this checkbox for no ingest but pre-processing only." class="submenuText" style="display:none;">
							<input checked type="radio" name="preingestOption" value="pre-processing">
							<span class="submenuText">Preview the converted RDF/XML only, no ingest.</span>
						</div>
                        <div title="Check this checkbox for pre-processing to convert to CSV format." class="submenuText">
                            <input type="radio" name="preingestOption" value="pre-processing-csv" checked>
                            <span class="submenuText">Preview converted source in CSV format, no ingest.</span>
                        </div>
					 	<div title="Check this checkbox to check files matching." class="submenuText">
							<input type="radio" name="preingestOption" value="file-match">
							<span class="text-special">File Match</span>
						</div>
					 	<div title="Check this checkbox to validate the files that match those in the metadata." class="submenuText">
							<input type="radio" name="preingestOption" value="file-validation">
							<span class="text-special">File Validation</span>
							<div class="submenuText" style="margin-top:3px;padding-left:25px;"  title="Enter a filter path for the location to speek up the search. From the popup, click on the folder to select/deselect a location. Multiple loations allowed.">Master Files location: 
								<input type="text" id="filesCheckPath" name="filesCheckPath" size="48" value="">&nbsp;<input type="button" onclick="showFilePicker('filesCheckPath', event)" value="&nbsp;...&nbsp;">
							</div>
						</div>
					</fieldset>
				</div>
			</td>
		</tr>
		<tr><td colspan="2" style="padding-left:6px;"><span class="submenuText"><span class="requiredLabel">*</span><b>Required Field</b></span></td>
	</table>
</div>
<div class="buttonDiv">

	<input type="button" name="atImport" value=" Submit " onClick="confirmImport();"/>&nbsp;&nbsp;
	<input type="button" name="atImportCancel" value=" Cancel " onClick="document.location.href='/damsmanager/'"/>
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
