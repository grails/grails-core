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

    String basedir = "."
    boolean overwrite = false
    def engine = new groovy.text.SimpleTemplateEngine()
    def ant = new AntBuilder()

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
				ant.input(message: "Controller ${destFile.name} already exists. Overwrite?","y,n", addproperty:"overwrite.controller")
                def overwrite = (ant.antProject.properties."overwrite.controller" == "y") ? true : false        
                if(!overwrite)return
            }
            destFile.parentFile.mkdirs()

            def templateText = getTemplateText("Controller.groovy")
		
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
            return "<input type='text' name='${property.name}' value=\"\${${domainClass.propertyName}?.${property.name}.toString().encodeHTML()}\" />"
        }
        else {
			if("textarea" == cp.widget || (cp.maxSize > 250 && !cp.password && !cp.inList)) {
                return "<textarea rows='5' cols='40' name='${property.name}'>\${${domainClass.propertyName}?.${property.name}.toString().encodeHTML()}</textarea>"
            }
            else {
                if(cp.inList) {
                   def sb = new StringBuffer('<g:select ')
                   sb << "name='${property.name}' from='\${${domainClass.propertyName}.constraints.${property.name}.inList}' value=\"\${${domainClass.propertyName}.${property.name}.toString().encodeHTML()}\">"
                   sb << '</g:select>'
                   return sb.toString()
                }
                else {
                    def sb = new StringBuffer('<input ')
                    cp.password ? sb << 'type="password" ' : sb << 'type="text" '
                    if(!cp.editable) sb << 'readonly="readonly" '
                    if(cp.maxSize) sb << "maxlength='${cp.maxSize}' "
                    sb << "name='${property.name}' value=\"\${${domainClass.propertyName}?.${property.name}.toString().encodeHTML()}\"></input>"
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
            return "<g:select optionKey=\"id\" from=\"\${${property.type.name}.list()}\" name='${property.name}.id' value=\"\${${domainClass.propertyName}?.${property.name}?.id}\"></g:select>"
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
                return "<g:select from='\${-128..127}' name='${property.name}' value=\"\${${domainClass.propertyName}?.${property.name}.toString().encodeHTML()}\"></g:select>"
            }
            else {
                return "<input type='text' name='${property.name}' value=\"\${${domainClass.propertyName}?.${property.name}.toString().encodeHTML()}\"></input>"
            }
        }
        else {
            if(cp.range) {
                return "<g:select from='\${${cp.range.from}..${cp.range.to}}' name='${property.name}' value=\"\${${domainClass.propertyName}?.${property.name}.toString().encodeHTML()}\"></g:select>"
            }
            else if(cp.size) {
                return "<g:select from='\${${cp.size.from}..${cp.size.to}}' name='${property.name}' value=\"\${${domainClass.propertyName}?.${property.name}.toString().encodeHTML()}\"></g:select>"
            }
            else {
                return "<input type='text' name='${property.name}' value=\"\${${domainClass.propertyName}?.${property.name}.toString().encodeHTML()}\"></input>"
            }
        }
    }

    private renderBooleanEditor(domainClass,property) {

        def cp = domainClass.constrainedProperties[property.name]
        if(!cp) {
            return "<g:checkBox name='${property.name}' value=\"\${${domainClass.propertyName}?.${property.name}.toString().encodeHTML()}\"></g:checkBox>"
        }
        else {
            def buf = new StringBuffer('<g:checkBox ')
            if(cp.widget) buf << "widget='${cp.widget}'";

            buf << "name='${property.name}' value=\"\${${domainClass.propertyName}?.${property.name}.toString().encodeHTML()}\" "
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
            return "<g:datePicker name='${property.name}' value=\"\${${domainClass.propertyName}?.${property.name}.toString().encodeHTML()}\"></g:datePicker>"
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
            buf << "name='${property.name}' value=\"\${${domainClass.propertyName}?.${property.name}.toString().encodeHTML()}\"></g:datePicker>"
            return buf.toString()
          }
        }
    }

    private renderSelectTypeEditor(type,domainClass,property) {
       def cp = domainClass.constrainedProperties[property.name]
        if(!cp) {
            return "<g:${type}Select name='${property.name}' value=\"\${${domainClass.propertyName}?.${property.name}.toString().encodeHTML()}\"></g:${type}Select>"
        }
        else {
            def buf = new StringBuffer('<g:${type}Select ')
            if(cp.widget) buf << "widget='${cp.widget}' ";
            cp.attributes.each { k,v ->
                  buf << "${k}=\"${v}\" "
            }
            buf << "name='${property.name}' value=\"\${${domainClass.propertyName}?.${property.name}.toString().encodeHTML()}\"></g:${type}Select>"
            return buf.toString()
        }
    }




    private generateListView(domainClass, destDir) {
        def listFile = new File("${destDir}/list.gsp")
        def localOverwrite = false
        if(!overwrite) {
            if(listFile.exists()) {
				ant.input(message: "View ${listFile} already exists. Overwrite?","y,n", addproperty:"overwrite.listview")
                localOverwrite = (ant.antProject.properties."overwrite.listview" == "y") ? true : false        
                if(!localOverwrite)return
            }    
            else {
            	localOverwrite = true
            }
        }
        if(localOverwrite || overwrite) {
            def templateText = getTemplateText("list.gsp")

            def t = engine.createTemplate(templateText)
            def packageName  = domainClass.packageName ? "<%@ page import=\"${domainClass.fullName}\" %>" : ""
            def binding = [ packageName:packageName,
                            domainClass: domainClass, 
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
        def localOverwrite = false
        if(!overwrite) {
            if(showFile.exists()) {
				ant.input(message: "View ${showFile} already exists. Overwrite?","y,n", addproperty:"overwrite.showview")
                localOverwrite = (ant.antProject.properties."overwrite.showview" == "y") ? true : false        
                if(!localOverwrite)return
            }   
            else {
            	localOverwrite = true
            }

        }
        if(localOverwrite || overwrite) {
            def templateText = getTemplateText("show.gsp")

            def t = engine.createTemplate(templateText)
            def packageName  = domainClass.packageName ? "<%@ page import=\"${domainClass.fullName}\" %>" : ""
            def binding = [ packageName:packageName,
                            domainClass: domainClass, 
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
        def localOverwrite = false
        if(!overwrite) {
            if(editFile.exists()) {
				ant.input(message: "View ${editFile} already exists. Overwrite?","y,n", addproperty:"overwrite.editview")
                localOverwrite = (ant.antProject.properties."overwrite.editview" == "y") ? true : false        
                if(!localOverwrite)return
            } 
            else {
            	localOverwrite = true
            }
            
        }
        if(localOverwrite || overwrite) {
            def templateText = getTemplateText("edit.gsp")

            def t = engine.createTemplate(templateText)
            def multiPart = domainClass.properties.find{it.type==([] as Byte[]).class || it.type==([] as byte[]).class}
            def packageName  = domainClass.packageName ? "<%@ page import=\"${domainClass.fullName}\" %>" : ""
                def binding = [ packageName:packageName,
                                domainClass: domainClass, 
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
        def localOverwrite = false
        if(!overwrite) {
            if(createFile.exists()) {
				ant.input(message: "View ${createFile} already exists. Overwrite?","y,n", addproperty:"overwrite.createview")
                localOverwrite = (ant.antProject.properties."overwrite.createview" == "y") ? true : false        
                if(!localOverwrite)return
            }
            else {
            	localOverwrite = true
            }
            
        }
        if(localOverwrite || overwrite) {
            def templateText = getTemplateText("create.gsp")

            def t = engine.createTemplate(templateText)
            def multiPart = domainClass.properties.find{it.type==([] as Byte[]).class || it.type==([] as byte[]).class}
            
            def packageName  = domainClass.packageName ? "<%@ page import=\"${domainClass.fullName}\" %>" : ""
                def binding = [ packageName:packageName,
                                domainClass: domainClass, 
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
    
    private getTemplateText(String template) {
        // first check for presence of template in application               
        def templateFile = "${basedir}/templates/scaffolding/${template}"
        if (!new File(templateFile).exists()) {
            // template not found in application, use default template
            def ant = new AntBuilder()
            ant.property(environment:"env")   
            def grailsHome = ant.antProject.properties."env.GRAILS_HOME" 
            templateFile = "${grailsHome}/src/grails/templates/scaffolding/${template}"
        }
        return new File(templateFile).getText()
    }
    
}