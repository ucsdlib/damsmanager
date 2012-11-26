<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
<body style="background-color:#fff;">
<script language="javascript">
	function analyzeQuery(){
		var sqlTxtArea = document.mainForm.sql;
		if( sqlTxtArea != null)
			sqlTxtArea.value = '';
		document.getElementById("result").innerHTML = '';
		document.mainForm.analyze.value = "true";
		document.mainForm.submit();
	}
	
	function clearAll(){
		document.mainForm.reset();
		var sqlTxtArea = document.mainForm.sql;
		if(sqlTxtArea != null) 
			sqlTxtArea.value='';
		document.getElementById("message").innerHTML = '';
		document.getElementById("result").innerHTML = '';
		document.mainForm.sparql.value='';
	}
	
	function onTSChange(){
		document.mainForm.sparql.value = '';
		document.mainForm.submit();
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
			<li>TripleStore Analyzer</li>
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
<div id="messageDiv">
	<div id="message" align="left" class="errorBody">${data.message}</div>
</div>
<div class="adjustDiv"  align="center">
<div class="emBox">
<form id="mainForm" name="mainForm" action="/damsmanager/tsAnalyzer.do" method="post">
<div class="emBoxBanner">TripleStore Analyzer</div>
<div id="stsDiv" align="left">
	<span class="menuText"><b>Triplestore:</b></span> 
	<span>
		<select id="sts" name="sts" onChange="onTSChange();">
			<option value="sts/dams" <c:if test="${data.sts == 'sts/dams'}">selected</c:if>>sts/dams (Oracle)</option>	
			<option value="sts/star" <c:if test="${data.sts == 'sts/star'}">selected</c:if>>sts/star (Oracle)</option>
			<option value="sts/sio" <c:if test="${data.sts == 'sts/sio'}">selected</c:if>>sts/sio (Oracle)</option>
			<option value="sts/etd" <c:if test="${data.sts == 'sts/etd'}">selected</c:if>>sts/etd (Oracle)</option>
			<option value="sts/tag" <c:if test="${data.sts == 'sts/tag'}">selected</c:if>>sts/tag (Oracle)</option>	
			<option value="ts/dams" <c:if test="${data.sts == 'ts/dams'}">selected</c:if>>ts/dams (AG)</option>
			<option value="ts/star" <c:if test="${data.sts == 'ts/star'}">selected</c:if>>ts/star (AG)</option>
			<option value="ts/sio" <c:if test="${data.sts == 'ts/sio'}">selected</c:if>>ts/sio (AG)</option>
			<option value="ts/xdremanager" <c:if test="${data.sts == 'ts/xdremanager'}">selected</c:if>>ts/xdremanager (AG)</option>
		</select>
	</span>
</div>
<div id="mainDiv" align="left" style="padding-left:20px;padding-top:6px">
	<div align="left" style="padding-top:2px;"><span class="menuText">Enter SPARQL:</span></div> 
	<textarea id="sparql" name="sparql" rows="4" cols="68">${data.sparql}</textarea>
	<c:if test="${data.sts == 'sts/dams' or data.sts == 'sts/star' or data.sts == 'sts/sio' or data.sts == 'sts/dams' or data.sts == 'sts/etd' or data.sts == 'sts/tag'}">
		<div style="margin-top:2px;"><span class="menuText">SQL Converted:</span></div>
		<textarea id="sql" name="sql" rows="4" cols="68" readOnly>${data.sql}</textarea>
	</c:if>
	<div align='center' style="margin-top:3px;">
		<input type="hidden" name="analyze" value="false"/>	
		<input type="button" name="clearButton" value=" Clear All " onClick="clearAll();"/>
		<c:if test="${data.sts == 'sts/dams' or data.sts == 'sts/star' or data.sts == 'sts/sio' or data.sts == 'sts/etd' or data.sts == 'sts/tag'}">
			<input type="button" name="convertButton" value=" SQL Convert " onClick="document.mainForm.submit()"/>
		</c:if>
		<input type="button" name="analyzeButton" value=" Analyze " onClick="javascript:analyzeQuery();" <c:if test="${data.sts == 'DARS_TRIPLES'}">disabled</c:if> />	
	</div>
</div>
</form>
</div>
</div>
<div id="analyzeDiv" align="center" style="min-height:30px;margin-bottom:10px;">
	<div id="result">${data.result}</div>
</div>
</div>
</td>
</tr>
</table>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
<jsp:include flush="true" page="/jsp/popmenus.jsp" />
</html>
