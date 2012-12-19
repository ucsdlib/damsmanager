   var status = "";
   var operations;
   var checkedCount = 0;
   var errorsCount = 0;
   var cancelEnabled = false;
   var currServletId;
   var url = "/damsmanager/operationHandler.do";
   var progressUrl = "/damsmanager/progressHandler.do";
   var progressBarWidth = 500;
   var progress = {
	 success: dispMessage,
	 failure: rfailed		
   };
   var canceled ={
	 success: resetPanel,
	 failure: cfailed		
    };
   var assignment={
     success: displayProgress,
	 failure: cfailed
    };
   var loadPage={
     success: displayPage,
	 failure: cfailed
    };
    
   function submitForm(){
      var formObj = document.mainForm;
      var collectionIndex = document.mainForm.category.selectedIndex;
      var collectionName = "";
      operations = "";
      
      var rdfImport = formObj.rdfImport.checked;
      var jhoveReport = formObj.jhoveReport.checked;
      var urlParams = "?activeButton=" + formObj.activeButton.value; 
      var validateFileCount = formObj.validateFileCount.checked;
      var validateChecksums = formObj.validateChecksums.checked;
      var createDerivatives = formObj.createDerivatives.checked;
      var uploadRDF = formObj.uploadRDF.checked;
      var createMETSFiles = formObj.createMETSFiles.checked; 
      var sendToCDL = formObj.sendToCDL.checked;
      var luceneIndex = formObj.luceneIndex.checked;
      
      if((checkedCount > 1 && collectionIndex == 0) || (checkedCount == 1 && collectionIndex == 0 && (!(rdfImport || (rdfImport && (formObj.tsRepopulation.checked || formObj.samePredicatesReplacement.checked)) || (jhoveReport && formObj.bsJhoveReport.checked))))){
         alert("Please choose a collection.");
         return false;
      } else if(collectionIndex != 0) {
      	 var selectedOption =  document.mainForm.category.options[collectionIndex];     	    
         collectionName = selectedOption.text;        
      	 category = selectedOption.value;
      }
      
      if(checkedCount == 0){
       	 alert("Please choose one or more operations.");
         return false;
      }
      

      if(validateFileCount == true){
         operations += "- Validate file count \n"; 
      }
      
      if(validateChecksums == true){
         if(!validateDate(formObj.checksumDate))
             return false;

          operations += "- Validate checksum \n";
       }
       
       if(jhoveReport == true){ 
       		if(formObj.bsJhoveUpdate.checked && !formObj.bsJhoveReport.checked){
       			alert("Option Jhove report for BYTESTREAM files only need to check for Jhove format update as well!");
       			return false;
       		}  
          	operations += "- Jhove Report \n";
       }
       
       if(createDerivatives == true){
          operations += "- Create derivatives: ";
       }
       
       if(uploadRDF == true){
          operations += "- Upload RDF XML files ";
       }
       
       if(createMETSFiles == true){
         operations += "- METS creation and upload \n";
       }
                
      if (luceneIndex == true) {
      	operations += "- Solr Index \n";
      }
       
       if(sendToCDL == true){
          operations += "- Send object to CDL ";
       }
             
     if(rdfImport == true){
         
         operations += "- TripleStore Population \n";
         var fileOptions = formObj.fileToIngest;        
          if(fileOptions[1].checked == true){
             var fileUrl = formObj.saemUrl.value;
             if(fileUrl == null || trim(fileUrl).length == 0){
                alert("Please enter a url for the RDF file.");
                return false;
             }            
         }else{
             
             var exeConfirm = confirm("Are you sure you want to perform the following operations " + collectionName + " \n" + operations);
             if(!exeConfirm){
                return false;
             }
             
             setDispStyle("main", "none");
             setDispStyle("saemFileDiv", "inline");
             displayMessage("message", "");
             document.formSaemFile.action = "/damsmanager/fileUpload.do?formId=formSaemFile&" + urlParams + "&sid=" + getSid();
            return false;
         }
      }
     
     if(operations.length == 1){
        alert("Please choose an operation.");
        return false;
     }
     
      var exeConfirm = confirm("Are you sure you want to perform the following operations on the " + collectionName + " collection? \n" + operations);
       if(!exeConfirm){
           return false;
      }
      
     document.mainForm.action = url + "?progress=0&formId=mainForm&" + urlParams + "&sid=" + getSid();
     displayMessage("message", "");
     getAssignment("mainForm");
     displayProgressBar(0);
   }
      
   function checkSelections(checkboxObj, childName){
      var formObj = document.mainForm;
      var collectionIndex = formObj.category.selectedIndex;
      var rdfImport = formObj.rdfImport.checked;
      var jhoveReport = formObj.jhoveReport.checked;
      
     if(checkboxObj.checked == true){
     	if((collectionIndex == 0 && !(rdfImport || jhoveReport)) || (rdfImport && collectionIndex == 0 && checkedCount == 1)){
     	    checkboxObj.checked = false;
     		alert("Please select a collection to start the operations.");
     		return false;
     	}
        checkedCount++;
     }else{
        checkedCount--;
        if(childName != null){
           var cObj = eval("document.mainForm." + childName);
           if(cObj != null && cObj.checked == true)
           	  cObj.checked = false;
        }else if(checkboxObj.name == 'sendToCDL'){
           var cObj = eval("document.mainForm.cdlResend");
           if(cObj.checked == true)
               cObj.checked = false;
            else{
            	cObj = eval("document.mainForm.cdlResendMets");
            	if(cObj.checked == true)
              	    cObj.checked = false;
            }
        }else if(checkboxObj.name == 'rdfImport'){
           var cObj = eval("document.mainForm.tsRenew");
           if(cObj.checked == true)
               cObj.checked = false;
           cObj = eval("document.mainForm.tsRepopulation");
           if(cObj.checked == true)
               cObj.checked = false;
           cObj = eval("document.mainForm.tsRepopulateOnly");
           if(cObj.checked == true)
              cObj.checked = false;
        }
     }
   }
  
  function confirmSelection(checkObj, message, parentName){
     var collectionIndex = document.getElementById("collection").selectedIndex;
     if(checkObj.checked == true){
     	if((collectionIndex == 0 && !checkObj.name=="tsRepopulation") || (collectionIndex == 0 && checkedCount > 1)){
     	    checkObj.checked = false;
     		alert("Please select a collection to begin ...");
     		return false;
     	}
      	var checkedConfirm = confirm("Are you sure you want to " + message + "?");
      	if(!checkedConfirm){
           checkObj.checked = false;
           return false;
          }
        if(parentName != null){
     		var parentObj = document.getElementById(parentName);
     		if(parentObj != null && parentObj.checked == false){
     		   	parentObj.checked = true;
     		   	checkedCount++;
     		 }
     	}
     } 
  }
  
 function specialSelections(checkObj,  message, parentName){
   	 var formObj = document.mainForm;
     if(checkObj.checked == true){
         if(checkObj.name == "cdlResend"){
            if(formObj.cdlResendMets.checked == true){
            	checkObj.checked = false;
            	alert("Resend METS only option is selected. Please uncheck it for another option.");
                return false;
            }
         }else if(checkObj.name == "cdlResendMets"){
             if(formObj.cdlResend.checked == true){
             	checkObj.checked = false;
            	alert("Resend option is selected. Please uncheck it for another option.");
            	return false; 
            }       
         }else if(checkObj.name == "tsRenew"){
             var collectionIndex = document.mainForm.category.selectedIndex;
             if(collectionIndex == 0){
             	checkObj.checked = false;
             	alert("Please select a collection to start a new round of triplestore population.");
            	return false; 
             }         
             if(formObj.tsRepopulation.checked == true){
             	checkObj.checked = false;
            	alert("Metadata Repopulation option is selected. Please uncheck it for another option.");
            	return false; 
            }else if(formObj.tsRepopulateOnly.checked == true){
             	checkObj.checked = false;
            	alert("Metadata Repopulate option is selected. Please uncheck it for another option.");
            	return false; 
            }         
         }else if(checkObj.name == "tsRepopulation"){
             if(formObj.tsRenew.checked == true){
             	checkObj.checked = false;
            	alert("Metadata Renew option is selected. Please uncheck it for another option.");
            	return false; 
            } else if(formObj.tsRepopulateOnly.checked == true){
             	checkObj.checked = false;
            	alert("Metadata Repopulate option is selected. Please uncheck it for another option.");
            	return false; 
            }       
         }else if(checkObj.name == "tsRepopulateOnly"){
             if(formObj.tsRenew.checked == true){
             	checkObj.checked = false;
            	alert("Metadata Renew option is selected. Please uncheck it for another option.");
            	return false; 
            } else if(formObj.tsRepopulation.checked == true){
             	checkObj.checked = false;
            	alert("Metadata Replace option is selected. Please uncheck it for another option.");
            	return false; 
            }       
         }
         confirmSelection(checkObj, message, parentName);
      }
  }
  
  function checkMetsXslFile(){
    var formFileObj = document.formMetsXslFile;
   
    var fileName = formFileObj.metsXslFilePath.value;
    if(fileName == null || trim(fileName).length == 0){
       alert("Please choose an XSL file for transformation.");
       return false;      
    }
    var ext = fileName.substring(fileName.length-4, fileName.length);
    ext = ext.toLowerCase();
    if(ext != '.xsl'){
      alert('Only a file with .xsl extension will be accepted.');
      return false;      
    }
    
    displayMessage("message", "");
    getAssignment("formMetsXslFile");
  }
  
  function checkSaemFile(){
    var formFileObj = document.formSaemFile;
   
    var fileName = formFileObj.saemFilePath.value;
    if(fileName == null || trim(fileName).length == 0){
       alert("Please choose a file.");
       return false;      
    }
    displayMessage("message", "");
    getAssignment("formSaemFile");
  }
  
  function load(page){
     //document.getElementById("tdr_crumbs").style.display = "inline";
     httpcall(progressUrl + "?progress&sid=" +getSid(), loadPage);
   }
  
  function displayPage(resp){
     var results = resp.responseXML.getElementsByTagName('status');
     var message = results[0].firstChild.nodeValue;
     status = message;
     
     if(status == "Done" || status == "Error" || status == "Canceled" || status == "Busy"){
         displayMainDiv("statusDiv");
         resetForm("mainForm"); 
         document.getElementById('tdr_crumbs').style.display='inline';
      }else{
     	document.getElementById('tdr_crumbs').style.display='none';
        setDispStyle("main", "none");
        setDispStyle("statusDiv", "inline");
        dispMessage(resp);
      }
  }
  
  var timeoutObj = null;
  var timeoutReload = null;
  function getAssignment(formId){
	    var progressId = getSid();
        var formObj = document.getElementById(formId);
        var action = formObj.action + "&progressId=" + progressId;  
        currServletId = progressId;             
        formObj.action = action;
        formObj.submit();
        var url = progressUrl + "?progress=0&formId=" + formId + "&sid="+progressId;
        timeoutObj = setTimeout(function(){httpcall(url, assignment);},200 );
       	timeoutReload = setTimeout(
        	function(){document.location.href = document.location.href;},1000
        );
  }
  
  function rfailed(resp){
     errorsCount += 1;
     document.getElementById('status').innerHTML="Error: " + resp.statusText;
     if(errorsCount < 2){
        getStatus(progressUrl + "?progress&sid=" + getSid()); 
     }else{
        alert("Failed to connect to server.");
        displayMainDiv("statusDiv");
     }
  }
		 
  function dispMessage(resp){
     errorsCount = 0;
     var results = resp.responseXML.getElementsByTagName('result');
     var resultsMessageNode = null;
     if(results != null && results[0] != null)
        resultsMessageNode = results[0].firstChild;
     var resultsMessage = "";
     if(resultsMessageNode != null)
         resultsMessage = resultsMessageNode.nodeValue;
         
     var status = resp.responseXML.getElementsByTagName('status');
     var message = "";
     if(status != null && status[0] != null && status[0].firstChild != null)
     	message = status[0].firstChild.nodeValue;
     status = message;
     displayMessage("status", resultsMessage + "<br />" + message);
           
     var progressElement = resp.responseXML.getElementsByTagName('progressPercentage');
     var progressPercentage = null;
     if(progressElement != null && progressElement[0] != null && progressElement[0].firstChild != null){
        progressPercentage = progressElement[0].firstChild.nodeValue;
        displayProgressBar(progressPercentage);
     }
     
     var errors = resp.responseXML.getElementsByTagName('log');
     var logMessageNode = null;
     var logMessage = "";
     if(errors != null && errors[0] != null){
        logMessageNode = errors[0].firstChild;
        if(logMessageNode != null){
           logMessage = logMessageNode.nodeValue;
        }
     }
     
    if((resultsMessage != null && resultsMessage.length > 0) || (logMessage != null && logMessage.length > 0)){
     	var preMessage = getInnerHTML("message");
     	preMessage = truncateMessage(preMessage);
     	if(resultsMessage.length > 0 && logMessage.length > 0)
     	    preMessage = resultsMessage + "<br />" + preMessage + "<br />" + logMessage;	     	
     	else if(resultsMessage.length > 0)
     		preMessage = resultsMessage + "<br />" + preMessage;
     	else
     		preMessage += "<br />" + logMessage;
     	displayMessage("message", preMessage);
     }
     
     if(!(status == "Done" || status == "Error" || status == "Canceled" || status == "Busy")){
         //var url = "/damsmanager/collectionManagement";
         
         if( status.substring(0, 9) == "Preparing" || status.substring(0, 10) == "Optimizing"){
            if(cancelEnabled){
               document.getElementById("Cancel").disabled = true;
               cancelEnabled = false;
             }
         } else if(status != "Accepted" && status != "Processing request ..." && status != "Uploading file. Please wait ..."){
            if(!cancelEnabled){
               document.getElementById("Cancel").disabled = false;
               cancelEnabled = true;
            }
         }
         getStatus(progressUrl + "?progress&sid=" + getSid()); 
      }else{
        setTimeout('getSid()', 2000);
        document.getElementById("tdr_crumbs").style.display='inline';
        if(document.getElementById("statusDiv") != null){
           displayMainDiv("statusDiv");
           var preMessage = getInnerHTML("message");
           preMessage = truncateMessage(preMessage);
           displayMessage("message", preMessage + logMessage);
         }
      }
  }
  
  function displayProgress(resp){
    if(timeoutReload != null){
  		clearTimeout(timeoutReload);
  		timeoutReload = null;
  	}
     var responseDoc = resp.responseXML;
     if(responseDoc == null){
     	var login = resp.responseText.indexOf("loginForm"); 
     	if(login > 0 ){
     		alert("Session expired. Please try all over again.");
     		return false;
     	}else
     		alert(resp.responseText);
     }
     
	 var statusNodes = responseDoc.getElementsByTagName('status');    
     var status = "";
     if(statusNodes != null && statusNodes[0] != null && statusNodes[0].firstChild != null)
     	status = statusNodes[0].firstChild.nodeValue;    
     if(status == "Busy"){
       alert("Warning: Another process is running ...");
       return;
     }else{
    // alert("Spring displayProgress(resp)");
       document.getElementById('tdr_crumbs').style.display='none';
       var progressIdNode = resp.responseXML.getElementsByTagName('progressId');
       var progressId = null;
       if(progressIdNode != null && progressIdNode[0] != null && progressIdNode[0].firstChild != null)
       	progressId = progressIdNode[0].firstChild.nodeValue;
       if(progressId != null){
           var formIdNode = resp.responseXML.getElementsByTagName('formId');
           var formId = formIdNode[0].firstChild.nodeValue;
           setDispStyle("statusDiv", "inline");
           displayMessage("status", "Accepting request ...");
           getStatus(progressUrl + "?progress&sid=" + getSid());     
           if(formId == "mainForm")
              setDispStyle("main", "none");
           else if(formId == "formMetsXslFile")
              setDispStyle("metsXslFileDiv", "none");
           else if(formId == "formSaemFile")
              setDispStyle("saemFileDiv", "none");
           else if(formId == "multipleUploadForm")
              setDispStyle("multipleUploadDiv", "none");
           else
              alert("Error: Unknown from id.");           
       }else
           alert("Error: process id missed ...");
     }
  }
        	
  function getStatus(url){
	  setTimeout("httpcall('" + url + "', progress)",2000);
  }   
  
  function actionCanceled(){
     //var url = "/damsmanager/collectionManagement";
     httpcall(progressUrl + "?canceled&progressId=" + currServletId + "&sid=" + getSid(), canceled);
     displayMessage("status", "Canceling ...");
  }
  
   function resetPanel(resp){
     var statusNodes = resp.responseXML.getElementsByTagName('status'); 
         
     var status = "";
     if(statusNodes != null && statusNodes[0] != null && statusNodes[0].firstChild != null)
     	status = statusNodes[0].firstChild.nodeValue;    
     if(status == "No Response"){
       alert("Server has no response. Please try again ...");
       displayMessage("status", "Unable to cancel operation. Please try again.");
       return;
     }
     displayMessage("status", "Operation canceled.");
     displayMainDiv("statusDiv");
  }
  
  function displayMainDiv(divId){
      setDispStyle(divId, "none");
      setDispStyle("main", "inline");
  }
  
  function cfailed(resp){
       alert("Error: " + resp.statusText);
   }
   
   function displayProgressBar(percent) {
	    var leftBarWidth = (progressBarWidth * (percent/100));
	    var rightBarWidth = progressBarWidth - leftBarWidth;
	   	document.getElementById("leftBar").style.width = leftBarWidth + "px";
	   	document.getElementById("rightBar").style.width = rightBarWidth + "px";
	   	document.getElementById("percent").innerHTML = '&nbsp;&nbsp;' + percent + '%';
   }
   
	function requestStats(selectObj) {
		var selectedIndex = selectObj.selectedIndex;
		if (selectedIndex == 0) {
			document.mainForm.action = "/damsmanager/controlPanel.do";
			document.mainForm.submit();
		}
		else {
			var collectionId = selectObj.options[selectedIndex].value;
			document.mainForm.action = "/damsmanager/controlPanel.do?requestStats=true&category=" + collectionId;
			document.mainForm.submit();
		}
	}

  function submitFiles(){
    
    displayMessage("message", "");
    getAssignment("multipleUploadForm");
  }
	
 function complexObjectChange(objSelect){
 	var selectedIndex = objSelect.selectedIndex;
 	var itemsObj = document.getElementById("items");
 	if(selectedIndex == 0)
 		itemsObj.style.display = 'none';
 	else
 		itemsObj.style.display = 'inline';		
 }
 
 function truncateMessage(message){
 	var newMessage = null;
 	if(message.length > 3000){
 		var ind = message.indexOf(" ...", 400);
 		if(ind < 0)
 			ind = message.indexOf("<a ", 400);
 		if(ind < 0)
 			ind = 500;
        newMessage = message.substring(0, ind) + " ...<br />...<br />";
    }else
       newMessage = message;
    return newMessage;
 }