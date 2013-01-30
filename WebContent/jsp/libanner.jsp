<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
	<div id="tdr_title">
		<div id="tdr_title_content">
			<div id="tdr_title_ucsd_title"><a href="http://www.ucsd.edu/">UC San Diego</a></div>
			<div id="tdr_title_page_title"><a href="http://libraries.ucsd.edu/index.html">The Library</a></div>
		</div><!-- /tdr_title_content -->
	</div><!-- /tdr_title -->
       <noscript>Your browser doesn't support Javascript.  You can either view our <a href="/apps/mobile/">mobile site</a>, or use a Javascript-enabled browser to view the main Digital Library Collections site.</noscript>

	<div id="tdr_nav">
		<div id="tdr_nav_content">
			<script>
				draw_ucsd_menu(top_nav,"tdr_nav_list");
			</script>
			<noscript>
				<ul id="tdr_nav_list">
					<li><a href="http://libraries.ucsd.edu/">Home</a></li>
					<li><a href="http://libraries.ucsd.edu/locations">Libraries</a></li>
					<li><a href="http://libraries.ucsd.edu/hours">Hours</a></li>
					<li><a href="http://libraries.ucsd.edu/tools">Research Tools</a></li>
					<li><a href="http://libraries.ucsd.edu/collections">Collections</a></li>
					<li><a href="http://libraries.ucsd.edu/services">Services</a></li>
					<li><a href="http://libraries.ucsd.edu/resources/course-reserves">Reserves</a></li>
					<li><a href="http://roger.ucsd.edu/">Catalogs</a></li>
					<li><a href="http://roger.ucsd.edu/patroninfo">My Library Account</a></li>
					<li><a href="http://libraries.ucsd.edu/help/ask-a-librarian">Ask a Librarian</a></li>
					<li><a href="http://libraries.ucsd.edu/help">Help</a></li>
				</ul>
			</noscript>
		</div><!-- /tdr_nav_content -->
	</div><!-- /tdr_nav -->
	
	<div id="tdr_search">
		<div id="tdr_search_content">
			<form action="http://www-act.ucsd.edu/cwp/tools/search-redir" method="get">
				<fieldset>
					<legend>Site Search</legend>
					<label for="search-scope">Search</label>
					<select id="search-scope" name="search-scope">
						<option id="this-site" selected="selected" value="cascade-lib">This Site</option>
						<option id="search-ucsd" value="default_collection">All UCSD Sites</option>
						<option id="search-people" value="faculty-staff">Faculty/Staff</option>
						<option id="search-people" value="students">Students</option>
					</select>
					<input name="search-term" size="20" type="text"/>
					<button type="submit">Search!</button>
				</fieldset>
			</form>
		</div><!-- /tdr_search_content -->
	</div><!-- /tdr_search -->
