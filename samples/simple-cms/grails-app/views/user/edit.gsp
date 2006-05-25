
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <meta name="layout" content="main" />
         <title>Edit User</title>
         <link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
    </head>
    <body>
        <div class="body">
           <h1>Edit User</h1>
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <g:hasErrors bean="${user}">
                <div class="errors">
                    <g:renderErrors bean="${user}" as="list" />
                </div>
           </g:hasErrors>
           <div class="prop">
	      <span class="name">Id:</span>
	      <span class="value">${user?.id}</span>
	      <input type="hidden" name="user.id" value="${user?.id}" />
           </div>           
           <g:form controller="user" method="post">
               <input type="hidden" name="id" value="${user?.id}" />
               <div class="dialog">
                <table>

                       
                       
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='company'>Company:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'company','errors')}'><input type='text' name='company' value='${user?.company}' /></td></tr>
                       
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='email'>Email:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'email','errors')}'><input type='text' name='email' value='${user?.email}' /></td></tr>
                       
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='firstName'>First Name:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'firstName','errors')}'><input type='text' name='firstName' value='${user?.firstName}' /></td></tr>
                       
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='lastName'>Last Name:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'lastName','errors')}'><input type='text' name='lastName' value='${user?.lastName}' /></td></tr>
                       
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='login'>Login:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'login','errors')}'><input type='text' name='login' value='${user?.login}' /></td></tr>
                       
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='pwd'>Pwd:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'pwd','errors')}'><input type='text' name='pwd' value='${user?.pwd}' /></td></tr>
                       
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='role'>Role:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'role','errors')}'><g:select optionKey="id" from="${Role.list()}" name='role.id' value='${user?.role?.id}'></g:select></td></tr>
                       
				<tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='title'>Title:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'title','errors')}'><input type='text' name='title' value='${user?.title}' /></td></tr>
                       
                </table>
               </div>

               <div class="buttons">
                     <span class="button"><g:actionSubmit value="Update" /></span>
                     <%-- <span class="button"><g:actionSubmit value="Delete" /></span> --%>
               </div>
            </g:form>
        </div>
    </body>
</body>
            