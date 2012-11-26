<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<body style="background-color:#fff;">
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
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
			<li>XDRE Manager</li>
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
<table width="100%">
<tr><td colspan="2">
<div class="title" align="center" style="font-size:22px;">XDRE Manager</div>
<div class="dtext" style="padding-top:18px;">
	<span style="padding:10px;">&nbsp;</span>As the management console of the Digital Asset Management System (DAMS) in the UCSD Libraries, 
	XDRE Manager, which consists of a set of software applications and tools, is developed on behalf of 
	the DAMS in the Development Unit. It is part of the Extensible Digital Resource Environment (XDRE), 
	a large, complex and scalable system of software and hardware components which provides a flexible 
	Service Oriented Architecture for the development and delivery of a wide variety of application with 
	extensive storage and metadata requirements. XDRE is the software development framework upon which the 
	Libraries' Development Unit builds applications in support of the Libraries' core mission: "to be 
	leaders in providing and promoting information resources and services to the UCSD community".
</div>
<div class="dtext" style="padding-top:18px;">
	<span style="padding:10px;">&nbsp;</span>XDRE Manager provides a platform for automatic system resource management, such as triplestore syncing, 
	performance tuning, and SOLR index etc.
</div>
</td></tr>
<tr><td style="vertical-align:top;">
<div class="dtext" style="padding-top:18px;">
	<span style="padding:10px;">&nbsp;</span>In order to automate and consolidate the procedures for the digital projects development, 
	<a href="/damsmanager/jsp/introduction.jsp">Collection Manager</a>, which is developed in the UCSD Libraries' 
	IT Development Unit, provides the functionalities to support the development and management of 
	the digital collections. Now it's possible to develop a digital collection and expose it in DAMS for 
	searching in the fly without any efforts of our software developers.
</div>
</td>
<td style="padding-left:30px;">
<img src="/damsmanager/images/controlPanel.jpg" style="border:0px;">
</td>
</tr>
</table>
</div><!-- adjustDiv -->
</td>
</tr>
</table>
</div><!-- gallery -->
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
<jsp:include flush="true" page="popmenus.jsp" />
</html>
