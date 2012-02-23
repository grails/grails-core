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

import grails.build.logging.GrailsConsole
import grails.util.BuildSettingsHolder
import groovy.text.SimpleTemplateEngine
import groovy.text.Template

import org.apache.commons.logging.Log
import org.apache.commons.logging.LogFactory
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginManagerAware
import org.springframework.context.ResourceLoaderAware
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.FileSystemResource
import org.springframework.core.io.ResourceLoader
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.util.Assert
import org.springframework.core.io.AbstractResource

/**
 * Default implementation of the generator that generates grails artifacts (controllers, views etc.)
 * from the domain model.
 *
 * @author Graeme Rocher
 */
class DefaultGrailsTemplateGenerator implements GrailsTemplateGenerator, ResourceLoaderAware, PluginManagerAware {

    static final Log LOG = LogFactory.getLog(DefaultGrailsTemplateGenerator)

    String basedir = "."
    boolean overwrite = false
    def engine = new SimpleTemplateEngine()
    ResourceLoader resourceLoader
    Template renderEditorTemplate
    String domainSuffix = 'Instance'
    GrailsPluginManager pluginManager
    GrailsApplication grailsApplication

    /**
     * Used by the scripts so that they can pass in their AntBuilder instance.
     */
    DefaultGrailsTemplateGenerator(ClassLoader classLoader) {
        engine = new SimpleTemplateEngine(classLoader)
    }

    /**
     * Default constructor.
     */
    DefaultGrailsTemplateGenerator() {}

    void setGrailsApplication(GrailsApplication ga) {
        this.grailsApplication = ga
        if (ga != null) {
            def suffix = ga.config?.grails?.scaffolding?.templates?.domainSuffix
            if (suffix != [:]) {
                domainSuffix = suffix
            }
        }
    }

    void setResourceLoader(ResourceLoader rl) {
        LOG.info "Scaffolding template generator set to use resource loader ${rl}"
        this.resourceLoader = rl
    }

    // uses the type to render the appropriate editor
    def renderEditor = { property ->
        def domainClass = property.domainClass
        def cp
        if (pluginManager?.hasGrailsPlugin('hibernate')) {
            cp = domainClass.constrainedProperties[property.name]
        }

        if (!renderEditorTemplate) {
            // create template once for performance
            def templateText = getTemplateText("renderEditor.template")
            renderEditorTemplate = engine.createTemplate(templateText)
        }

        def binding = [pluginManager: pluginManager,
                       property: property,
                       domainClass: domainClass,
                       cp: cp,
                       domainInstance:getPropertyName(domainClass)]
        return renderEditorTemplate.make(binding).toString()
    }

    void generateViews(GrailsDomainClass domainClass, String destdir) {
        Assert.hasText destdir, "Argument [destdir] not specified"

        def viewsDir = new File("${destdir}/grails-app/views/${domainClass.propertyName}")
        if (!viewsDir.exists()) {
            viewsDir.mkdirs()
        }

        for (t in getTemplateNames()) {
            LOG.info "Generating $t view for domain class [${domainClass.fullName}]"
            generateView domainClass, t, viewsDir.absolutePath
        }
    }

