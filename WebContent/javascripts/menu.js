var className;
var activeMenu;
var timeoutId;
function reset(obj){
	obj.className = className;
}
function resetButton(buttonId){    
	document.getElementById(buttonId).className = className;
    timeoutId = setTimeout("blur('" + buttonId + "')", 300);
}
function activate(obj, aClass){
    className = obj.className;
	obj.className = aClass;
}
function showMenu(buttonId){
 	var menu = document.getElementById(buttonId + 'Menu');
 	if(menu == activeMenu)
 		clearTimeout(timeoutId);
    var obj = document.getElementById(buttonId);
    
	className = obj.className;
	obj.className = 'activemenu';

	if(activeMenu != null && menu != activeMenu){
		activeMenu.style.display = "none";	
	}
	menu.style.display = "inline";
	activeMenu = menu;			
	var curleft = 0;
	var curtop = 0;
	if (obj.offsetParent) {
	do {
		curleft += obj.offsetLeft;
		curtop += obj.offsetTop;
	} while (obj = obj.offsetParent);
	}
	document.getElementById(buttonId + "Menu").style.top = curtop + 18;
	document.getElementById(buttonId + "Menu").style.left = curleft;	
}
function activadeMenuItem(obj, buttonId){
    clearTimeout(timeoutId);
    var menuObj = document.getElementById(buttonId + "Menu");
    if( activeMenu != null && menuObj != activeMenu ){
		activeMenu.style.display = "none";
		activeMenu = menuObj;
	}else
		activeMenu = menuObj;
	obj.className = "aMenuItem";
}
function resetMenuItem(obj, buttonId){
	obj.className = "menuItem";	
	var menuObj = document.getElementById(buttonId + "Menu");	
	if( menuObj == activeMenu ){
		resetButton(buttonId);
	}
}
function blur(buttonId){
	var menu = document.getElementById(buttonId + "Menu");
	document.getElementById(buttonId).className = className;		
	menu.style.display = "none";
}