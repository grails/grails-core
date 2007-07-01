package org.codehaus.groovy.grails.web.taglib;

import org.codehaus.groovy.grails.support.*
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
import org.springframework.test.AbstractDependencyInjectionSpringContextTests;
import org.springframework.web.servlet.DispatcherServlet;
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.context.MessageSource;
import org.springframework.context.support.StaticMessageSource;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler
import grails.util.*

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
	def originalHandler
	def appCtx
	def ga
	def mockManager

	GrailsApplication grailsApplication;
	MessageSource messageSource;


	GroovyClassLoader gcl = new GroovyClassLoader()

	def withTag(String tagName, Writer out, Closure callable) {
		def result = null
		runTest {
			def mockController = grailsApplication.getControllerClass("MockController").newInstance()

	        request.setAttribute(GrailsApplicationAttributes.CONTROLLER, mockController);
	        request.setAttribute(DispatcherServlet.LOCALE_RESOLVER_ATTRIBUTE, new AcceptHeaderLocaleResolver());

	        def tagLibrary = grailsApplication.getArtefactForFeature(TagLibArtefactHandler.TYPE, "g:" + tagName)
            if(!tagLibrary) {
	            fail("No tag library found for tag $tagName")
            }
	        def go = tagLibrary.newInstance()
	        if(go.properties.containsKey("grailsUrlMappingsHolder"))   {
	            go.grailsUrlMappingsHolder = appCtx.grailsUrlMappingsHolder
            }
	        def webRequest = RequestContextHolder.currentRequestAttributes()

	        webRequest.out = out
	        println "calling tag '${tagName}'"
	        result = callable.call(go.getProperty(tagName))
		}
		return result
	}

    protected final void onSetUp() throws Exception {
        originalHandler = 	GroovySystem.metaClassRegistry.metaClassCreationHandle

		GroovySystem.metaClassRegistry.metaClassCreationHandle = new ExpandoMetaClassCreationHandle();


        grailsApplication.initialise()
        ga = grailsApplication
        
        onInit() 

        gcl.loadedClasses.find { it.name.endsWith("TagLib") }.each {
            grailsApplication.addArtefact(TagLibArtefactHandler.TYPE, it)
        }

		def mockControllerClass = gcl.parseClass("class MockController {  def index = {} } ")
        ctx = new MockApplicationContext();


        grailsApplication.setApplicationContext(ctx);
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, grailsApplication);
		mockManager = new MockGrailsPluginManager(grailsApplication)

        grailsApplication.addArtefact(ControllerArtefactHandler.TYPE, mockControllerClass)

		
		messageSource = new StaticMessageSource()
		ctx.registerMockBean("manager", mockManager )
		ctx.registerMockBean("messageSource", messageSource )
				
		def dependantPluginClasses = []
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CodecsGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ServletsGrailsPlugin")
	    dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.mapping.UrlMappingsGrailsPlugin")		
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin")


		
		def dependentPlugins = dependantPluginClasses.collect { new DefaultGrailsPlugin(it, grailsApplication)}
		def springConfig = new DefaultRuntimeSpringConfiguration(ctx)
        webRequest = GrailsWebUtil.bindMockWebRequest()

        servletContext =  webRequest.servletContext

		springConfig.servletContext = servletContext

		dependentPlugins*.doWithRuntimeConfiguration(springConfig)
		dependentPlugins.each{ mockManager.registerMockPlugin(it); it.manager = mockManager }
			
		appCtx = springConfig.getApplicationContext()
		mockManager.applicationContext = appCtx
		servletContext.setAttribute( GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx)
		GroovySystem.metaClassRegistry.removeMetaClass(String.class)
		GroovySystem.metaClassRegistry.removeMetaClass(Object.class)		
		mockManager.doDynamicMethods()
        request = webRequest.currentRequest
        request.characterEncoding = "utf-8"
        response = webRequest.currentResponse
		

		assert appCtx.grailsUrlMappingsHolder
    }
    
    protected final void onTearDown() {
        RequestContextHolder.setRequestAttributes(null)
		InvokerHelper.getInstance()
		.getMetaRegistry()
		.setMetaClassCreationHandle(originalHandler);
    	
        onDestroy()

        servletContext = null
        webRequest = null
        request = null
        response = null
        ctx = null
        originalHandler = null
        appCtx = null
        ga = null
        mockManager = null

        grailsApplication = null
        messageSource = null

        gcl = null
    }

	protected void onInit() {
		
	}	
	
	protected void onDestroy() {
		
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
		  callable.call()
	}
	
}