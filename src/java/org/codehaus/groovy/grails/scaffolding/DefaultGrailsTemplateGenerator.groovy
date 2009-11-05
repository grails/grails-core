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
package org.codehaus.groovy.grails.scaffolding

import grails.util.BuildSettingsHolder
import groovy.text.SimpleTemplateEngine
import groovy.text.Template
import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.scaffolding.DomainClassPropertyComparator
import org.codehaus.groovy.grails.scaffolding.GrailsTemplateGenerator
import org.springframework.context.ResourceLoaderAware
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.codehaus.groovy.grails.cli.CommandLineHelper
import org.codehaus.groovy.grails.commons.ConfigurationHolder

/**
 * Default implementation of the generator that generates grails artifacts (controllers, views etc.)
 * from the domain model
 *
 * @author Graeme Rocher
 * @since 09-Feb-2006
 */
class DefaultGrailsTemplateGenerator implements GrailsTemplateGenerator, ResourceLoaderAware {

    static final Log LOG = LogFactory.getLog(DefaultGrailsTemplateGenerator.class)

    String basedir = "."
    boolean overwrite = false
    def engine = new SimpleTemplateEngine()
    ResourceLoader resourceLoader
    Template renderEditorTemplate
    String domainSuffix = 'Instance'


    /**
     * Used by the scripts so that they can pass in their AntBuilder
     * instance.
     */
    DefaultGrailsTemplateGenerator(ClassLoader classLoader) {
        engine = new SimpleTemplateEngine(classLoader)        
        def suffix = ConfigurationHolder.config?.grails?.templates?.domainSuffix
        if (suffix == [:]) { // default for compatibility
            suffix = 'Instance'
        }
        domainSuffix = suffix ?: 'Instance'
    }

    /**
     * Creates an instance
     */
    DefaultGrailsTemplateGenerator() {
    }



    void setResourceLoader(ResourceLoader rl) {
        LOG.info "Scaffolding template generator set to use resource loader ${rl}"
        this.resourceLoader = rl
    }

    // a closure that uses the type to render the appropriate editor
    def renderEditor = {property ->
        def domainClass = property.domainClass
        def cp = domainClass.constrainedProperties[property.name]

        if (!renderEditorTemplate) {
            // create template once for performance
            def templateText = getTemplateText("renderEditor.template")
            renderEditorTemplate = engine.createTemplate(templateText)
        }

        def binding = [property: property, domainClass: domainClass, cp: cp, domainInstance:getPropertyName(domainClass)]
        return renderEditorTemplate.make(binding).toString()
    }

    public void generateViews(GrailsDomainClass domainClass, String destdir) {
        if (!destdir)
            throw new IllegalArgumentException("Argument [destdir] not specified")

        def viewsDir = new File("${destdir}/grails-app/views/${domainClass.propertyName}")
        if (!viewsDir.exists())
            viewsDir.mkdirs()

        def templateNames = getTemplateNames()

        for(t in templateNames) {
           LOG.info "Generating $t view for domain class [${domainClass.fullName}]"
           generateView domainClass, t, viewsDir.absolutePath
        }

    }

    public void generateController(GrailsDomainClass domainClass, String destdir) {
        if (!destdir)
            throw new IllegalArgumentException("Argument [destdir] not specified")

        if (domainClass) {
            def fullName = domainClass.fullName
            def pkg = ""
            def pos = fullName.lastIndexOf('.')
            if (pos != -1) {
                // Package name with trailing '.'
                pkg = fullName[0..pos]
            }

            def destFile = new File("${destdir}/grails-app/controllers/${pkg.replace('.' as char, '/' as char)}${domainClass.shortName}Controller.groovy")
            if (canWrite(destFile)) {
                destFile.parentFile.mkdirs()

                destFile.withWriter {w ->
                    generateController(domainClass, w)
                }

                LOG.info("Controller generated at ${destFile}")
            }
        }
    }

    private generateListView(domainClass, destDir) {
        def listFile = new File("${destDir}/list.gsp")
        if (canWrite(listFile)) {
            listFile.withWriter {w ->
                generateView(domainClass, "list", w)
            }
            LOG.info("list view generated at ${listFile.absolutePath}")
        }
    }

    private generateShowView(domainClass, destDir) {
        def showFile = new File("${destDir}/show.gsp")
        if (canWrite(showFile)) {
            showFile.withWriter {w ->
                generateView(domainClass, "show", w)
            }
            LOG.info("Show view generated at ${showFile.absolutePath}")
        }
    }

    private generateEditView(domainClass, destDir) {
        def editFile = new File("${destDir}/edit.gsp")
        if (canWrite(editFile)) {
            editFile.withWriter {w ->
                generateView(domainClass, "edit", w)
            }
            LOG.info("Edit view generated at ${editFile.absolutePath}")
        }
    }

