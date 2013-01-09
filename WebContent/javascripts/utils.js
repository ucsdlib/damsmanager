 function setDispStyle(whichId, displayStyle){
   var obj = document.getElementById(whichId);
   obj.style.display = displayStyle;
 }
 
  function setClass(whichId, className){
   var obj = document.getElementById(whichId);
   obj.className = className;
 }
 
  function displayMessage(divId, message){
   document.getElementById(divId).innerHTML = message;
 }
 
   function getInnerHTML(divId){
     return document.getElementById(divId).innerHTML;
 }

  function trim(nText) {
  	if(nText != null){
	    while (nText.length > 0 && nText.substring(0,1) == ' '){
	        nText = nText.substring(1, nText.length);
	     }
	    while (nText.length > 0 && nText.substring(nText.length-1, nText.length) == ' '){
	       nText = nText.substring(0,nText.length-1);
	     }
     }
  return nText;
  }
  
  function validateDate(dateField){
    var dateText = trim(dateField.value);
    if(dateText == ""){
       alert("Please enter a revalidated date for checksum.");
       return false;
    }
    if(dateText.length < 6 || dateText.length > 10){
       alert("Please enter a date in valid format: mm-dd-yyyy | mm/dd/yyyy");
       return false;
       }
    var tokens = dateText.split('-');
    if(tokens.length != 3 ){
        tokens = dateText.split('/');
        if(tokens.length != 3 ){
           alert("Please enter a date in valid format: mm-dd-yyyy | mm/dd/yyyy");
           return false;
        }
     }
    var day = tokens[1];
    var month = tokens[0];
    var year = tokens[2];
    var leap = 0;
    var err = "";

    if (!isInteger(year) || year == 0) 
      err += "Invalid year value. \n";
    if(year.length == 2)
       year = "20" + year;
    if (!isInteger(month) || (month < 1) || (month > 12))
      err += "Invalid month value. \n";
   
   if (!isInteger(day) || day < 1) 
     err += "Invalid day value. \n";
   if(err.length > 0) {
      alert(err);
      return false;
   }
   /* Validation leap-year / february / day */     
   if ((year % 4 == 0) || (year % 100 == 0) || (year % 400 == 0))
     leap = 1;
   if ((month == 2) && (leap == 1) && (day > 29)) 
      err += "23";
   if ((month == 2) && (leap != 1) && (day > 28))
      err += "24";

   /* Validation of other months */
   if ((day > 31) && ((month == "01") || (month == "03") || (month == "05") || (month == "07") || (month == "08") || (month == "10") || (month == "12")))
      err += "25";
   if ((day > 30) && ((month == "04") || (month == "06") || (month == "09") || (month == "11")))
      err += "26"; 

   if(err.length > 0){
     alert("Invalid date.");
     return false;
   }
   
    dateField.value = month + "-" + day + "-" + year;
    return true;
  }
      
  function httpcall(url, handler){   
     //YAHOO.util.Connect.asyncRequest('GET', url, handler, null);  
	  $.ajax({
		  url: url,
		  dataType: "xml",
		  success: handler.success,
		  error: handler.failure
	  });
  }
  
  function isInteger(nText){
   var checkstr = "0123456789";
   for (var i = 0; i < nText.length; i++) {
	  if (checkstr.indexOf(nText.substr(i,1)) < 0) {
	     return false;
	  }
   }
   
    return true; 
   }
   
   function getSid(){
     var d = new Date(); 
     return d.getTime();
  }
  
  function resetForm(formId){
     document.getElementById(formId).reset();
   }
   
  function popWindow(url){
  	window.open(url);
  }
  
  var gField;
  function setFilePaths(filePaths){
     document.getElementById(gField).value = filePaths;
   }

  function showFilePicker(id, event){
	  gField = id;
	  var popwin = window.open("/damsmanager/directory.do", "dirPicker", "toolbar=0,scrollbars=1,location=0,statusbar=0,menubar=0,resizable=0,width=450,height=300,left=400,top=184");
	  popwin.focus();
  }
  