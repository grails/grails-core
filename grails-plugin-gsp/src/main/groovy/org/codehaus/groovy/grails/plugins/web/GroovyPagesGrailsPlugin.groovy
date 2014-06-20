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

import grails.core.GrailsApplication
import grails.core.GrailsClass
import grails.core.GrailsTagLibClass
import grails.gsp.PageRenderer
import grails.util.BuildSettings
import grails.util.BuildSettingsHolder
import grails.util.Environment
import grails.util.GrailsUtil
import groovy.transform.CompileStatic
import org.grails.core.artefact.TagLibArtefactHandler
import org.grails.spring.RuntimeSpringConfiguration
import org.codehaus.groovy.grails.plugins.GrailsPluginManager
import org.codehaus.groovy.grails.plugins.GrailsPluginUtils
import org.codehaus.groovy.grails.plugins.PluginManagerAware
import grails.core.support.GrailsApplicationAware
import org.codehaus.groovy.grails.plugins.web.api.ControllerTagLibraryApi
import org.codehaus.groovy.grails.plugins.web.api.TagLibraryApi
import org.codehaus.groovy.grails.plugins.web.taglib.*
import org.grails.web.servlet.context.GrailsConfigUtils
import org.grails.web.errors.ErrorsViewStackTracePrinter
import org.codehaus.groovy.grails.web.filters.JavascriptLibraryHandlerInterceptor
import org.codehaus.groovy.grails.web.pages.*
import org.codehaus.groovy.grails.web.pages.discovery.CachingGrailsConventionGroovyPageLocator
import org.codehaus.groovy.grails.web.pages.discovery.CachingGroovyPageStaticResourceLocator
import org.codehaus.groovy.grails.web.pages.ext.jsp.TagLibraryResolverImpl
import grails.web.util.GrailsApplicationAttributes
import org.codehaus.groovy.grails.web.servlet.view.GroovyPageViewResolver
import org.codehaus.groovy.grails.web.sitemesh.GroovyPageLayoutFinder
import org.codehaus.groovy.grails.web.util.StreamCharBufferMetaUtils
import org.codehaus.groovy.grails.web.util.TagLibraryMetaUtils
import org.grails.web.pages.DefaultGroovyPagesUriService
import org.grails.web.pages.FilteringCodecsByContentTypeSettings
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.boot.context.embedded.ServletContextInitializer
import org.springframework.context.ApplicationContext
import org.springframework.util.ClassUtils
import org.springframework.web.servlet.view.InternalResourceViewResolver

import javax.servlet.ServletContext
import javax.servlet.ServletException

