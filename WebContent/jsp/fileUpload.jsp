<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
    
<div id="saemFileDiv" align="center" style="display:none;">
<form id="formSaemFile" name="formSaemFile" method="post" enctype="multipart/form-data" action="/damsmanager/UploadFile">
<div class="adjustDiv"  align="center">
<div class="emBox">
<div class="emBoxBanner">TripleStore Population</div>
<div class="emBoxLabelDiv">Please select a file:</div>
<div class="fileInputDiv"><input type="file" name="saemFilePath" size="40"></div>
<div class="buttonDiv">
	<input type="button" name="saemFileCancelButton" value="Cancel" onClick="displayMainDiv('saemFileDiv');"/>&nbsp;&nbsp;
	<input type="button" name="saemFileButton" value="Import" onClick="checkSaemFile();"/>
</div>
</div>
</div>
</form>
</div>