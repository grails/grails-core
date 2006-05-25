
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <meta name="layout" content="main" />
         <title>AccessKey List</title>
         <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
    </head>
    <body>
        <div class="body">
           <h1>AccessKey List</h1>
            <g:if test="flash['message']">
                 <div class="message">
                       ${flash['message']}
                 </div>
            </g:if>
			
           <table>
               <tr>
                   
                                      
                        <th>Id</th>
                                      
                        <th>Start Date</th>
                                      
                        <th>End Date</th>
                                      
                        <th>Expiry Date</th>
                                      
                        <th>Usages</th>
                                      
                        <th>Code</th>
                   
                   <th></th>
               </tr>
               <g:each in="${accessKeyList}">
                    <tr>
                       
                            <td>${it.id}</td>
                       
                            <td>${it.startDate}</td>
                       
                            <td>${it.endDate}</td>
                       
                            <td>${it.expiryDate}</td>
                       
                            <td>${it.usages}</td>
                       
                            <td>${it.code}</td>
                       
                       <td class="actionButtons">
                            <span class="actionButton"><g:link action="show" id="${it.id}">Show</g:link></span>
                       </td>
                    </tr>
               </g:each>
           </table>
		   <div class="paginateButtons">
		   		<g:paginate total="${AccessKey.count()}" />
		   </div>		   
		   <div class="buttons">
		   		<g:form url="[action:'create']" method="get">
					<input type="submit" value="Create New" />
				</g:form>
		   </div>
        </div>
    </body>
</html>
            