
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		 <meta name="layout" content="${session.site.domain}" />
		 <%-- <link rel="stylesheet" href="<%createLinkTo(dir:"css/${session.site.domain}",file:'login.css')%>" type="text/css" media="screen,projection" /> --%>
		 <link rel="stylesheet" href="<%createLinkTo(dir:"css/${session.site.domain}",file:'login.css')%>" type="text/css" media="screen,projection" />
         <title>Register for ${session.site.name}</title>
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
           <g:form action="handleRegistration" method="post">
               <div class="dialog">
                <table class="userForm">

                       <tr class='prop'>
					   		<td valign='top' style='text-align:left;' width='20%'>
								<label for='code'>Access Key:</label>
							</td>
							<td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'login','errors')}'>
								<input type='text' name='key' value='${flash.key}' />
							</td>
						</tr>				
                       <tr class='prop'>
					   		<td valign='top' style='text-align:left;' width='20%'>
								<label for='login'>Login:</label>
							</td>
							<td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'login','errors')}'>
								<input type='text' name='login' value='${user?.login}' />
							</td>
						</tr>
						
                       <tr class='prop'>
					   		<td valign='top' style='text-align:left;' width='20%'>
								<label for='login'>Password:</label>
							</td>
							<td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'login','errors')}'>
								<input type='password' name='pwd' value='${user?.pwd}' />
							</td>
						</tr>		
						
                     <tr class='prop'>
					   		<td valign='top' style='text-align:left;' width='20%'>
								<label for='login'>Confirm Password:</label>
							</td>
							<td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'login','errors')}'>
								<input type='password' name='confirm' value='${flash.confirm}' />
							</td>
						</tr>							
						  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='title'>Title:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'title','errors')}'><input type='text' name='title' value='${user?.title}' /></td></tr>

						  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='firstName'>First Name:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'firstName','errors')}'><input type='text' name='firstName' value='${user?.firstName}' /></td></tr>
			   
						  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='lastName'>Last Name:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'lastName','errors')}'><input type='text' name='lastName' value='${user?.lastName}' /></td></tr>
						  
						  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='company'>Company:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'company','errors')}'><input type='text' name='company' value='${user?.company}' /></td></tr>
			   
						  <tr class='prop'><td valign='top' style='text-align:left;' width='20%'><label for='email'>Email:</label></td><td valign='top' style='text-align:left;' width='80%' class='${hasErrors(bean:user,field:'email','errors')}'><input type='text' name='email' value='${user?.email}' /></td></tr>
						  
               </table>
			   <p>Agree to <g:link controller="page" action="terms" target="_blank">Terms & Conditions</g:link>: <g:checkBox name="terms" value="${flash.terms}" /></p>
               </div>
               <div class="buttons">
                     <span class="formButton">
                        <input type="submit" value="Register"></input>
                     </span>
               </div>
            </g:form>
        </div>
    </body>
</body>
            
