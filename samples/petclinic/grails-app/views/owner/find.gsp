<html>
	<head>
		<meta http-equiv="Content-type" content="text/html; charset=utf-8">
		<meta name="layout" content="main">
		<title>Find Owners</title>		
	</head>
	
	<body id="find">
		<h2>Find Owners:</h2>

		
		<g:form action="find">
		  <table>
		    <tr>
		      <th>
		        Last Name: 
		        <br/> 
		        <g:textField name="lastName"/>
			    <span class="errors"><g:message code="${message}"></g:message></span>
		      </th>
		    </tr>
		    <tr>
		      <td><p class="submit"><input type="submit" value="Find Owners"/></p></td>
		    </tr>
		  </table>
		</g:form>

		<br/>
		<g:link controller="owner" action="add">Add Owner</g:link></a>		
	</body>
</html>