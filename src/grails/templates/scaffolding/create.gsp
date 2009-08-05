<% import grails.persistence.Event %>
<%=packageName%>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
        <meta name="layout" content="main" />
        <g:set var="entityName" value="\${message(code:'${domainClass.propertyName}.label', default:'${className}')}" />
        <title>Create \${entityName}</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="\${resource(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link class="list" action="list">\${entityName} List</g:link></span>
        </div>
        <div class="body">
            <h1>Create \${entityName}</h1>
            <g:if test="\${flash.message}">
            <div class="message">\${flash.message}</div>
            </g:if>
            <g:hasErrors bean="\${${propertyName}}">
            <div class="errors">
                <g:renderErrors bean="\${${propertyName}}" as="list" />
            </div>
            </g:hasErrors>
            <g:form action="save" method="post" <%= multiPart ? ' enctype="multipart/form-data"' : '' %>>
                <div class="dialog">
                    <table>
                        <tbody>
                        <%

                            excludedProps = Event.allEvents.toList() << 'version' << 'id'
                            props = domainClass.properties.findAll { !excludedProps.contains(it.name) }

                            Collections.sort(props, comparator.constructors[0].newInstance([domainClass] as Object[]))
                            props.each { p ->
                                if(!Collection.class.isAssignableFrom(p.type)) {
                                    cp = domainClass.constrainedProperties[p.name]
                                    display = (cp ? cp.display : true)        
                                    if(display) { %>
                            <tr class="prop">
                                <td valign="top" class="name">
                                    <label for="${p.name}">
                                      <g:message code="${domainClass.propertyName}.${p.name}.label" default="${p.naturalName}" />
                                    </label>
                                </td>
                                <td valign="top" class="value \${hasErrors(bean:${propertyName},field:'${p.name}','errors')}">
                                    ${renderEditor(p)}
                                </td>
                            </tr> 
                        <%  }   }   } %>
                        </tbody>
                    </table>
                </div>
                <div class="buttons">
                    <span class="button"><input class="save" type="submit" value="Create" /></span>
                </div>
            </g:form>
        </div>
    </body>
</html>
