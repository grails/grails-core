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

abstract class AbstractGrailsControllerTests extends GroovyTestCase {
		
	def servletContext
	def webRequest
	def request
	def response
	GroovyClassLoader gcl = new GroovyClassLoader()
    def ga;
	def mockManager
    MockApplicationContext ctx;
    def appCtx;
	def originalHandler
	
	void onSetUp() {
		
	}
	void setUp() {		
		
        super.setUp();
        
        originalHandler = 	InvokerHelper.getInstance()
								.getMetaRegistry()
								.metaClassCreationHandle

		InvokerHelper.getInstance()
						.getMetaRegistry()
						.metaClassCreationHandle = new ExpandoMetaClassCreationHandle();
        

        ctx = new MockApplicationContext();
        onSetUp();
        ga = new DefaultGrailsApplication(gcl.getLoadedClasses(),gcl);
        
        ga.setApplicationContext(ctx);
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, ga);
		
		mockManager = new MockGrailsPluginManager(ga)
		ctx.registerMockBean("manager", mockManager )
		PluginManagerHolder.setPluginManager(mockManager)
		
		def dependantPluginClasses = []
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")					
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin")

		
		def dependentPlugins = dependantPluginClasses.collect { new DefaultGrailsPlugin(it, ga)}
		def springConfig = new DefaultRuntimeSpringConfiguration(ctx)
        webRequest = GrailsWebUtil.bindMockWebRequest()
        request = webRequest.currentRequest
        response = webRequest.currentResponse

		servletContext =  webRequest.servletContext
		springConfig.servletContext = servletContext

		
		dependentPlugins*.doWithRuntimeConfiguration(springConfig)
		dependentPlugins.each{ mockManager.registerMockPlugin(it); it.manager = mockManager }
			
		appCtx = springConfig.getApplicationContext()

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

		RequestContextHolder.setRequestAttributes(null)
        GroovySystem.metaClassRegistry.setMetaClassCreationHandle(originalHandler);
		
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