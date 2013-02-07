
var DebuggableObject = function(obj) {
  this.obj = obj;
};

(function() {

function isArray(a) {
  return typeof(a) == 'object' && a != undefined && a.constructor == Array;
}

function isSerializable(a) {
  var t = typeof(a);
  return t == 'string' || t == 'number' || t == 'boolean' || t == 'object' || t == 'function';
}

function isPrintable(a) {
  var t = typeof(a);
  return t == 'string' || t == 'number' || t == 'boolean' || a instanceof Date;
}

function isFunction(a) {
  return typeof(a)=='function';
}

var rootText = "[Staging Area]";	 
function renderObject(obj, element, depth) {
  if (isPrintable(obj)) {
 	 var toggle = document.createElement('div');
 	 toggle.setAttribute("title",obj);
 	 var folder = document.createElement('img');
 	 folder.setAttribute("src","/damsmanager/images/dir.gif");
 	 toggle.appendChild(folder);
 	 toggle.appendChild(document.createTextNode(obj));
 	 toggle.className = 'dbg-toggle';
     element.appendChild(toggle);
     toggle.onclick = function() {
     	var parent = toggle.parentNode;
     	var pathName = parent.getAttribute("title");
     	var paths = toggle.getAttribute("title");
	    while( parent != null  &&  pathName!=rootText){
	   	    if(pathName != null && pathName.length > 0)
	    		paths = pathName + "/" + paths;
	    	parent = parent.parentNode;	
	    	if(parent != null){
	    		pathName = parent.getAttribute("title"); 
	    	}   	
	    }
	    paths = "/" + paths;	
	    //document.getElementById("dir").value = dirValues;
	    addDir(paths);
	 };
     return;
  } else {
    var label = isFunction(obj) ? '(function)' :
                isArray(obj)    ? '(array)'    : '(object)';
    
 	if(label == '(object)'){
    	for (var propName in obj) {
    		var block = document.createElement('div');
    		block.setAttribute("title",propName);
    		element.appendChild(createToggleElement(obj[propName], block, propName, depth));
    	} 
    }else{
    	var block = document.createElement('div');
    	element.appendChild(createToggleElement(obj, block, label, depth));
    }
  }
}

function renderObjectDetails(obj, element, depth) {
  depth++;
  if (isArray(obj)) {
    var arrtab = document.createElement('table');
    arrtab.className = 'dbg-arr';
    var arrtbody = document.createElement('tbody');
    for (var i=0; i<obj.length; i++) {
      if (isSerializable(obj[i])) {
        var row = document.createElement('tr');
        var identCol = document.createElement('td');
        identCol.className = 'ident';
        var valueCol = document.createElement('td');
        valueCol.className = 'dbg-value';
        renderObject(obj[i], valueCol, depth);
        row.appendChild(identCol);
        row.appendChild(valueCol);
        arrtbody.appendChild(row);
      }
    }
    arrtab.appendChild(arrtbody);
    element.appendChild(arrtab);

  } else if (isFunction(obj)) {
    element.appendChild(document.createTextNode(obj));

  } else {
    var objtab = document.createElement('table');
    objtab.className = 'dbg-obj';
    var objtbody = document.createElement('tbody');
    for (var propName in obj) {
      if (isSerializable(obj[propName])) {
        var row = document.createElement('tr');
        var labelCol = document.createElement('td');
        labelCol.className = 'dbg-label';
        var elemName = propName;
        //var lname = replaceValue(elemName, "_", ":");
        labelCol.appendChild(document.createTextNode(elemName));
        
        var valueCol = document.createElement('td');
        valueCol.className = 'dbg-value';
        renderObject(obj[propName], valueCol, depth);

        row.appendChild(labelCol);
        row.appendChild(valueCol);
        objtbody.appendChild(row);
      }
    }
    objtab.appendChild(objtbody);
    element.appendChild(objtab);
  }
}


function createToggleElement(obj, target, label, depth) {
  target.style.display = 'none';
  var toggle = document.createElement('div');
  var folder = document.createElement('img');
  folder.setAttribute("src","/damsmanager/images/dir.gif");
  toggle.appendChild(folder);
  toggle.appendChild(document.createTextNode(label));
  toggle.className = 'dbg-toggle';
  var wrapper = document.createElement('div');

  wrapper.className = 'dbg-toggle-closed';

  if(depth == 1){
     wrapper.className = 'dbg-toggle-open';
     if (!target.firstChild) {
        renderObjectDetails(obj, target, depth);
      }
      target.style.display = 'block';
  }
  
  toggle.setAttribute("title", label);
  toggle.onclick = function() {
    if (wrapper.className=='dbg-toggle-closed') {
      wrapper.className = 'dbg-toggle-open';
      if (!target.firstChild) {
        renderObjectDetails(obj, target, depth);
      }
      target.style.display = 'block';
    } else {
      wrapper.className = 'dbg-toggle-closed';
      target.style.display = 'none';
    }
    
   // if(wrapper.className == 'dbg-toggle-open'){
    var parent = toggle.parentNode;
    var pathName = parent.getAttribute("title");
    var paths = toggle.getAttribute("title");
    while( parent != null  &&  pathName!=rootText){
   	    if(pathName != null && pathName.length > 0)
    		paths = pathName + "/" + paths;
    	parent = parent.parentNode;	
    	if(parent != null){
    		pathName = parent.getAttribute("title"); 
    	}   	
    }
    paths = "/" + paths;
    addDir(paths);
    //document.getElementById("dir").value = paths;
   // }
  };
  wrapper.appendChild(toggle);
  wrapper.appendChild(target);
  return wrapper;
}

function toJSON(obj) {
  if (isPrintable(obj)) {
    return '"' + obj.toString()
                    .replace(/(\\|\")/g, '\\$1')
                    .replace(/\n|\r|\t/g, function(a){
                       return (a=='\n') ? '\\n':
                              (a=='\r') ? '\\r':
                              (a=='\t') ? '\\t': '';
                    }) + '"';

  } if (isArray(obj)) {
    var out = [];
    for (var i=0; i<obj.length; i++) {
      if (isSerializable(obj[i]) && !isFunction(obj[i])) {
        out.push(toJSON(obj[i]));
      }
    }
    return '['+out.join(',')+']';

  } else {
    var out = [];
    for (var propName in obj) {
      if (isSerializable(obj[propName]) && !isFunction(obj[propName])) {
        out.push('"'+propName+'":'+toJSON(obj[propName]));
      }
    }
    return '{'+out.join(',')+'}';
  }
}

function addDir(directory){
    var dirValues = document.getElementById("dir").value;
    var dirArr = dirValues.split(";");
    var toAdd = true;
    for(var i=0; i<dirArr.length; i++){
    	var iDir = dirArr[i];
    	if(iDir != null &&  iDir == directory){
    		// Remove it when click on the folder again
    		dirArr[i] = "";
    		toAdd = false;
    		break;
    	}else if(iDir.indexOf(directory) == 0){
    		// Choose parent directory need clear all child directory
    		if(toAdd){
    			dirArr[i] = directory;
    			toAdd = false;
    		}else
    			dirArr[i] = "";

    	}else if(directory.indexOf(iDir) == 0){
    		// Choose child directory, replace it
    		dirArr[i] = directory;
    		toAdd = false;
    		break;
    	}
    }
    if(toAdd)
    	dirValues += directory + ";";
    else {
		dirValues = ""; 
    	// Re-assemble the path string.
    	for(var i=0; i<dirArr.length; i++){
    		var iDir = dirArr[i];
    		if(iDir != null && iDir.length > 0){
    			dirValues += iDir + ";"; 
    		}
    	}
    }
    	
    document.getElementById("dir").value = dirValues;
}

DebuggableObject.prototype = {

  render : function (element) {
    while (element.firstChild) element.removeChild(element.firstChild);
    var cell = document.createElement('div');
    cell.className = 'dbg-value';
    renderObject(this.obj, cell, 1);
    element.appendChild(cell);
  }
  ,

  toString : function () {
    return toJSON(this.obj);
  }
};

})();

