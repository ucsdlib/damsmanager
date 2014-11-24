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
	function confirmRelease(){
		var collIndex = document.mainForm.category.selectedIndex;
	    if(collIndex == 0){
	    	alert("Please select a collection to release!");
	    	return false;
	    }
	    
		var collTitle = document.mainForm.category.options[collIndex].text;
		var message = "Are you sure to ";
		var selectedOptionIdx = document.mainForm.releaseOption.selectedIndex;
        var optionVal = document.mainForm.releaseOption.options[selectedOptionIdx].value;
        if (selectedOptionIdx == 0) {
        	alert("Please select a release option for releasing collections.");
        	return false;
        } else if (optionVal == 'mergeRelease') {
        	var collToMergeSelect = document.mainForm.collectionToMerge;
        	if (collToMergeSelect.selectedIndex == 0) {
        		alert("Please select a collection to merge.");
        		return false;
        	}
        	
        	var collToMergeTitle = collToMergeSelect.options[collToMergeSelect.selectedIndex].text;
        	if (collToMergeTitle == collTitle) {
        		alert("The collection to merge could not be the same as the releasing collection '" + collTitle.replace("\"", "\\\"") + "'.");
        		return false;
        	}

        	message += " merge objects in collection '" + collTitle +  "' to '" + collToMergeTitle + "'?"
        } else if (optionVal == 'one-offsRelease') {
        	message += " release objects in collection \"" + collTitle.replace("\"", "\\\"") + "\" as one-offs?";
        }  else {
        	message += " release objects in collection \"" + collTitle.replace("\"", "\\\"") + "\"?";
        }

	    var exeConfirm = confirm(message);
	    if(!exeConfirm)
	    	return false;
	    
    	document.mainForm.action = "/damsmanager/operationHandler.do?collectionRelease&progress=0&formId=mainForm&sid=" + getSid();
    	displayMessage("message", "");
    	getAssignment("mainForm");
		displayProgressBar(0);
	}

	function releaseOptionChange(){
		var obj = document.mainForm.releaseOption;
		var releaseOptions = obj.options;
		var releaseOptionVal = releaseOptions[obj.selectedIndex].value;
		if (releaseOptionVal == 'mergeRelease') {
			$("#collToMergeLabel").show();
			$("#mergeTo").show();
			$("#releaseStateLabel").hide();
			$("#releaseState").hide();
			$("#releaseStateRow").css("height", "1px");
		} else if (releaseOptionVal == 'one-offsRelease') {
			$("#collToMergeLabel").hide();
			$("#mergeTo").hide();
			$("#releaseStateLabel").hide();
			$("#releaseState").hide();
		} else {
			$("#releaseStateLabel").show();
			$("#releaseState").show();
			$("#releaseStateRow").css("height", "30px");
			$("#collToMergeLabel").hide();
			$("#mergeTo").hide();
		}
	}
	
	$(function() {
		releaseOptionChange();
	})

	var crumbs = [{"Home":"http://library.ucsd.edu"}, {"Digital Library Collections":"/dc"},{"DAMS Manager":"/damsmanager/"}, {"Collection Release":""}];
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
<form id="mainForm" name="mainForm" method="post" action="/damsmanager/operationHandler.do?collectionRelease" >
<div class="emBox_ark">
<div class="emBoxBanner">Collection Release</div>
<div style="background:#DDDDDD;padding-top:8px;padding-bottom:8px;padding-left:25px;" align="left">
		<span class="submenuText" style="width:140px"><b>Collection Selection:&nbsp;</b></span>
		<span style="padding:0px 5px 0px 0px;">
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
<div style="margin-top:10px;padding-left:20px;text-align:left;">
	<table>
		<tr align="left">
			<td height="30px">
				<span class="submenuText"><b>Release As: </b></span>
			</td>
			<td>
				<select id="releaseOption" name="releaseOption" class="inputText" onChange="releaseOptionChange(this);">
					<option value="" selected> -- Options -- </option>
					<option value="newRelease" selected>New Collection</option>
					<option value="mergeRelease">Merge To Collection</option>
					<option value="one-offsRelease">One-Offs</option>
				</select>
		    </td>
		</tr>
		<tr align="left" id="releaseStateRow">
			<td width="140px">
				<div class="submenuText" id="releaseStateLabel"><b>Release State: </b></div>
			</td>
			<td>
				<select id="releaseState" name="releaseState" class="inputText">
					<option value="public" selected>Public Access</option>
					<option value="local">Local Access</option>
					<option value="curator">Curator Only</option>
				</select>
		    </td>
		</tr>
		<tr align="left">
			<td height="30px">
				<div id="collToMergeLabel" class="submenuText" style="display:none;"><b>Merge To Collection: </b></div>
			</td>
			<td>
				<div id="mergeTo" class="submenuText" style="display:none;">
					<select id="collectionToMerge" name="collectionToMerge" class="inputText" >
						<option value=""> -- collections -- </option>
						<c:forEach var="entry" items="${model.collectionsToMerge}">
							<c:set var="colNameLen"> ${fn:length(entry.key)}</c:set>
							<option value="${entry.value}" <c:if test="${model.collectionToMerge == entry.value}">selected</c:if>>
		                     	<c:choose>
									<c:when test="${colNameLen > 75}"><c:out value="${fn:substring(entry.key, 0, 71)}" /> ...</c:when>
									<c:otherwise><c:out value="${entry.key}" /></c:otherwise>
								</c:choose>
		                      	</option>
						</c:forEach>
					</select>
				</div>
			</td>
		</tr>
	</table>
</div>
<div class="buttonDiv">
	<input type="button" name="release" value=" Release " onClick="confirmRelease();"/>&nbsp;&nbsp;
	<input type="button" name="releaseCancel" value=" Cancel " onClick="document.location.href='/damsmanager/'"/>
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
