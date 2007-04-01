<%=packageName%>  
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
         <meta name="layout" content="main" />
         <title>${className} List</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a href="\${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link action="create">New ${className}</g:link></span>
        </div>
        <div class="body">
           <h1>${className} List</h1>
            <g:if test="\${flash.message}">
                 <div class="message">
                       \${flash.message}
                 </div>
            </g:if>
           <table>
             <thead>
               <tr>
               <% props = domainClass.properties.findAll { it.name != 'version' && it.type != Set.class }
                  Collections.sort(props, comparator.constructors[0].newInstance([domainClass] as Object[]))
                  props.eachWithIndex { p,i ->
                   	if(i < 6) {
                   	  if(p.isAssociation()) {%>
                   	    <th>${p.naturalName}</th>
                   	<%} else {%>
                   	    <g:sortableColumn property="${p.name}" title="${p.naturalName}" />
                  <%}}}%>
                        <th></th>
               </tr>
             </thead>
             <tbody>
               <g:each in="\${${propertyName}List}">
                    <tr>
                       <%props.eachWithIndex { p,i ->
                             if(i < 6) {%>
                            <td>\${it.${p.name}?.encodeAsHTML()}</td>
                       <%}}%>
                       <td class="actionButtons">
                            <span class="actionButton"><g:link action="show" id="\${it.id}">Show</g:link></span>
                       </td>
                    </tr>
               </g:each>
             </tbody>
           </table>
               <div class="paginateButtons">
                   <g:paginate total="\${${className}.count()}" />
               </div>
        </div>
    </body>
</html>
