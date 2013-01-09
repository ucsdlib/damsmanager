		<div id="tdr_content" class="tdr_fonts">
			<div id="tdr_content_content">
					
				<!-- BEGIN CUSTOM CONTENT AREA (MAIN BODY) -->
				<h1 style="text-align:left;padding:0px;margin:0px;padding-top:10px;padding-bottom:5px;">DAMS Manager</h1>
				<div style="float:left;border: 1px solid #e2e2da;padding-left:10px;padding-right:20px;padding-top:10px;padding-bottom:5px;margin-bottom: 100px;">
					<h5 style="text-align:left;padding:0px;margin:0px;">Login using active directory username/password:</h5><br>
					<form method="post" action="/damsmanager/j_security_check" id="login" name="loginForm" target="_top"> 
					<table cellspacing=5 style="margin:0px;margin-top:0px;">
					<tr>
						<td width="70">
							<strong>Username:&nbsp;</strong>
						</td>
						<td align="left">
							<input type="text" name="j_username" size="18"/>
						</td>
					</tr>

					<tr>
						<td width="70">
							<strong>Password:&nbsp;</strong>
						</td> 
						<td align="left">
							<input type="password" name="j_password" size="18"/>
						</td>
					</tr>
					<tr>
						<td></td>

						<td align="center" style="padding-top:10px;">
							<input type="submit" name="action" value="login" class="login" style="padding:2px 5px;font-size:12px;"/>
						</td>
					</tr>
					</table>
					<input type="hidden" name="errorPage" value="/apps/public/loginError.html"/>
					</form>
				</div>
				<!-- END CUSTOM CONTENT AREA (MAIN BODY) -->
			</div><!-- /tdr_content_content -->
			
		</div><!-- /tdr_content -->