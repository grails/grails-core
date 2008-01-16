package org.codehaus.groovy.grails.web.servlet.mvc

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.metaclass.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*
import org.springframework.web.context.request.*
import org.codehaus.groovy.grails.web.servlet.mvc.*
import org.codehaus.groovy.grails.web.servlet.*
import org.springframework.mock.web.*
import org.springframework.validation.*
import org.springframework.web.servlet.*
import org.codehaus.groovy.grails.support.MockApplicationContext;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.codehaus.groovy.runtime.InvokerHelper;
import grails.util.*
import org.springframework.web.context.WebApplicationContext

abstract class AbstractGrailsControllerTests extends GroovyTestCase {
		
	def servletContext
	def webRequest
	MockHttpServletRequest request
	MockHttpServletResponse response
	GroovyClassLoader gcl = new GroovyClassLoader()
    GrailsApplication ga;
	def mockManager
    MockApplicationContext ctx;
    def appCtx;
	def originalHandler
	
	void onSetUp() {
		
	}
	void setUp() {		
		
        super.setUp();
        
        originalHandler = 	GroovySystem.metaClassRegistry.metaClassCreationHandle

		GroovySystem.metaClassRegistry.metaClassCreationHandle = new ExpandoMetaClassCreationHandle();
        

        ctx = new MockApplicationContext();
        onSetUp();
        ga = new DefaultGrailsApplication(gcl.getLoadedClasses(),gcl);
        mockManager = new MockGrailsPluginManager(ga)
        ctx.registerMockBean("manager", mockManager )
        PluginManagerHolder.setPluginManager(mockManager)

		def dependantPluginClasses = []
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CodecsGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ServletsGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.mapping.UrlMappingsGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.mimes.MimeTypesGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.filters.FiltersGrailsPlugin")
        dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.converters.ConvertersGrailsPlugin")        
        def dependentPlugins = dependantPluginClasses.collect { new DefaultGrailsPlugin(it, ga)}

        dependentPlugins.each{ mockManager.registerMockPlugin(it); it.manager = mockManager }
        mockManager.doArtefactConfiguration();
        ctx.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager(new Resource[0]));

        ga.initialise()
        ga.setApplicationContext(ctx);
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, ga);




		

		def springConfig = new WebRuntimeSpringConfiguration(ctx)
		servletContext = new MockServletContext()


        springConfig.servletContext = servletContext


        dependentPlugins*.doWithRuntimeConfiguration(springConfig)
        dependentPlugins.each{ mockManager.registerMockPlugin(it); it.manager = mockManager }

        appCtx = springConfig.getApplicationContext()
        webRequest = GrailsWebUtil.bindMockWebRequest(appCtx)
        request = webRequest.currentRequest
        request.characterEncoding = "utf-8"
        response = webRequest.currentResponse
        dependentPlugins*.doWithApplicationContext(appCtx)
        servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)

		mockManager.applicationContext = appCtx
		servletContext.setAttribute( GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx)
		mockManager.doDynamicMethods()		
		
	}
	
	void tearDown() {
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
        GroovySystem.metaClassRegistry.setMetaClassCreationHandle(originalHandler);

		PluginManagerHolder.setPluginManager(null)

    	originalHandler = null
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