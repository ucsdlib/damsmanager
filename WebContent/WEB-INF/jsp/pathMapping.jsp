<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html>
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
<body onLoad="load('pathMapping')" style="background-color:#fff;">
<script type="text/javascript">
	function addMapping(){
		var path = document.mainForm.path.value;
		var target = document.mainForm.target.value;
		var message = "";
		if(path.length == 0)
			message += "source path"
		if(target.length == 0){
		if(message.length > 0)
			message += " and "
			message += "Target Mapping"
		}
		if(message.length > 0){
			message = "Please enter " + message + ".";
			alert(message);
			return false;
		}
		var pair = path + "\t" + target + "\n"
		var mText = document.mainForm.pathMap.value;
		document.mainForm.pathMap.value = mText + pair;
	}
	
	function viewRdf(option){
		var sheetNo = document.mainForm.sheetNo.selectedIndex;
	    var mText = document.mainForm.pathMap.value;
	    var url = "/damsmanager/viewRdf.do?sheetNo=" + sheetNo;
	    
	    if(option != null && option == "export")
	    	url += "&export";
	    	
	    if(mText.length == 0){
	    	alert("Please enter source to taget mapping.");
			return false;
	    }	    	
		window.open(url + "&pathMap=" + encodeURIComponent(mText));
	}
	
	function cancelMapping(){
		window.location.href = "/damsmanager/cancelConvert.do";
	}
	
	function editMapping(){
	 	document.mainForm.pathMap.readOnly = false;
	}
	
	function sipCheck(){
		var mText = document.mainForm.pathMap.value;
		if(mText.length == 0){
	    	alert("Please enter source to taget mapping.");
			return false;
	    }
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
			<li><a href="/damsmanager/dataConverter.do">Data Converter</a></li>
			<li>Mapping</li>
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
<div id="saemFileDiv" align="center">
<div class="adjustDiv"  align="center">
<div class="emBox">
<form id="mainForm" name="mainForm" action="/damsmanager/customSip.do" method="post">
<div class="emBoxBanner">Source To Target Map</div>
<div id="excelSheetNoDiv" align="left" style="<c:if test="${data.format!='excel'}"><c:out value="visibility:hidden;" /></c:if>padding-left:25px;margin-top:10px;margin-bottom:5px;">
	<span class="submenuText"><b>Please enter the Sheet # used:</b></span> 
	<span>
		<select id="sheetNo" name="sheetNo">
			<option value="0" selected>Sheet1</option>
			<option value="1">Sheet2</option>
			<option value="2">Sheet3</option>
			<option value="3">Sheet4</option>
			<option value="4">Sheet5</option>
			<option value="5">Sheet6</option>
		</select>
	</span>
</div>
<div id="pathMapDiv">
	<textarea id="pathMap" name="pathMap" readOnly="true" rows="6" cols="66">${data.pathMap}</textarea>
</div>
<div id="editDiv" align="left" style="padding-left:25px;padding-top:10px">
		<div><span class="emBoxLabelDivt"><strong>Please add the mapping below</strong></span> 
			<span class="submenuText">(Example <a href="/damsmanager/files/sampleExcelMapping.txt" target="_blank">Excel Mapping</a> &nbsp; 
			<a href="/damsmanager/files/sampleMsclMapping.txt" target="_blank">XML Mapping</a>):</span>
		</div>
		<div>
		<table><tr><td height="28px">
		<span class="submenuText"><b>Source Path:</b> </span>
		</td><td>
		<span class="submenuText">
			<input type="text" name="path" size="55" value="">
		</span>
		</td>
		</tr>
		<tr><td height="28px">
		<span class="submenuText"><b>Target Mapping:</b> </span>
		</td>
		<td>
		<span class="submenuText">
			<input type="text" name="target" size="55" value=""> &nbsp;&nbsp;
			<input type="button" name="addMappingButton" onClick="addMapping();" value=" &lt; add " >
		</span>
		</td>
		</tr>
		</table>		
</div>
</div>
<div class="buttonDiv"  style="padding-top:40px">
	<input type="button" name="clearButton" value=" Clear All " onClick="document.mainForm.reset();document.mainForm.pathMap.value='';"/>
	<input type="button" onClick="cancelMapping();" value="Cancel"/>
	<span title="Populate the triplestore with this convertion."><input style="color:red;border:1px solid red" type="button" onClick="sipCheck();" value="&nbsp; SIP &nbsp;"/></span>
	<input type="button" name="editButton" value=" Edit " onClick="editMapping();"/>	
	<input type="button" name="saemFileButton" value=" View RDF " onClick="viewRdf('view');"/>
</div>
</form>
</div>
</div>
</div>
</div>
	<jsp:include flush="true" page="/jsp/status.jsp" />
	<div id="messageDiv">
		<div id="message" align="left" class="errorBody">${data.message}</div>
	</div>
</td>
</tr>
</table>
</div>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
<jsp:include flush="true" page="/jsp/popmenus.jsp" />
</html>
