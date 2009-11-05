package org.codehaus.groovy.grails.web.servlet.mvc

import grails.test.GrailsUnitTestCase
import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration
import org.codehaus.groovy.grails.plugins.*
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.springframework.context.support.StaticMessageSource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.web.pages.DefaultGroovyPagesUriService
import org.codehaus.groovy.grails.web.pages.GroovyPagesUriService
import org.codehaus.groovy.grails.commons.spring.GrailsApplicationContext
import org.springframework.context.ApplicationContext

abstract class AbstractGrailsControllerTests extends GrailsUnitTestCase {

    def servletContext
    GrailsWebRequest webRequest
    MockHttpServletRequest request
    MockHttpServletResponse response
    GroovyClassLoader gcl = new GroovyClassLoader(this.getClass().classLoader)
    GrailsApplication ga;
    def mockManager
    MockApplicationContext ctx;
    ApplicationContext appCtx;
    def originalHandler

    protected void onSetUp() {
    }

    protected void setUp() {
        super.setUp();

        ExpandoMetaClass.enableGlobally()


        GroovySystem.metaClassRegistry.metaClassCreationHandle = new ExpandoMetaClassCreationHandle();

        ctx = new MockApplicationContext();
        onSetUp();
        ga = new DefaultGrailsApplication(gcl.getLoadedClasses(), gcl);
        mockManager = new MockGrailsPluginManager(ga)
        ctx.registerMockBean("manager", mockManager)
        PluginManagerHolder.setPluginManager(mockManager)

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
        mockManager.doArtefactConfiguration();
        ctx.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager());

        ga.initialise()
        ga.setApplicationContext(ctx);
        ApplicationHolder.application = ga

        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, ga);
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

        webRequest = GrailsWebUtil.bindMockWebRequest(appCtx)
        request = webRequest.currentRequest
        request.characterEncoding = "utf-8"
        response = webRequest.currentResponse        
    }

    protected void tearDown() {
        servletContext = null
        webRequest = null
        request = null
        response = null
        gcl = null
        ga = null
        mockManager = null
        ctx = null
        appCtx = null

        RequestContextHolder.setRequestAttributes(null)
        ExpandoMetaClass.disableGlobally()

        ApplicationHolder.application = null
        PluginManagerHolder.setPluginManager(null)

        originalHandler = null

        super.tearDown()
    }



    void runTest(Closure callable) {
        callable.call()
	}

	protected MockServletContext createMockServletContext() {
		return new MockServletContext();
	}

	protected MockApplicationContext createMockApplicationContext() {
		return new MockApplicationContext();
	}

	protected Resource[] getResources(String pattern) throws IOException {
		return new PathMatchingResourcePatternResolver().getResources(pattern);
	}

}
