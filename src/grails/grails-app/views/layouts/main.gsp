<html>
	<head>
		<title><g:layoutTitle default="Grails" /></title>
		<link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
		<g:layoutHead />		
	</head>
	<body onload="<g:pageProperty name='body.onload'/>">
        <div class="logo"><img src="${createLinkTo(dir:'images',file:'grails_logo.jpg')}" alt="Grails" /></div>	
		<g:layoutBody />		
	</body>	
</html>