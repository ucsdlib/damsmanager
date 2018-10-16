<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<head>
<jsp:include flush="true" page="/jsp/libheader.jsp" />
<script src="//code.jquery.com/jquery-1.10.2.js"></script>
<script src="//code.jquery.com/ui/1.11.1/jquery-ui.js"></script>
</head>
<body style="background-color:#fff;">
<script type="text/javascript">
	var collTypes = ["AssembledCollection", "ProvenanceCollection", "ProvenanceCollectionPart"];
	function confirmSubmit() {

		var collTitle = document.mainForm.collTitle.value.trim();
		if (collTitle.length == 0) {
	    	alert("Please enter a collection title.");
	    	document.mainForm.collTitle.focus();
			return false;
		}

	    
		var collTypeIndex = document.mainForm.collType.selectedIndex;  
	    if(collTypeIndex == 0){
	    	alert("Please select a collection type.");
			return false;
	    }

		var unitIndex = document.mainForm.unit.selectedIndex;  
	    if(unitIndex == 0){
	    	alert("Please select a unit.");
			return false;
	    }
	    
		var visibilityIndex = document.mainForm.visibility.selectedIndex;  
	    if(visibilityIndex == 0){
	    	alert("Please select a visibility value.");
			return false;
	    }
	    
	    var collIndex = document.mainForm.category.selectedIndex;
		var parentIndex = document.mainForm.parentCollection.selectedIndex;  
	    if(parentIndex != 0){
	    	var collTypeValue = document.mainForm.collType.options[collTypeIndex].value;
	    	var parentColl = document.mainForm.parentCollection.options[parentIndex].text;
	    	var parentCollType = parentColl.substring(parentColl.lastIndexOf("[")+1, parentColl.lastIndexOf("]"));

    		var error = "";
    		if (category.options[collIndex].value == parentCollection.options[parentIndex].value) {
	    		error = "Parent collection can't be the same collection.";
	   		} else if (collTypeValue == collTypes[2]) { // ProvenanceCollectionPart
	   			if (parentCollType != collTypes[1]) 
	   				error = "A " + collTypes[2] + " can only have a " + collTypes[1] + " parent.";
	    	} else if (collTypeValue == collTypes[1]) { // ProvenanceCollection
	   			if (parentCollType != collTypes[0])
    				error = "A " + collTypes[1] + " can only have a " + collTypes[0] + " parent.";
	   		} else if (collTypeValue == collTypes[0]) { // AssembledCollection
	   			if (parentCollType != collTypes[0])
    				error = "An " + collTypes[0] + " can only have an " + collTypes[0] + " parent.";
    		}
    		
    		if (error.length > 0) {
    			alert ("Please select a valid parent collection!" + "\nNote: " + error);
    			return false;
    		}
	    }
        
	    var message = "Are you sure you want to Create collection '" + collTitle + "'? \n";
	    if (collIndex > 0)
	    	message = "Are you sure you want to Edit " + document.mainForm.category.options[collIndex].text + "? \n";

	    var exeConfirm = confirm(message);
	    if(!exeConfirm)
	    	return false;
	    
	    document.mainForm.actionValue.value = "save";
    	document.mainForm.action = "/damsmanager/collection.do";
    	mainForm.submit();
	}

	function selectCollection(select) {
		document.mainForm.actionValue.value = "edit";
		mainForm.submit();
	}

	var crumbs = [{"Home":"http://library.ucsd.edu"}, {"Digital Library Collections":"/dc"},{"DAMS Manager":"/damsmanager/"}, {"Collection Create/Edit":""}];
	drawBreadcrumbNMenu(crumbs, "tdr_crumbs_content", true);
</script>
<jsp:include flush="true" page="/jsp/libanner.jsp" />
<table align="center" cellspacing="0px" cellpadding="0px">
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
<div id="main" class="mainDiv" style="margin-bottom:50px;margin-top:30px;">
<form id="mainForm" name="mainForm" method="post" action="/damsmanager/collection.do" >
<div class="doc_link"><a href=" https://lisn.ucsd.edu/display/dlpg/DAMS+4+Access+Control+Implementation#DAMS4AccessControlImplementation-ProductionIngestProposalProductionIngestProposal(AT/MARCandExcelingesttools)" target="_blank">View Documentation</a></div>
<div class="emBox_ark">
<div class="emBoxBanner">Collection Create/Edit</div>

