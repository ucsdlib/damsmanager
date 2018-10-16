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
    function confirmSubmit() {
        var collIndex = document.mainForm.category.selectedIndex;
        var collTitle = document.mainForm.category.options[collIndex].text;
        var message = "Are you sure you want to generate files report for '" + collTitle + "'? \n";
        if (collIndex > 0)
            message = "Are you sure you want to generate files report for " + document.mainForm.category.options[collIndex].text + "? \n";

        var exeConfirm = confirm(message);
        if(!exeConfirm)
            return false;

        displayMessage("message", "");
        getAssignment("mainForm");
        displayProgressBar(0);
    }

    function selectCollection(select) {
        document.mainForm.actionValue.value = "edit";
        mainForm.submit();
    }

    var crumbs = [{"Home":"http://library.ucsd.edu"}, {"Digital Library Collections":"/dc"},{"DAMS Manager":"/damsmanager/"}, {"Files Report":""}];
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
<form id="mainForm" name="mainForm" method="post" action="/damsmanager/operationHandler.do?fileReport" >
<div class="emBox_ark">
<div class="emBoxBanner">File Report</div>

<div style="background:#DDDDDD;padding-top:8px;padding-bottom:18px;padding-left:25px;" align="left">
    <span class="submenuText"><span class="requiredLabel">*</span><b>Collection Selection:&nbsp;</b></span>
    <span style="padding:0px 5px 0px 7px;">
        <select id="category" name="category" onChange="selectCollection(this);" class="inputText" >
            <option value=""> -- Choose Collection -- </option>
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
<div class="buttonDiv">
    <input type="button" name="collectionEdit" value=" Submit " onClick="confirmSubmit();"/>&nbsp;&nbsp;
    <input type="button" name="actionCancel" value=" Cancel " onClick="document.location.href='/damsmanager/'"/>
</div>
</div>
</form>
</div>
    <jsp:include flush="true" page="/jsp/status.jsp" />
    <div id="message" class="submenuText" style="text-align:left;">${model.message}</div>
</td>
</div>
</td>
</tr>
</table>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
</html>