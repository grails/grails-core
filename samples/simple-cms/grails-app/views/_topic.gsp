<g:def var="t" expr="${it}" />
<div class="topic">
	<div class="title"><g:link controller="topic" action="show" params="['forum.id':t.forum.id]" id="${t.id}">${t.title}</g:link> - Created by <g:link controller="user" action="profile" id="${t.createdBy.id}">${t.createdBy}</g:link> on ${t.dateAdded}</div>
	<div class="desc">${t.description}</div>
</div>