/**
 * Sets up and configures the GSP and GSP tag library support in Grails.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
class GroovyPagesGrailsPlugin implements ServletContextInitializer, GrailsApplicationAware, PluginManagerAware{

    def watchedResources = ["file:./plugins/*/grails-app/taglib/**/*TagLib.groovy",
                            "file:./grails-app/taglib/**/*TagLib.groovy"]

    def version = GrailsUtil.getGrailsVersion()
    def dependsOn = [core: version, i18n: version]
    def observe = ['controllers']
    def loadAfter = ['filters']

    def providedArtefacts = [
        ApplicationTagLib,
        CountryTagLib,
        FormatTagLib,
        FormTagLib,
        JavascriptTagLib,
        RenderTagLib,
        UrlMappingTagLib,
        ValidationTagLib,
        PluginTagLib,
        SitemeshTagLib
    ]

    GrailsApplication grailsApplication
    GrailsPluginManager pluginManager

    /**
     * Clear the page cache with the ApplicationContext is loaded
     */
    @CompileStatic
    def doWithApplicationContext(ApplicationContext ctx) {
        ctx.getBean("groovyPagesTemplateEngine", GroovyPagesTemplateEngine).clearPageCache()
    }

    /**
     * Configures the various Spring beans required by GSP
     */
    def doWithSpring = {
        RuntimeSpringConfiguration spring = springConfig
        def application = grailsApplication
        // resolves JSP tag libraries
        jspTagLibraryResolver(TagLibraryResolverImpl)
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

        boolean customResourceLoader = false
        // If the development environment is used we need to load GSP files relative to the base directory
        // as oppose to in WAR deployment where views are loaded from /WEB-INF
        def viewsDir = application.config.grails.gsp.view.dir
        if (viewsDir) {
            log.info "Configuring GSP views directory as '${viewsDir}'"
            customResourceLoader = true
            groovyPageResourceLoader(GroovyPageResourceLoader) {
                baseResource = "file:${viewsDir}"
                pluginSettings = GrailsPluginUtils.getPluginBuildSettings()
            }
        }
        else {
            if (developmentMode) {
                customResourceLoader = true
                groovyPageResourceLoader(GroovyPageResourceLoader) { bean ->
                    bean.lazyInit = true
                    BuildSettings settings = BuildSettingsHolder.settings
                    def location = settings?.baseDir ? GroovyPagesGrailsPlugin.transformToValidLocation(settings.baseDir.absolutePath) : '.'
                    baseResource = "file:$location"
                    pluginSettings = GrailsPluginUtils.getPluginBuildSettings()
                }
            }
            else {
                if (warDeployedWithReload && env.hasReloadLocation()) {
                    customResourceLoader = true
                    groovyPageResourceLoader(GroovyPageResourceLoader) {
                        def location = GroovyPagesGrailsPlugin.transformToValidLocation(env.reloadLocation)
                        baseResource = "file:${location}"
                        pluginSettings = GrailsPluginUtils.getPluginBuildSettings()
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

        spring.addAlias('groovyTemplateEngine', 'groovyPagesTemplateEngine')

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
        }

        // Setup the GroovyPagesUriService
        groovyPagesUriService(DefaultGroovyPagesUriService) { bean ->
            bean.lazyInit = true
        }
        
        boolean jstlPresent = ClassUtils.isPresent(
            "javax.servlet.jsp.jstl.core.Config", InternalResourceViewResolver.class.getClassLoader())
        
        abstractViewResolver {
            prefix = GrailsApplicationAttributes.PATH_TO_VIEWS
            suffix = jstlPresent ? GroovyPageViewResolver.JSP_SUFFIX : GroovyPageViewResolver.GSP_SUFFIX
            templateEngine = groovyPagesTemplateEngine
            groovyPageLocator = groovyPageLocator
            if (enableReload) {
                cacheTimeout = gspCacheTimeout
            }
        }
        // Configure a Spring MVC view resolver
        jspViewResolver(GroovyPageViewResolver) { bean ->
            bean.lazyInit = true
            bean.parent = "abstractViewResolver"
        }
        
        // "grails.gsp.view.layoutViewResolver=false" can be used to disable GrailsLayoutViewResolver
        // containsKey check must be made to check existence of boolean false values in ConfigObject
        if(!(application.config.grails.gsp.view.containsKey('layoutViewResolver') && application.config.grails.gsp.view.layoutViewResolver==false)) {
            grailsLayoutViewResolverPostProcessor(GrailsLayoutViewResolverPostProcessor)
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

        javascriptLibraryHandlerInterceptor(JavascriptLibraryHandlerInterceptor, ref('grailsApplication'))

        filteringCodecsByContentTypeSettings(FilteringCodecsByContentTypeSettings, ref('grailsApplication'))
    }

    static String transformToValidLocation(String location) {
        if (location == '.') return location
        if (!location.endsWith(File.separator)) return "${location}${File.separator}"
        return location
    }


    /**
     * Sets up dynamic methods required by the GSP implementation including dynamic tag method dispatch
     */
    @CompileStatic
    def doWithDynamicMethods(ApplicationContext ctx) {
        StreamCharBufferMetaUtils.registerStreamCharBufferMetaClass()

        TagLibraryLookup gspTagLibraryLookup = ctx.getBean('gspTagLibraryLookup',TagLibraryLookup)

        for(GrailsClass cls in grailsApplication.getArtefacts(TagLibArtefactHandler.TYPE)) {
            TagLibraryMetaUtils.enhanceTagLibMetaClass((GrailsTagLibClass)cls, gspTagLibraryLookup)
        }
    }

    def onChange = { event ->
        def application = grailsApplication
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
                def lookup = event.ctx.gspTagLibraryLookup
                lookup.registerTagLib(taglibClass)

                TagLibraryMetaUtils.enhanceTagLibMetaClass(taglibClass, ctx.gspTagLibraryLookup)
            }
        }
        // clear uri cache after changes
        ctx.groovyPagesUriService.clear()
    }

    def onConfigChange = { event ->
        def ctx = event.ctx ?: application.mainContext
        ctx.filteringCodecsByContentTypeSettings.initialize(application)
    }

    @Override
    @CompileStatic
    void onStartup(ServletContext servletContext) throws ServletException {
        def gspServlet = servletContext.addServlet("gsp", GroovyPagesServlet)
        gspServlet.addMapping("*.gsp")
        if(Environment.isDevelopmentMode()) {
            gspServlet.setInitParameter("showSource", "1")
        }
    }
}
