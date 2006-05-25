<html>
	<head>
		<title>${page?.title}</title>
		<meta name="layout" content="${site?.domain}" />
		<meta name="page.id" content="${page?.id}" />
		<meta name="page.title" content="${page?.title}" />
		<g:if test="${levels.level1}">
			<meta name="level.1" content="${levels.level1.title}" />
		</g:if>
		<g:if test="${levels.level2}">
			<meta name="level.2" content="${levels.level2.title}" />
		</g:if>
		<g:if test="${levels.level3}">
			<meta name="level.3" content="${levels.level3.title}" />
		</g:if>
		<g:if test="${levels.level4}">
			<meta name="level.4" content="${levels.level4.title}" />
		</g:if>
		<g:javascript>
			function populateBody() {
				var editorAPI = window.opener.FCKeditorAPI.GetInstance('editor')
				document.getElementById('body').innerHTML = editorAPI.GetXHTML(true);
			}
		</g:javascript>
	</head>
	<body onload="populateBody()">		
	</body>
</html>	
