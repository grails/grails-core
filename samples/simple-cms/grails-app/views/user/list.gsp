
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <meta name="layout" content="main" />
         <title>User List</title>
         <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
    </head>
    <body>
        <div class="body">
           <h1>User List</h1>
            <g:if test="flash['message']">
                 <div class="message">
                       ${flash['message']}
                 </div>
            </g:if>
           <table>
               <tr>
                   
                                      
                        <th>Id</th>
                                      
                        <th>Company</th>
                                      
                        <th>Email</th>
                                      
                        <th>First Name</th>
                                      
                        <th>Last Name</th>
                                      
                        <th>Login</th>
                   
                   <th></th>
               </tr>
               <g:each in="${userList}">
                    <tr>
                       
                            <td>${it.id}</td>
                       
                            <td>${it.company}</td>
                       
                            <td>${it.email}</td>
                       
                            <td>${it.firstName}</td>
                       
                            <td>${it.lastName}</td>
                       
                            <td>${it.login}</td>
                       
                       <td class="actionButtons">
                            <span class="actionButton"><g:link action="show" id="${it.id}">Show</g:link></span>
                       </td>
                    </tr>
               </g:each>
           </table>
		   <div class="paginateButtons">
		   		<g:paginate total="${User.count()}" />
		   </div>		   
		   <div class="buttons">
		   		<g:form url="[action:'create']" method="get">
					<input type="submit" value="Create New" />
				</g:form>
		   </div>		   
        </div>
    </body>
</body>
            