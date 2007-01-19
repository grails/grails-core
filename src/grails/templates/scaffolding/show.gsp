<%=packageName%>  
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
          <meta name="layout" content="main" />
         <title>Show ${className}</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a href="\${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link action="list">${className} List</g:link></span>
            <span class="menuButton"><g:link action="create">New ${className}</g:link></span>
        </div>
        <div class="body">
           <h1>Show ${className}</h1>
           <g:if test="\${flash.message}">
                 <div class="message">\${flash.message}</div>
           </g:if>
           <div class="dialog">
                 <table>
                   <%
                        props = domainClass.properties.findAll { it.name != 'version' }
                        Collections.sort(props, comparator.constructors[0].newInstance([domainClass] as Object[]))
                   %>
                   <tbody>
                   <%props.each { p ->%>
                        <tr class="prop">
                              <td valign="top" class="name">${p.naturalName}:</td>
                              <% if(p.oneToMany) { %>
                                     <td  valign="top" style="text-align:left;" class="value">
                                        <ul>
                                            <g:each var="${p.name[0]}" in="\${${propertyName}.${p.name}}">
                                                <li><g:link controller="${p.referencedDomainClass?.propertyName}" action="show" id="\${${p.name[0]}.id}">\${${p.name[0]}}</g:link></li>
                                            </g:each>
                                        </ul>
                                     </td>
                              <% } else if(p.manyToOne || p.oneToOne) { %>
                                    <td valign="top" class="value"><g:link controller="${p.referencedDomainClass?.propertyName}" action="show" id="\${${propertyName}?.${p.name}?.id}">\${${propertyName}?.${p.name}}</g:link></td>
                              <% } else  { %>
                                    <td valign="top" class="value">\${${propertyName}.${p.name}}</td>
                              <% } %>
                        </tr>
                   <%}%>
                   </tbody>
                 </table>
           </div>
           <div class="buttons">
               <g:form controller="${propertyName}">
                 <input type="hidden" name="id" value="\${${propertyName}?.id}" />
                 <span class="button"><g:actionSubmit value="Edit" /></span>
                 <span class="button"><g:actionSubmit value="Delete" /></span>
               </g:form>
           </div>
        </div>
    </body>
</html>
