<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <meta name="layout" content="main" />
         <title>Questionnaire List</title>
         <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
    </head>
    <body>
        <div class="body">
           <h1>Questionnaire List</h1>
            <g:if test="flash['message']">
                 <div class="message">
                       ${flash['message']}
                 </div>
            </g:if>
           <table>
               <tr>
                        <th>Id</th>
                                      
                        <th>Title</th>
                                      
                        <th>Site</th>
                                      
                        <th>Type</th>
                                      
                   <th></th>
               </tr>
               <g:each in="${tests?}">
                    <tr>
                       
                            <td>${it.id}</td>
                       
                            <td>${it.title}</td>
                       
                            <td>${it.site}</td>
                       
                            <td>${it.type}</td>
                       
                       <td class="actionButtons">
                            <span class="actionButton"><g:link action="show" id="${it.id}">Show</g:link></span>
                       </td>
                    </tr>
               </g:each>
           </table>
        </div>
    </body>
</html>
            
