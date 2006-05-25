<html>
	<head>
		<title><g:layoutTitle default="G-CMS" /></title>
		<link rel="stylesheet" href="${createLinkTo(dir:'css',file:'main.css')}"></link>
		<style type="text/css">
			.logo {
				font-weight:bold;
				font-size:2.0em;
				margin:10px;
			}
		</style>
		<g:layoutHead />		
	</head>
	<body oncontextmenu="${pageProperty(name:'body.oncontextmenu')}" onload="${pageProperty(name:'body.onload')}">
		<div class="logo">Content Manager<%-- <img src="${createLinkTo(dir:'images',file:'grails_logo.jpg')}" alt="Grails" /> --%></div>
		<g:render template="/menu" model="[:]" />
		<g:layoutBody />		
	</body>	
</html>