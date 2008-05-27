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

import groovy.text.*;
import org.apache.commons.logging.Log;
import org.springframework.core.io.*
import org.apache.commons.logging.LogFactory;
import org.codehaus.groovy.grails.commons.GrailsDomainClass;
import org.codehaus.groovy.grails.commons.GrailsApplication;
import org.codehaus.groovy.grails.scaffolding.GrailsTemplateGenerator;
import org.codehaus.groovy.grails.commons.GrailsClassUtils as GCU
import org.codehaus.groovy.grails.commons.ApplicationHolder;
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
    def engine = new SimpleTemplateEngine()
    def ant = new AntBuilder()  
	ResourceLoader resourceLoader
	Template renderEditorTemplate
	
	void setResourceLoader(ResourceLoader rl) { 
		LOG.info "Scaffolding template generator set to use resource loader ${rl}"
		this.resourceLoader = rl
	}

    // a closure that uses the type to render the appropriate editor
    def renderEditor = { property ->
        def domainClass = property.domainClass
        def cp = domainClass.constrainedProperties[property.name]
                                                   
        if(!renderEditorTemplate) {
        	// create template once for performance
        	def templateText = getTemplateText("renderEditor.template")
        	renderEditorTemplate = engine.createTemplate(templateText)
        }

        def binding = [property:property,domainClass:domainClass,cp:cp]
        return renderEditorTemplate.make(binding).toString()
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
            def fullName = domainClass.fullName
            def pkg = ""
            def pos = fullName.lastIndexOf('.')
            if (pos != -1) {
                // Package name with trailing '.'
                pkg = fullName[0..pos]
            }

            def destFile = new File("${destdir}/grails-app/controllers/${pkg.replace('.' as char, '/' as char)}${domainClass.shortName}Controller.groovy")
			if(canWrite(destFile)) {
	            destFile.parentFile.mkdirs()

	            destFile.withWriter { w ->
	                generateController(domainClass, w)
	            }

	            LOG.info("Controller generated at ${destFile}")
			}
        }
    }

    private generateListView(domainClass, destDir) {
        def listFile = new File("${destDir}/list.gsp")
        if(canWrite(listFile)) {
            listFile.withWriter { w ->
                generateView(domainClass, "list", w)
            }
            LOG.info("list view generated at ${listFile.absolutePath}")
        }
    }

    private generateShowView(domainClass,destDir) {
        def showFile = new File("${destDir}/show.gsp")
        if(canWrite(showFile)) {
            showFile.withWriter { w ->
                generateView(domainClass, "show", w)
            }
            LOG.info("Show view generated at ${showFile.absolutePath}")
        }
    }

    private generateEditView(domainClass,destDir) {
        def editFile = new File("${destDir}/edit.gsp")
        if(canWrite(editFile)) {
            editFile.withWriter { w ->
                generateView(domainClass, "edit", w)
            }
            LOG.info("Edit view generated at ${editFile.absolutePath}")
        }
    }

    private generateCreateView(domainClass,destDir) {
        def createFile = new File("${destDir}/create.gsp")
        if(canWrite(createFile)) {

            createFile.withWriter { w ->
                   generateView(domainClass, "create", w)
            }
            LOG.info("Create view generated at ${createFile.absolutePath}")
        }
    }

    void generateView(GrailsDomainClass domainClass, String viewName, Writer out) {
        def templateText = getTemplateText("${viewName}.gsp")

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

        t.make(binding).writeTo(out)
    }

    void generateController(GrailsDomainClass domainClass, Writer out) {
        def templateText = getTemplateText("Controller.groovy")

        def binding = [ packageName:domainClass.packageName,
                        domainClass:domainClass,
                        className:domainClass.shortName,
                        propertyName:domainClass.propertyName,
                        comparator:org.codehaus.groovy.grails.scaffolding.DomainClassPropertyComparator.class]

        def t = engine.createTemplate(templateText)
        t.make(binding).writeTo(out)
    }
    
    private canWrite(testFile) {
        if(!overwrite && testFile.exists()) {
			try {
                ant.input(message: "File ${testFile} already exists. Overwrite?", "y,n,a", addproperty: "overwrite.${testFile.name}")
                overwrite = (ant.antProject.properties."overwrite.${testFile.name}" == "a") ? true : overwrite
                return overwrite || ((ant.antProject.properties."overwrite.${testFile.name}" == "y") ? true : false)
            } catch (Exception e) {
                // failure to read from standard in means we're probably running from an automation tool like a build server
                return true
            }
        }
        return true
    }
    
    private getTemplateText(String template) {
        def application = ApplicationHolder.getApplication()
        // first check for presence of template in application               
		if(resourceLoader && application?.warDeployed) {
			return resourceLoader
					.getResource("/WEB-INF/templates/scaffolding/${template}")
					.inputStream
					.text
		}   
		else {
	        def templateFile = "${basedir}/src/templates/scaffolding/${template}"
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
    
}