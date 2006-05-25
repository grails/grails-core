
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <meta name="layout" content="main" />
         <title>Create Role</title>
         <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
    </head>
    <body>
        <div class="body">
           <h1>Create Role</h1>
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <g:hasErrors bean="${role}">
                <div class="errors">
                    <g:renderErrors bean="${role}" as="list" />
                </div>
           </g:hasErrors>
           <g:form action="save" method="post">
               <div class="dialog">
                <table>

                       
                       
                                  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='title'>Title:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:role,field:'title','errors')}'><input type="text" name='title' value="${role?.title}" /></td></tr>
                       
                                  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='name'>Name:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:role,field:'name','errors')}'><select name='name'><option value='System Administrator'>System Administrator</option><option value='Content Editor'>Content Editor</option><option value='Content Approver'>Content Approver</option><option value='General User'>General User</option></select></td></tr>
                       
                                  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='site'>Site:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:role,field:'site','errors')}'><g:select optionKey="id" from="${Site.list()}" name='site.id' value='${role?.site?.id}'></g:select></td></tr>
                       
               </table>
               </div>
               <div class="buttons">
                     <span class="formButton">
                        <input type="submit" value="Create"></input>
                     </span>
               </div>
            </g:form>
        </div>
    </body>
</html>
            