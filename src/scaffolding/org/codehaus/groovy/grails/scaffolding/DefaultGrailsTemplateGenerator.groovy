/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.codehaus.groovy.grails.scaffolding;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.scaffolding.GrailsTemplateGenerator;
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU;
/**
 * Default implementation of the generator that generates grails artifacts (controllers, views etc.)
 * from the domain model
 *
 * @author Graeme Rocher
 * @since 09-Feb-2006
 */
class DefaultGrailsTemplateGenerator implements GrailsTemplateGenerator  {

    static final Log LOG = LogFactory.getLog(DefaultGrailsTemplateGenerator.class);

    String basedir
    boolean overwrite = false
    def engine = new groovy.text.SimpleTemplateEngine()

    // a closure that uses the type to render the appropriate editor
    def renderEditor = { property ->
        def domainClass = property.domainClass
        def cp = domainClass.constrainedProperties[property.name]
        
        def display = (cp ? cp.display : true)        
        if(!display) return ''
        
	def buf = new StringBuffer("<tr class='prop'>")
	buf << "<td valign='top' class='name'><label for='${property.name}'>${property.naturalName}:</label></td>"
	buf << "<td valign='top' class='value \${hasErrors(bean:${domainClass.propertyName},field:'${property.name}','errors')}'>"
		    if(property.type == Boolean.class || property.type == boolean.class)
		        buf << renderBooleanEditor(domainClass,property)	
            else if(Number.class.isAssignableFrom(property.type) || (property.type.isPrimitive() && property.type != boolean.class))
                buf << renderNumberEditor(domainClass,property)
            else if(property.type == String.class)
                buf << renderStringEditor(domainClass,property)
            else if(property.type == Date.class || property.type == java.sql.Date.class || property.type == java.sql.Time.class)
                buf << renderDateEditor(domainClass,property)
            else if(property.type == Calendar.class)
                buf << renderDateEditor(domainClass,property)  
            else if(property.type == URL.class) 
           		buf << renderStringEditor(domainClass,property)
            else if(property.type == TimeZone.class)
                buf << renderSelectTypeEditor("timeZone",domainClass,property)
            else if(property.type == Locale.class)
                buf << renderSelectTypeEditor("locale",domainClass,property)
            else if(property.type == Currency.class)
                buf << renderSelectTypeEditor("currency",domainClass,property)
            else if(property.type==([] as Byte[]).class) //TODO: Bug in groovy means i have to do this :(
                buf << renderByteArrayEditor(domainClass,property)
            else if(property.type==([] as byte[]).class) //TODO: Bug in groovy means i have to do this :(
                buf << renderByteArrayEditor(domainClass,property)                
            else if(property.manyToOne || property.oneToOne)
                buf << renderManyToOne(domainClass,property)
            else if(property.oneToMany || property.manyToMany)
                buf << renderOneToMany(domainClass,property)
                
       buf << '</td></tr>'
       return buf.toString()
    }

    public void generateViews(GrailsDomainClass domainClass, String destdir) {
        if(!destdir)
            throw new IllegalArgumentException("Argument [destdir] not specified")

        def viewsDir = new File("${destdir}/grails-app/views/${domainClass.propertyName}")
        if(!viewsDir.exists())
            viewsDir.mkdirs()

        LOG.info("Generating list view for domain class [${domainClass.fullName}]")
        generateListView(domainClass,viewsDir)
        LOG.info("Generating show view for domain class [${domainClass.fullName}]")
        generateShowView(domainClass,viewsDir)
        LOG.info("Generating edit view for domain class [${domainClass.fullName}]")
        generateEditView(domainClass,viewsDir)
        LOG.info("Generating create view for domain class [${domainClass.fullName}]")
        generateCreateView(domainClass,viewsDir)
    }

