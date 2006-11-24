<html>
      <head>
           <title>Login Page</title>
           <meta name="layout" content="main" />
		  <style type="text/css" media="screen">
				form { width: 300px; }
		  		input {
					position: absolute;
					left: 130px;
			    }          
				p {	margin-left: 30px; } 
				.button { margin-top: 30px;	}
		  </style>
      </head>
      <body>
             <g:if test="${flash.message}">
				<div class="message">${flash.message}</div>
			 </g:if>
             <p>
                 Welcome to Your Bookmarks. Login below or 
                  <g:link action="register">register here</g:link>.
             </p>        
             <form action="${request.contextPath}/j_acegi_security_check">
                     <p>
                         <label for="login">Login:</label>
                         <input type="text" name="j_username" />
                     </p>
                     <p>
                         <label for="password">Password:</label>
                         <input type="password" name="j_password" />
                     </p>
                    <input class="button" type="submit" value="Login" />
             </form>
      </body>
</html>
