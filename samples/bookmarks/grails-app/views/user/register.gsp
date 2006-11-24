<html>
      <head>
           <title>Registration Page</title>
           <meta name="layout" content="main" />
      </head>    
	  <style type="text/css" media="screen">
			form {
				width: 300px;
			}
	  		input {
				position: absolute;
				left: 200px;
		    }          
			p {
				margin-left: 30px;
			} 
			.button {
				margin-top: 30px;
			}
	  </style>
	  
      <body>
             <g:if test="${user}">
				 <div class="errors"> 
	                    <g:renderErrors bean="${flash.user}" />
	             </div>
   			 </g:if>
             <p>Enter your details below to register for Your Bookmarks. If you have a del.icio.us account register with the same login details for del.icio.us integration!</p>        
             <form action="handleRegistration">
                     <p>
                         <label class="label" for="login">Login:</label>
                         <input type="text" name="login" />
                     </p>
                     <p>
                         <label for="password">Password:</label>
                         <input type="password" name="password" />
                     </p> 
                     <p>
                         <label for="confirm">Confirm Password:</label>
                         <input type="password" name="confirm" />
                     </p>
                     <p>
                         <label for="firstName">First Name:</label>
                         <input type="text" name="firstName" />
                     </p>
                     <p>
                         <label for="lastName">Last Name:</label>
                         <input type="text" name="lastName" />
                     </p>
                     <p>
                         <label for="email">Email:</label>
                         <input type="text" name="email" />
                     </p>
                    <input class="button" type="submit" value="Register" />
             </form>
      </body>
</html>