    public void generateController(GrailsDomainClass domainClass, String destdir) {
        if(!destdir)
            throw new IllegalArgumentException("Argument [destdir] not specified")

        if(domainClass) {
            def destFile = new File("${destdir}/grails-app/controllers/${domainClass.shortName}Controller.groovy")
            if(destFile.exists()) {
                LOG.info("Controller ${destFile.name} already exists skipping")
                return
            }
            destFile.parentFile.mkdirs()

            def templateText = '''
<%=packageName ? "import ${packageName}.${className}" : ''%>            
class ${className}Controller {
    def index = { redirect(action:list,params:params) }

    def list = {
        if(!params.max)params.max = 10
        [ ${propertyName}List: ${className}.list( params ) ]
    }

    def show = {
        [ ${propertyName} : ${className}.get( params.id ) ]
    }

    def delete = {
        def ${propertyName} = ${className}.get( params.id )
        if(${propertyName}) {
            ${propertyName}.delete()
            flash.message = "${className} \\${params.id} deleted."
            redirect(action:list)
        }
        else {
            flash.message = "${className} not found with id \\${params.id}"
            redirect(action:list)
        }
    }

    def edit = {
        def ${propertyName} = ${className}.get( params.id )

        if(!${propertyName}) {
                flash.message = "${className} not found with id \\${params.id}"
                redirect(action:list)
        }
        else {
            return [ ${propertyName} : ${propertyName} ]
        }
    }

    def update = {
        def ${propertyName} = ${className}.get( params.id )
        if(${propertyName}) {
             ${propertyName}.properties = params
            if(${propertyName}.save()) {
                redirect(action:show,id:${propertyName}.id)
            }
            else {
                render(view:'edit',model:[${propertyName}:${propertyName}])
            }
        }
        else {
            flash.message = "${className} not found with id \\${params.id}"
            redirect(action:edit,id:params.id)
        }
    }

    def create = {
        def ${propertyName} = new ${className}()
        ${propertyName}.properties = params
        return ['${propertyName}':${propertyName}]
    }

    def save = {
        def ${propertyName} = new ${className}()
        ${propertyName}.properties = params
        if(${propertyName}.save()) {
            redirect(action:show,id:${propertyName}.id)
        }
        else {
            render(view:'create',model:[${propertyName}:${propertyName}])
        }
    }

}'''
		
            def binding = [ packageName:domainClass.packageName,className: domainClass.shortName, propertyName:domainClass.propertyName ]
            def t = engine.createTemplate(templateText)

            destFile.withWriter { w ->
                t.make(binding).writeTo(w)
            }

            LOG.info("Controller generated at ${destFile}")
        }
    }

    private renderStringEditor(domainClass, property) {
        def cp = domainClass.constrainedProperties[property.name]
        if(!cp) {
            return "<input type='text' name='${property.name}' value='\${${domainClass.propertyName}?.${property.name}}' />"
        }
        else {
			if("textarea" == cp.widget || ((cp.maxLength > 250 || cp.length?.to > 250) && cp.maxLength != Integer.MAX_VALUE && !cp.password && !cp.inList)) {
                return "<textarea rows='1' cols='1' name='${property.name}'>\${${domainClass.propertyName}?.${property.name}}</textarea>"
            }
            else {
                if(cp.inList) {
                   def sb = new StringBuffer('<g:select ')
                   sb << "name='${property.name}' from='\${${domainClass.propertyName}.constraints.${property.name}.inList}' value='\${${domainClass.propertyName}.${property.name}}'>"
                   sb << '</g:select>'
                   return sb.toString()
                }
                else {
                    def sb = new StringBuffer('<input ')
                    cp.password ? sb << 'type="password" ' : sb << 'type="text" '
                    if(!cp.editable) sb << 'readonly="readonly" '
                    if(cp.maxLength < Integer.MAX_VALUE ) sb << "maxlength='${cp.maxLength}' "
                    sb << "name='${property.name}' value='\${${domainClass.propertyName}?.${property.name}}'></input>"
                    return sb.toString()
                }
            }
        }
    }

    private renderByteArrayEditor(domainClass,property) {
        return "<input type='file' name='${property.name}'></input>"
    }

    private renderManyToOne(domainClass,property) {
        if(property.association) {            
            return "<g:select optionKey=\"id\" from=\"\${${property.type.name}.list()}\" name='${property.name}.id' value='\${${domainClass.propertyName}?.${property.name}?.id}'></g:select>"
        }
    }

    private renderOneToMany(domainClass,property) {
        def sw = new StringWriter()
        def pw = new PrintWriter(sw)
        pw.println '<ul>'
        pw.println "    <g:each var='${property.name[0]}' in='\${${domainClass.propertyName}?.${property.name}?}'>"
        pw.println "        <li><g:link controller='${property.referencedDomainClass.propertyName}' action='show' id='\${${property.name[0]}.id}'>\${${property.name[0]}}</g:link></li>"
        pw.println "    </g:each>"
        pw.println "</ul>"
        pw.println "<g:link controller='${property.referencedDomainClass.propertyName}' params='[\"${domainClass.propertyName}.id\":${domainClass.propertyName}?.id]' action='create'>Add ${property.referencedDomainClass.shortName}</g:link>"
        return sw.toString()
    }

