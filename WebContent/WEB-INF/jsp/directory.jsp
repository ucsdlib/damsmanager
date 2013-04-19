<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.0 Transitional//EN">
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page errorPage="/jsp/errorPage.jsp"%>
<html>
<HEAD>
	<TITLE>Directory Chooser</TITLE>
	<META http-equiv=Content-Type content="text/html; charset=iso-8859-1"/>
	<LINK href="/damsmanager/css/jsonobj.css" rel="stylesheet"/>
	<script type="text/javascript" src="/damsmanager/javascripts/jsonobj.js"></script>
	<script type="text/javascript" src="/damsmanager/javascripts/jquery-1.4.2.min.js"></script>
	<script type="text/javascript">
	var dirRoot = 'dirPanel';
	function loadPicker(output, paths){
		var dirobj;
		if(output == null)
			output = document.getElementById(dirRoot);
		if(paths == null){
			dirobj = ${model.dirPaths};
			var dbo = new DebuggableObject(dirobj);
			dbo.render(output);
		}else{
		  $.ajax({
			  url: "/damsmanager/directory.do?listOnly&subList&filter=" +paths,
			  dataType: "json",
			  success: function(data){
					var dbo = new DebuggableObject(data);
					dbo.render(output);
			  },
			  error: function(xhr, desc, exceptionobj){
					alert("error list directories: " + desc + "; status: " + xhr.status);
				}
		  });
		}
	}

		
	function setPath(){
		var paths = document.getElementById("dir").value;
		window.opener.setFilePaths(paths);
		self.close();
	}
	</script>
</HEAD>
<body onload="loadPicker()" style="margin: 0px;">
<div style="padding:5px;background:#DDDDDD;"><span class="menuText"><b>Path:&nbsp;&nbsp;</b></span><span class="menuText">[Staging Area] <input id="dir" name="dir" type="text" size="40" value=""/></span></div>
<div style="padding-left:20px;padding-top:3px;padding-bottom:3px;background:#F8F8F8;color:#336699;"><span class="menuText"><b>Please click to select:</b></span></div>
<table cellspacing=0 cellpadding=0 border=0><tr><td>
<tr><td height="178px" valign="top">
	<div id="dirPanel" style="margin-left:20px;"></div>
</td></tr>
</table>
<div style="margin-top:10px;padding-left:20px;padding-top:4px;padding-bottom:3px;background:#F8F8F8;"><span><input id="cancel" name="cancel" type="button" onClick="javascript: self.close();" value="Cancel"/>&nbsp;&nbsp;<input id="ok" name="ok" type="button" onClick="setPath();" value=" OK "/></span></div>
<div style="background:#DDDDDD;height:2px"></div>
</body>
</html>
