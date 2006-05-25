
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <meta name="layout" content="main" />
         <title>Edit Role</title>
         <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
    </head>
    <body>
        <div class="body">
           <h1>Edit Role</h1>
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <g:hasErrors bean="${role}">
                <div class="errors">
                    <g:renderErrors bean="${role}" as="list" />
                </div>
           </g:hasErrors>
           <div class="prop">
	      <span class="name">Id:</span>
	      <span class="value">${role?.id}</span>
	      <input type="hidden" name="role.id" value="${role?.id}" />
           </div>           
           <g:form controller="role" method="post">
               <input type="hidden" name="id" value="${role?.id}" />
               <div class="dialog">
                <table>

                       
                       
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='title'>Title:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:role,field:'title','errors')}'><input type="text" name='title' value="${role?.title}" /></td></tr>
                       
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='name'>Name:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:role,field:'name','errors')}'><select name='name'><option value='System Administrator'>System Administrator</option><option value='Content Editor'>Content Editor</option><option value='Content Approver'>Content Approver</option><option value='General User'>General User</option></select></td></tr>
                       
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='site'>Site:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:role,field:'site','errors')}'><g:select optionKey="id" from="${Site.list()}" name='site.id' value='${role?.site?.id}'></g:select></td></tr>
                       
                </table>
               </div>

               <div class="buttons">
                     <span class="button"><g:actionSubmit value="Update" /></span>
                     <span class="button"><g:actionSubmit value="Delete" /></span>
               </div>
            </g:form>
        </div>
    </body>
</html>
            