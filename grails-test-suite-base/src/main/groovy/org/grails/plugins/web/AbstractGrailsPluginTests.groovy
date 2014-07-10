package org.grails.plugins.web

import grails.util.Holders
import grails.util.Metadata
import grails.util.MockHttpServletResponse
import grails.web.CamelCaseUrlConverter
import grails.web.UrlConverter

import javax.servlet.ServletContext

import grails.core.DefaultGrailsApplication
import grails.core.GrailsApplication
import org.grails.web.servlet.context.support.GrailsRuntimeConfigurator
import org.grails.web.servlet.context.support.WebRuntimeSpringConfiguration
import org.grails.plugins.DefaultGrailsPlugin
import org.grails.plugins.MockGrailsPluginManager
import org.grails.support.MockApplicationContext
import org.grails.core.io.MockResourceLoader
import grails.web.util.GrailsApplicationAttributes
import org.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.context.ApplicationContext
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockServletContext

abstract class AbstractGrailsPluginTests extends GroovyTestCase {

    ServletContext servletContext
    GrailsWebRequest webRequest
    MockHttpServletRequest request
    MockHttpServletResponse response
    def gcl = new GroovyClassLoader()
    def ga
    def mockManager
    def ctx
    def originalHandler
    def springConfig
    ApplicationContext appCtx
    def pluginsToLoad = []
    def resolver = new PathMatchingResourcePatternResolver()

    protected void onSetUp() {}

    protected final void setUp() {
        super.setUp()

        ExpandoMetaClass.enableGlobally()

        ctx = new MockApplicationContext()
        onSetUp()
        ga = new DefaultGrailsApplication(gcl.getLoadedClasses(),gcl)
        ga.metadata[Metadata.APPLICATION_NAME] = getClass().name
        ctx.registerMockBean UrlConverter.BEAN_NAME, new CamelCaseUrlConverter()
        mockManager = new MockGrailsPluginManager(ga)
        def dependentPlugins = pluginsToLoad.collect { new DefaultGrailsPlugin(it, ga)}
        dependentPlugins.each{ mockManager.registerMockPlugin(it); it.manager = mockManager }
        mockManager.doArtefactConfiguration()
        ga.initialise()
        ga.setApplicationContext(ctx)
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, ga)
        ctx.registerMockBean(GrailsApplication.CLASS_LOADER_BEAN, gcl)

        ctx.registerMockBean("manager", mockManager)

        def configurator = new GrailsRuntimeConfigurator(ga, ctx)
        configurator.pluginManager = mockManager
        ctx.registerMockBean(GrailsRuntimeConfigurator.BEAN_ID, configurator)

        springConfig = new WebRuntimeSpringConfiguration(ctx)
        servletContext = new MockServletContext(new MockResourceLoader())
        springConfig.servletContext = servletContext
        mockManager.registerProvidedArtefacts(ga)
        dependentPlugins*.doWithRuntimeConfiguration(springConfig)

        ga.mainContext = springConfig.getUnrefreshedApplicationContext()
        appCtx = springConfig.getApplicationContext()
        
        mockManager.applicationContext = appCtx
        servletContext.setAttribute(GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx)
        dependentPlugins*.doWithDynamicMethods(appCtx)
        dependentPlugins*.doWithApplicationContext(appCtx)
    }

    protected final void tearDown() {
        ga.mainContext.close()
        pluginsToLoad = []
        ExpandoMetaClass.disableGlobally()
        Holders.setPluginManager null
    }
}
