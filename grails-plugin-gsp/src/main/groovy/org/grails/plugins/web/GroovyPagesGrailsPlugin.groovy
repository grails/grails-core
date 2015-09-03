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
package org.grails.plugins.web
import grails.config.Config
import grails.core.GrailsClass
import grails.core.GrailsTagLibClass
import grails.gsp.PageRenderer
import grails.plugins.Plugin
import grails.util.BuildSettings
import grails.util.Environment
import grails.util.GrailsUtil
import grails.util.Metadata
import grails.web.pages.GroovyPagesUriService
import groovy.transform.CompileStatic
import groovy.util.logging.Commons
import org.grails.buffer.StreamCharBufferMetaUtils
import org.grails.core.artefact.TagLibArtefactHandler
import org.grails.gsp.GroovyPageResourceLoader
import org.grails.gsp.GroovyPagesTemplateEngine
import org.grails.gsp.io.CachingGroovyPageStaticResourceLocator
import org.grails.gsp.jsp.TagLibraryResolverImpl
import org.grails.plugins.web.taglib.*
import org.grails.spring.RuntimeSpringConfiguration
import org.grails.taglib.TagLibraryLookup
import org.grails.taglib.TagLibraryMetaUtils
import org.grails.web.errors.ErrorsViewStackTracePrinter
import org.grails.web.gsp.GroovyPagesTemplateRenderer
import org.grails.web.gsp.io.CachingGrailsConventionGroovyPageLocator
import org.grails.web.pages.DefaultGroovyPagesUriService
import org.grails.web.pages.FilteringCodecsByContentTypeSettings
import org.grails.web.pages.GroovyPagesServlet
import org.grails.web.servlet.view.GroovyPageViewResolver
import org.grails.web.sitemesh.GroovyPageLayoutFinder
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.beans.factory.config.PropertiesFactoryBean
import org.springframework.boot.context.embedded.ServletRegistrationBean
import org.springframework.context.ApplicationContext
import org.springframework.core.io.Resource
import org.springframework.util.ClassUtils
import org.springframework.web.servlet.view.InternalResourceViewResolver
/**
 * Sets up and configures the GSP and GSP tag library support in Grails.
 *
 * @author Graeme Rocher
 * @since 1.1
 */
@Commons
class GroovyPagesGrailsPlugin extends Plugin {

    public static final String GSP_RELOAD_INTERVAL = "grails.gsp.reload.interval"
    public static final String GSP_VIEWS_DIR = 'grails.gsp.view.dir'
    public static final String GSP_VIEW_LAYOUT_RESOLVER_ENABLED = 'grails.gsp.view.layoutViewResolver'
    public static final String SITEMESH_DEFAULT_LAYOUT = 'grails.sitemesh.default.layout'
    public static final String SITEMESH_ENABLE_NONGSP = 'grails.sitemesh.enable.nongsp'

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


    /**
     * Clear the page cache with the ApplicationContext is loaded
     */
    @CompileStatic
    @Override
    void doWithApplicationContext() {
        applicationContext.getBean("groovyPagesTemplateEngine", GroovyPagesTemplateEngine).clearPageCache()
    }

