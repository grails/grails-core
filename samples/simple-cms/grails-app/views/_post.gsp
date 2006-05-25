<g:def var="p" expr="${it}" />
<div class="post">
	<div><span class="title">${p.title}</span> - Created ${p.datePosted}</div>
	<div class="msg">${p.message}</div>
	<div class="leftby">Left by - <g:link controller="user" action="contact" id="${p.by.id}">${p.by}</g:link></div>
	
		<div class="controls">
			<g:if test="${p.by?.id == session.user?.id}">
				<g:link controller="message" action="edit" id="${p.id}">Edit</g:link> 
			</g:if>
			<g:isContentEditor>
				<g:if test="${p.by?.id != session.user?.id}">
					<g:link controller="message" action="edit" id="${p.id}">Edit</g:link> -
				</g:if>
				<g:else> - </g:else>
				<g:link controller="message" action="delete" id="${p.id}">Delete</g:link>
			</g:isContentEditor>
		</div>
	
</div>

