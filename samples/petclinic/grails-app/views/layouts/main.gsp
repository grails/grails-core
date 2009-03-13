<html>
    <head>
        <title><g:layoutTitle default="PetClinic :: a Grails Framework demonstration" /></title>
  	    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
		<link rel="stylesheet" href="${createLinkTo(dir:'css', file:'petclinic.css')}" type="text/css"/>
    </head>
    <body>
	   <div id="main">
         <g:layoutBody />		

		 <table class="footer">
		    <tr>
		      <td><g:link controller="clinic">Home</g:link></td>
		      <td align="right"><img src="${createLinkTo(dir:'images', file:'springsource-logo.png')}"/></td>
		    </tr>
		  </table>
	   </div>
    </body>	
</html>