    private renderNumberEditor(domainClass,property) {
        def cp = domainClass.constrainedProperties[property.name]
        if(!cp) {
            if(property.type == Byte.class) {
                return "<g:select from='\${-128..127}' name='${property.name}' value='\${${domainClass.propertyName}?.${property.name}}'></g:select>"
            }
            else {
                return "<input type='text' name='${property.name}' value='\${${domainClass.propertyName}?.${property.name}}'></input>"
            }
        }
        else {
            if(cp.range) {
                return "<g:select from='\${${cp.range.from}..${cp.range.to}}' name='${property.name}' value='\${${domainClass.propertyName}?.${property.name}}'></g:select>"
            }
            else if(cp.size) {
                return "<g:select from='\${${cp.size.from}..${cp.size.to}}' name='${property.name}' value='\${${domainClass.propertyName}?.${property.name}}'></g:select>"
            }
            else {
                return "<input type='text' name='${property.name}' value='\${${domainClass.propertyName}?.${property.name}}'></input>"
            }
        }
    }

    private renderBooleanEditor(domainClass,property) {

        def cp = domainClass.constrainedProperties[property.name]
        if(!cp) {
            return "<g:checkBox name='${property.name}' value='\${${domainClass.propertyName}?.${property.name}}'></g:checkBox>"
        }
        else {
            def buf = new StringBuffer('<g:checkBox ')
            if(cp.widget) buf << "widget='${cp.widget}'";

            buf << "name='${property.name}' value='\${${domainClass.propertyName}?.${property.name}}' "
            cp.attributes.each { k,v ->
                  buf << "${k}=\"${v}\" "
            }
            buf << '></g:checkBox>'
            return buf.toString()
        }

    }

    private renderDateEditor(domainClass,property) {
        def cp = domainClass.constrainedProperties[property.name]
        if(!cp) {
            return "<g:datePicker name='${property.name}' value='\${${domainClass.propertyName}?.${property.name}}'></g:datePicker>"
        }
        else {
          if(!cp.editable) {
            return "\${${domainClass.propertyName}?.${property.name}?.toString()}"
          }
          else {
            def buf = new StringBuffer('<g:datePicker ')
            if(cp.widget) buf << "widget='${cp.widget}' ";

            if(cp.format) buf << "format='${cp.format}' ";
            cp.attributes.each { k,v ->
                  buf << "${k}=\"${v}\" "
            }
            buf << "name='${property.name}' value='\${${domainClass.propertyName}?.${property.name}}'></g:datePicker>"
            return buf.toString()
          }
        }
    }

    private renderSelectTypeEditor(type,domainClass,property) {
       def cp = domainClass.constrainedProperties[property.name]
        if(!cp) {
            return "<g:${type}Select name='${property.name}' value='\${${domainClass.propertyName}?.${property.name}}'></g:${type}Select>"
        }
        else {
            def buf = new StringBuffer('<g:${type}Select ')
            if(cp.widget) buf << "widget='${cp.widget}' ";
            cp.attributes.each { k,v ->
                  buf << "${k}=\"${v}\" "
            }
            buf << "name='${property.name}' value='\${${domainClass.propertyName}?.${property.name}}'></g:${type}Select>"
            return buf.toString()
        }
    }




