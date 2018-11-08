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
<body style="background-color:#fff;">
<script type="text/javascript">
    function confirmSubmit() {
       // begin date validation
       var beginDate = document.mainForm.beginDate.value.trim();

       if (beginDate.length == 0) {
           alert ("Please enter begin date in format yyyy-mm-dd.");
           document.mainForm.beginDate.focus();
           return false;
       }

       beginDate = parseDate(beginDate);
       if (beginDate == null) {
           alert ("Please enter a valid begin date in format yyyy-mm-dd.");
           document.mainForm.beginDate.focus();
           return false;
       }

        var message = "Generate collection reports will take some time. Are you sure to you want to continue? \n";;

        var exeConfirm = confirm(message);
        if(!exeConfirm)
            return false;

        document.mainForm.submit();

    }

    // parse a date in yyyy-mm-dd format
    function parseDate(input) {
      var parts = input.split('-');
      if (parts.length != 3) {
          return null;
      }
      return new Date(parts[0], parts[1]-1, parts[2]); // Note: months are 0-based
    }

    $(function() {
        var beginCal = $( "#beginDate" );
        $(beginCal).datepicker({
            dateFormat: "yy-mm-dd",
            buttonImage: "/damsmanager/images/calendar.jpg"
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
<div class="main-title">DAMS Collection Status Report</div>
<form id="mainForm" name="mainForm" method="post" action="/damsmanager/collectionStatusReport.do" >
  <div style="margin-top:10px;padding-left:20px;" align="left">
    <div style="margin:30 0;text-alignment:left">
        <label class="menuText"><b>Begin Date: </b></label>
        <span class="menuText">
            <input type="text" id="beginDate" name="beginDate" size="25" style="cursor:pointer;margin-right:2px;" value="${model.beginDate}">
            <input type="button" style="margin-left:5px;" name="collectionReport" id="collectionReport" value=" Search " onClick="confirmSubmit();"/>
        </span>
    </div>
    <hr />
    <table>
        <c:if test="${fn:length(model.beginDate) > 0}">
            <tr align="left">
                <td height="25px" colspan="2">
                    <span class="submenuText"><b>Collections Released: </b></span>
                </td>
            </tr>
            <c:forEach var="entry" items="${model.recordReleased}">
                <tr align="left">
                    <td height="25px">
                        <a href="${entry.key}" class="result" target="_blank"/>${entry.key}</a>
                    </td>
                    <td  align="left">
                        <span class="result">${entry.value}</span>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${model.recordReleased.size() == 0}">
                <tr align="left">
                    <td height="25px" colspan="2">
                        <span class="result">No results.</span>
                    </td>
                </tr>
            </c:if>
        </c:if>

        <c:if test="${fn:length(model.beginDate) > 0}">
            <tr align="left">
                <td height="25px" colspan="2">
                    <span class="submenuText"><b>Collections with objects added: </b></span>
                </td>
            </tr>
            <c:forEach var="entry" items="${model.recordAdded}">
                <tr align="left">
                    <td height="25px">
                        <a href="${entry.key}" class="result" target="_blank"/>${entry.key}</a>
                    </td>
                    <td  align="left">
                        <span class="result">${entry.value}</span>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${model.recordAdded.size() == 0}">
                <tr align="left">
                    <td height="25px" colspan="2">
                        <span class="result">No results.</span>
                    </td>
                </tr>
            </c:if>
        </c:if>

        <c:if test="${fn:length(model.beginDate) > 0}">
            <tr align="left">
                <td height="25px" colspan="2">
                    <span class="submenuText"><b>Collections with objects removed: </b></span>
                </td>
            </tr>
            <c:forEach var="entry" items="${model.recordRemoved}">
                <tr align="left">
                    <td height="25px">
                        <a href="${entry.key}" class="result"/>${entry.key}</a>
                    </td>
                    <td  align="left">
                        <span class="result">${entry.value}</span>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${model.recordRemoved.size() == 0}">
                <tr align="left">
                    <td height="25px" colspan="2">
                        <span class="result">No results.</span>
                    </td>
                </tr>
            </c:if>
        </c:if>

        <c:if test="${fn:length(model.beginDate) > 0}">
            <tr align="left">
                <td height="25px" colspan="2">
                    <span class="submenuText"><b>Collections with objects edited: </b></span>
                </td>
            </tr>
            <c:forEach var="entry" items="${model.recordEdited}">
                <tr align="left">
                    <td height="25px">
                        <a href="${entry.key}" class="result"/>${entry.key}</a>
                    </td>
                    <td  align="left">
                        <span class="result">${entry.value}</span>
                    </td>
                </tr>
            </c:forEach>
            <c:if test="${model.recordEdited.size() == 0}">
                <tr align="left">
                    <td height="25px" colspan="2">
                        <span class="result">No results.</span>
                    </td>
                </tr>
            </c:if>
        </c:if>

    </table>
    <c:if test="${fn:length(model.beginDate) > 0}">
        <hr />
        <div class="export">
            <a href="/damsmanager/collectionStatusReport.do?beginDate=${model.beginDate}&export">
                <img src="images/excel-icon.png" border="0" width="16px" />
                <span style="display:table-cell;vertical-align:top;font-size:11ps;font-weight:bold;">&nbsp;Export CSV</span></a>
        </div>
    </c:if>
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
