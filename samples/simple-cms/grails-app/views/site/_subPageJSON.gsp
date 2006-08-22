{
	nodes: [
<g:set var="count" value="${0}" ></g:set>
<g:each in="${pages?}">
	<g:set var="count" value="${count+1}" ></g:set>
	{ id: "${it.ident()}", label: "${it}"} 
	${(count != pages.size()? "," : "")}
</g:each>
 ]
}
