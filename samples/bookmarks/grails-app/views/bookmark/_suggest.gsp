<g:if test="${tags}"><h3>Suggestions:</h3></g:if>
<g:each var="t" in="${tags?}">
	<span id="${t.id}" class="suggestion">
		<a href="javascript:void(0);"
		   onclick="$('tags${bookmark?.id ? bookmark.id : ''}').value+=' '+this.innerHTML;this.style.display='none';">${t}</a> 
	</span>
</g:each>