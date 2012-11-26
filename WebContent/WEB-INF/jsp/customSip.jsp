<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<c:set var="tsNameLen"> ${fn:length(data.triplestore)}</c:set>  
<c:set var="tsNameFl" scope="page">${fn:substring(data.triplestore, 0, 1)}</c:set> 
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
<body style="background-color:#fff;">
<script type="text/javascript">	
	function confirmPopulation(){
		var dsIdx = document.mainForm.ds.selectedIndex;
		var ds = document.mainForm.ds.options[dsIdx].value;
		var collectionIndex = document.mainForm.collection.selectedIndex;
      	var sipOptions = document.mainForm.sipOption;
      	var sipOption = "tsNew";
      	var selectedSipOption = null;
      	for(var i=0; i<sipOptions.length;i++){
      		if(sipOptions[i].checked){
      			selectedSipOption = sipOptions[i];
      		}
      	}
		sipOption = selectedSipOption.value;
      	if(collectionIndex == 0 && (sipOption != sipOptions[3].value && sipOption != sipOptions[4].value)){
	    	alert("Please select a collection.");
			return false;
	    }
	    var exeConfirm = confirm("Are you sure to populate the triplestore?");
	    if(!exeConfirm)
	    	return false;
	    
    	document.mainForm.action = "/damsmanager/operationHandler.do?ds=" + ds + "&dataConvert&progress=0&formId=mainForm&sid=" + getSid();
    	displayMessage("message", "");
    	getAssignment("mainForm");
		displayProgressBar(0);
	}
	
	function exportRdf(){
		var dsIdx = document.mainForm.ds.selectedIndex;
		var ds = document.mainForm.ds.options[dsIdx].value;	    
	    var collectionIndex = document.mainForm.collection.selectedIndex;

	    var selectedOption =  document.mainForm.collection.options[collectionIndex];     	          
      	var category = selectedOption.value;
      	var sipOptions = document.mainForm.sipOption;
      	var sipOption = "tsNew";
      	var selectedSipOption = null;
      	for(var i=0; i<sipOptions.length;i++){
      		if(sipOptions[i].checked){
      			selectedSipOption = sipOptions[i];
      		}
      	}
		sipOption = selectedSipOption.value;
      	if(collectionIndex == 0 && (sipOption != sipOptions[4].value && sipOption != sipOptions[3].value)){
	    	alert("Please select a collection.");
			return false;
	    }	
	    var url = "/damsmanager/viewRdf.do?ds=" + ds + "&export&collectionId=" + category + "&op=" + sipOption;		    	
		window.open(url);
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
		var dsIdx = document.mainForm.ds.selectedIndex;
		var ds = document.mainForm.ds.options[dsIdx].value;
		document.location.href="/damsmanager/customSip.do?ds=" + ds;
	}
</script>
<jsp:include flush="true" page="/jsp/libanner.jsp" />
<table align="center" cellspacing="0px" cellpadding="0px" class="bodytable">
<tr><td>
	
<div id="tdr_crumbs">
	<div id="tdr_crumbs_content">
		<span class="location" style="float:right;"><a href="logout.do?">Log out</a></span><span style="float:right;"><jsp:include flush="true" page="/jsp/menus.jsp" /></span>
		<ul>
			<li><a href="http://libraries.ucsd.edu">Home</a></li>
			<li><a href="/curator">Digital Library Collections</a></li>
			<li><a href="/damsmanager/">XDRE Manager</a></li>
			<li><a href="/damsmanager/dataConverter.do">Data Converter</a></li>
			<li>Custom Sip</li>
		</ul>
	</div><!-- /tdr_crumbs_content -->
	
	<!-- This div is for temporarily writing breadcrumbs to for processing purposes -->
		<div id="temporaryBreadcrumb" style="display: none">
	</div>
