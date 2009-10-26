<% import grails.persistence.Event %>
<%=packageName%>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8" />
        <meta name="layout" content="main" />
        <g:set var="entityName" value="\${message(code: '${domainClass.propertyName}.label', default: '${className}')}" />
        <title><g:message code="default.show.label" args="[entityName]" /></title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a class="home" href="\${resource(dir: '')}">Home</a></span>
            <span class="menuButton"><g:link class="list" action="list"><g:message code="default.list.label" args="[entityName]" /></g:link></span>
            <span class="menuButton"><g:link class="create" action="create"><g:message code="default.new.label" args="[entityName]" /></g:link></span>
        </div>
        <div class="body">
            <h1><g:message code="default.show.label" args="[entityName]" /></h1>
            <g:if test="\${flash.message}">
            <div class="message">\${flash.message}</div>
            </g:if>
            <div class="dialog">
                <table>
                    <tbody>
                    <%  excludedProps = Event.allEvents.toList() << 'version'
						      //Three types of things we are tracking - properties, short property names, and natural property names - all need to combine normal properties and embedded
                        props = domainClass.properties.findAll { !excludedProps.contains(it.name) && (!it.isEmbedded())}
                        propNames = props.name
                        propNaturalNames = props.naturalName

                        allEmbeddedTopLevelProps = domainClass.properties.findAll { it.isEmbedded() }

						embeddedProps = allEmbeddedTopLevelProps.component*.properties.flatten().findAll { !(excludedProps<<'id').contains(it.name) }
                        embeddedPropNames = []
                        embeddedNaturalPropNames = []
                        allEmbeddedTopLevelProps.each { topLevelProp ->
                          topLevelProp.component.properties.each {
                             if (!(excludedProps<<'id').contains(it.name)) {
                                embeddedPropNames << "${topLevelProp.name}.${it.name}"
                                embeddedNaturalPropNames << "${topLevelProp.naturalName} ${it.naturalName}"
                             }
                          }
                        }

  						props.addAll(embeddedProps)
  					    propNames.addAll(embeddedPropNames)
						propNaturalNames.addAll(embeddedNaturalPropNames)

                        Collections.sort(props, comparator.constructors[0].newInstance([domainClass] as Object[]))
                        props.eachWithIndex { p, i -> %>
                        <tr class="prop">
                            <td valign="top" class="name"><g:message code="${domainClass.propertyName}.${propNames[i]}.label" default="${propNaturalNames[i]}" /></td>
                            <%  if (p.isEnum()) { %>
                            <td valign="top" class="value">\${${propertyName}?.${propNames[i]}?.encodeAsHTML()}</td>
                            <%  } else if (p.oneToMany || p.manyToMany) { %>
                            <td valign="top" style="text-align: left;" class="value">
                                <ul>
                                <g:each in="\${${propertyName}.${propNames[i]}}" var="${propNames[i][0]}">
                                    <li><g:link controller="${p.referencedDomainClass?.propertyName}" action="show" id="\${${propNames[i][0]}.id}">\${${propNames[i][0]}?.encodeAsHTML()}</g:link></li>
                                </g:each>
                                </ul>
                            </td>
                            <%  } else if (p.manyToOne || p.oneToOne) { %>
                            <td valign="top" class="value"><g:link controller="${p.referencedDomainClass?.propertyName}" action="show" id="\${${propertyName}?.${propNames[i]}?.id}">\${${propertyName}?.${propNames[i]}?.encodeAsHTML()}</g:link></td>
                            <%  } else if (p.type == Boolean.class || p.type == boolean.class) { %>
                            <td valign="top" class="value"><g:formatBoolean boolean="\${${propertyName}?.${propNames[i]}}" /></td>
                            <%  } else if (p.type == Date.class || p.type == java.sql.Date.class || p.type == java.sql.Time.class || p.type == Calendar.class) { %>
                            <td valign="top" class="value"><g:formatDate date="\${${propertyName}?.${propNames[i]}}" /></td>
                            <%  } else { %>
                            <td valign="top" class="value">\${fieldValue(bean: ${propertyName}, field: "${propNames[i]}")}</td>
                            <%  } %> 


                        </tr>
                    <%  } 
%>
                    </tbody>
                </table>
            </div>
            <div class="buttons">
                <g:form>
                    <g:hiddenField name="id" value="\${${propertyName}?.id}" />
                    <span class="button"><g:actionSubmit class="edit" action="edit" value="\${message(code: 'default.button.edit.label', default: 'Edit')}" /></span>
                    <span class="button"><g:actionSubmit class="delete" action="delete" value="\${message(code: 'default.button.delete.label', default: 'Delete')}" onclick="return confirm('\${message(code: 'default.button.delete.confirm.message', default: 'Are you sure?')}');" /></span>
                </g:form>
            </div>
        </div>
    </body>
</html>
