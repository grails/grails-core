
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <meta name="layout" content="${session.site.domain}" />
         <title>Password Reminder</title>
		 <link rel="stylesheet" href="<%createLinkTo(dir:"css/${session.site.domain}",file:'login.css')%>" type="text/css" media="screen,projection" />
		 <style type="text/css">
		 	input { width: 150px; }
		 </style>
    </head>
    <body>
        <div class="body">
           <h1>Retrieve Password for ${session.site.name}</h1>
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <g:hasErrors bean="${user}">
                <div class="errors">
                    <g:renderErrors bean="${user}" as="list" />
                </div>
           </g:hasErrors>
           <g:form action="password" method="post">
           		<input name="forward" type="hidden" value="${flash.forward}" />
               <div class="dialog">
			   	 <p>Enter the email address which you registered with below:</p>
                <table  class="userForm">
                  <tr class='prop'>
                      <td valign='top' style='text-align:left;' width='20%'>
                          <label for='login'>Email:</label>
                      </td>
                      <td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'company','errors')}'>
                          <input id="email" type='text' name='email' value='${email}' />
                      </td>
                  </tr>

                       
               </table>
               </div>
               <div class="buttons">
                     <span class="formButton">
                        <input type="submit" value="Retrieve Password"></input>
                     </span>
               </div>
            </g:form>
        </div>
    </body>
</html>
            
