
var Directories = function(obj) {
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

var inputIdx = 1;	 
function renderObject(obj, element, depth, path) {

  if (isPrintable(obj)) {
 	 var toggle = document.createElement('div');
 	 toggle.setAttribute("title",obj);
	 var src = "/damsmanager/images/dir.gif";
	 if(obj.lastIndexOf(".") > 0){
	  	src = "/damsmanager/images/space.gif";
	 }
	 var folder = document.createElement('img');
	 folder.setAttribute("src",src);
	 var input = document.createElement('input');
	 input.setAttribute("id",label+inputIdx++);
	 input.setAttribute("type","checkbox");
	 input.setAttribute("name",obj);
	 input.setAttribute("value",(path+(path.length>0?"/":"")+obj));
	 input.onclick = function(){checkboxChange(this)};
	 toggle.appendChild(input);
 	 toggle.appendChild(folder);
 	 toggle.appendChild(document.createTextNode(obj));
 	 toggle.className = 'dbg-toggle';
     element.appendChild(toggle);
     return;
  } else {
    var label = isFunction(obj) ? '(function)' :
                isArray(obj)    ? '(array)'    : '(object)';
    
 	if(label == '(object)'){
    	for (var propName in obj) {
    		var block = document.createElement('div');
    		block.setAttribute("title",propName);
    		var wrapper =createToggleElement(obj[propName], block, propName, depth, (path + (path.length>0?"/":"")+propName));
    		element.appendChild(wrapper);
    		$(element).find(".dbg-toggle :last-child").trigger("click");
    	} 
    }else{
    	var block = document.createElement('div');
    	element.appendChild(createToggleElement(obj, block, label, depth, path));
    	$(element).find(".dbg-toggle :last-child").trigger("click");
    }
  }
}

function renderObjectDetails(obj, element, depth, path) {
  depth++;
  if (isArray(obj)) {
    var arrtab = document.createElement('table');
    arrtab.className = 'dbg-arr';
    var arrtbody = document.createElement('tbody');
    var cnt=0;
    for (var i=0; i<obj.length; i++) {
      if (isSerializable(obj[i])) {
        var row = document.createElement('tr');
        var identCol = document.createElement('td');
        identCol.className = 'ident';
        var valueCol = document.createElement('td');
        valueCol.className = 'dbg-value';
        renderObject(obj[i], valueCol, depth, path);
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
        renderObject(obj[propName], valueCol, depth, path);

        row.appendChild(labelCol);
        row.appendChild(valueCol);
        objtbody.appendChild(row);
      }
    }
    objtab.appendChild(objtbody);
    element.appendChild(objtab);
  }
}


function createToggleElement(obj, target, label, depth, path) {
  target.style.display = 'none';
  var toggle = document.createElement('div');
  var src = "/damsmanager/images/dir.gif";
  var dotIdx = label.lastIndexOf(".");
  if(depth == 1 || (dotIdx > 0 && dotIdx >= label.length - 4)){
  	src = "/damsmanager/images/space.gif";
  }
  
  var textNode = null;
  if(depth !== 1){
      textNode = document.createTextNode(label);
	  var folder = document.createElement('img');
	  folder.setAttribute("src",src);
	  
	  var input = document.createElement('input');
	  input.setAttribute("id",label+inputIdx++);
	  input.setAttribute("type","checkbox");
	  input.setAttribute("value",path);
	  input.onclick = function(){checkboxChange(this)};
	  toggle.appendChild(input);
	  toggle.appendChild(folder);
	  toggle.className = 'dbg-toggle';
	  toggle.onclick = function() {
	    if (wrapper.className=='dbg-toggle-closed') {
	      wrapper.className = 'dbg-toggle-open';
	      if (!target.firstChild) {
	        renderObjectDetails(obj, target, depth, path);
	      }
	      target.style.display = 'block';
	    } else {
	      wrapper.className = 'dbg-toggle-closed';
	      target.style.display = 'none';
	    }
	  }
	  toggle.appendChild(textNode);
  }else{
    var folder = document.createElement('img');
  	folder.setAttribute("src","/damsmanager/images/openfldr.gif");
  	toggle.appendChild(folder);
  	var spanNode = document.createElement('span');
  	toggle.appendChild(spanNode)
  	spanNode.appendChild(document.createTextNode(label));
  	spanNode.className = 'dbg-title';
  }

  var wrapper = document.createElement('div');

  wrapper.className = 'dbg-toggle-closed';

  if(depth == 1){
     wrapper.className = 'dbg-toggle-open';
     if (!target.firstChild) {
        renderObjectDetails(obj, target, depth, path);
      }
      target.style.display = 'block';
  }
  
  toggle.setAttribute("title", label);
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
                              (a=='\t') ? '\\t': ''
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

Directories.prototype = {

  render : function (element) {
    while (element.firstChild) element.removeChild(element.firstChild);
    var cell = document.createElement('div');
    cell.className = 'dbg-value';
    var path = "";
    renderObject(this.obj, cell, 1, path);
    element.appendChild(cell);
  }
  ,

  toString : function () {
    return toJSON(this.obj);
  }
}

})();

