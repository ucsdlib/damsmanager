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
<body style="background-color:#fff;">
<script type="text/javascript">
	var letters = /^[A-Za-z]+$/;
	function confirmImport(){
		var collectionIndex = document.mainForm.category.selectedIndex;
       	var file = document.mainForm.dataPath.value;
       	if (file == "") {
   	    	alert("Please choose Excel metadata source file location.");
   	    	document.mainForm.dataPath.focus();
   			return false;
       	}
       	document.mainForm.enctype = "multipart/form-data";
        
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
	    
	    if (csSelectedValue == 'Copyrighted (Person)' || csSelectedValue == 'Copyrighted (Corporate)' || csSelectedValue == 'Copyrighted (Other)') {
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
        
	    var message = "Are you sure to import objects from Excel metadata source? \n";
	    if(collectionIndex == 0){
	    	message = "No collections selected for DAMS staging ingest! \nAre you sure to continue?";
	    }
	    var exeConfirm = confirm(message);
	    if(!exeConfirm)
	    	return false;
	    
    	document.mainForm.action = "/damsmanager/operationHandler.do?excelImport&progress=0&formId=mainForm&sid=" + getSid();
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
			document.mainForm.action = "/damsmanager/excelImport.do?ts=" + ds;
			document.mainForm.submit();
		}
		else {
			var collectionId = selectObj.options[selectedIndex].value;
			document.mainForm.action = "/damsmanager/excelImport.do?ts=" + ds + "&reset&category=" + collectionId;
			document.mainForm.submit();
		}
	}
	
	function reloadPage(){
		var dsIdx = document.mainForm.ts.selectedIndex;
		var ds = document.mainForm.ts.options[dsIdx].value;
		document.location.href="/damsmanager/excelImport.do?ts=" + ds;
	}
	
	function selectUnit(unitOpt){
		var unitName = unitOpt.options[unitOpt.selectedIndex].text;
		var unitID = unitOpt.options[unitOpt.selectedIndex].value;
		var fsSelected = fsDefault;
		if(unitName == "UCSD Research Data Collections" || unitName.indexOf("Research Data Curation") >= 0 || unitID.indexOf("bb6827300d") >= 0) {
			fsSelected = "openStack";
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
		document.mainForm.fs.value = fsSelected;
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
		} else if (csSelectedValue == 'Copyright UC Regents') {
			$(copyrightOwnerField).show();
			$("#copyrightOwner").val("UC Regents").prop('disabled', true);

			// access override
			$(accessOverrideField).show();
			var aoOptions = ["Curator", "Click through - cultural sensitivity", "Restricted - cultural sensitivity"];
			addAccessOverrideOptions(aoOptions);

			$(licenseBeginDateField).hide();
			$(licenseEndDateField).hide();
		} else if (csSelectedValue == 'Copyrighted (Person)' || csSelectedValue == 'Copyrighted (Corporate)' || csSelectedValue == 'Copyrighted (Other)') {
			$(copyrightOwnerField).show();
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
	
	var crumbs = [{"Home":"http://library.ucsd.edu"}, {"Digital Library Collections":"/dc"},{"DAMS Manager":"/damsmanager/"}, {"Excel Import":""}];
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
<form id="mainForm" name="mainForm" method="post" action="/damsmanager/operationHandler.do?excelImport" >
<div class="emBox_ark">
<div class="emBoxBanner">Excel Import</div>
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
		<tr align="left">
			<td height="25px">
				<span class="submenuText">
					<span class="requiredLabel">*</span><span id="sourceTitle" style="font-weight:bold;">Metadata Location</span><b>: </b>
				</span>
			</td>
			<td  align="left">
				<div class="submenuText" id="modsSpan">
					<div id="excel"><input type="text" id="dataPath" name="dataPath" size="52" value="">&nbsp;<input type="button" onclick="showFilePicker('dataPath', event)" value="&nbsp;...&nbsp;"></div>
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
				<span class="submenuText"><span class="requiredLabel">*</span><b>Copyright Status: </b></span>
			</td>
			<td>
				<select id="copyrightStatus" name="copyrightStatus" class="inputText" onChange="copyrightStatusChanged();">
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
					<select id="countryCode" name="countryCode" class="inputText">
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
				<span class="submenuText"><input type="text" id="copyrightOwner" name="copyrightOwner" size="20"></span>
			</td>
		</tr>
		<tr align ="left" id="accessOverrideField" style="display:none;">
			<td height="25px">
				<span class="submenuText"><span class="requiredLabel" id="accessOverride_required" style="padding-left:6px;"></span><b>Access Override: </b></span>
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
				<div>
					 <fieldset class="groupbox_modsIngestOpts"><legend class="slegandText">Import Options</legend>
					    <div title="Check this checkbox for no ingest but pre-processing only." class="submenuText">
							<input checked type="radio" name="importOption" value="pre-processing" checked>
							<span class="submenuText">Preview the converted RDF/XML only, no ingest.</span>
						</div>
					 	<div title="Check this checkbox to import metadata and files." class="submenuText">
							<input type="radio" name="importOption" value="metadataAndFiles">
							<span class="text-special">Ingest metadata and files</span>
							<div class="submenuText" style="margin-top:3px;padding-left:25px;"  title="Enter a filter path for the location to speek up the search. From the popup, click on the folder to select/deselect a location. Multiple loations allowed.">Master Files location: 
								<input type="text" id="filesPath" name="filesPath" size="48" value="">&nbsp;<input type="button" onclick="showFilePicker('filesPath', event)" value="&nbsp;...&nbsp;">
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
	<input type="hidden" name="fs" value=""/>
	<input type="button" name="import" value=" Import " onClick="confirmImport();"/>&nbsp;&nbsp;
	<input type="button" name="cancel" value=" Cancel " onClick="document.location.href='/damsmanager/excelImport.do'"/>
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
