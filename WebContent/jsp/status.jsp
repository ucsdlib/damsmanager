<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<div id="statusDiv" align="center" style="display:none;">
	<div class="adjustDiv" align="center">
	<div class="emBox_status">
	<div class="emBoxBanner">XDRE Manager Progress Report</div>
	<div align="center" id="statusBarDiv">
	<table height="120px"><tr><td>
			<table cellpadding="0" cellspacing="0" border="2">
			 	<tr>
			 		<td bgcolor="#336699" style="height:20px; width:0px;" id="leftBar"></td>
			 		<td bgcolor="#DADADA" style="height:20px; width:500px;" id="rightBar"></td>
			 	</tr>
			 </table>
			 </td>
			 <td>
			 <span id="percent">&nbsp;&nbsp;0%</span>
			 </td>
			 </tr>
			<tr height="70px"><td>
				<div id="status" class="status"></div>			
			</td></tr> 
		</table>
	<div class="buttonDiv"><input type="button" id="Cancel" value="Cancel" onClick="actionCanceled();" disabled></div>	 	
	</div>
	</div>
	</div>
</div>