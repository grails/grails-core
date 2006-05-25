
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <meta name="layout" content="main" />
         <title>Show Role</title>
         <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
    </head>
    <body>
        <div class="body">
           <h1>Show Role</h1>
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <div class="dialog">
                 <table>
                   
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Id:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${role.id}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Title:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${role.title}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Name:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${role.name}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Site:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value"><g:link controller="site" action="show" id="${role?.site?.id}">${role?.site}</g:link></td>
                              
                        </tr>
                   
                 </table>
           </div>
           <div class="buttons">
               <g:form controller="role">
                 <input type="hidden" name="id" value="${role?.id}" />
                 <span class="button"><g:actionSubmit value="Edit" /></span>
               </g:form>
           </div>
        </div>
    </body>
</html>
            