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

    function confirmImport() {
    var file = document.mainForm.dataPath.value;
        if (file == "") {
            alert("Please choose Excel metadata source file location.");
            document.mainForm.dataPath.focus();
            return false;
        }

        var isIngest = document.getElementById("importOption").checked;
        var message = "Are you sure you want to preview Subjects? \n";
        if (isIngest)
            message = "Are you sure you want to create Subjects? \n";
        var exeConfirm = confirm(message);
        if(!exeConfirm)
            return false;
        
        document.mainForm.action = "/damsmanager/operationHandler.do?subjectImport&progress=0&formId=mainForm&sid=" + getSid();
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

	function reloadPage(){
		var dsIdx = document.mainForm.ts.selectedIndex;
		var ds = document.mainForm.ts.options[dsIdx].value;
		document.location.href="/damsmanager/subjectImport.do?ts=" + ds;
	}
	
	var crumbs = [{"Home":"http://library.ucsd.edu"}, {"Digital Library Collections":"/dc"},{"DAMS Manager":"/damsmanager/"}, {"Subject Import":""}];
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
<div id="main" class="mainDiv" style="margin-top:15px;">
<div class="doc_link"><a href=" https://lisn.ucsd.edu/display/dlpg/DAMS+4+Access+Control+Implementation#DAMS4AccessControlImplementation-ProductionIngestProposalProductionIngestProposal(AT/MARCandExcelingesttools)" target="_blank">View Documentation</a></div>
<form id="mainForm" name="mainForm" method="post" action="/damsmanager/collectionImport.do" >
<div class="emBox_ark">
<div class="emBoxBanner">Subject Import</div>

<div style="background:#DDDDDD;padding-top:8px;padding-bottom:8px;padding-left:25px;" align="left"></div>

<div style="margin-top:20px;padding-left:20px;" align="left">
    <table>
        <tr align="left">
            <td height="25px">
                <span class="submenuText">
                    <span class="requiredLabel">*</span><span id="sourceTitle" style="font-weight:bold;">Metadata Location</span><b>: </b>
                </span>
            </td>
            <td  align="left">
                <div class="submenuText" id="modsSpan">
                    <div id="excel"><input type="text" id="dataPath" name="dataPath" size="72" value="">&nbsp;<input type="button" onclick="showFilePicker('dataPath', event)" value="&nbsp;...&nbsp;"></div>
                </div>
            </td>
        </tr>
		<tr align="left">
			<td colspan="2">
				<div title="Ingest subject" class="menuText" style="padding-top:10px;margin-bottom:-10px">
					<span style="padding-left: 2px"><input class="pcheckbox" type="checkbox" name="importOption" id="importOption" onchange="onIngestSelectionChange(this);"></span>
					<span class="submenuText" style="vertical-align:2px;"><b>Ingest subject</b></span>
				</div>
			</td>
		</tr>
        <tr align="left">
            <td colspan="2">
                <div>
                    <fieldset class="groupbox_modsIngestOpts"><legend class="slegandText">Pre-ingest validation</legend>
                        <div title="Check this checkbox for no ingest but pre-processing only." class="submenuText">
                            <input checked type="radio" name="preingestOption" value="pre-processing" checked>
                            <span class="submenuText">Preview the converted RDF/XML only, no ingest.</span>
                        </div>
                        <div title="Check this checkbox to pre-ingest test for matching subjects." class="submenuText">
                            <input type="radio" name="preingestOption" value="test-matching">
                            <span class="text-special">Test for matching subjects</span>
                        </div>
                    </fieldset>
                </div>
            </td>
        </tr>
		<tr><td colspan="2" style="padding-left:6px;"><span class="submenuText"><span class="requiredLabel">*</span><b>Required Field</b></span></td>
	</table>
</div>
<div class="buttonDiv">
    <input checked type="hidden" name="filesCheckPath" value="" />
	<input type="button" name="import" value=" Submit " onClick="confirmImport();"/>&nbsp;&nbsp;
	<input type="button" name="cancel" value=" Cancel " onClick="document.location.href='/damsmanager/subjectImport.do'"/>
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
