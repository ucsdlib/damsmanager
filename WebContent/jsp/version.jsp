<%@ page language="java" contentType="text/html; charset=ISO-8859-1"%>
<%
    String version = application.getInitParameter("app-version");
    if ( version == null || version.trim().equals("") )
    {
            version = "0.0.0";
    }
    
    String build = application.getInitParameter("build-timestamp");
    if ( build == null || build.trim().equals("") )
    {
            build = "Not Available";
    }
    
	String srcVersion = null;
	try
	{
		srcVersion = application.getInitParameter("src-version");
	}catch ( Exception ex ){}
	srcVersion = srcVersion==null?"Not Available":srcVersion;
	
%><html>
  <head>
    <title>DAMS Manager Version <%=version%> (<%=build%>), Git Version <%=srcVersion%></title>
  </head>
  <body>
  	<h1>DAMS Manager</h1>
    <p>App Version: <%=version%></p>
    <p>Git Version: <%=srcVersion%></p>
    <p>Build: <%=build%></p>
  </body>
</html>
