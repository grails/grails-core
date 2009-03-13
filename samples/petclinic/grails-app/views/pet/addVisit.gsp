<html>
	<head>
		<meta http-equiv="Content-type" content="text/html; charset=utf-8">
		<meta name="layout" content="main">
		<title>Add Visit</title>		
	</head>
	<body id="addvisit">
		<h2><g:if test="${!visit.id}">New </g:if>Visit:</h2>

		<g:form action="addVisit">
		  <b>Pet:</b>
		  <table width="333">
		    <tr>
		    <thead>
		      <th>Name</th>
		      <th>Birth Date</th>
		      <th>Type</th>
		      <th>Owner</th>
		    </thead>
		    </tr>
		    <tr>
		      <td>${visit.pet.name}</td>
		      <td><g:formatDate date="${visit.pet.birthDate}" format="yyyy-MM-dd"/></td>
		      <td>${visit.pet.type.name}</td>
		      <td>${visit.pet.owner.firstName} ${visit.pet.owner.lastName}</td>
		    </tr>
		  </table>

		  <table width="333">
		    <tr>
		      <th>
		        Date:
		        <br/><span class="errors"><g:fieldError bean="${visit}" field="date" /></span>
		      </th>
		      <td>
		        <g:datePicker name="visit.date" value="${visit.date}" precision="day"></g:datePicker>
		      </td>
		    <tr/>
		    <tr>
		      <th valign="top">
		        Description:
		        <br/><span class="errors"><g:fieldError bean="${visit}" field="description" /></span>
		      </th>
		      <td>
		        <g:textArea name="visit.description" rows="10" cols="25"/>
		      </td>
		    </tr>
		    <tr>
		      <td colspan="2">
		        <input type="hidden" name="visit.pet.id" value="${visit.pet.id}"/>
		        <p class="submit"><input type="submit" value="Add Visit"/></p>
		      </td>
		    </tr>
		  </table>
		</g:form>

		<br/>
		<b>Previous Visits:</b>
		<table width="333">
		  <tr>
		    <th>Date</th>
		    <th>Description</th>
		  </tr>
		  <g:each var="v" in="${visit.pet.visits}">
		    <g:if test="${v.id}">
		      <tr>
		        <td><g:formatDate date="${v.date}" format="yyyy-MM-dd"/></td>
		        <td>${v.description}</td>
		      </tr>
		    </g:if>
		  </g:each>
		</table>
		
	</body>
</html>