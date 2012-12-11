<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<div id="cButtonMenu" class="popMenu">
 	<div id="cMenu1" class="menuItem" onclick="document.location.href='/damsmanager/jsp/introduction.jsp';" onMouseOver="activadeMenuItem(this, 'cButton');" onMouseOut="resetMenuItem(this, 'cButton');">Introduction</div>
	<div id="cMenu3" class="menuItem" onclick="document.location.href='/damsmanager/ingest.do'" onMouseOver="activadeMenuItem(this, 'cButton');" onMouseOut="resetMenuItem(this, 'cButton');">Staging Ingest</div>
 	<div id="cMenu4" class="menuItem" onclick="document.location.href='/damsmanager/controlPanel.do'" onMouseOver="activadeMenuItem(this, 'cButton');" onMouseOut="resetMenuItem(this, 'cButton');">Process Manager</div>
 	<div id="cMenu5" class="menuItem" onclick="document.location.href='/damsmanager/dataConverter.do'" onMouseOver="activadeMenuItem(this, 'cButton');" onMouseOut="resetMenuItem(this, 'cButton');">Data Converter</div>
</div>
<div id="mButtonMenu" class="popMenu">
	<div id="mMenu0" class="menuItem" onclick="document.location.href='/damsmanager/solrDump.do'" onMouseOver="activadeMenuItem(this, 'mButton');" onMouseOut="resetMenuItem(this, 'mButton');">SOLR Dump Utility</div>
	<div id="mMenu1" class="menuItem" onclick="document.location.href='/damsmanager/emu.do'" onMouseOver="activadeMenuItem(this, 'mButton');" onMouseOut="resetMenuItem(this, 'mButton');">Export Metadata Utility</div>
	<div id="mMenu2" class="menuItem" onclick="document.location.href='/damsmanager/devUpload.do'" onMouseOver="activadeMenuItem(this, 'mButton');" onMouseOut="resetMenuItem(this, 'mButton');">FileStore Upload</div>
	<div id="mMenu3" class="menuItem" onclick="document.location.href='/damsmanager/statusUpload.do'" onMouseOver="activadeMenuItem(this, 'mButton');" onMouseOut="resetMenuItem(this, 'mButton');">Status Upload</div>
	<div id="mMenu4" class="menuItem" onclick="document.location.href='/damsmanager/tsAnalyzer.do'" onMouseOver="activadeMenuItem(this, 'mButton');" onMouseOut="resetMenuItem(this, 'mButton');">SPARQL Analyzer</div>
</div>
<div id="tButtonMenu" class="popMenuShort">
 	<div id="tMenu1" class="menuItem" onclick="document.location.href='/damsmanager/stats.do'" onMouseOver="activadeMenuItem(this, 'tButton');" onMouseOut="resetMenuItem(this, 'tButton');">DLC</div>
 	<div id="tMenu2" class="menuItem" onclick="document.location.href='/damsmanager/dlPopular.do'" onMouseOver="activadeMenuItem(this, 'tButton');" onMouseOut="resetMenuItem(this, 'tButton');">DLC Popular</div>
 	<div id="tMenu3" class="menuItem" onclick="document.location.href='/damsmanager/statsKeywords.do'" onMouseOver="activadeMenuItem(this, 'tButton');" onMouseOut="resetMenuItem(this, 'tButton');">DLC Keywords</div>
 	<div id="tMenu4" class="menuItem" onclick="document.location.href='/damsmanager/statsSummary.do?start=2010-03-01'" onMouseOver="activadeMenuItem(this, 'tButton');" onMouseOut="resetMenuItem(this, 'tButton');">DLC Summary</div>
</div>
<div id="rButtonMenu" class="popMenuShort">
 	<div id="rMenu1" class="menuItem" onclick="javascript:popWindow('http://pelennor.ucsd.edu:8080')" onMouseOver="activadeMenuItem(this, 'rButton');" onMouseOut="resetMenuItem(this, 'rButton');">JIRA</div>
 	<div id="rMenu2" class="menuItem" onclick="javascript:popWindow('http://andram.ucsd.edu/trac/')" onMouseOver="activadeMenuItem(this, 'rButton');" onMouseOut="resetMenuItem(this, 'rButton');">Subversion Log</div>
 	<div id="rMenu3" class="menuItem" onclick="javascript:popWindow('https://www.preserve.cdlib.org/')" onMouseOver="activadeMenuItem(this, 'rButton');" onMouseOut="resetMenuItem(this, 'rButton');">Libraries DPR</div>
 	<div id="rMenu4" class="menuItem" onclick="javascript:popWindow('http://roger.ucsd.edu')" onMouseOver="activadeMenuItem(this, 'rButton');" onMouseOut="resetMenuItem(this, 'rButton');">Roger</div>
</div>