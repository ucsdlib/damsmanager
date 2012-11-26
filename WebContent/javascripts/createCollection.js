function validateForm()
{
	var form = document.createForm;
	var message = "";
	if (form.collectionTitle.value.length == 0) {
		message += "Collection Title is required.\n";
	}
	if (form.collectionName.value.length == 0) {
		message += "Collection Name is required.\n";
	}
	/*
	if (form.collectionType.selectedIndex == 0) {
		message += "Collection Type is required.\n";
	}
	if (form.visible.checked) {
		if (form.parentCollection.selectedIndex == 0) {
			message += "Parent collection is required.\n";
		} 
	}
	*/
	if (form.mergeCollectionId.selectedIndex > 0 && parentCollectionId > 0) {
		message += "Please choose either a parent collection or a collection to merge but not both.\n";
	}
	if(form.complexObject.selectedIndex != 0){
		var numOfItems = trim(form.itemsCount.value)
		if(!isInteger(numOfItems))
			message += "Invalid number in Number of Items field: " + numOfItems + ".\n";
	}
	
	var pasSelectObj = form.pasDisplay;
	var idx = pasSelectObj.selectedIndex;
	var pasDisplay = pasSelectObj.options[idx].value;
	
	//Master file access
	var fileSelectObj = form.accessGroupMasterFile;
	idx = fileSelectObj.selectedIndex;
	var fileGroup = fileSelectObj.options[idx].value;
	if(pasDisplay == 'ucsd' && fileGroup == 'public')
		message += "Master file access group " + fileGroup + " is not allowed for " + pasDisplay + " display.\n";
	else if(pasDisplay == 'curator' && (fileGroup == 'public' || fileGroup == 'ucsd')){
		message += "Master file access group " + fileGroup + " is not allowed for " + pasDisplay + " display.\n";
	}
	
	//Thumbnail access
	fileSelectObj = form.accessGroupSecondFile;
	idx = fileSelectObj.selectedIndex;
	var fileGroup = fileSelectObj.options[idx].value;
	if(pasDisplay == 'ucsd' && fileGroup == 'public')
		message += "Second file (thumbnail) access group " + fileGroup + " is not allowed for " + pasDisplay + " display.\n";
	else if(pasDisplay == 'curator' && (fileGroup == 'public' || fileGroup == 'ucsd')){
		message += "Second file (thumbnail) access group " + fileGroup + " is not allowed for " + pasDisplay + " display.\n";
	}
	
	//Medium resolution derivative access
	fileSelectObj = form.accessGroupThirdFile;
	idx = fileSelectObj.selectedIndex;
	var fileGroup = fileSelectObj.options[idx].value;
	if(pasDisplay == 'ucsd' && fileGroup == 'public')
		message += "Third file (medium resolution) access group " + fileGroup + " is not allowed for " + pasDisplay + " display.\n";
	else if(pasDisplay == 'curator' && (fileGroup == 'public' || fileGroup == 'ucsd')){
		message += "Third file (medium resolution) access group " + fileGroup + " is not allowed for " + pasDisplay + " display.\n";
	}
	
	if (message.length > 0) {
		alert(message);
		return false;
	}
	else {
		return true;
	}
}

function dispCollection(selectObj){
	var ds = "";
	var dsObj = document.getElementById("ds");
	if(dsObj != null){
		var dsIdx = dsObj.selectedIndex;
		if(dsIdx > 0)
			ds = "ds=" + dsObj.options[dsIdx].value;
	}
    var collectionId = "";
    var selectedIndex = selectObj.selectedIndex;
	if(selectedIndex != 0){
		collectionId = selectObj.options[selectedIndex].value;
		document.location.href = "/damsmanager/collectionUpdate.do?collectionId=" + collectionId + (ds.length>0?"&":"") + ds;			
	}else
		document.location.href = "/damsmanager/collectionUpdate.do?" + ds;				
}

function cancel()
{
	document.cancelForm.submit();
}

function cancelUpdate(){
    document.location.href = "/damsmanager/jsp/introduction.jsp";
}

function isCollectionSelected() {
	if (document.form1.collectionId.selectedIndex == 0) {
		alert("Please select a collection.");
	}
	return document.form1.collectionId.selectedIndex > 0;
}

 var gField;
 function setFilePaths(filePaths){
        document.getElementById(gField).value = filePaths;
  }

  function showFilePicker(id, event){
      gField = id;
      var popwin = window.open("/damsmanager/directory.do", "dirPicker", "toolbar=0,scrollbars=1,location=0,statusbar=0,menubar=0,resizable=0,width=450,height=280,left=400,top=184");
      popwin.focus();
  }