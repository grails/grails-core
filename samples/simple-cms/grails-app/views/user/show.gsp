
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <meta name="layout" content="main" />
         <title>Show User</title>
         <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
    </head>
    <body>
        <div class="body">
           <h1>Show User</h1>
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <div class="dialog">
                 <table>
                   
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Id:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${user.id}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Company:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${user.company}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Email:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${user.email}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">First Name:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${user.firstName}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Last Name:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${user.lastName}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Login:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${user.login}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Pwd:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${user.pwd}</td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Role:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value"><g:link controller="role" action="show" id="${user?.role?.id}">${user?.role}</g:link></td>
                              
                        </tr>
                   
                        <tr class="prop">
                              <td valign="top" style="text-align:left;" width="20%" class="name">Title:</td>
                              
                                    <td valign="top" style="text-align:left;" class="value">${user.title}</td>
                              
                        </tr>
                   
                 </table>
           </div>
           <div class="buttons">
               <g:form controller="user">
                 <input type="hidden" name="id" value="${user?.id}" />
                 <span class="button"><g:actionSubmit value="Edit" /></span>
                 <%-- <span class="button"><g:actionSubmit value="Delete" /></span> --%>
               </g:form>
           </div>
        </div>
    </body>
</html>
            