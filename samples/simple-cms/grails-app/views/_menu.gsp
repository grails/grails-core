<div class="nav">
	<g:if test="${session.user}">
		<span class="menuButton"><g:link controller="site" action="list">Sites</g:link></span>
		<g:isSysAdmin>
			<span class="menuButton"><g:link controller="accessKey" action="list">Keys</g:link></span>	
			<span class="menuButton"><g:link controller="user" action="list">Users</g:link></span>
			<span class="menuButton"><g:link controller="role" action="list">Roles</g:link></span>
		</g:isSysAdmin>		
		<span class="menuButton"><g:link controller="forum" action="list">Forums</g:link></span>
		<span class="menuButton"><g:link controller="test" action="list">Questionnaires</g:link></span>
	</g:if>
</div>		
