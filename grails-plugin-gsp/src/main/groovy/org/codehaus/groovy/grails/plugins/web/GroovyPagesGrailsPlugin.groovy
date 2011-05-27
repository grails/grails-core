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
import java.lang.reflect.Modifier
import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.commons.GrailsClassUtils
import org.codehaus.groovy.grails.commons.GrailsTagLibClass
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler
import org.codehaus.groovy.grails.commons.metaclass.MetaClassEnhancer
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.web.api.ControllerTagLibraryApi
import org.codehaus.groovy.grails.plugins.web.api.TagLibraryApi
import org.codehaus.groovy.grails.web.context.GrailsConfigUtils
import org.codehaus.groovy.grails.web.filters.JavascriptLibraryFilters
import org.codehaus.groovy.grails.web.pages.ext.jsp.TagLibraryResolver
import org.codehaus.groovy.grails.web.plugins.support.WebMetaUtils
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.view.GrailsViewResolver
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.context.ApplicationContext
import org.springframework.web.servlet.view.JstlView
import grails.util.*
import org.codehaus.groovy.grails.plugins.web.taglib.*
import org.codehaus.groovy.grails.web.pages.*

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
    def nonEnhancedTagLibClasses = []

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
        // A bean used to resolve JSP tag libraries
        jspTagLibraryResolver(TagLibraryResolver)
        // A bean used to resolve GSP tag libraries
        gspTagLibraryLookup(TagLibraryLookup)

        boolean developmentMode = !application.warDeployed
        Environment env = Environment.current
        boolean enableReload = env.isReloadEnabled() ||
            GrailsConfigUtils.isConfigTrue(application, GroovyPagesTemplateEngine.CONFIG_PROPERTY_GSP_ENABLE_RELOAD) ||
            (developmentMode && env == Environment.DEVELOPMENT)
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

        // Setup the main templateEngine used to render GSPs
        groovyPagesTemplateEngine(GroovyPagesTemplateEngine) { bean ->
            classLoader = ref("classLoader")
            if (customResourceLoader) {
                resourceLoader = groovyPageResourceLoader
                bean.lazyInit = true
            }
            if (enableReload) {
                reloadEnabled = enableReload
            }
            tagLibraryLookup = gspTagLibraryLookup
            jspTagLibraryResolver = jspTagLibraryResolver
            if (application.warDeployed) {
                precompiledGspMap = { PropertiesFactoryBean pfb ->
                    ignoreResourceNotFound = true
                    location = "classpath:gsp/views.properties"
                }
            }
            cacheResources = enableCacheResources
        }

        // Setup the GroovyPagesUriService
        groovyPagesUriService(DefaultGroovyPagesUriService) { bean ->
            bean.lazyInit = true
        }

        // Configure a Spring MVC view resolver
        jspViewResolver(GrailsViewResolver) { bean ->
            bean.lazyInit = true
            viewClass = JstlView
            prefix = GrailsApplicationAttributes.PATH_TO_VIEWS
            suffix = ".jsp"
            templateEngine = groovyPagesTemplateEngine
            if (developmentMode) {
                resourceLoader = groovyPageResourceLoader
            }
        }

        final pluginManager = manager
        instanceTagLibraryApi(TagLibraryApi, pluginManager)
        instanceControllerTagLibraryApi(ControllerTagLibraryApi, pluginManager)
        // Now go through tag libraries and configure them in spring too. With AOP proxies and so on
        for (taglib in application.tagLibClasses) {

            final tagLibClass = taglib.clazz
            def enhancedAnn = tagLibClass.getAnnotation(Enhanced)
            if (enhancedAnn == null) {
                nonEnhancedTagLibClasses << taglib
            }

            "${taglib.fullName}"(tagLibClass) { bean ->
                bean.autowire = true
                bean.lazyInit = true

                // Taglib scoping support could be easily added here. Scope could be based on a static field in the taglib class.
                //bean.scope = 'request'
            }
        }
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

    /**
     * Sets up dynamic methods required by the GSP implementation including dynamic tag method dispatch
     */
    def doWithDynamicMethods = { ApplicationContext ctx ->

        WebMetaUtils.registerStreamCharBufferMetaClass()

        TagLibraryLookup gspTagLibraryLookup = ctx.getBean("gspTagLibraryLookup")
        GrailsPluginManager pluginManager = getManager()

        if (manager?.hasGrailsPlugin("controllers")) {

            def namespaceGetters = [:]
            for (namespace in gspTagLibraryLookup.availableNamespaces) {
                def propName = namespace
                def namespaceDispatcher = gspTagLibraryLookup.lookupNamespaceDispatcher(namespace)
                namespaceGetters[propName] = namespaceDispatcher
            }


            def controllerClasses = application.controllerClasses*.clazz
            for (Class controller in controllerClasses) {
                Class controllerClass = controller
                MetaClass mc = controllerClass.metaClass
                for (entry in namespaceGetters) {
                    final String propertyName = entry.key
                    final dispatcher = entry.value
                    if (!mc.getMetaProperty(propertyName)) {
                        mc."${GrailsClassUtils.getGetterName(propertyName)}" = {-> dispatcher }
                    }
                }

                if (!controllerClass.getAnnotation(Enhanced)) {
                    registerControllerMethodMissing(mc, gspTagLibraryLookup, ctx)
                    Class superClass = controllerClass.superclass
                    // deal with abstract super classes
                    while (superClass != Object) {
                        if (Modifier.isAbstract(superClass.getModifiers())) {
                            registerControllerMethodMissing(superClass.metaClass, gspTagLibraryLookup, ctx)
                        }
                        superClass = superClass.superclass
                    }
                }

            }
        }


        if (nonEnhancedTagLibClasses) {
            def tagLibApi = ctx.getBean("instanceTagLibraryApi")

            def enhancer = new MetaClassEnhancer()
            enhancer.addApi tagLibApi

            for (GrailsTagLibClass t in nonEnhancedTagLibClasses) {
                GrailsTagLibClass taglib = t
                MetaClass mc = taglib.metaClass
                enhancer.enhance mc
                String namespace = taglib.namespace ?: GroovyPage.DEFAULT_NAMESPACE

                for (tag in taglib.tagNames) {
                    WebMetaUtils.registerMethodMissingForTags(mc, gspTagLibraryLookup, namespace, tag)
                }

                mc.propertyMissing = { String name ->
                    def result = gspTagLibraryLookup.lookupNamespaceDispatcher(name)
                    if (result == null) {
                        def tagLibrary = gspTagLibraryLookup.lookupTagLibrary(namespace, name)
                        if (!tagLibrary) {
                            tagLibrary = gspTagLibraryLookup.lookupTagLibrary(GroovyPage.DEFAULT_NAMESPACE, name)
                        }

                        def tagProperty = tagLibrary?."$name"
                        result = tagProperty ? tagProperty.clone() : null
                    }

                    if (result != null) {
                        mc."${GrailsClassUtils.getGetterName(name)}" = {-> result }
                        return result
                    }

                    throw new MissingPropertyException(name, delegate.class)
                }

                mc.methodMissing = { String name, args ->
                    def usednamespace = namespace
                    def tagLibrary = gspTagLibraryLookup.lookupTagLibrary(namespace, name)
                    if (!tagLibrary) {
                        tagLibrary = gspTagLibraryLookup.lookupTagLibrary(GroovyPage.DEFAULT_NAMESPACE, name)
                        usednamespace = GroovyPage.DEFAULT_NAMESPACE
                    }
                    if (tagLibrary) {
                        WebMetaUtils.registerMethodMissingForTags(mc, gspTagLibraryLookup, usednamespace, name)
                        //WebMetaUtils.registerMethodMissingForTags(mc, tagLibrary, name)
                    }
                    if (mc.respondsTo(delegate, name, args)) {
                        return mc.invokeMethod(delegate, name, args)
                    }

                    throw new MissingMethodException(name, delegate.class, args)
                }
            }
        }


    }

    def registerControllerMethodMissing(MetaClass mc, TagLibraryLookup lookup, ApplicationContext ctx) {
        // allow controllers to call tag library methods
        mc.methodMissing = {String name, args ->
            args = args == null ? [] as Object[] : args
            def tagLibrary = lookup.lookupTagLibrary(GroovyPage.DEFAULT_NAMESPACE, name)
            if (tagLibrary) {
                MetaClass controllerMc = delegate.class.metaClass
                WebMetaUtils.registerMethodMissingForTags(controllerMc, lookup, GroovyPage.DEFAULT_NAMESPACE, name)
                if (controllerMc.respondsTo(delegate, name, args)) {
                    return controllerMc.invokeMethod(delegate, name, args)
                }

                throw new MissingMethodException(name, delegate.class, args)
            }

            throw new MissingMethodException(name, delegate.class, args)
        }
    }

    def onChange = { event ->
        if (application.isArtefactOfType(TagLibArtefactHandler.TYPE, event.source)) {
            GrailsClass taglibClass = application.addArtefact(TagLibArtefactHandler.TYPE, event.source)
            if (taglibClass) {
                // replace tag library bean
                def beanName = taglibClass.fullName
                def beans = beans {
                    "$beanName"(taglibClass.clazz) { bean ->
                        bean.autowire = true
                        if (taglibClass.clazz.getAnnotation(Enhanced)) {

                            instanceTagLibraryApi = ref("instanceTagLibraryApi")
                        }
                        else {
                            nonEnhancedTagLibClasses << taglibClass
                        }
                    }
                }
                beans.registerBeans(event.ctx)

                // The tag library lookup class caches "tag -> taglib class"
                // so we need to update it now.
                def lookup = event.ctx.getBean("gspTagLibraryLookup")
                lookup.registerTagLib(taglibClass)
            }
        }

        event.manager?.getGrailsPlugin("groovyPages")?.doWithDynamicMethods(event.ctx)

        // clear uri cache after changes
        event.ctx.getBean("groovyPagesUriService").clear()
    }
}
