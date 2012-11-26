<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<div align="right" class="menudiv">
	<div class="menubar">
      <span id="about" title="About XDRE Manager" class="menu" onMouseOut="reset(this);" onMouseOver="activate(this, 'activemenu');" onClick="javascript:window.location.href='/damsmanager/jsp/about.jsp';">About</span><span class="seperator">|</span>
      <span id="cButton" class="menu" onMouseOut="resetButton('cButton')" onMouseOver="showMenu('cButton');">Collection</span><span class="seperator">|</span>
      <span id="mButton" class="menu" onMouseOut="resetButton('mButton')" onMouseOver="showMenu('mButton');">XDRE</span><span class="seperator">|</span>
      <span id="tButton" class="menu" onMouseOut="resetButton('tButton')" onMouseOver="showMenu('tButton');">Statistics</span><span class="seperator">|</span>
      <span id="rButton" class="menu" onMouseOut="resetButton('rButton')" onMouseOver="showMenu('rButton');">Resources</span>
      <!-- <span id="logout" class="logoutButton" onMouseOut="reset(this);" onMouseOver="activate(this, 'activemenu');" onClick="javascript:window.location.href='/damsmanager/logout.do';">Log out</span> -->
	</div>
</div>