    private generateCreateView(domainClass, destDir) {
        def createFile = new File("${destDir}/create.gsp")
        if (canWrite(createFile)) {

            createFile.withWriter {w ->
                generateView(domainClass, "create", w)
            }
            LOG.info("Create view generated at ${createFile.absolutePath}")
        }
    }


    public void generateView(GrailsDomainClass domainClass, String viewName, String destDir) {
        File destFile = new File("$destDir/${viewName}.gsp")
        if (canWrite(destFile)) {
            destFile.withWriter {Writer writer ->
                generateView domainClass, viewName, writer
            }
        }
    }

    void generateView(GrailsDomainClass domainClass, String viewName, Writer out) {
        def templateText = getTemplateText("${viewName}.gsp")

        def t = engine.createTemplate(templateText)
        def multiPart = domainClass.properties.find {it.type == ([] as Byte[]).class || it.type == ([] as byte[]).class}

        def packageName = domainClass.packageName ? "<%@ page import=\"${domainClass.fullName}\" %>" : ""
        def binding = [packageName: packageName,
                domainClass: domainClass,
                multiPart: multiPart,
                className: domainClass.shortName,
                propertyName:  getPropertyName(domainClass),
                renderEditor: renderEditor,
                comparator: org.codehaus.groovy.grails.scaffolding.DomainClassPropertyComparator.class]

        t.make(binding).writeTo(out)
    }

    void generateController(GrailsDomainClass domainClass, Writer out) {
        def templateText = getTemplateText("Controller.groovy")

        def binding = [packageName: domainClass.packageName,
                domainClass: domainClass,
                className: domainClass.shortName,
                propertyName: getPropertyName(domainClass),
                comparator: org.codehaus.groovy.grails.scaffolding.DomainClassPropertyComparator.class]

        def t = engine.createTemplate(templateText)
        t.make(binding).writeTo(out)
    }

    private String getPropertyName(GrailsDomainClass domainClass) {
        return "${domainClass.propertyName}${domainSuffix}"
    }

    private helper = new CommandLineHelper()
    private canWrite(testFile) {
        if (!overwrite && testFile.exists()) {
            try {
                def response = helper.userInput("File ${testFile} already exists. Overwrite?",['y','n','a'] as String[])
                overwrite = overwrite || response == "a"
                return overwrite || response == "y"
            }
            catch (Exception e) {
                // failure to read from standard in means we're probably running from an automation tool like a build server
                return true
            }
        }
        return true
    }

    private getTemplateText(String template) {
        def application = ApplicationHolder.getApplication()
        // first check for presence of template in application
        if (resourceLoader && application?.warDeployed) {
            return resourceLoader.getResource("/WEB-INF/templates/scaffolding/${template}").inputStream.text
        }
        else {
            def templateFile = new FileSystemResource("${basedir}/src/templates/scaffolding/${template}")
            if (!templateFile.exists()) {
                // template not found in application, use default template
                def grailsHome = BuildSettingsHolder.settings?.grailsHome

                if (grailsHome) {
                    templateFile = new FileSystemResource("${grailsHome}/src/grails/templates/scaffolding/${template}")
                }
                else {
                    templateFile = new ClassPathResource("src/grails/templates/scaffolding/${template}")
                }
            }
            return templateFile.inputStream.getText()
        }
    }

    def getTemplateNames() {
        def resources = []
        Closure filter = { it[0..-5] }
        if(resourceLoader && application?.isWarDeployed()) {
            def resolver = new PathMatchingResourcePatternResolver(resourceLoader)
            try {
                resources = resolver.getResources("/WEB-INF/templates/scaffolding/*.gsp").filename.collect(filter)
            }
            catch (e) {
                return []
            }
        }
        else {
            def resolver = new PathMatchingResourcePatternResolver()
            String templatesDirPath = "${basedir}/src/templates/scaffolding"
            def templatesDir = new FileSystemResource(templatesDirPath)
            if(templatesDir.exists()) {
                try {
                    resources = resolver.getResources("file:$templatesDirPath/*.gsp").filename.collect(filter)
                }
                catch (e) {
                    LOG.info("Error while loading views from grails-app scaffolding folder", e)
                }
            }

            def grailsHome = BuildSettingsHolder.settings?.grailsHome
            if(grailsHome) {
                try {
                    def grailsHomeTemplates = resolver.getResources("file:${grailsHome}/src/grails/templates/scaffolding/*.gsp").filename.collect(filter)
                    resources.addAll(grailsHomeTemplates)
                }
                catch (e) {
                    // ignore                    
                    LOG.debug("Error locating templates from GRAILS_HOME: ${e.message}", e)
                }
            }
            else {
                try {
                    def templates = resolver.getResources("classpath:src/grails/templates/scaffolding/*.gsp").filename.collect(filter)
                    resources.addAll(templates)
                }
                catch (e) {
                    // ignore
                    LOG.debug("Error locating templates from classpath: ${e.message}", e)
                }
            }
        }
        return resources
    }

}
