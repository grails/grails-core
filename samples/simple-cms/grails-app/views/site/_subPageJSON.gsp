{
	nodes: [
<g:set var="count" expr="${0}" ></g:set>
<g:each in="${pages?}">
	<g:set var="count" expr="${count+1}" ></g:set>
	{ id: "${it.ident()}", label: "${it}"} 
	${(count != pages.size()? "," : "")}
</g:each>
 ]
}
