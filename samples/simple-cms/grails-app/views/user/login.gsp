
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
			<g:if test="${flash.admin}">
				<meta name="layout" content="main" />
			</g:if>
			<g:else>			
				<meta name="layout" content="${session.site.domain}" />
				<link rel="stylesheet" href="<%createLinkTo(dir:"css/${session.site.domain}",file:'login.css')%>" type="text/css" media="screen,projection" />
			</g:else>		 
		 
         <title>Logon to ${session.site.name}</title>
    </head>
    <body>
        <div class="body">
           <g:if test="${flash['message']}">
                 <div class="message">${flash['message']}</div>
           </g:if>
           <g:hasErrors bean="${user}">
                <div class="errors">
                    <g:renderErrors bean="${user}" as="list" />
                </div>
           </g:hasErrors>
           <g:form action="handleLogin" method="post">
           		<input name="forward" type="hidden" value="${flash.forward}" />
               <div class="dialog">
			   	 <p>Enter your login details below:</p>
                <table  class="userForm">
                  <tr class='prop'>
                      <td valign='top' style='text-align:left;' width='20%'>
                          <label for='login'>Login:</label>
                      </td>
                      <td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'company','errors')}'>
                          <input id="login" type='text' name='login' value='${user?.login}' />
                      </td>
                  </tr>

                  <tr class='prop'>
                      <td valign='top' style='text-align:left;' width='20%'>
                          <label for='pwd'>Password:</label>
                      </td>
                      <td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'company','errors')}'>
                          <input id="pwd" type='password' name='pwd' value='${user?.pwd}' />
                      </td>
                  </tr>
                       
               </table>			   	
				<g:if test="${!flash.admin}">
					<p>Forgotten your password? <g:link action="remind">Click Here</g:link></p>
					<p>New users <g:link action="register">Register Here</g:link></p>
				</g:if>
               </div>
               <div class="buttons">
                     <span class="formButton">
                        <input type="submit" value="Login"></input>
                     </span>
               </div>
            </g:form>
        </div>
    </body>
</html>
            