
var tmpClassName;
function displayPanel(obj){
	var bId = obj.id;
	if(bId != activeButtonId){
		var panelObj = document.getElementById(bId + "Div");
		panelObj.style.display = "inline";
		obj.className = "apanelbutton";
		tmpClassName = "apanelbutton";;
		panelObj = document.getElementById(activeButtonId + "Div");
		panelObj.style.display = "none";
		document.getElementById(activeButtonId).className = "panelbutton";
		activeButtonId = bId;
		document.getElementById("activeButton").value = bId;
	}
}


function activateClass(obj){
 	tmpClassName = obj.className;
 	obj.className = "apanelbutton";
}

function resetClass(obj){
 	obj.className = tmpClassName;
}