<title>${page?.title}</title>
<g:if test="${page}">
	<meta name="layout" content="${page?.site?.domain}" />
	<meta name="page.id" content="${page?.id}" />
	<meta name="page.title" content="${page?.title}" />	
</g:if>
<g:else>
	<meta name="layout" content="${session.site?.domain}" />
</g:else>

<g:if test="${levels}">
	<g:if test="${levels.level1}">
		<meta name="level.1" content="${levels.level1.id}" />
	</g:if>
	<g:if test="${levels.level2}">
		<meta name="level.2" content="${levels.level2.id}" />
	</g:if>
	<g:if test="${levels.level3}">
		<meta name="level.3" content="${levels.level3.id}" />
	</g:if>
	<g:if test="${levels.level4}">
		<meta name="level.4" content="${levels.level4.id}" />
	</g:if>
</g:if>	
