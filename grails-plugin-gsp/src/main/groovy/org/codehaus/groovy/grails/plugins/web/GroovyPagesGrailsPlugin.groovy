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
package org.codehaus.groovy.grails.plugins.web

import grails.artefact.Enhanced
import grails.gsp.PageRenderer
import grails.util.*

import java.lang.reflect.Modifier

import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsTagLibClass
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler
import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.web.api.ControllerTagLibraryApi
import org.codehaus.groovy.grails.plugins.web.api.TagLibraryApi
import org.codehaus.groovy.grails.plugins.web.taglib.*
import org.codehaus.groovy.grails.web.context.GrailsConfigUtils
import org.codehaus.groovy.grails.web.errors.ErrorsViewStackTracePrinter
import org.codehaus.groovy.grails.web.filters.JavascriptLibraryFilters
import org.codehaus.groovy.grails.web.pages.*
import org.codehaus.groovy.grails.web.pages.discovery.CachingGrailsConventionGroovyPageLocator
import org.codehaus.groovy.grails.web.pages.discovery.CachingGroovyPageStaticResourceLocator
import org.codehaus.groovy.grails.web.pages.ext.jsp.TagLibraryResolver
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.view.GrailsViewResolver
import org.codehaus.groovy.grails.web.sitemesh.GroovyPageLayoutFinder
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.web.servlet.view.JstlView
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler

