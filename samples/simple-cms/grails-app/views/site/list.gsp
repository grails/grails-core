
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <meta name="layout" content="main" />
         <title>Site List</title>         
    </head>
    <body>
        <div class="body">
           <h1>Site List</h1>
            <g:if test="flash['message']">
                 <div class="message">
                       ${flash['message']}
                 </div>
            </g:if>
           <table class="dialog">
               <tr>
                   
                                      
                        <th>Id</th>
                                      
                        <th>Name</th>
                                      
                        <th>Domain</th>
                                      
                        <th>Home Page</th>
                   
                   <th></th>
               </tr>
               <g:each in="${siteList}">
                    <tr>
                       
                            <td>${it.id}</td>
                       
                            <td>${it.name}</td>
                       
                            <td>${it.domain}</td>
                       
                            <td>${it.homePage}</td>
                       
                       <td class="actionButtons">
                            <span class="actionButton"><g:link action="show" id="${it.id}">Show</g:link></span>
                       </td>
                    </tr>
               </g:each>
           </table>
		   <div class="paginateButtons">
		   		<g:paginate total="${Site.count()}" />
		   </div>		   		   
		   <div class="buttons">
		   		<g:form url="[action:'create']" method="get">
					<input type="submit" value="Create New" />
				</g:form>
		   </div>			   
		   </div>		   		  
        </div>
    </body>
</html>
            