</div><!-- /tdr_crumbs -->
</td>
</tr>
<tr>
<td align="center">
<div id="main" class="gallery" align="center">
<div id="mainFormDiv" align="center">
<form id="mainForm" name="mainForm" method="post" action="/damsmanager/operationHandler.do?dataConvert" >
<div class="adjustDiv"  align="center">
<div class="emBox">
<div class="emBoxBanner">TripleStore Population</div>
<div id="collectionDiv" style="background:#DDDDDD;" align="left">
		<span id="dsSpan" class="menuText" Title="Double click to change the triplestore used for operation." ondblclick="setTriplestore();" onMouseOver="this.style.cursor='pointer'">${fn:toUpperCase(tsNameFl)}${fn:substring(data.triplestore, 1, tsNameLen)} </span>
			<span id="dsSelectSpan" ondblclick="resetTriplestore();" style="display:none" >
				<select name="ds" id="ds" onChange="reloadPage();"><option value=""> -- Triplestore -- </option>
						<c:forEach var="entry" items="${data.triplestores}">
							<option value="${entry.value}" <c:if test="${data.triplestore == entry.key}">selected</c:if>>
                       			<c:out value="${entry.key}" />
                        	</option>
						</c:forEach>
				</select>&nbsp;
			</span>
			<span class="menuText">Collection Chooser:&nbsp;</span>
			<span><select id="collection" name="collection" class="inputText">
						<option value=""> -- collections -- </option>
						<c:forEach var="entry" items="${data.collections}">
							<option value="${entry.value}" <c:if test="${data.collectionId == entry.value}">selected</c:if>>
                       			<c:out value="${entry.key}" />
                        	</option>
						</c:forEach>
					</select>
			</span>
</div>
<div style="margin-top:20px;">
	 <fieldset class="groupbox_sip"><legend class="slegandText">Population Options</legend>
	 <div title="Check this checkbox to populate the TripleStore for the first round." class="specialmenuText">
				<input type="radio" name="sipOption" value="tsNew" class="pmcheckbox" checked>
				 Populate the triplestore for the first round
	 </div>
	 <div title="Check this checkbox to start a new round of TripleStore population." class="specialmenuText">
				<input type="radio" name="sipOption" value="tsRenew" class="pmcheckbox">
				 Start the process to add metadata
	 </div>
	 <div title="Check this checkbox to repopulate the metadata (JHOVE data excluded) for all the subjects included in the submitted RDF." class="specialmenuText">
				<input type="radio" name="sipOption" value="tsRepopulateOnly" class="pmcheckbox">
				 Repopulate metadata (keep JHOVE extracted metadata)
	  </div>
	 <div title="Check this checkbox to replace the subject with the subjects included in the submitted RDF." class="specialmenuText">
				<input type="radio" name="sipOption" value="tsRepopulation" class="pmcheckbox">
				 Replace subject with the metadata submitted 
	  </div>
	  <div title="Check this checkbox for the same predicates replacement with the triples included in the submitted RDF." class="specialmenuText">
				<input type="radio" name="sipOption"  value="samePredicatesReplacement" class="pmcheckbox">
				 <span class="text-special">Same predicates replacement with the metadata submitted</span>
	  </div>
	  </fieldset>
</div>
<div class="buttonDiv">
	<input type="button" name="saemFileCancelButton" value="Cancel" onClick="document.location.href='/damsmanager/pathMapping.do'"/>&nbsp;&nbsp;
	<input type="button" name="saemFileButton" value=" Populate " onClick="confirmPopulation();"/>
	<input type="button" name="rdfExportButton" value=" Export RDF " onClick="exportRdf();"/>
</div>
</div>
</div>
</form>
</div>
</div>
	<jsp:include flush="true" page="/jsp/status.jsp" />
	<div id="messageDiv">
		<div id="message" align="left" class="errorBody">${data.message}</div>
	</div>
</td>
</tr>
</table>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
<jsp:include flush="true" page="/jsp/popmenus.jsp" />
</html>