    private generateListView(domainClass, destDir) {
        def listFile = new File("${destDir}/list.gsp")
        if(!listFile.exists() || overwrite) {
            def templateText = '''
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
         <meta name="layout" content="main" />
         <title>${className} List</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a href="\\${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link action="create">New ${className}</g:link></span>
        </div>
        <div class="body">
           <h1>${className} List</h1>
            <g:if test="\\${flash.message}">
                 <div class="message">
                       \\${flash.message}
                 </div>
            </g:if>
           <table>
               <tr>
                   <%
                        props = domainClass.properties.findAll { it.name != 'version' && it.type != Set.class }
                   Collections.sort(props, comparator.constructors[0].newInstance([domainClass] as Object[]))
                   %>
                   <%props.eachWithIndex { p,i ->
                   	if(i < 6) {%>                   
                        <th>${p.naturalName}</th>
                   <%}}%>
                   <th></th>
               </tr>
               <g:each in="\\${${propertyName}List}">
                    <tr>
                       <%props.eachWithIndex { p,i ->
                             if(i < 6) {%>
                            <td>\\${it.${p.name}}</td>
                       <%}}%>
                       <td class="actionButtons">
                            <span class="actionButton"><g:link action="show" id="\\${it.id}">Show</g:link></span>
                       </td>
                    </tr>
               </g:each>
           </table>
		   <div class="paginateButtons">
				<g:paginate total="\\${$className.count()}" />
			</div>
        </div>
    </body>
</html>
            '''

            def t = engine.createTemplate(templateText)
            def binding = [ domainClass: domainClass, 
                            className:domainClass.shortName,
                            propertyName:domainClass.propertyName,
                            comparator:org.codehaus.groovy.grails.scaffolding.DomainClassPropertyComparator.class]

            listFile.withWriter { w ->
                t.make(binding).writeTo(w)
            }
            LOG.info("list view generated at ${listFile.absolutePath}")
        }
    }

    private generateShowView(domainClass,destDir) {
        def showFile = new File("${destDir}/show.gsp")
        if(!showFile.exists() || overwrite) {
            def templateText = '''
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
          <meta name="layout" content="main" />
         <title>Show ${className}</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a href="\\${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link action="list">${className} List</g:link></span>
            <span class="menuButton"><g:link action="create">New ${className}</g:link></span>
        </div>
        <div class="body">
           <h1>Show ${className}</h1>
           <g:if test="\\${flash.message}">
                 <div class="message">\\${flash.message}</div>
           </g:if>
           <div class="dialog">
                 <table>
                   <%
                        props = domainClass.properties.findAll { it.name != 'version' }
                        Collections.sort(props, comparator.constructors[0].newInstance([domainClass] as Object[]))
                   %>
                   <%props.each { p ->%>
                        <tr class="prop">
                              <td valign="top" class="name">${p.naturalName}:</td>
                              <% if(p.oneToMany) { %>
                                     <td  valign="top" style="text-align:left;" class="value">
                                        <ul>
                                            <g:each var="${p.name[0]}" in="\\${${propertyName}.${p.name}}">
                                                <li><g:link controller="${p.referencedDomainClass?.propertyName}" action="show" id="\\${${p.name[0]}.id}">\\${${p.name[0]}}</g:link></li>
                                            </g:each>
                                        </ul>
                                     </td>
                              <% } else if(p.manyToOne || p.oneToOne) { %>
                                    <td valign="top" class="value"><g:link controller="${p.referencedDomainClass?.propertyName}" action="show" id="\\${${propertyName}?.${p.name}?.id}">\\${${propertyName}?.${p.name}}</g:link></td>
                              <% } else  { %>
                                    <td valign="top" class="value">\\${${propertyName}.${p.name}}</td>
                              <% } %>
                        </tr>
                   <%}%>
                 </table>
           </div>
           <div class="buttons">
               <g:form controller="${propertyName}">
                 <input type="hidden" name="id" value="\\${${propertyName}?.id}" />
                 <span class="button"><g:actionSubmit value="Edit" /></span>
                 <span class="button"><g:actionSubmit value="Delete" /></span>
               </g:form>
           </div>
        </div>
    </body>
</html>
            '''

            def t = engine.createTemplate(templateText)
            def binding = [ domainClass: domainClass, 
                            className:domainClass.shortName,
                            propertyName:domainClass.propertyName, 
                            comparator:org.codehaus.groovy.grails.scaffolding.DomainClassPropertyComparator.class ]

            showFile.withWriter { w ->
                t.make(binding).writeTo(w)
            }
            LOG.info("Show view generated at ${showFile.absolutePath}")
        }
    }

