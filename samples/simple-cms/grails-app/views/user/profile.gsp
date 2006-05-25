
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <title>Profile: ${user}</title>
		 <g:render template="/pagemeta" model="[:]" />	 	
    </head>
    <body>
        <div class="body">
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <div class="dialog">
                 <table class="userForm">
                   
				 		 <g:if test="${session.user.id == user.id}">
							<tr class="prop">
								  <td valign="top" style="text-align:left;" width="20%" class="name">Login:</td>
								  
										<td valign="top" style="text-align:left;" class="value">${user?.login}</td>
								  
							</tr>						 
							<tr class="prop">
								  <td valign="top" style="text-align:left;" width="20%" class="name">Email:</td>
								  
										<td valign="top" style="text-align:left;" class="value">${user?.email}</td>
								  
							</tr>							
						 </g:if>
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Title:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${user?.title}</td>
                              
                        </tr>
						
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">First Name:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${user?.firstName}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Last Name:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${user?.lastName}</td>
                              
                        </tr>
						
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Company:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${user?.company}</td>
                              
                        </tr>
						
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Job Title:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${user?.jobTitle}</td>
                              
                        </tr>						

                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Employment Status:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${user?.employmentStatus}</td>
                              
                        </tr>
						
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Job Function:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${user?.jobFunction}</td>
                              
                        </tr>		
						
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Job Level:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${user?.jobLevel}</td>
                              
                        </tr>
						

                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Industry Sector:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${user?.industrySector}</td>
                              
                        </tr>                   
                   

                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Location:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${user?.location}</td>
                              
                        </tr>
						
                 </table>
           </div>
           <div class="buttons">
               <g:form controller="user">
                 <input type="hidden" name="id" value="${user?.id}" />
				  <g:if test="${session.user.id == user.id}">
				 	<span class="button"><g:actionSubmit value="Edit Profile" /></span>
				  </g:if>
				  <g:else>
					<span class="button"><g:actionSubmit value="Contact" /></span>
				  </g:else>
               </g:form>
           </div>
        </div>
    </body>
</html>
            
