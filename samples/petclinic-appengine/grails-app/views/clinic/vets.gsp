<html>
	<head>
		<meta http-equiv="Content-type" content="text/html; charset=utf-8">
		<meta name="layout" content="main">
		<title>Veterinarians</title>
		
	</head>
	<body id="vets">
		<h2>Veterinarians:</h2>

		<table>
		  <tr>
		  <thead>
		    <th>Name</th>
		    <th>Specialties</th>
		  </thead>
		  </tr>
		  <g:each var="vet" in="${vets}">
		    <tr>
		      <td>${vet.firstName} ${vet.lastName}</td>
		      <td>
			    <g:each var="speciality" in="${vet.specialities}">
		          ${speciality.name}
		        </g:each>
		        <g:if test="${vet.specialities.size() == 0}">none</g:if>
		      </td>
		    </tr>
		  </g:each>
		</table>		
	</body>
</html>