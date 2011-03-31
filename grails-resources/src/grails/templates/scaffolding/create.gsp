<% import grails.persistence.Event %>
<% import org.codehaus.groovy.grails.plugins.PluginManagerHolder %>
<%=packageName%>
<!doctype html>
<html>
    <head>
        <meta name="layout" content="main">
        <g:set var="entityName" value="\${message(code: '${domainClass.propertyName}.label', default: '${className}')}" />
        <title><g:message code="default.create.label" args="[entityName]" /></title>
    </head>
    <body>
		<a href="#create-${domainClass.propertyName}" class="skip" tabindex="-1"><g:message code="default.link.skip.label" default="Skip to content&hellip;"/></a>
        <div class="nav" role="navigation">
			<ul>
				<li><a class="home" href="\${createLink(uri: '/')}"><g:message code="default.home.label"/></a></li>
				<li><g:link class="list" action="list"><g:message code="default.list.label" args="[entityName]" /></g:link></li>
			</ul>
        </div>
        <div id="create-${domainClass.propertyName}" class="content scaffold-create" role="main">
            <h1><g:message code="default.create.label" args="[entityName]" /></h1>
            <g:if test="\${flash.message}">
            <div class="message" role="status"><p>\${flash.message}</p></div>
            </g:if>
            <g:hasErrors bean="\${${propertyName}}">
            <div class="errors" role="alert">
                <g:renderErrors bean="\${${propertyName}}" as="list" />
            </div>
            </g:hasErrors>
            <g:form action="save" <%= multiPart ? ' enctype="multipart/form-data"' : '' %>>
				<fieldset class="form">
				<%  excludedProps = Event.allEvents.toList() << 'version' << 'id' << 'dateCreated' << 'lastUpdated'
					persistentPropNames = domainClass.persistentProperties*.name
					props = domainClass.properties.findAll { persistentPropNames.contains(it.name) && !excludedProps.contains(it.name) }
					Collections.sort(props, comparator.constructors[0].newInstance([domainClass] as Object[]))
					display = true
					required = false
					boolean hasHibernate = PluginManagerHolder.pluginManager.hasGrailsPlugin('hibernate')
					props.each { p ->
						if (hasHibernate) {
							cp = domainClass.constrainedProperties[p.name]
							display = (cp ? cp.display : true)
							required = (cp ? !(cp.propertyType in [boolean, Boolean]) && !cp.nullable && (cp.propertyType != String || !cp.blank) : false)
						}
						if (display) { %>
					<div class="fieldcontain \${hasErrors(bean: ${propertyName}, field: '${p.name}', 'error')} ${required ? 'required' : ''}">
						<label for="${p.name}"><g:message code="${domainClass.propertyName}.${p.name}.label" default="${p.naturalName}" /><% if (required) { %><span class="required-indicator">*</span><% } %></label>
						${renderEditor(p)}
					</div>
				<%  }   } %>
				</fieldset>
                <fieldset class="buttons">
                    <g:submitButton name="create" class="save" value="\${message(code: 'default.button.create.label', default: 'Create')}" />
                </fieldset>
            </g:form>
        </div>
    </body>
</html>
