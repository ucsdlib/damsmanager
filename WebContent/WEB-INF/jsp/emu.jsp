<%@ page language="java" contentType="text/html; charset=UTF-8"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>

<c:set var="libAAL"> ${model.libraries.aal}</c:set>
<c:set var="libSIO"> ${model.libraries.sio}</c:set>
<c:set var="libSSHL"> ${model.libraries.sshl}</c:set>
<c:set var="libMSCL"> ${model.libraries.mscl}</c:set>
<html>
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
<body style="background-color:#fff;">
<script type="text/javascript">
	function check(ark){
	 /*
	 $('input[type="checkbox"]:checked').each(function(){
	 	if(ark != $(this).val()){
	 		$(this).attr("checked", false);
	 	}
	 });
	 */
	}
	
	function exportCSV(){
		var count = 0;
		var colId = null;
		$('input[type="radio"]:checked').each(function(){
			count++;
		 	colId = $(this).val();
		 	if(count > 1){
		 		alert("Please select only one collection each time.");
		 		return;
		 	}
		 });
		 if(colId == null){
		 	alert("Please select a collection.");
		 }else{
		 	if(confirm("By clicking the OK button, metadata for the collection will be exported in CSV. \n" + "You will be notified by email if it takes too long to complete. \n" + "Should I go ahead?")){
		 		var exportUrl = "${(model.isProxy?"/xdre":"")}/damsmanager/csvExport.do?collection&subject=" + colId;
		 		var xsl = $('#xsl').val();
		 		if(xsl != null && xsl.length > 0)
		 			exportUrl += "&xsl=" + xsl;
		 		$('body').css('cursor','wait');
		 		document.location.href = exportUrl;
		 		setTimeout("$('body').css('cursor','default');document.location.href = '" + exportUrl + "';", 2000);
			}
		}
	}

</script>
<jsp:include flush="true" page="/jsp/libanner.jsp" />
<table align="center" cellspacing="0px" cellpadding="0px" class="bodytable">
<tr><td>
	
<div id="tdr_crumbs">
	<div id="tdr_crumbs_content">
		<ul>
			<li><a href="http://libraries.ucsd.edu">Home</a></li>
			<li><a href="/curator">Digital Library Collections</a></li>
			<li>EMU</li>
		</ul>
	</div><!-- /tdr_crumbs_content -->
	
	<!-- This div is for temporarily writing breadcrumbs to for processing purposes -->
		<div id="temporaryBreadcrumb" style="display: none">
	</div>
</div><!-- /tdr_crumbs -->
<div align="center">
	<div style="font-size:18px;font-weight:bold;color:#333;margin-bottom:10px;margin-top:10px;">
	UCSD Libraries' Digital Asset Management Export Metadata Utility (EMU)</div>
	<div style="margin-bottom:10px;text-align:left;width:760px;color:#000;">
		<p>
		This export metadata function writes the metadata for a single collection to a .csv (comma separated value) file. Field names are automatically recorded as the first row in your export file.
		</p>
		<p>
		To view the data file in a spreadsheet application, proceed with the steps you normally follow to open an existing spreadsheet (i.e. import .csv file with MS Excel).
		</p>
	<div id="message" style="color:red;font-weight:bold;font-size:14;text-align:center;margin:10px;">${model.message}</div>
	<div id="collectionListDiv">
	<table style="margin-bottom:10px;text-align:left">
		<tr>
			<td width="50%"><div class="title" style="padding-bottom:0px;padding-top:5px;text-align:left;">Arts Library</div></td>
			<td width="50%"><div class="title" style="padding-bottom:0px;padding-top:5px;text-align:left;">Scripps Institution of Oceanography Library</div></td>
		</tr>
		<tr>
			<td>
				<div class="colList">
					<ul>
						<c:forEach var="item" items="${model.libraries.al}" >
							<li><input type="radio" name="collection" value="${item.subject}" onClick="check('${item.subject}');"/><span class="listTitle">${item.title} (</span><span class="count">${item.count}</span><span class="listTitle">)</span></li>
						</c:forEach>
					</ul>
				</div>
			</td>
			<td  rowspan="4" valign="top">
				<div class="colList">
					<ul>
						<c:forEach var="item" items="${model.libraries.sio}" >
							<li><input type="radio" name="collection" value="${item.subject}" onClick="check('${item.subject}');"/><span class="listTitle">${item.title} (</span><span class="count">${item.count}</span><span class="listTitle">)</span></li>
						</c:forEach>
					</ul>
				</div>
			</td>
		</tr>
		<tr><td><div class="title" style="padding-bottom:0px;padding-top:16px;text-align:left;">Social Sciences and Humanities Library</div></td></tr>
		<tr>
			<td>
				<div class="colList">
					<ul>
						<c:forEach var="item" items="${model.libraries.sshl}" >
							<li><input type="radio" name="collection" value="${item.subject}" onClick="check('${item.subject}');"/><span class="listTitle">${item.title} (</span><span class="count">${item.count}</span><span class="listTitle">)</span></li>
						</c:forEach>
					</ul>
				</div>
			</td>
		</tr>
		<tr><td colspan="2" valign="top"><div class="title" style="padding-bottom:0px;padding-top:16px;text-align:left;">Mandeville Special Collection Libraries</div></td></tr>
		<tr>
			<td valign="top">
				<div class="colList">
					<ul>
						<c:forEach var="item" items="${model.libraries.mscl}" varStatus="status">
							<c:if test="${(status.count-1)%2==0}">
								<li><input type="radio" name="collection" value="${item.subject}" onClick="check('${item.subject}');"/><span class="listTitle">${item.title} (</span><span class="count">${item.count}</span><span class="listTitle">)</span></li>
							</c:if>
						</c:forEach>
					</ul>
				</div>
			</td>
			<td valign="top">
				<div class="colList">
					<ul>
						<c:forEach var="item" items="${model.libraries.mscl}" varStatus="status">
							<c:if test="${(status.count-1)%2!=0}">
								<li><input type="radio" name="collection" value="${item.subject}" onClick="check('${item.subject}');"/><span class="listTitle">${item.title} (</span><span class="count">${item.count}</span><span class="listTitle">)</span></li>
							</c:if>
						</c:forEach>
					</ul>
				</div>
			</td>
		</tr>
		<tr><td colspan="2" style="text-align:right;color:#000;padding-top:5px;">If your collection set does not appear here, please email us at: <a href="mailto:dlp@ucsd.edu?subject=Digital Asset Management Export Metadata Utility (EMU)">dlp@ucsd.edu</a></td></tr>
	</table>
	</div>
	<div style="padding-top:5px;font-weight:bold;font-size:20px;"><a href="javascript:exportCSV();" style="text-decoration:none;" title="Export in CSV format"><img src="images/export-icon.png" style="border:0;" /><span style="padding:5px;vertical-align:40px;">Export</span></a></div>
	<input type="hidden" id="xsl" name="xsl" value="<c:if test="${model.xsl != null}">${model.xsl}</c:if>"/>
</div>
</div>
</td>
</tr>
</table>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
</html>