    /**
     * Configures the various Spring beans required by GSP
     */
    Closure doWithSpring() {{->
        def application = grailsApplication
        Config config = application.config
        boolean developmentMode = isDevelopmentMode()
        Environment env = Environment.current

        boolean enableReload = env.isReloadEnabled() ||
                                config.getProperty(GroovyPagesTemplateEngine.CONFIG_PROPERTY_GSP_ENABLE_RELOAD, Boolean, false) ||
                                    (developmentMode && env == Environment.DEVELOPMENT)

        boolean warDeployed = application.warDeployed
        boolean warDeployedWithReload = warDeployed && enableReload

        long gspCacheTimeout = config.getProperty(GSP_RELOAD_INTERVAL, Long,  (developmentMode && env == Environment.DEVELOPMENT) ? 0L : 5000L)
        boolean enableCacheResources = !config.getProperty(GroovyPagesTemplateEngine.CONFIG_PROPERTY_DISABLE_CACHING_RESOURCES, Boolean, false)
        String viewsDir = config.getProperty(GSP_VIEWS_DIR, '')
        def disableLayoutViewResolver = config.getProperty(GSP_VIEW_LAYOUT_RESOLVER_ENABLED, Boolean, true)
        String defaultDecoratorNameSetting = config.getProperty(SITEMESH_DEFAULT_LAYOUT, '')
        def sitemeshEnableNonGspViews = config.getProperty(SITEMESH_ENABLE_NONGSP, Boolean, false)



        RuntimeSpringConfiguration spring = springConfig



        // resolves JSP tag libraries
        jspTagLibraryResolver(TagLibraryResolverImpl)
        // resolves GSP tag libraries
        gspTagLibraryLookup(TagLibraryLookup)


        boolean customResourceLoader = false
        // If the development environment is used we need to load GSP files relative to the base directory
        // as oppose to in WAR deployment where views are loaded from /WEB-INF

        if (viewsDir) {
            log.info "Configuring GSP views directory as '${viewsDir}'"
            customResourceLoader = true
            groovyPageResourceLoader(GroovyPageResourceLoader) {
                baseResource = "file:${viewsDir}"
            }
        }
        else {
            if (developmentMode) {
                customResourceLoader = true
                groovyPageResourceLoader(GroovyPageResourceLoader) { bean ->
                    bean.lazyInit = true
                    def location = GroovyPagesGrailsPlugin.transformToValidLocation(BuildSettings.BASE_DIR.absolutePath)
                    baseResource = "file:$location"
                }
            }
            else {
                if (warDeployedWithReload && env.hasReloadLocation()) {
                    customResourceLoader = true
                    groovyPageResourceLoader(GroovyPageResourceLoader) {
                        def location = GroovyPagesGrailsPlugin.transformToValidLocation(env.reloadLocation)
                        baseResource = "file:${location}"
                    }
                }
            }
        }

        def deployed = !Metadata.getCurrent().isDevelopmentEnvironmentAvailable()
        groovyPageLocator(CachingGrailsConventionGroovyPageLocator) { bean ->
            bean.lazyInit = true
            if (customResourceLoader) {
                resourceLoader = groovyPageResourceLoader
            }
            if (deployed) {
                def context = grailsApplication?.mainContext
                def allViewsProperties = context?.getResources("classpath*:gsp/views.properties")
                allViewsProperties = allViewsProperties?.findAll { Resource r ->
                    def p = r.URL.path
                    if(warDeployed && p.contains('/WEB-INF/classes')) {
                        return true
                    }
                    else if(!warDeployed && !p.contains("!/lib")) {
                        return true
                    }

                    return false
                }
                precompiledGspMap = { PropertiesFactoryBean pfb ->
                    ignoreResourceNotFound = true
                    locations = allViewsProperties ? allViewsProperties as Resource[] : 'classpath:gsp/views.properties'
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
            defaultDecoratorName = defaultDecoratorNameSetting ?: null
            enableNonGspViews = sitemeshEnableNonGspViews
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

        if(disableLayoutViewResolver) {
            grailsLayoutViewResolverPostProcessor(GrailsLayoutViewResolverPostProcessor)
        }

        final pluginManager = manager
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
        filteringCodecsByContentTypeSettings(FilteringCodecsByContentTypeSettings, application)

        groovyPagesServlet(ServletRegistrationBean, new GroovyPagesServlet(), "*.gsp") {
            if(Environment.isDevelopmentMode()) {
                initParameters = [showSource:"1"]
            }
        }
    }}

    protected boolean isDevelopmentMode() {
        Metadata.getCurrent().isDevelopmentEnvironmentAvailable()
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
    @Override
    void doWithDynamicMethods() {
        StreamCharBufferMetaUtils.registerStreamCharBufferMetaClass()
        TagLibraryLookup gspTagLibraryLookup = applicationContext.getBean('gspTagLibraryLookup',TagLibraryLookup)

        for(GrailsClass cls in grailsApplication.getArtefacts(TagLibArtefactHandler.TYPE)) {
            TagLibraryMetaUtils.enhanceTagLibMetaClass((GrailsTagLibClass)cls, gspTagLibraryLookup)
        }
    }

    @Override
    void onChange(Map<String, Object> event) {
        def application = grailsApplication
        def ctx = applicationContext

        if (application.isArtefactOfType(TagLibArtefactHandler.TYPE, event.source)) {
            GrailsTagLibClass taglibClass = (GrailsTagLibClass)application.addArtefact(TagLibArtefactHandler.TYPE, event.source)
            if (taglibClass) {
                // replace tag library bean
                def beanName = taglibClass.fullName
                beans {
                    "$beanName"(taglibClass.clazz) { bean ->
                        bean.autowire = true
                    }
                }

                // The tag library lookup class caches "tag -> taglib class"
                // so we need to update it now.
                def lookup = applicationContext.getBean('gspTagLibraryLookup', TagLibraryLookup)
                lookup.registerTagLib(taglibClass)
                TagLibraryMetaUtils.enhanceTagLibMetaClass(taglibClass, lookup)
            }
        }
        // clear uri cache after changes
        ctx.getBean('groovyPagesUriService',GroovyPagesUriService).clear()
    }

    @CompileStatic
    void onConfigChange(Map<String, Object> event) {
        applicationContext.getBean('filteringCodecsByContentTypeSettings', FilteringCodecsByContentTypeSettings).initialize(grailsApplication)
    }

}
