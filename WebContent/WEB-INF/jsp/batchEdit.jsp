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
    var letters = /^[A-Za-z]+$/;
    function confirmEdit(){
        var isIngest = document.getElementById("importOption").checked;
        var preingestOption = $('input[name="preingestOption"]:checked').val();

        var file = document.mainForm.dataPath.value;
        if (file == "") {
            alert("Please choose Excel metadata source file location.");
            document.mainForm.dataPath.focus();
            return false;
        }

        document.mainForm.enctype = "multipart/form-data";

        var message = "Are you sure to you want to do pre-ingest validation? \n";;

        if (isIngest) {
            message = "This will update object decriptive metadata in DAMS production. \nHave all pre-ingest validations been completed? \n";
        } else if (preingestOption == 'pre-processing')
            message = "Are you sure to you want to review the converted RDF/XML? \n";

        var exeConfirm = confirm(message);
        if(!exeConfirm)
            return false;

        document.mainForm.action = "/damsmanager/operationHandler.do?batchEdit&progress=0&formId=mainForm&sid=" + getSid();
        displayMessage("message", "");
        getAssignment("mainForm");
        displayProgressBar(0);
        document.getElementById("import").disabled = false;
    }

    function onIngestSelectionChange(o) {
        if ($(o).prop('name') === 'importOption') {
            $('input[name="preingestOption"]').prop('disabled', false);
            $('input[name="preingestOption"]').prop('checked', true);
            $(o).prop('disabled', true);
        } else {
            $('input[name="importOption"]').prop('disabled', false);
            $('input[name="importOption"]').prop('checked', true);
            $(o).prop('disabled', true);
        }
    }

    $( document ).ready(function() {
      document.getElementById("import").onclick = function() {
        //disable
        this.disabled = true;

        if ( !confirmEdit() )
          this.disabled = false;
      }
    });

    var crumbs = [{"Home":"http://library.ucsd.edu"}, {"Digital Library Collections":"/dc"},{"DAMS Manager":"/damsmanager/"}, {"Batch Edit":""}];
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
<form id="mainForm" name="mainForm" method="post" action="/damsmanager/operationHandler.do?batchEdit" >
<div class="emBox_ark">
<div class="emBoxBanner">Batch Edit</div>

<div style="margin-top:10px;padding-left:20px;" align="left">
    <table>
        <tr align="left">
            <td height="30px">
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
        <tr align="left">
            <td colspan="2">
                <div style="padding-top:20px;">
                    <fieldset class="groupbox_modsIngestOpts"><legend class="slegandText">Batch Edit</legend>
                        <div title="Check this checkbox for no ingest but pre-processing only." class="submenuText">
                            <input type="checkbox" name="preingestOption" id="preingestOption" onchange="onIngestSelectionChange(this);" checked >
                            <span class="submenuText" style="margin:10px;">Preview the converted RDF/XML only, no ingest.</span>
                        </div>
                        <div title="Batch Overlay metadata" class="submenuText">
                            <input type="checkbox" name="importOption" id="importOption" onchange="onIngestSelectionChange(this);" disabled >
                            <span class="submenuText" style="margin:10px;">Batch metadata overlay.</span>
                        </div>
                    </fieldset>
                </div>
            </td>
        </tr>
        <tr><td colspan="2" style="padding-left:6px;"><span class="submenuText"><span class="requiredLabel">*</span><b>Required Field</b></span></td>
    </table>
</div>
<div class="buttonDiv">

    <input type="button" name="import" id="import" value=" Submit "/>&nbsp;&nbsp;
    <input type="button" name="cancel" value=" Cancel " onClick="document.location.href='/damsmanager/batchEdit.do'"/>
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
