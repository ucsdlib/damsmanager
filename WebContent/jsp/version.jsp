<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%
    String version = application.getInitParameter("version-number");
    if ( version == null || version.trim().equals("") )
    {
            version = "0.0.0";
    }
    
    String build = application.getInitParameter("build-date");
    if ( build == null || build.trim().equals("") )
    {
            build = "Not Available";
    }
    
	String userVersion = null;
	try
	{
		userVersion = application.getInitParameter("user-version");
	}catch ( Exception ex ){}
	userVersion = userVersion==null?"Not Available":userVersion;
	
	String buildBranch = null;
	try
	{
		buildBranch = application.getInitParameter("build-branch");
	}catch ( Exception ex ){}
	buildBranch = buildBranch==null?"Not Available":buildBranch;
%><html>
  <head>
    <title>XDRE Manager Version <%=version%> (<%=build%>), DAMS Version <%=userVersion%></title>
  </head>
  <body>
    <p>XDRE Manager Version <%=version%>, Build <%=build%></p>
    <p>DAMS Version <%=userVersion%></p>
    <p>Built from: <%=buildBranch%></p>
  </body>
</html>
