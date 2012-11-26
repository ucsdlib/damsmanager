<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
<body style="background-color:#fff;">
<jsp:include flush="true" page="/jsp/libanner.jsp" />
<div id="main" class="gallery" align="center">
<table align="center" cellspacing="0px" cellpadding="0px" class="bodytable">
<tr><td>
	
<div id="tdr_crumbs">
	<div id="tdr_crumbs_content">
		<span style="float:right;"><jsp:include flush="true" page="/jsp/menus.jsp" /></span>
		<ul>
			<li><a href="http://libraries.ucsd.edu">Home</a></li>
			<li><a href="/curator">Digital Library Collections</a></li>
			<li><a href="/damsmanager/">XDRE Manager</a></li>
			<li>Process Manager</li>
		</ul>
	</div><!-- /tdr_crumbs_content -->
	
	<!-- This div is for temporarily writing breadcrumbs to for processing purposes -->
		<div id="temporaryBreadcrumb" style="display: none">
	</div>
</div><!-- /tdr_crumbs -->
</td>
</tr>
<tr>
<td>
<div class="adjustDiv" style="padding-top:0px;">
<div class="title" align="center" style="font-size:22px;">Collection Manager</div>
<div class="dtext" style="padding-top:18px;">
	<span style="padding:10px;">&nbsp;</span>With the mission of delivering high quality and stable software products to our end users, 
	the Collection Manager are designed and implemented carefully with the algorithm to protect itself 
	from any misuse or wrong submission to the server by mistake, so that the user can use it feely without 
	any pressures. It also provides in time report regarding the progress of a submitted task, 
	and a submitted task can be stopped and restarted at anytime. With the features and the supports of 
	incremental digital collection development of the Collection Manager, a digital collection can be developed 
	step by step, little by little. The procedures for Digital Libraries Collection development include:
<table width="100%">
<tr><td width="50%">
	<ul style="padding-bottom:0px;margin-bottom:0px;list-style-type:square;">
	<li>Create new digital collection</li>
	<li>Upload masters files using JETL</li>
	<li>Validate file count</li>
	<li>Validate checksums</li>
	<li>Create the derivatives</li>
	<li>Cache the derivatives</li>
	</ul>
</td>
<td width="50%">
	<ul style="padding-bottom:0px;margin-bottom:0px;list-style-type:square;">
	<li>Populate the triplestore</li>
	<li>Index the metadata with SOLR</li>
	<li>Create and upload RDF to SRB</li>
	<li>Create and upload METS to SRB</li>
	<li>Send objects to CDL</li>
	<li>Synchronize the Triplestores</li>
	</ul>
</td></tr>
</table>
</div>
<div class="dtext" style="padding-top:18px;">
	<span style="padding:10px;">&nbsp;</span>To easy the updating of the digital collections, the Collection Manager also provides a 
	corresponding set of functions for collection maintenance. The Collection Manager includes the following tool sets:
	<table width="100%">
		<tr><td width="50%">
			<ul style="padding-bottom:0px;margin-bottom:0px;list-style-type:square;">
				<li><a href="/damsmanager/collectionUpdate.do"><b>Collecion Creation &amp; Update</b></a><br>
					With this tool, a collection or a complex object can be created at the time it is asigned, and a collection/complex object
					can be updated at any time.
				</li>
				<li><a href="/damsmanager/jetl.do"><b>JETL</b></a><br>
					The JETL tool is used to load content files into DAMS. With JETL in DAMS Manager, one can drop the content files
					to the Staging Area for automatic ingestion.
				</li>
				<li><a href="/damsmanager/controlPanel.do"><b>Process Manager</b></a><br>
					The requests for collection development/maintenance can be submitted through the Process Manager's Control Panel. It includes a set 
					of tools that are necessary for incremental collection development and maintenance.
				</li>
			</ul>
			</td>
			<td><img src="/damsmanager/images/jetl.jpg" style="border:0px;"></td>
		</tr>
		<tr>
			<td colspan="2" valign="top">
				<ul style="padding-bottom:0px;margin-bottom:0px;list-style-type:square;">
					<li><a href="/damsmanager/dataConverter.do"><b>Metadata Converter</b></a><br>
						The Metadata Converter is a tool which provides special supports to load metadata, possibly in any formats, into DAMS. 
						XPath and XPath like syntext are applied so that the conversion looks straight forward and is easy to understand. 
						The formats that the Metadata Converter can support including Excel, XML, and CSV etc.
					</li>
				</ul>
			</td>
		</tr>
	</table>
</div>
</div>
</td>
</tr>
</table>
</div>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
<jsp:include flush="true" page="popmenus.jsp" />
</html>
