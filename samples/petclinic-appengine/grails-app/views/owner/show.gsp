<html>
	<head>
		<meta http-equiv="Content-type" content="text/html; charset=utf-8">
		<meta name="layout" content="main">
		<title>Owner Information</title>
		
	</head>
	
	<body id="show">
		<h2>Owner Information</h2>

		  <table>
		    <tr>
		      <th>Name</th>
		      <td><b>${ownerBean.firstName} ${ownerBean.lastName}</b></td>
		    </tr>
		    <tr>
		      <th>Address</th>
		      <td>${ownerBean.address}</td>
		    </tr>
		    <tr>
		      <th>City</th>
		      <td>${ownerBean.city}</td>
		    </tr>
		    <tr>
		      <th>Telephone </th>
		      <td>${ownerBean.telephone}</td>
		    </tr>
		  </table>
		  <table class="table-buttons">
		    <tr>
		      <td colspan="2" align="center">
		        <g:form method="GET" url="[action:'edit', id:ownerBean.id.id]">
		          <p class="submit"><input type="submit" value="Edit Owner"/></p>
		        </g:form>
		      </td>
		      <td>
		        <g:form method="GET" url="[controller:'pet', action:'add']" name="formAddPet">
		          <input type="hidden" name="owner.id" value="${ownerBean.id.id}"/>
		          <p class="submit"><input type="submit" value="Add New Pet"/></p>
		        </g:form>
		      </td>
		    </tr>
		  </table>

		  <h2>Pets and Visits</h2>

		  <g:each var="pet" in="${ownerBean.pets}">
		    <table width="94%">
		      <tr>
		        <td valign="top">
		          <table>
		            <tr>
		              <th>Name</th>
		              <td><b>${pet.name}</b></td>
		            </tr>
		            <tr>
		              <th>Birth Date</th>
		              <td><g:formatDate date="${pet.birthDate}" format="yyyy-MM-dd"/></td>
		            </tr>
		            <tr>
		              <th>Type</th>
		              <td>${pet.type?.name}</td>
		            </tr>
		          </table>
		        </td>
		        <td valign="top">
		          <table>
		            <tr>
		            <thead>
		              <th>Visit Date</th>
		              <th>Description</th>
		            </thead>
		            </tr>
		            <g:each var="visit" in="${pet.visits}">
		              <tr>
		                <td><g:formatDate date="${visit.date}" format="yyyy-MM-dd"/></td>
		                <td>${visit.description}</td>
		              </tr>
		            </g:each>
		          </table>
		        </td>
		      </tr>
		    </table>
		    <table class="table-buttons">
		      <tr>
		        <td>
		          <g:form method="GET" url="[controller:'pet', action:'edit', id:pet.id.id]"
		 							name="formEditPet${pet.id}">
		            <p class="submit"><input type="submit" value="Edit Pet"/></p>
		          </g:form>
		        </td>
		        <td>
		          <g:form method="GET" url="[controller:'pet', action:'addVisit', id:pet.id.id]"
		                  name="formVisitPet${pet.id}">
		            <p class="submit"><input type="submit" value="Add Visit"/></p>
		          </g:form>
		        </td>
		      </tr>
		    </table>
		  </g:each>
		
	</body>
</html>