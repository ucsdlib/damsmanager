<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
<body style="background-color:#fff;">
<script type="text/javascript">
	var crumbs = [{"Home":"http://libraries.ucsd.edu"}, {"Digital Library Collections":"/curator"},{"DAMS Manager":""}];
	drawBreadcrumbNMenu(crumbs, "tdr_crumbs_content", true);
</script>
<jsp:include flush="true" page="/jsp/libanner.jsp" />
<table align="center" cellspacing="0px" cellpadding="0px" class="bodytable">
<tr><td>
<div id="tdr_crumbs">
	<div id="tdr_crumbs_content">
	</div><!-- /tdr_crumbs_content -->
	<script type="text/javascript">
	</script>
	<!-- This div is for temporarily writing breadcrumbs to for processing purposes -->
		<div id="temporaryBreadcrumb" style="display: none">
	</div>
</div><!-- /tdr_crumbs -->
</td>
</tr>
<tr>
<td align="center" style="width:950px;padding-bottom:30px;">
<table>
<tr><td colspan="2">
<div class="title" align="center" style="font-size:22px;">DAMS Manager</div>
<div class="dtext" style="padding-top:18px;">
	As the management console of the Digital Asset Management System (DAMS) in the UCSD Libraries, 
	XDRE Manager, which consists of a set of software applications and tools, is developed on behalf of 
	the DAMS in the Development Unit. It is part of the Extensible Digital Resource Environment (XDRE), 
	a large, complex and scalable system of software and hardware components which provides a flexible 
	Service Oriented Architecture for the development and delivery of a wide variety of application with 
	extensive storage and metadata requirements. XDRE is the software development framework upon which the 
	Libraries' Development Unit builds applications in support of the Libraries' core mission: "to be 
	leaders in providing and promoting information resources and services to the UCSD community".
</div>
<div class="dtext" style="padding-top:18px;">
	XDRE Manager provides a platform for automatic system resource management, such as triplestore syncing, 
	performance tuning, and SOLR index etc.
</div>
</td></tr>
<tr><td style="vertical-align:top;">
<div class="dtext" style="padding-top:18px;">
	In order to automate and consolidate the procedures for the digital projects development, 
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
</td>
</tr>
</table>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
</html>
