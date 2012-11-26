<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<c:set var="tsNameLen"> ${fn:length(model.triplestore)}</c:set>  
<c:set var="tsNameFl" scope="page">${fn:substring(model.triplestore, 0, 1)}</c:set> 
<c:set var="opTitle">
	<c:choose>
		<c:when test="${model.creator.collectionId != null}">
			DAMS Collection Update
		</c:when>
 		<c:otherwise>DAMS New Collection Creation</c:otherwise>
	</c:choose>
</c:set>
<c:set var="buttonText">
	<c:choose>
		<c:when test="${model.creator.collectionId != null}">
			&nbsp;Update&nbsp;
		</c:when>
		<c:otherwise>&nbsp;Create&nbsp;</c:otherwise>
	</c:choose>
</c:set>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<html> 
<jsp:include flush="true" page="/jsp/libhtmlheader.jsp" />
<body style="background-color:#fff;">
<script language="javaScript">
	function setTriplestore(){
		document.getElementById("dsSpan").style.display = "none";
		document.getElementById("dsSelectSpan").style.display = "inline";
	}
	
	function resetTriplestore(){
		document.getElementById("dsSpan").style.display = "inline";
		document.getElementById("dsSelectSpan").style.display = "none";
	}
	
	function reloadPage(){
		var dsIdx = document.createForm.ds.selectedIndex;
		var ds = document.createForm.ds.options[dsIdx].value;
		document.location.href="/damsmanager/collectionUpdate.do?ds=" + ds;
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
			<li>Collection Create/update</li>
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
<div id="main" align="center" style="padding-top:20px;padding-bottom:30px;">
<div align="left" class="statusText" style="padding-left:140px;">${model.message}</div>
<div id="formDiv">
<div class="emBoxBanner" style="margin-bottom:5px"><c:out value="${opTitle}" /></div>
<form action="/damsmanager/collectionUpdate.do?update" method="post" onsubmit="return validateForm()" name="createForm">
<table border="0" cellspacing="0" cellpadding="0" align="center">
	 <tr align="left">
		<td valign="top" align="left" width="100%">    
			<table width="100%" border="0" cellspacing="5" cellpadding="0">
				<tr>
					<td width="18%" class="label"><span class="requiredLabel">&nbsp; </span>
					<span id="dsSpan" Title="Double click to change the triplestore used for updating." class="submenuText" ondblclick="setTriplestore();" onMouseOver="this.style.cursor='pointer'">${fn:toUpperCase(tsNameFl)}${fn:substring(model.triplestore, 1, tsNameLen)} </span>
					<span id="dsSelectSpan" ondblclick="resetTriplestore();" style="display:none" >
						<select name="ds" id="ds" onChange="reloadPage();"><option value=""> -- Triplestore -- </option>
								<c:forEach var="entry" items="${model.triplestores}">
									<option value="${entry.value}" <c:if test="${model.triplestore == entry.key}">selected</c:if>>
		                       			<c:out value="${entry.key}" />
		                        	</option>
								</c:forEach>
						</select>&nbsp;
					</span>Collections 
					</td>
					<td>
						<select name="collectionId" onChange="dispCollection(this)">
							<option value="">- New Collection - </option>
							<c:forEach var="entry" items="${model.creator.collectionsMap}" >
								<option value="${entry.value}" <c:if test="${entry.value == model.creator.collectionId}"> selected</c:if>><c:out value="${entry.key}" /></option>
							</c:forEach>
						</select>
					</td>
				</tr>
				<tr>
					<td><span class="label"><span class="requiredLabel">* </span>Collection Title</span></td>
					<td><input type="text" name="collectionTitle" size="40" value="<c:if test="${model.creator.collectionTitle != null}">${model.creator.collectionTitle}</c:if>"/></td>
				</tr>
				<tr>
					<td><span class="requiredLabel">* </span><span class="label">Collection Name</span></td>
					<td><input type="text" name="collectionName" size="40" value="<c:if test="${model.creator.collectionName != null}">${model.creator.collectionName}</c:if>"/></td>
				</tr>
				<tr>
					<td><span class="requiredLabel">&nbsp; </span><span class="label">Parent Collection</span></td>
					<td>
						<select name="parentCollectionId">
							<option value="">Select a collection</option>
							<c:forEach var="entry" items="${model.creator.collectionsOnlyMap}">
								<option value="${entry.value}" <c:if test="${entry.value == model.creator.parentCollectionId}"> selected</c:if>><c:out value="${entry.key}" /></option>
							</c:forEach>
						</select>
					</td>
				</tr>
				<tr>
					<td><span class="requiredLabel">&nbsp; </span><span class="label">Merge To</span></td>
					<td>
						<select name="mergeCollectionId">
							<option value="">Select a collection</option>
							<c:forEach var="entry" items="${model.creator.collectionsOnlyMap}">
								<option value="${entry.value}" <c:if test="${entry.value == model.creator.mergeCollectionId}"> selected</c:if>><c:out value="${entry.key}" /></option>
							</c:forEach>
						</select>
					</td>
				</tr>
				<tr>
					<td><span class="requiredLabel">&nbsp; </span><span class="label" title="Choose to use the FileStore Servlet for OpenStack access.">Filestore:</span></td>
					<td>
						<select id="fileStore" name="fileStore" class="inputText">
							<option value="">- Default -</option>
							<c:forEach var="entry" items="${model.filestores}">
								<option value="${entry.value}" <c:if test="${model.creator.fileStore == entry.value}">selected</c:if>>
	                       			<c:out value="${entry.key}" />
	                        	</option>
							</c:forEach>
						</select>
					<!--
						<select name="collectionType">
							<option value="">Select a type</option>
							<option value="image" <c:if test="${model.creator.collectionType == 'image'}"> selected</c:if>>Still Image</option>
							<option value="document" <c:if test="${model.creator.collectionType == 'document'}"> selected</c:if>>Document</option>
							<option value="video" <c:if test="${model.creator.collectionType == 'video'}"> selected</c:if>>Video</option>
							<option value="audio" <c:if test="${model.creator.collectionType == 'audio'}"> selected</c:if>>Audio</option>
							<option value="mix" <c:if test="${model.creator.collectionType == 'mix'}"> selected</c:if>>Mix Materials</option>
						</select>
						-->
					</td>
				</tr>
				<tr>
					<td><span class="requiredLabel">&nbsp; </span><span class="label">Number of Items </span></td>
					<td>
						<span><input type="text" id="itemsCount" name="itemsCount" size="5" value='<c:out value="${model.creator.numberOfItems}"/>'></span>&nbsp;
						<span class="label">Is Complex Object: </span>
						<select name="complexObject"">
							<option value="no">&nbsp;No&nbsp;</option>
							<option value="yes" <c:if test="${model.creator.isComplexObject == true}"> selected</c:if>>&nbsp;YES&nbsp;</option>
						</select>
					</td>
				</tr>
				<tr>
					<td><span class="requiredLabel">&nbsp; </span><span class="label">Collection Status</span></td>
					<td><select name="colStatus">
							<option value="">-- Step --</option>
							<c:forEach var="entry" items="${model.creator.colStatusMap}">
								<option value="${entry.key}" <c:if test="${entry.key == model.creator.colStatus}"> selected</c:if>><c:out value="${entry.value}" /></option>
							</c:forEach>
						</select></td>
				</tr>
				<tr>
					<td><span class="requiredLabel">&nbsp; </span><span class="label">Staging Area Location</span></td>
					<td><input type="text" id="fileLocation" name="fileLocation" size="40" value="<c:if test="${model.creator.fileLocation != null}">${model.creator.fileLocation}</c:if>"/>&nbsp;<input type="button" onclick="showFilePicker('fileLocation', event)" value="&nbsp;...&nbsp;"></td>
				</tr>
				<tr>
					<td><span class="requiredLabel">&nbsp; </span><span class="label">Default Ark Setting</span></td>
					<td>
						<select name="arkSetting">
							<option value="">Select one</option>
							<option value="one2one" <c:if test="${model.creator.arkSetting == 'one2one'}"> selected</c:if>>One ark to one file</option>
							<option value="one2many" <c:if test="${model.creator.arkSetting == 'one2many'}"> selected</c:if>>One ark to files in the same directory</option>
							<option value="mix" <c:if test="${model.creator.arkSetting == 'mix'}"> selected</c:if>>One ark to files ending with 'p' plus order</option>
							<option value="share" <c:if test="${model.creator.arkSetting == 'share'}"> selected</c:if>>One ark to files with the same name</option>
							<option value="mixshare" <c:if test="${model.creator.arkSetting == 'mixshare'}"> selected</c:if>>One ark to a complex object and its derivatives</option>
							<option value="one2two" <c:if test="${model.creator.arkSetting == 'one2two'}"> selected</c:if>>One ark to a file and its edited file</option>
						</select>
					</td>
				</tr>
				<tr>
					<td><span class="requiredLabel">&nbsp; </span><span class="label">CDL Inventory ID</span></td>
					<td><input type="text" name="cdlGroupName" size="40" value=<c:if test="${model.creator.cdlGroupName != null}">${model.creator.cdlGroupName}</c:if>></td>
				</tr>
				<tr>
					<td nowrap><span class="requiredLabel">&nbsp; </span><span class="label">Master File Access Group</span></td>
					<td>
						<select name="accessGroupMasterFile">
							<option value="">Select a group</option>
							<option value="-1" <c:if test="${'-1' == model.creator.accessGroupMasterFile}"> selected</c:if>>No Access</option>
							<c:forEach var="entry" items="${model.creator.groupIdsMap}">
								<option value="${entry.value}" <c:if test="${entry.value == model.creator.accessGroupMasterFile}"> selected</c:if>><c:out value="${entry.key}" /></option>
							</c:forEach>
						</select>
					</td>
				</tr>
				<tr>
					<td nowrap><span class="requiredLabel">&nbsp; </span><span class="label">Second File Access Group</span></td>
					<td>
						<select name="accessGroupSecondFile">
							<option value="">Select a group</option>
							<option value="-1" <c:if test="${'-1' == model.creator.accessGroupSecondFile}"> selected</c:if>>No Access</option>
							<c:forEach var="entry" items="${model.creator.groupIdsMap}">
								<option value="${entry.value}" <c:if test="${entry.value == model.creator.accessGroupSecondFile}"> selected</c:if>><c:out value="${entry.key}" /></option>
							</c:forEach>
						</select>
					</td>
				</tr>
				<tr>
					<td nowrap><span class="requiredLabel">&nbsp; </span><span class="label">Third File Access Group</span></td>
					<td>
						<select name="accessGroupThirdFile">
							<option value="">Select a group</option>
							<option value="-1" <c:if test="${'-1' == model.creator.accessGroupThirdFile}"> selected</c:if>>No Access</option>
							<c:forEach var="entry" items="${model.creator.groupIdsMap}">
								<option value="${entry.value}" <c:if test="${entry.value == model.creator.accessGroupThirdFile}"> selected</c:if>><c:out value="${entry.key}" /></option>
							</c:forEach>
						</select>
					</td>
				</tr>
				<tr>
					<%-- <td nowrap><span class="requiredLabel">&nbsp; </span><span><input type="checkbox" id="pasDisplaying" name="pasDisplaying" class="pascheckbox" <c:if test="${model.creator.pasDisplaying == true}"><c:out value="checked"/></c:if>><span class="label" title="Check it if required in the Public Access System">In Public Access System</span></td>--%>
					<td><span class="requiredLabel">&nbsp; </span><span  class="label">PAS View </span>
					</td>
					<td>
						<span>
							<select name="pasDisplay" id="pasDisplay">
								<option value=""> - Views - </option>
								<c:forEach var="item" items="${model.creator.pasViewList}">
									<option value="${item}" <c:if test="${item == model.creator.pasDisplay}"> selected</c:if>><c:out value="${item}" /></option>
								</c:forEach>
						</select></span>
						<span class="requiredLabel">&nbsp; </span><span><!-- <input type="checkbox" id="pasThumbnailOnly" name="pasThumbnailOnly" class="pascheckbox" <c:if test="${model.creator.pasThumbnailOnly == true}"><c:out value="checked"/></c:if>> --><span class="label" title="Select restrited components for the Public Access System">Display: </span>
						<span>
							<select name="pasThumbnailOnly" id="pasThumbnailOnly">
								<option value=""> - Component - </option>
								<c:forEach var="item" items="${model.creator.pasComponentViewList}">
									<option value="${item}" <c:if test="${item == model.creator.pasThumbnailOnly}"> selected</c:if>><c:out value="${item}" /></option>
								</c:forEach>
						</select></span>
						<span class="requiredLabel">&nbsp; </span>
						<span><input type="checkbox" id="pasSuppress" name="pasSuppress" class="pascheckbox" <c:if test="${model.creator.pasSuppress == true}"><c:out value="checked"/></c:if>><span class="label" title="Check it if suppressed from PAS display">PAS Suppress</span>
					</td>
				</tr>
				<tr>
					<td colspan="2" align="center" style="padding-top:5px">
						<input type="submit" value="${buttonText}"/>&nbsp;&nbsp;
						<input type="button" onClick="cancelUpdate();" value="Cancel"/>
					</td>
				</tr>
			</table>
		</td>
	</tr>
</table>
</form>
</div>
</div>
</td>
</tr>
</table>
<jsp:include flush="true" page="/jsp/libfooter.jsp" />
</body>
<jsp:include flush="true" page="/jsp/popmenus.jsp" />
</html>