    void generateController(GrailsDomainClass domainClass, String destdir) {
        Assert.hasText destdir, "Argument [destdir] not specified"

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

                destFile.withWriter { w ->
                    generateController(domainClass, w)
                }

                LOG.info("Controller generated at ${destFile}")
            }
        }
    }

    private generateListView(domainClass, destDir) {
        def listFile = new File("${destDir}/list.gsp")
        if (canWrite(listFile)) {
            listFile.withWriter { w ->
                generateView(domainClass, "list", w)
            }
            LOG.info("list view generated at ${listFile.absolutePath}")
        }
    }

    private generateShowView(domainClass, destDir) {
        def showFile = new File("${destDir}/show.gsp")
        if (canWrite(showFile)) {
            showFile.withWriter { w ->
                generateView(domainClass, "show", w)
            }
            LOG.info("Show view generated at ${showFile.absolutePath}")
        }
    }

    private generateEditView(domainClass, destDir) {
        def editFile = new File("${destDir}/edit.gsp")
        if (canWrite(editFile)) {
            editFile.withWriter { w ->
                generateView(domainClass, "edit", w)
            }
            LOG.info("Edit view generated at ${editFile.absolutePath}")
        }
    }

    private generateCreateView(domainClass, destDir) {
        def createFile = new File("${destDir}/create.gsp")
        if (canWrite(createFile)) {
            createFile.withWriter { w ->
                generateView(domainClass, "create", w)
            }
            LOG.info("Create view generated at ${createFile.absolutePath}")
        }
    }

    void generateView(GrailsDomainClass domainClass, String viewName, String destDir) {
        File destFile = new File("$destDir/${viewName}.gsp")
        if (canWrite(destFile)) {
            destFile.withWriter { Writer writer ->
                generateView domainClass, viewName, writer
            }
        }
    }

    void generateTest(GrailsDomainClass domainClass, String destDir) {
        File destFile = new File("$destDir/${domainClass.packageName.replace('.','/')}/${domainClass.shortName}ControllerTests.groovy")
        def templateText = getTemplateText("Test.groovy")
        def t = engine.createTemplate(templateText)

        def binding = [pluginManager: pluginManager,
                       packageName: domainClass.packageName,
                       domainClass: domainClass,
                       className: domainClass.shortName,
                       propertyName: domainClass.logicalPropertyName]

        if (canWrite(destFile)) {
            destFile.parentFile.mkdirs()
            destFile.withWriter {
                t.make(binding).writeTo(it)
            }
        }
    }

    void generateView(GrailsDomainClass domainClass, String viewName, Writer out) {
        def templateText = getTemplateText("${viewName}.gsp")

        if(templateText) {
            def t = engine.createTemplate(templateText)
            def multiPart = domainClass.properties.find {it.type == ([] as Byte[]).class || it.type == ([] as byte[]).class}

            boolean hasHibernate = pluginManager?.hasGrailsPlugin('hibernate')
            def packageName = domainClass.packageName ? "<%@ page import=\"${domainClass.fullName}\" %>" : ""
            def binding = [pluginManager: pluginManager,
                    packageName: packageName,
                    domainClass: domainClass,
                    multiPart: multiPart,
                    className: domainClass.shortName,
                    propertyName:  getPropertyName(domainClass),
                    renderEditor: renderEditor,
                    comparator: hasHibernate ? DomainClassPropertyComparator : SimpleDomainClassPropertyComparator]

            t.make(binding).writeTo(out)
        }

    }

    void generateController(GrailsDomainClass domainClass, Writer out) {
        def templateText = getTemplateText("Controller.groovy")

        boolean hasHibernate =pluginManager?.hasGrailsPlugin('hibernate')
        def binding = [pluginManager: pluginManager,
                       packageName: domainClass.packageName,
                       domainClass: domainClass,
                       className: domainClass.shortName,
                       propertyName: getPropertyName(domainClass),
                       comparator: hasHibernate ? DomainClassPropertyComparator : SimpleDomainClassPropertyComparator]

        def t = engine.createTemplate(templateText)
        t.make(binding).writeTo(out)
    }

    private String getPropertyName(GrailsDomainClass domainClass) { "${domainClass.propertyName}${domainSuffix}" }

    private canWrite(File testFile) {
        if (!overwrite && testFile.exists()) {
            try {
                def response = GrailsConsole.getInstance().userInput("File ${makeRelativeIfPossible(testFile.absolutePath, basedir)} already exists. Overwrite?",['y','n','a'] as String[])
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

    public static String makeRelativeIfPossible(String fileName, String base = System.getProperty("base.dir")) {
        if (base) {
            fileName = fileName - new File(base).canonicalPath
        }
        return fileName
    }


    public getTemplateText(String template) {
        def application = grailsApplication
        // first check for presence of template in application
        if (resourceLoader && application?.warDeployed) {
            return resourceLoader.getResource("/WEB-INF/templates/scaffolding/${template}").inputStream.text
        }

        AbstractResource templateFile = getTemplateResource(template)
        if(templateFile.exists()) {
            return templateFile.inputStream.getText()
        }
    }

    AbstractResource getTemplateResource(String template) {
        def templateFile = new FileSystemResource(new File("${basedir}/src/templates/scaffolding/${template}").absoluteFile)

        if (!templateFile.exists()) {
            templateFile = new FileSystemResource(new File("${basedir}/src/grails/templates/scaffolding/${template}").absoluteFile)
        }
        if (!templateFile.exists()) {
            // template not found in application, use default template
            def grailsHome = BuildSettingsHolder.settings?.grailsHome

            if (grailsHome) {
                templateFile = new FileSystemResource(new File("${grailsHome}/src/grails/templates/scaffolding/${template}").absoluteFile)
                if (!templateFile.exists()) {
                    templateFile = new FileSystemResource(new File("${grailsHome}/grails-resources/src/grails/templates/scaffolding/${template}").absoluteFile)
                }
            }
            else {
                if(template.startsWith('/')) {
                    template = template.substring(1)
                }
                templateFile = new ClassPathResource("src/grails/templates/scaffolding/${template}")
            }
        }
        return templateFile
    }

    def getTemplateNames() {
        Closure filter = { it[0..-5] }
        if (resourceLoader && application?.isWarDeployed()) {
            def resolver = new PathMatchingResourcePatternResolver(resourceLoader)
            try {
                return resolver.getResources("/WEB-INF/templates/scaffolding/*.gsp").filename.collect(filter)
            }
            catch (e) {
                return []
            }
        }

        def resources = []
        def resolver = new PathMatchingResourcePatternResolver()
        String templatesDirPath = "${basedir}/src/templates/scaffolding"
        def templatesDir = new FileSystemResource(templatesDirPath)
        if (templatesDir.exists()) {
            try {
                resources = resolver.getResources("file:$templatesDirPath/*.gsp").filename.collect(filter)
            }
            catch (e) {
                LOG.info("Error while loading views from grails-app scaffolding folder", e)
            }
        }

        templatesDirPath = "${basedir}/src/grails/templates/scaffolding"
        templatesDir = new FileSystemResource(templatesDirPath)
        if (templatesDir.exists()) {
            try {
                resources.addAll(resolver.getResources("file:$templatesDirPath/*.gsp").filename.collect(filter))
            }
            catch (e) {
                LOG.info("Error while loading views from the src/grails/templates/scaffolding folder", e)
            }
        }

        def grailsHome = BuildSettingsHolder.settings?.grailsHome
        if (grailsHome) {
            try {
                def grailsHomeTemplates = resolver.getResources("file:${grailsHome}/src/grails/templates/scaffolding/*.gsp").filename.collect(filter)
                resources.addAll(grailsHomeTemplates)
            }
            catch (e) {
                // ignore
                LOG.debug("Error locating templates from GRAILS_HOME: ${e.message}", e)
            }

            try {
                def grailsHomeTemplates = resolver.getResources("file:${grailsHome}/grails-resources/src/grails/templates/scaffolding/*.gsp").filename.collect(filter)
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
        return resources
    }
}
