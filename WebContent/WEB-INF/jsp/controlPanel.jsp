<%@ page language="java" contentType="text/html; charset=ISO-8859-1"
    pageEncoding="ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>

<c:set var="tsNameLen"> ${fn:length(model.triplestore)}</c:set>  
<c:set var="tsNameFl">${fn:substring(model.triplestore, 0, 1)}</c:set> 
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
<body onLoad="load('controlPanel')" style="background-color:#fff;">
<script type="text/javascript">
	var activeButtonId = "<c:out value="${model.activeButton}" />";
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
		if(dsIdx > 0){
			var ds = document.mainForm.ds.options[dsIdx].value;
			document.location.href="/damsmanager/controlPanel.do?ds=" + ds;
		}else{
			alert("Please choose a triplestore.");
			return false;
		}
	}

	var crumbs = [{"Home":"http://libraries.ucsd.edu"}, {"Digital Library Collections":"/curator"},{"DAMS Manager":"/damsmanager/"}, {"Process Manager":""}];
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
<td align="center" colspan="2">
<div id="main" class="gallery" align="center">
	<form id="mainForm" name="mainForm" method="post" action="/damsmanager/collectionManagement">
	<div id="controlpanel">
		<div class="paneltitle">DAMS Control Panel</div>
		<div id="collectionDiv" align="left">
			<span id="dsSpan" class="menuText" Title="Double click to choose a triplestore for the operation." ondblclick="setTriplestore();" onMouseOver="this.style.cursor='pointer'">${fn:toUpperCase(tsNameFl)}${fn:substring(model.triplestore, 1, tsNameLen)} </span>
			<span id="dsSelectSpan" ondblclick="resetTriplestore();" style="display:none" >
				<select name="ts" id="ts" onChange="reloadPage();"><option value=""> -- Triplestores -- </option>
						<c:forEach var="entry" items="${model.triplestores}">
							<option value="${entry}" <c:if test="${model.triplestore == entry}">selected</c:if>>
                       			<c:out value="${entry}" />
                        	</option>
						</c:forEach>
				</select>&nbsp;
			</span>
			<span class="menuText">Collection Chooser:&nbsp; </span>
			
			<span><select id="category" name="category" class="inputText" onChange="requestStats(this);">
						<option value=""> -- collections -- </option>
						<c:forEach var="entry" items="${model.collections}">
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
		<div class="panelbar">
			<span id="validateButton" class="<c:if test="${model.activeButton == 'validateButton'}">a</c:if>panelbutton" onClick="displayPanel(this);" onMouseOver="activateClass(this);" onMouseOut="resetClass(this);">Objects </span><span class="<c:if test="${model.activeButton == 'imagesButton'}">a</c:if>panelbutton" id="imagesButton" onClick="displayPanel(this);"  onMouseOver="activateClass(this);" onMouseOut="resetClass(this);"> Derivatives </span><span class="<c:if test="${model.activeButton == 'metadataButton'}">a</c:if>panelbutton" id="metadataButton" onClick="displayPanel(this);"  onMouseOver="activateClass(this);" onMouseOut="resetClass(this);"> Metadata </span><span class="<c:if test="${model.activeButton == 'sipButton'}">a</c:if>panelbutton" id="sipButton" onClick="displayPanel(this);"  onMouseOver="activateClass(this);" onMouseOut="resetClass(this);"> Import </span><span class="<c:if test="${model.activeButton == 'cdlButton'}">a</c:if>panelbutton" id="cdlButton" onClick="displayPanel(this);"  onMouseOver="activateClass(this);" onMouseOut="resetClass(this);"> Export </span><span class="<c:if test="${model.activeButton == 'preferenceButton'}">a</c:if>panelbutton" id="preferenceButton" onClick="displayPanel(this);"  onMouseOver="activateClass(this);" onMouseOut="resetClass(this);"> Preference</span>
		</div>
		<div class="panelbody">
		    <div id="processesDiv">             
				<div id="validateButtonDiv" <c:if test="${model.activeButton != 'validateButton'}">style="display:none;"</c:if>>
				    <div id="fileCountDiv" class="processlayout">
						<div title="Validate master files for duplicate files, mixed file format, and file count" class="menuText"><input class="pcheckbox" type="checkbox" name="validateFileCount" id="validateFileCount" onClick="checkSelections(this);">
							<span class="text-special">File Count Validation <c:if test="${model.itemsCount > 0}">(${model.itemsCount} objects)</c:if></span></div>
						<div title="Check this checkbox to ingest the file when it's missing." class="specialmenuText"><input type="checkbox" id="ingestFile" name="ingestFile" class="pmcheckbox" onClick="confirmSelection(this, 'ingest the missing files.', 'validateFileCount');">
							<span class="text-special"  title="Enter a filter path for the location to speek up the search. From the popup, click on the folder to select/deselect a location. Multiple loations allowed.">Ingest missing files from staging:&nbsp;<input type="text" id="filesLocation" name="filesLocation" size="30" value="">&nbsp;<input type="button" onclick="showFilePicker('filesLocation', event)" value="&nbsp;...&nbsp;"/></span>
						</div>  
					</div>
				    <div id="jhoveReportDiv" class="processlayout">
						<span title="Generate Jhove Report" class="menuText">
							<input class="pcheckbox" type="checkbox" name="jhoveReport" id="jhoveReport" onClick="checkSelections(this);">
							<span class="text-special">Jhove Report</span>
						</span><br />
						<div title="Check this checkbox to generate Jhove report for the BYTESTREAM files only." class="specialmenuText"><input type="checkbox" id="bsJhoveReport" name="bsJhoveReport" class="pmcheckbox" onClick="confirmSelection(this, 'Jhove report for BYTESTREAM files only', 'jhoveReport');">
							<span class="text-special">Jhove report for BYTESTREAM files only.</span>
						</div>
						<div title="Check this checkbox to update the format when it's validated by Jhove." class="specialmenuText"><input type="checkbox" id="bsJhoveUpdate" name="bsJhoveUpdate" class="pmcheckbox" onClick="confirmSelection(this, 'Update technical metadata metadata', 'jhoveReport');">
							<span class="text-special">Update technical metadata:</span>
						</div>
						<div>
							<fieldset class="groupbox_rdf"><legend class="slegandText" style="padding-left:8px;padding-right:8px;">Update Options</legend>
								<span title="Check this checkbox to update the format when it's validated by Jhove." class="submenuText" >&nbsp;<input type="radio" name="jhoveUpdate" value="ByteStream" checked>
									Correct format and format version.
								</span><br />
								<span title="Check this checkbox to update duration when it's available." class="submenuText">&nbsp;<input type="radio" name="jhoveUpdate" value="Duration">
									Update duration for audio/video.
								</span>
							</fieldset>
						</div>
					</div>
					<div id="checksumDiv" class="processlayout">
						<span title="Validate checksum or revalidate if checksum date before the date entered." class="menuText">
				              <input class="pcheckbox" type="checkbox" id="validateChecksums" name="validateChecksums" onClick="checkSelections(this);">
									<span class="text-special">Checksum Validation</span></span><br/>								   
						<span class="submenuText" style="padding-left:26px;">Revalidate if validated before:&nbsp;<input disabled type="text" name="checksumDate" size="12" class="inputText">&nbsp;(mm/dd/yyyy)</span>
					
					</div>
				</div>
				<div id="imagesButtonDiv" <c:if test="${model.activeButton != 'imagesButton'}">style="display:none;"</c:if>>
					<div id="derivativeDiv" class="processlayout">
						<div title="Create thumbnails and medium resource images." class="menuText">
								    <span><input class="pcheckbox" type="checkbox" id="createDerivatives" name="createDerivatives" onClick="checkSelections(this, 'derReplace');">
								   <span class="text-special">Derivatives Creation</span></span><br/>								   
						</div>
						<div>
								   <fieldset class="groupbox_der"><legend class="slegandText">Image Type</legend>
									   <span class="submenuText"><input type="radio" name="size" value="" checked> All Derivatives (1600px, 1200px, 768px, 450px, 150px, 65px)</span><br />
									   <span class="submenuText"><input type="radio" name="size" value="5" > Icon (65px) </span><br />
									   <span class="submenuText"><input type="radio" name="size" value="4" > Thumbnail (150px)</span><br />
									   <span class="submenuText"><input type="radio" name="size" value="3" > Preview (450px)</span><br />
									   <span class="submenuText"><input type="radio" name="size" value="2" > Service (768px)</span><br />
									   <span class="submenuText"><input type="radio" name="size" value="6" > Large (1200px)</span><br />
									   <span class="submenuText"><input type="radio" name="size" value="7" > Huge (1600px)</span><br />
									</fieldset>
						</div>
						<div title="Check this checkbox to replace the derivatives if exist." class="specialmenuText"><input type="checkbox" id="derReplace" name="derReplace" class="pmcheckbox" onClick="confirmSelection(this, 'replace the selected derivative type above', 'createDerivatives');">
									 <span class="text-special">Replace the derivatives for the selected type(s)</span>
						</div>
					</div>
				</div>
				<div id="sipButtonDiv" <c:if test="${model.activeButton != 'sipButton'}">style="display:none;"</c:if>>
					<div id="populationDiv" class="processlayout">
						<div title="Populate the triple store with RDF." class="menuText"><input class="pcheckbox" type="checkbox" id="rdfImport" name="rdfImport" onClick="checkSelections(this);">
									  <span class="text-special">Metadata Import</span>
						</div>
						<div>
							 <fieldset class="groupbox_ingestOpts"><legend class="slegandText">Special Options</legend>
								 <div title="Check this checkbox to start a new round of TripleStore population." class="specialmenuText">
											<input type="radio" id="importMode" name="importMode" value="add">
											 <span class="text-special">Add metadata</span>
								 </div>
								 <div title="Check this checkbox to repopulate metadata but keep file characterize metadata for all the subjects included in the file submitted." class="specialmenuText">
											<input type="radio" id="importMode" name="importMode" value="descriptive">
											 <span class="text-special">Replace descriptive metadata only to keep keep file properties</span>
								  </div>
								 <div title="Check this checkbox to replace the subject with the subjects included in the submitted RDF." class="specialmenuText">
											<input type="radio" id="importMode" name="importMode" value="all">
											 <span class="text-special">Replace the whole record with metadata submitted</span>
								  </div>
								  <div title="Check this checkbox for same predicates replacement with the triples included in the submitted RDF." class="specialmenuText">
											<input type="radio" id="importMode" name="importMode" value="samePredicates">
											 <span class="text-special">Same predicates replacement with metadata submitted</span>
								  </div>
							  </fieldset>
						  </div>
						  <div>
							   <fieldset class="groupbox_ts"><legend class="slegandText">File</legend>
									<div id="fileFormat">
											<span class="submenuText"><strong>Choose File Format: </strong></span><br/>
											<span class="submenuText"><input type="radio" name="dataFormat" value="RDF/XML" checked><span class="text-special">RDF XML</span></span>
											<span class="submenuText"><input type="radio" name="dataFormat" value="N-TRIPLE"><span class="text-special">N-Triples</span></span><br>
									</div>
									<div id="fileLocation" style="padding-top:5px;">	
											<span class="submenuText"><strong>Choose File: </strong></span><input type="file" name="dataFile" size="40" /><br>
									</div>
							   </fieldset>
						   </div>
					</div>
					<div id="externalImportDiv" class="processlayout">
					   <div title="Import records to DAMS with custom conversion." class="menuText"><input class="pcheckbox" type="checkbox" id="externalImport" name="externalImport" onClick="checkSelections(this);">
							<span class="text-special">External Import</span>
						</div>
						<div>
							 <fieldset class="groupbox_ingestOpts"><legend class="slegandText">Import Options</legend>
							 	<div title="Check this checkbox to import metadata and files." class="specialmenuText">
									<input checked type="radio" name="importOption" value="metadataAndFiles">
									<span class="text-special">Metadata and files</span>
								 </div>
								 <div title="Check this checkbox to import metadata only." class="specialmenuText">
									<input type="radio" name="importOption" value="metadata">
									<span class="text-special">Metadata only</span>
								 </div>
							  </fieldset>
						</div>
															  
						<div class="specialmenuText" style="margin-top:3px;padding-left:22px;"  title="Enter a filter path for the location to speek up the search. From the popup, click on the folder to select/deselect a location. Multiple loations allowed.">Data location:
							<input type="text" id="dataPath" name="dataPath" size="40" value="">&nbsp;<input type="button" onclick="showFilePicker('dataPath', event)" value="&nbsp;...&nbsp;">
						</div>
						<div class="specialmenuText" style="margin-top:3px;padding-left:22px;"  title="Enter a filter path for the location to speek up the search. From the popup, click on the folder to select/deselect a location. Multiple loations allowed.">Files location: 
							<input type="text" id="filesPath" name="filesPath" size="40" value="">&nbsp;<input type="button" onclick="showFilePicker('filesPath', event)" value="&nbsp;...&nbsp;">
						</div>
					</div>									
				</div>
				<div id="metadataButtonDiv" <c:if test="${model.activeButton != 'metadataButton'}">style="display:none;"</c:if>>
					<div id="solrIndexDiv" class="processlayout">
						 <div><span title="SOLR index" class="menuText">
					            <input class="pcheckbox" type="checkbox" id="luceneIndex" name="luceneIndex" onClick="checkSelections(this, 'indexReplace');">
						          <span class="text-special">SOLR Indexing</span></span><br />
						  </div>
						  <div title="Check this checkbox to Clean up SOLR the collection selected [optional]." class="specialmenuText"><input type="checkbox" id="indexReplace" name="indexReplace" class="pmcheckbox" onClick="confirmSelection(this, 'clean up SOLR for the collection selected', 'luceneIndex');">
									 <span class="text-special">Clean up SOLR for the collection selected</span>
						  </div>
					</div>
					<div id="rdfUploadDiv" class="processlayout">
						 <div><span title="Create and upload the RDF files." class="menuText"><input disabled class="pcheckbox" type="checkbox" id="uploadRDF" name="uploadRDF" onClick="checkSelections(this, 'rdfXmlReplace');">
									<span class="text-special">RDF Creation &amp; uploading</span></span><br />
						 </div>	
						 <div>
							<fieldset class="groupbox_rdf"><legend class="slegandText">Meta Data</legend>
   						        <span class="submenuText"><input disabled type="radio" name="rdfXmlDataType" value="all" checked>All</span><br />
						        <span class="submenuText"><input disabled type="radio" name="rdfXmlDataType" value="jhove" >JHOVE extracted metadata only</span><br />
						   </fieldset>
						 </div>	
						 <div title="Check this checkbox to replace the RDF XML files if exist." class="specialmenuText"><input disabled type="checkbox" id="rdfXmlReplace" name="rdfXmlReplace" class="pmcheckbox" onClick="confirmSelection(this, 'replace the RDF XML files', 'uploadRDF');">
									 <span class="text-special">Replace the RDF XML files</span>
						  </div>
					</div>
					<div id="metsUploadDiv" class="processlayout">
						<div><span class="menuText">
								   <input disabled class="pcheckbox" type="checkbox" id="createMETSFiles" name="createMETSFiles" onClick="checkSelections(this, 'metsReplace');">
									 <span class="text-special">METS Creation &amp; uploading</span></span><br />
						</div>
						<div title="Check this checkbox to replace the METS files if exist [optional]." class="specialmenuText"><input disabled type="checkbox" id="metsReplace" name="metsReplace" class="pmcheckbox" onClick="confirmSelection(this, 'replace the METS', 'createMETSFiles');">
									 <span class="text-special">Replace the METS files</span>
						</div>
					</div>
				</div>
				<div id="cdlButtonDiv" <c:if test="${model.activeButton != 'cdlButton'}">style="display:none;"</c:if>>
					<div id="metadataExportDiv" class="processlayout">
						<div class="menuText"><input class="pcheckbox" type="checkbox" id="metadataExport" name="metadataExport" onClick="checkSelections(this, 'metadataExport');"><span class="text-special"><strong>Metadata Export: </strong></span><br />
						    <div style="padding-left:18px;">
								<div class="specialmenuText"><input type="radio" name="exportFormat" value="RDF/XML-ABBREV" checked><span class="text-special">RDF XML</span></div>
								<div class="specialmenuText"><input type="radio" name="exportFormat" value="N-TRIPLE"><span class="text-special">N-Triples</span></div>
								<!-- <div class="specialmenuText"><input disabled type="radio" name="exportFormat" value="csv"><span class="text-special">CSV Export</span></div> -->
							</div>
							<div>
								<fieldset class="groupbox_emOptions"><legend class="slegandText">Special Options</legend>
									<div title="Export metadata with namespaces limitation." class="specialmenuText"><span style="color:red;font-size:12px;padding-left:5px;">*</span>&nbsp;Namespace(s) delimited by comma: <input type="text" name="nsInput" size="35" class="inputText" /></div> 
									<div title="Check this checkbox to exclude metadata in the components and files." class="specialmenuText"><input type="checkbox" id="exComponents" name="exComponents" onClick="confirmSelection(this, 'exclude metadata in components and files', 'metadataExport');">
										<span class="text-special">Exclude metadata in components and files.</span>
									</div>
								</fieldset>
							</div>
						</div>
					</div>
					<div id="cdlIngestDiv" class="processlayout">
						<div title="Send objects to CDL" class="menuText">
						    <input disabled class="pcheckbox" type="checkbox" id="sendToCDL" name="sendToCDL" onClick="checkSelections(this);">
							<span class="text-special">CDL</span> 
						</div>
						<div>
							<fieldset class="groupbox_cdl"><legend class="slegandText">METS Feeder</legend>
							   <span class="submenuText"><input disabled type="radio" name="feeder" value="dpr" checked> DPR </span><br />
							   <span class="submenuText"><input disabled type="radio" name="feeder" value="merritt" checked> Merritt </span><br />
								<div style="margin-left:28px;">
									<span class="submenuInputText">Account #:&nbsp;<input disabled type="text" name="account" size="12" class="inputText" />&nbsp;</span><br /><span class="submenuInputText">Auth code:&nbsp;<input disabled type="password" name="password" size="12" class="inputText" /></span>						
								</div>
							</fieldset>
							<fieldset class="groupbox_cdlOptions"><legend class="slegandText">Special Options</legend>
							<span class="submenuText" title="Check this checkbox to include the embargo objects to send to CDL." class="specialmenuText">
								<input disabled type="checkbox" id="includeEmbargoed" name="includeEmbargoed"></span><span class="text-special">Include embargoed objects</span><br />
							<span class="submenuText" title="Check this checkbox to resend objects to CDL." class="specialmenuText">
								<input disabled type="checkbox" id="cdlResend" name="cdlResend" onClick="specialSelections(this, 'resend objects already sent to CDL', 'sendToCDL');"></span><span class="text-special">Resend objects already sent</span><br />
							<span class="submenuText" title="Check this checkbox to resend the METS file to CDL only.">
								<input type="checkbox" id="cdlResendMets" name="cdlResendMets" onClick="specialSelections(this, 'resend METS files to CDL only', 'sendToCDL');" disabled></span><span class="text-special">Resend METS files only</span>
							</fieldset>
						</div>
					</div>
				</div>
				<div id="preferenceButtonDiv" <c:if test="${model.activeButton != 'preferenceButton'}">style="display:none;"</c:if>>
					<div class="processlayout">
						<span title="FileStore selector" class="menuText">
							<span class="text-special">&nbsp;FileStore to use:&nbsp;</span>
							<select id="fs" name="fs" class="inputText">
								<option value=""> -- FileStore -- </option>							
								<c:forEach var="entry" items="${model.filestores}">
									<option value="${entry}" <c:if test="${model.filestore == entry}">selected</c:if>>
		                       			<c:out value="${entry}" /><c:if test="${model.filestoreDefault == entry}"> (default)</c:if>
		                        	</option>
								</c:forEach>
							</select>
						</span>
					</div>
				</div><!-- End preferenceButtonDiv -->
			</div>
		</div>
		<div class="buttonDiv">
			<input type="hidden" id="fileUrl" name="fileUrl" value=""/>
			<input type="hidden" id="activeButton" name="activeButton" value="${model.activeButton}"/>
			<input type="button" name="submitButton" value=" Perform Operation " onClick="submitForm();"/>
		</div>
	</div>
	</form>
	</div>
	<!-- jsp:include flush="true" page="/jsp/fileUpload.jsp" /-->
	<jsp:include flush="true" page="/jsp/status.jsp" />
	<div id="messageDiv">
		<div id="message" align="left">${model.message}</div>
	</div>
</td>
</tr>
</table>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
</html>
