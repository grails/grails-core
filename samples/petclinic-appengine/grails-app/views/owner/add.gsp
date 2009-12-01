<html>
	<head>
		<meta http-equiv="Content-type" content="text/html; charset=utf-8">
		<meta name="layout" content="main">
		<title>Add Owner</title>
		
	</head>
	
	<body id="add">
		<h2><g:if test="${!ownerBean.id}">New </g:if>Owner:</h2>
		<g:form action="${ ownerBean.id ? 'edit' : 'add'}" id="${ownerBean?.id}">
		  <table>
		    <tr>
		      <th>
				<g:render template="/common/formField" 
				          model="[name:'owner',bean:ownerBean, field:'firstName', label:'First Name']" />			
		      </th>
		    </tr>
		    <tr>
		      <th>
				<g:render template="/common/formField" 
				          model="[name:'owner',bean:ownerBean, field:'lastName', label:'Last Name']" />
		      </th>
		    </tr>
		    <tr>
		      <th>
				<g:render template="/common/formField" 
				          model="[name:'owner',bean:ownerBean, field:'address', label:'Address']" />
		      </th>
		    </tr>
		    <tr>
		      <th>
				<g:render template="/common/formField" 
				          model="[name:'owner',bean:ownerBean, field:'city', label:'City']" />

		      </th>
		    </tr>
		    <tr>
		      <th>
				<g:render template="/common/formField" 
				          model="[name:'owner',bean:ownerBean, field:'telephone', label:'Telephone']" />
		      </th>
		    </tr>
		    <tr>
		      <td>
	            <p class="submit"><input type="submit" value="${ ownerBean?.id ? 'Update' : 'Add'} Owner"/></p>
		      </td>
		    </tr>
		  </table>
		</g:form>		
	</body>
</html>