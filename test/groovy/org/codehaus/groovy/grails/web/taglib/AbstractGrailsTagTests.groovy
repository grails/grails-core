package org.codehaus.groovy.grails.web.taglib;

import org.codehaus.groovy.grails.support.*
import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*
import org.springframework.web.context.request.*
import org.codehaus.groovy.grails.web.servlet.mvc.*
import org.codehaus.groovy.grails.web.servlet.*
import org.springframework.mock.web.*
import org.springframework.validation.*
import org.springframework.web.servlet.*
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;

abstract class AbstractGrailsTagTests extends
AbstractDependencyInjectionSpringContextTests {
		
	
	public AbstractGrailsTagTests() {
		dependencyCheck = false		
	}
	
	protected String[] getConfigLocations() {
        return [ "org/codehaus/groovy/grails/web/taglib/grails-taglib-tests.xml" ] as String[]
    }
	
	def servletContext
	def webRequest
	def request
	def response
	def ctx
	
	GrailsApplication grailsApplication;
	MessageSource messageSource;

	
	GroovyClassLoader gcl = new GroovyClassLoader()
	
	def withTag(String tagName, Writer out, Closure callable) {
		def result = null
		runTest {
			def mockController = grailsApplication.getController("MockController").newInstance()
			
	        request.setAttribute(GrailsApplicationAttributes.CONTROLLER, mockController);
	        request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());
	        request.setAttribute(GrailsApplicationAttributes.CONTROLLER, mockController);
	        
	        def go = grailsApplication.getTagLibClassForTag(tagName).newInstance()
	        def webRequest = RequestContextHolder.currentRequestAttributes()
	        webRequest.out = out
	        result = callable.call(go.getProperty(tagName))
		}
		return result
	}
	
    protected final void onSetUp() throws Exception {
		def mockControllerClass = gcl.parseClass("class MockController {  def index = {} } ")
        ctx = new MockApplicationContext();
        grailsApplication.addControllerClass(mockControllerClass)
        
        grailsApplication.setApplicationContext(ctx);
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, grailsApplication);
		def mockManager = new MockGrailsPluginManager(grailsApplication)
		
		messageSource = new StaticMessageSource()
		ctx.registerMockBean("manager", mockManager )
		ctx.registerMockBean("messageSource", messageSource )
				
		def dependantPluginClasses = []
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")					
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.i18n.plugins.I18nGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.web.plugins.ControllersGrailsPlugin")

		
		def dependentPlugins = dependantPluginClasses.collect { new DefaultGrailsPlugin(it, grailsApplication)}
		def springConfig = new DefaultRuntimeSpringConfiguration(ctx)
		servletContext =  createMockServletContext()
		springConfig.servletContext = servletContext		
		
		dependentPlugins*.doWithRuntimeConfiguration(springConfig)
		dependentPlugins.each{ mockManager.registerMockPlugin(it); it.manager = mockManager }
			
		def appCtx = springConfig.getApplicationContext()
		mockManager.applicationContext = appCtx
		servletContext.setAttribute( GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx)
		mockManager.doDynamicMethods()
        
    }
    

	protected void onInit() {
		
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
	
	void runTest(Closure callable) {
		
		try {
			request = new MockHttpServletRequest()
			response =new MockHttpServletResponse()
			
			webRequest = new GrailsWebRequest(
					request,
					response,
					servletContext
			)			
			RequestContextHolder.setRequestAttributes( webRequest )		
			callable()
		}
		finally {
			RequestContextHolder.setRequestAttributes(null)	
		}
		
	}
	
}