/**
 * A Plugin that sets up and configures the GSP and GSP tag library support in Grails.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
class GroovyPagesGrailsPlugin {

    // monitor all resources that end with TagLib.groovy
    def watchedResources = ["file:./plugins/*/grails-app/taglib/**/*TagLib.groovy",
                            "file:./grails-app/taglib/**/*TagLib.groovy"]

    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [core: version, i18n: version]
    def observe = ['controllers']

    // Provide these tag libraries declaratively
    def providedArtefacts = [
        ApplicationTagLib,
        CountryTagLib,
        FormatTagLib,
        FormTagLib,
        JavascriptTagLib,
        RenderTagLib,
        ValidationTagLib,
        PluginTagLib,
        SitemeshTagLib,
        JavascriptLibraryFilters
    ]

    /**
     * Clear the page cache with the ApplicationContext is loaded
     */
    def doWithApplicationContext = {ApplicationContext ctx ->
        ctx.getBean("groovyPagesTemplateEngine").clearPageCache()
    }

    /**
     * Configures the various Spring beans required by GSP
     */
    def doWithSpring = {
        // resolves JSP tag libraries
        jspTagLibraryResolver(TagLibraryResolver)
        // resolves GSP tag libraries
        gspTagLibraryLookup(TagLibraryLookup)

        boolean developmentMode = !application.warDeployed
        Environment env = Environment.current
        boolean enableReload = env.isReloadEnabled() ||
            GrailsConfigUtils.isConfigTrue(application, GroovyPagesTemplateEngine.CONFIG_PROPERTY_GSP_ENABLE_RELOAD) ||
            (developmentMode && env == Environment.DEVELOPMENT)
        long gspCacheTimeout = Long.getLong("grails.gsp.reload.interval", (developmentMode && env == Environment.DEVELOPMENT) ? 0L : 5000L)
        boolean warDeployedWithReload = application.warDeployed && enableReload
        boolean enableCacheResources = !(application?.flatConfig?.get(GroovyPagesTemplateEngine.CONFIG_PROPERTY_DISABLE_CACHING_RESOURCES) == true)

        boolean customResourceLoader=false
        // If the development environment is used we need to load GSP files relative to the base directory
        // as oppose to in WAR deployment where views are loaded from /WEB-INF
        def viewsDir = application.config.grails.gsp.view.dir
        if (viewsDir) {
            log.info "Configuring GSP views directory as '${viewsDir}'"
            customResourceLoader=true
            groovyPageResourceLoader(GroovyPageResourceLoader) {
                baseResource = "file:${viewsDir}"
                pluginSettings = new PluginBuildSettings(BuildSettingsHolder.settings)
            }
        }
        else {
            if (developmentMode) {
                customResourceLoader=true
                groovyPageResourceLoader(GroovyPageResourceLoader) { bean ->
                    bean.lazyInit = true
                    BuildSettings settings = BuildSettingsHolder.settings
                    def location = settings?.baseDir ? GroovyPagesGrailsPlugin.transformToValidLocation(settings.baseDir.absolutePath) : '.'
                    baseResource = "file:$location"
                    pluginSettings = new PluginBuildSettings(settings)
                }
            }
            else {
                if (warDeployedWithReload && env.hasReloadLocation()) {
                    customResourceLoader=true
                    groovyPageResourceLoader(GroovyPageResourceLoader) {
                        def location = GroovyPagesGrailsPlugin.transformToValidLocation(env.reloadLocation)
                        baseResource = "file:${location}"
                        pluginSettings = new PluginBuildSettings(BuildSettingsHolder.settings)
                    }
                }
            }
        }

        def deployed = application.warDeployed
        groovyPageLocator(CachingGrailsConventionGroovyPageLocator) { bean ->
            bean.lazyInit = true
            if (customResourceLoader) {
                resourceLoader = groovyPageResourceLoader
            }
            if (deployed) {
                precompiledGspMap = { PropertiesFactoryBean pfb ->
                    ignoreResourceNotFound = true
                    location = "classpath:gsp/views.properties"
                }
            }
            if (enableReload) {
                cacheTimeout = gspCacheTimeout
            }
            reloadEnabled = enableReload
        }

        grailsResourceLocator(CachingGroovyPageStaticResourceLocator) { bean ->
            bean.parent = "abstractGrailsResourceLocator"
            if (enableReload) {
                cacheTimeout = gspCacheTimeout
            }
        }

        // Setup the main templateEngine used to render GSPs
        groovyPagesTemplateEngine(GroovyPagesTemplateEngine) { bean ->
            classLoader = ref("classLoader")
            groovyPageLocator = groovyPageLocator
            if (enableReload) {
                reloadEnabled = enableReload
            }
            tagLibraryLookup = gspTagLibraryLookup
            jspTagLibraryResolver = jspTagLibraryResolver
            cacheResources = enableCacheResources
        }

        groovyPageRenderer(PageRenderer, ref("groovyPagesTemplateEngine")) { bean ->
            bean.lazyInit = true
            groovyPageLocator = groovyPageLocator
        }

        groovyPagesTemplateRenderer(GroovyPagesTemplateRenderer) { bean ->
            bean.autowire = true
        }

        groovyPageLayoutFinder(GroovyPageLayoutFinder) {
            gspReloadEnabled = enableReload
            defaultDecoratorName = application.flatConfig['grails.sitemesh.default.layout'] ?: null
            enableNonGspViews = application.flatConfig['grails.sitemesh.enable.nongsp'] ?: false
            viewResolver = ref('jspViewResolver')
        }

        // Setup the GroovyPagesUriService
        groovyPagesUriService(DefaultGroovyPagesUriService) { bean ->
            bean.lazyInit = true
        }

        abstractViewResolver {
            viewClass = JstlView
            prefix = GrailsApplicationAttributes.PATH_TO_VIEWS
            suffix = ".jsp"
            templateEngine = groovyPagesTemplateEngine
            groovyPageLocator = groovyPageLocator
            if (enableReload) {
                cacheTimeout = gspCacheTimeout
            }
        }
        // Configure a Spring MVC view resolver
        jspViewResolver(GrailsViewResolver) { bean ->
            bean.lazyInit = true
            bean.parent = "abstractViewResolver"
        }

        final pluginManager = manager
        instanceTagLibraryApi(TagLibraryApi, pluginManager)
        instanceControllerTagLibraryApi(ControllerTagLibraryApi, pluginManager)
        // Now go through tag libraries and configure them in Spring too. With AOP proxies and so on
        for (taglib in application.tagLibClasses) {

            final tagLibClass = taglib.clazz

            "${taglib.fullName}"(tagLibClass) { bean ->
                bean.autowire = true
                bean.lazyInit = true

                // Taglib scoping support could be easily added here. Scope could be based on a static field in the taglib class.
                //bean.scope = 'request'
            }
        }

        errorsViewStackTracePrinter(ErrorsViewStackTracePrinter, ref('grailsResourceLocator'))
    }

    static String transformToValidLocation(String location) {
        if (location == '.') return location
        if (!location.endsWith(File.separator)) return "${location}${File.separator}"
        return location
    }

    /**
     * Modifies the web.xml when in development mode to allow viewing of sources
     */
    def doWithWebDescriptor = { webXml ->
        if (Environment.current != Environment.DEVELOPMENT) {
            return
        }

        // Find the GSP servlet and allow viewing generated source in development mode
        def gspServlet = webXml.servlet.find {it.'servlet-name'?.text() == 'gsp' }
        gspServlet.'servlet-class' + {
            'init-param' {
                description """
                Allows developers to view the intermediate source code, when they pass
                a spillGroovy argument in the URL.
                """
                'param-name'('showSource')
                'param-value'(1)
            }
        }
    }
    
    private void enhanceClasses(classes, apiObject) {
        def nonEnhancedClasses = [] as Set
        for (Class clazz in classes) {
            if (!clazz.getAnnotation(Enhanced)) {
                nonEnhancedClasses << clazz
            }
            Class superClass = clazz.superclass
            while (superClass != Object) {
                if (Modifier.isAbstract(superClass.getModifiers()) && !superClass.getAnnotation(Enhanced)) {
                    nonEnhancedClasses << superClass
                }
                superClass = superClass.superclass
            }
        }
        if(nonEnhancedClasses) {
            def enhancer = new MetaClassEnhancer()
            enhancer.addApi apiObject
            nonEnhancedClasses.each { enhancer.enhance it.getMetaClass() }
        }
    }
    

    /**
     * Sets up dynamic methods required by the GSP implementation including dynamic tag method dispatch
     */
    def doWithDynamicMethods = { ApplicationContext ctx ->
        WebMetaUtils.registerStreamCharBufferMetaClass()

        TagLibraryLookup gspTagLibraryLookup = ctx.getBean("gspTagLibraryLookup")
        GrailsPluginManager pluginManager = getManager()

        if (manager?.hasGrailsPlugin("controllers")) {
            def controllerClasses = application.controllerClasses*.clazz
            enhanceClasses(controllerClasses, ctx.getBean("instanceControllerTagLibraryApi"))
        }
        
        def tagLibClasses = application.tagLibClasses*.clazz
        enhanceClasses(tagLibClasses, ctx.getBean("instanceTagLibraryApi"))
        application.tagLibClasses.each { taglibClass -> 
            WebMetaUtils.enhanceTagLibMetaClass(taglibClass, gspTagLibraryLookup)
        }
    }

    def onChange = { event ->
        def ctx = event.ctx ?: application.mainContext
        
        if (application.isArtefactOfType(TagLibArtefactHandler.TYPE, event.source)) {
            GrailsClass taglibClass = application.addArtefact(TagLibArtefactHandler.TYPE, event.source)
            if (taglibClass) {
                // replace tag library bean
                def beanName = taglibClass.fullName
                def beans = beans {
                    "$beanName"(taglibClass.clazz) { bean ->
                        bean.autowire = true
                    }
                }
                beans.registerBeans(event.ctx)

                // The tag library lookup class caches "tag -> taglib class"
                // so we need to update it now.
                def lookup = event.ctx.getBean("gspTagLibraryLookup")
                lookup.registerTagLib(taglibClass)
                
                enhanceClasses([taglibClass.clazz], ctx.getBean("instanceTagLibraryApi"))
                WebMetaUtils.enhanceTagLibMetaClass(taglibClass, ctx.getBean("gspTagLibraryLookup"))
            }
        } else if (application.isArtefactOfType(ControllerArtefactHandler.TYPE, event.source)) {
            enhanceClasses([event.source], ctx.getBean("instanceControllerTagLibraryApi"))
        }

        // clear uri cache after changes
        ctx.getBean("groovyPagesUriService").clear()
    }
}
