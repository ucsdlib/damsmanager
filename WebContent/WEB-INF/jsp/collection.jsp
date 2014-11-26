<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<c:set var="actionNameLen">${fn:length(model.action)}</c:set>  
<c:set var="actionNameFl">${fn:substring(model.action, 0, 1)}</c:set>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<head>
<jsp:include flush="true" page="/jsp/libheader.jsp" />
<script src="//code.jquery.com/jquery-1.10.2.js"></script>
<script src="//code.jquery.com/ui/1.11.1/jquery-ui.js"></script>
</head>
<body style="background-color:#fff;">
<script type="text/javascript">
	function confirmSubmit() {

		var collTitle = document.mainForm.collTitle.value.trim();
		if (collTitle.length == 0) {
	    	alert("Please enter a collection title.");
	    	document.mainForm.collTitle.focus();
			return false;
		}

		var unitIndex = document.mainForm.unit.selectedIndex;  
	    if(unitIndex == 0){
	    	alert("Please select a unit.");
			return false;
	    }
	    
		var collTypeIndex = document.mainForm.collType.selectedIndex;  
	    if(collTypeIndex == 0){
	    	alert("Please select a collection type.");
			return false;
	    }
	    
		var visibilityIndex = document.mainForm.visibility.selectedIndex;  
	    if(visibilityIndex == 0){
	    	alert("Please select a visibility value.");
			return false;
	    }
        
	    var message = "Are you sure to create a new collection record? \n";

	    var exeConfirm = confirm(message);
	    if(!exeConfirm)
	    	return false;
	    
    	document.mainForm.action = "/damsmanager/collection.do?action=save&sid=" + getSid();
    	mainForm.submit();
	}
	
	function createCollection() {
		document.location.href="/damsmanager/collection.do?create";
	}
	
	var crumbs = [{"Home":"http://library.ucsd.edu"}, {"Digital Library Collections":"/dc"},{"DAMS Manager":"/damsmanager/"}, {"Collection Create":""}];
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
<form id="mainForm" name="mainForm" method="post" action="/damsmanager/collection.do?create" >
<div class="emBox_ark">
<div class="emBoxBanner">Collection <c:out value="${fn:toUpperCase(actionNameFl)}${fn:substring(model.action, 1, actionNameLen)}" /></div>

<div style="background:#DDDDDD;padding-top:8px;padding-bottom:8px;padding-left:25px;" align="left">
	<c:choose>
	  <c:when test="${model.action == 'create'}">
		<span class="submenuText" style="padding-right:15px;"><span class="requiredLabel">*</span><b>Collection Title</b></span>
		<span class="submenuText"><input type="text" id="collTitle" name="collTitle" size="56" value="${model.collTitle}"></span>
	  </c:when>
	  <c:otherwise>
		<span class="submenuText"><span class="requiredLabel">*</span><b>Collection Selection:&nbsp;</b></span>
		<span style="padding:0px 5px 0px 7px;">
			<select id="category" name="category" onChange="selectCollection(this);" class="inputText" >
				<option value=""> -- collections -- </option>
				<c:forEach var="entry" items="${model.categories}">
					<c:set var="colNameLen"> ${fn:length(entry.key)}</c:set>
					<option value="${entry.value}" <c:if test="${model.category == entry.value}">selected</c:if>>
                     			<c:choose>
							<c:when test="${colNameLen > 72}"><c:out value="${fn:substring(entry.key, 0, 68)}" /> ...</c:when>
							<c:otherwise><c:out value="${entry.key}" /></c:otherwise>
						</c:choose>
                      	</option>
				</c:forEach>
			</select>
		</span>
		<a class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-icon-primary" onclick="createCollection()" style="font-size:12px;font-weight:normal;padding:2px;">New</a>
	  </c:otherwise>
	</c:choose>
</div>

<div style="margin-top:10px;padding-left:20px;" align="left">
	<table>
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
						<c:set var="colNameLen"> ${fn:length(entry.key)}</c:set>
						<option value="${entry.value}" <c:if test="${model.parentCollection == entry.value}">selected</c:if>>
	                     			<c:choose>
								<c:when test="${colNameLen > 75}"><c:out value="${fn:substring(entry.key, 0, 71)}" /> ...</c:when>
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
		<tr align ="left">
			<td height="25px">
				<div class="submenuText"><span class="requiredLabel">*</span><b>Visibility: </b></div>
			</td>
			<td>
				<select id="visibility" name="visibility" class="inputText">
					<option value=""> -- Visibility -- </option>
					<c:forEach var="entry" items="${model.visibilities}">
						<option value="${entry}" <c:if test="${model.visibility == entry}">selected</c:if>>
                      			<c:out value="${entry}" />
                       	</option>
					</c:forEach>
				</select>
			</td>
		</tr>
		<tr><td colspan="2" style="height:40px;padding-top:20px;"><span class="submenuText">(<span class="requiredLabel">*</span>Required Field</span>)</td>
	</table>
</div>
<div class="buttonDiv">
	<input type="button" name="collectionEdit" value=" Submit " onClick="confirmSubmit();"/>&nbsp;&nbsp;
	<input type="button" name="ActionCancel" value=" Cancel " onClick="document.location.href='/damsmanager/'"/>
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