<div style="background:#DDDDDD;padding-top:8px;padding-bottom:8px;padding-left:25px;" align="left">
	<span class="submenuText"><span class="requiredLabel">*</span><b>Collection Selection:&nbsp;</b></span>
	<span style="padding:0px 5px 0px 7px;">
		<select id="category" name="category" onChange="selectCollection(this);" class="inputText" >
			<option value=""> -- Create New Collection -- </option>
			<c:forEach var="entry" items="${model.categories}">
				<c:set var="colNameLen"> ${fn:length(entry.key)}</c:set>
				<c:set var="splitText" value="${fn:split(entry.key,'[')}" />
				<c:set var="typeIndex">${fn:indexOf(entry.key, splitText[fn:length(splitText)-1])}</c:set>
				<option value="${entry.value}" title="${entry.key}" <c:if test="${model.category == entry.value}">selected</c:if>>
                   <c:choose>
						<c:when test="${colNameLen > 75}"><c:out value="${fn:substring(entry.key, 0, 72-(colNameLen-typeIndex))}" />...[<c:out value="${splitText[fn:length(splitText)-1]}" /></c:when>
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
				<span class="submenuText"><span class="requiredLabel">*</span><b>Collection Title: </b></span>
			</td>
			<td>
				<span class="submenuText"><input type="text" id="collTitle" name="collTitle" size="56" value="${model.collTitle}"></span>
			</td>
		</tr>
		<tr align ="left">
			<td height="25px">
				<span class="submenuText"><span class="requiredLabel">*</span><b>Collection Type: </b></span>
			</td>
			<td>
				<select id="collType" name="collType" class="inputText">
					<option value=""> -- Collection Type -- </option>
					<c:forEach var="entry" items="${model.collTypes}">
						<option value="${entry}" <c:if test="${model.collType == entry}">selected</c:if>>
                      			<c:out value="${entry}" />
                       	</option>
					</c:forEach>
				</select>
			</td>
		</tr>
		<tr align ="left">
			<td height="25px">
				<span class="submenuText" style="padding-left: 6px"><b>Parent Collection: </b></span>
			</td>
			<td>
				<select id="parentCollection" name="parentCollection" class="inputText" >
					<option value=""> -- collections -- </option>
					<c:forEach var="entry" items="${model.categories}">
						<c:set var="colNameLen">${fn:length(entry.key)}</c:set>
						<c:set var="splitText" value="${fn:split(entry.key,'[')}" />
						<c:set var="typeIndex">${fn:indexOf(entry.key, splitText[fn:length(splitText)-1])}</c:set>
						<option value="${entry.value}" <c:if test="${model.parentCollection == entry.value}">selected</c:if>>
	                     	<c:choose>
								<c:when test="${colNameLen > 75}"><c:out value="${fn:substring(entry.key, 0, 72-(colNameLen-typeIndex))}" />...[<c:out value="${splitText[fn:length(splitText)-1]}" /></c:when>
								<c:otherwise><c:out value="${entry.key}" /></c:otherwise>
							</c:choose>
	                    </option>
					</c:forEach>
				</select>
			</td>
		</tr>
		<tr align ="left">
			<td height="25px">
				<span class="submenuText"><span class="requiredLabel">*</span><b>Admin Unit: </b></span>
			</td>
			<td>
				<select id="unit" name="unit" class="inputText">
					<option value=""> -- units -- </option>
					<c:forEach var="entry" items="${model.units}">
						<option value="${entry.value}" <c:if test="${model.unit == entry.value}">selected</c:if>>
                      			<c:out value="${entry.key}" />
                       	</option>
					</c:forEach>
				</select>
			</td>
		</tr>
		<tr><td colspan="2" style="height:40px;padding-top:20px;"><span class="submenuText">(<span class="requiredLabel">*</span>Required Field</span>)</td>
	</table>
</div>
<div class="buttonDiv">
    <input type="hidden" name="visibility" value="${model.visibility}"/>
    <input type="hidden" name="actionValue" value="${model.actionValue}"/>
	<input type="button" name="collectionEdit" value=" Submit " onClick="confirmSubmit();"/>&nbsp;&nbsp;
	<input type="button" name="actionCancel" value=" Cancel " onClick="document.location.href='/damsmanager/'"/>
</div>
</div>
</form>
<div id="message" class="submenuText" style="text-align:left;">${model.message}</div>
</div>
</td>
</tr>
</table>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
</html>