    private generateEditView(domainClass,destDir) {
        def editFile = new File("${destDir}/edit.gsp")
        if(!editFile.exists() || overwrite) {
            def templateText = '''
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
         <meta name="layout" content="main" />
         <title>Edit ${className}</title>
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a href="\\${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link action="list">${className} List</g:link></span>
            <span class="menuButton"><g:link action="create">New ${className}</g:link></span>
        </div>
        <div class="body">
           <h1>Edit ${className}</h1>
           <g:if test="\\${flash.message}">
                 <div class="message">\\${flash.message}</div>
           </g:if>
           <g:hasErrors bean="\\${${propertyName}}">
                <div class="errors">
                    <g:renderErrors bean="\\${${propertyName}}" as="list" />
                </div>
           </g:hasErrors>
           <div class="prop">
	      <span class="name">Id:</span>
	      <span class="value">\\${${propertyName}?.id}</span>
	      <input type="hidden" name="${propertyName}.id" value="\\${${propertyName}?.id}" />
           </div>           
           <g:form controller="${propertyName}" method="post" <%= multiPart ? ' enctype="multipart/form-data"' : '' %>>
               <input type="hidden" name="id" value="\\${${propertyName}?.id}" />
               <div class="dialog">
                <table>

                       <%
                            props = domainClass.properties.findAll { it.name != 'version' && it.name != 'id' }
                       Collections.sort(props, comparator.constructors[0].newInstance([domainClass] as Object[]))
                       %>
                       <%props.each { p ->%>
				${renderEditor(p)}
                       <%}%>
                </table>
               </div>

               <div class="buttons">
                     <span class="button"><g:actionSubmit value="Update" /></span>
                     <span class="button"><g:actionSubmit value="Delete" /></span>
               </div>
            </g:form>
        </div>
    </body>
</html>
            '''

            def t = engine.createTemplate(templateText)
            def multiPart = domainClass.properties.find{it.type==([] as Byte[]).class || it.type==([] as byte[]).class}
            def binding = [ domainClass: domainClass,
                            multiPart:multiPart,
                            className:domainClass.shortName,
                            propertyName:domainClass.propertyName,
                            renderEditor:renderEditor,
                            comparator:org.codehaus.groovy.grails.scaffolding.DomainClassPropertyComparator.class]

            editFile.withWriter { w ->
                t.make(binding).writeTo(w)
            }
            LOG.info("Edit view generated at ${editFile.absolutePath}")
        }
    }

    private generateCreateView(domainClass,destDir) {
        def createFile = new File("${destDir}/create.gsp")
        if(!createFile.exists() || overwrite) {
            def templateText = '''
<html>
    <head>
         <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
         <meta name="layout" content="main" />
         <title>Create ${className}</title>         
    </head>
    <body>
        <div class="nav">
            <span class="menuButton"><a href="\\${createLinkTo(dir:'')}">Home</a></span>
            <span class="menuButton"><g:link action="list">${className} List</g:link></span>
        </div>
        <div class="body">
           <h1>Create ${className}</h1>
           <g:if test="\\${flash.message}">
                 <div class="message">\\${flash.message}</div>
           </g:if>
           <g:hasErrors bean="\\${${propertyName}}">
                <div class="errors">
                    <g:renderErrors bean="\\${${propertyName}}" as="list" />
                </div>
           </g:hasErrors>
           <g:form action="save" method="post" <%= multiPart ? ' enctype="multipart/form-data"' : '' %>>
               <div class="dialog">
                <table>

                       <%
                            props = domainClass.properties.findAll { it.name != 'version' && it.name != 'id' }
                       Collections.sort(props, comparator.constructors[0].newInstance([domainClass] as Object[]))
                       %>
                       <%props.each { p ->
                            if(p.type != Set.class) { %>
                                  ${renderEditor(p)}
                       <%}}%>
               </table>
               </div>
               <div class="buttons">
                     <span class="formButton">
                        <input type="submit" value="Create"></input>
                     </span>
               </div>
            </g:form>
        </div>
    </body>
</html>
            '''

            def t = engine.createTemplate(templateText)
            def multiPart = domainClass.properties.find{it.type==([] as Byte[]).class || it.type==([] as byte[]).class}
            
            def binding = [ domainClass: domainClass,
                            multiPart:multiPart,
                            className:domainClass.shortName,
                            propertyName:domainClass.propertyName,
                            renderEditor:renderEditor,
                            comparator:org.codehaus.groovy.grails.scaffolding.DomainClassPropertyComparator.class]

            createFile.withWriter { w ->
                t.make(binding).writeTo(w)
            }
            LOG.info("Create view generated at ${createFile.absolutePath}")
        }
    }
}