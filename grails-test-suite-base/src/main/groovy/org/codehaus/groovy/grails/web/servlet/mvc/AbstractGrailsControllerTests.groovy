package org.codehaus.groovy.grails.web.servlet.mvc

import grails.util.GrailsWebUtil
import grails.util.GrailsNameUtils
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.pages.DefaultGroovyPagesUriService
import org.codehaus.groovy.grails.web.pages.GroovyPagesUriService
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.context.ApplicationContext
import org.springframework.context.support.StaticMessageSource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.plugins.*
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.web.mime.MimeType

import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletRequest
import org.codehaus.groovy.grails.plugins.testing.GrailsMockHttpServletResponse

abstract class AbstractGrailsControllerTests extends GroovyTestCase {

    def servletContext
    GrailsWebRequest webRequest
    MockHttpServletRequest request
    MockHttpServletResponse response
    GroovyClassLoader gcl = new GroovyClassLoader(getClass().classLoader)
    GrailsApplication ga
    def mockManager
    MockApplicationContext ctx
    ApplicationContext appCtx
    def originalHandler

    protected void onSetUp() {}

    protected void setUp() {
        super.setUp()

        ExpandoMetaClass.enableGlobally()

        GroovySystem.metaClassRegistry.metaClassCreationHandle = new ExpandoMetaClassCreationHandle()

        ctx = new MockApplicationContext()
        onSetUp()
        ga = new DefaultGrailsApplication(gcl.getLoadedClasses(), gcl)
        mockManager = new MockGrailsPluginManager(ga)
        ctx.registerMockBean("manager", mockManager)
        def dependantPluginClasses = []
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CodecsGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ServletsGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.mapping.UrlMappingsGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.GroovyPagesGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.mimes.MimeTypesGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.filters.FiltersGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.converters.ConvertersGrailsPlugin")
        def dependentPlugins = dependantPluginClasses.collect { new DefaultGrailsPlugin(it, ga)}

        dependentPlugins.each { mockManager.registerMockPlugin(it); it.manager = mockManager }
        mockManager.doArtefactConfiguration()
        ctx.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager())

        ga.initialise()
        ga.setApplicationContext(ctx)

        ctx.registerMockBean("pluginManager", mockManager)
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, ga)
        ctx.registerMockBean("messageSource", new StaticMessageSource())
        ctx.registerMockBean(GroovyPagesUriService.BEAN_ID, new DefaultGroovyPagesUriService())

        def springConfig = new WebRuntimeSpringConfiguration(ctx)
        servletContext = ctx.getServletContext()

        springConfig.servletContext = servletContext

        dependentPlugins*.doWithRuntimeConfiguration(springConfig)
        dependentPlugins.each { mockManager.registerMockPlugin(it); it.manager = mockManager }

        appCtx = springConfig.getApplicationContext()

        dependentPlugins*.doWithApplicationContext(appCtx)
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)
        servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx)
        mockManager.applicationContext = appCtx
        mockManager.doDynamicMethods()

        request = new GrailsMockHttpServletRequest()
        request.characterEncoding = "utf-8"
        response = new GrailsMockHttpServletResponse()
        webRequest = GrailsWebUtil.bindMockWebRequest(appCtx, request, response)
    }

    protected setCurrentController(controller) {
        RequestContextHolder.requestAttributes.controllerName = GrailsNameUtils.getLogicalName(controller.class.name, "Controller")
    }

    protected void tearDown() {
        RequestContextHolder.setRequestAttributes(null)
        ExpandoMetaClass.disableGlobally()

        ConfigurationHolder.config = null
        MimeType.reset()
        ApplicationHolder.application = null
        PluginManagerHolder.setPluginManager(null)

        super.tearDown()
    }


    def withConfig(String text, Closure callable) {
        def config = new ConfigSlurper().parse(text)
        try {
            buildMockRequest(config)
            callable()
        }
        finally {
            RequestContextHolder.setRequestAttributes(null)

        }
    }

    GrailsWebRequest buildMockRequest(ConfigObject config) throws Exception {
        def appCtx = new MockApplicationContext()
        appCtx.registerMockBean(GroovyPagesUriService.BEAN_ID, new DefaultGroovyPagesUriService())

        ga.config = config

        appCtx.registerMockBean(GrailsApplication.APPLICATION_ID, ga)
        appCtx.getServletContext().setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx)
        appCtx.getServletContext().setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)
        return GrailsWebUtil.bindMockWebRequest(appCtx)
    }

    void runTest(Closure callable) {
        callable.call()
    }

    protected MockServletContext createMockServletContext() { new MockServletContext() }

    protected MockApplicationContext createMockApplicationContext() { new MockApplicationContext() }

    protected Resource[] getResources(String pattern) {
        new PathMatchingResourcePatternResolver().getResources(pattern)
    }
}
