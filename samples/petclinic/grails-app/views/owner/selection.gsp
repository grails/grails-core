<html>
	<head>
		<meta http-equiv="Content-type" content="text/html; charset=utf-8">
		<meta name="layout" content="main">
		<title>Select Owner</title>
		
	</head>
	
	<body id="selection">
		<h2>Owners:</h2>

		<table>
		  <tr>
		  <thead>
		    <th>Name</th>
		    <th>Address</th>
		    <th>City</th>
		    <th>Telephone</th>
		    <th>Pets</th>
		  </thead>
		  </tr>
		  <g:each var="owner" in="${owners}">
		    <tr>
		      <td>
		          <g:link action="show" id="${owner.id}">${owner.firstName} ${owner.lastName}</g:link></a>
		      </td>
		      <td>${owner.address}</td>
		      <td>${owner.city}</td>
		      <td>${owner.telephone}</td>
		      <td>
		        <g:each var="pet" in="${owner.pets}">
		          ${pet.name} &nbsp;
		        </g:each>
		      </td>
		    </tr>
		  </g:each>
		</table>		
	</body>
</html>