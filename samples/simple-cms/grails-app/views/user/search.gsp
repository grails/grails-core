
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
         <title>Search Results</title>
         <g:render template="/pagemeta" model="[:]" />
    </head>
    <body>
        <div class="body">
            <g:if test="flash['message']">
                 <div class="message">
                       ${flash['message']}
                 </div>
            </g:if>
           <table>
               <tr>
                   
                        <th>First Name</th>
                                      
                        <th>Last Name</th>
						<th>Company</th>
						<th>Job Title</th>
                   
                   <th></th>
               </tr>
               <g:each in="${results?}">
                    <tr>
                       
                       
                            <td>${it.firstName}</td>
                       
                            <td>${it.lastName}</td>
							
                            <td>${it.company}</td>
							<td>${it.jobTitle}</td>
                       
                       
                       <td class="actionButtons">
                            <span class="actionButton"><g:link action="profile" id="${it.id}">Show Profile</g:link></span>
                       </td>
                    </tr>
               </g:each>
           </table>
		   <div class="paginateButtons">
		   		<g:paginate total="${flash.total}" />
		   </div>		   
        </div>
    </body>
</body>
            
