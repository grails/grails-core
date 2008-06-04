package org.codehaus.groovy.grails.web.taglib

import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.TagLibArtefactHandler
import org.codehaus.groovy.grails.commons.spring.DefaultRuntimeSpringConfiguration
import org.codehaus.groovy.grails.plugins.DefaultGrailsPlugin
import org.codehaus.groovy.grails.plugins.DefaultPluginMetaManager
import org.codehaus.groovy.grails.plugins.MockGrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginMetaManager
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.runtime.InvokerHelper
import org.springframework.context.ApplicationContextAware
import org.springframework.context.support.StaticMessageSource
import org.springframework.core.io.Resource
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.servlet.DispatcherServlet
import org.springframework.web.servlet.i18n.AcceptHeaderLocaleResolver;
import org.codehaus.groovy.grails.commons.ApplicationHolder
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse

abstract class AbstractGrailsTagTests extends GroovyTestCase {



	MockServletContext servletContext
	def webRequest
	MockHttpServletRequest request
	MockHttpServletResponse response
	def ctx
	def originalHandler
	def appCtx
	def ga
	def mockManager
	def gcl = new GroovyClassLoader()

	GrailsApplication grailsApplication;
	StaticMessageSource messageSource;


    def withConfig(String text, Closure callable) {
        def config = new ConfigSlurper().parse(text)
        try {
            ConfigurationHolder.config = config
            callable()

        }finally {
            ConfigurationHolder.config = null            
        }
    }

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
			if(go instanceof ApplicationContextAware) {
				go.applicationContext = appCtx
			}
	        def webRequest = RequestContextHolder.currentRequestAttributes()

	        webRequest.out = out
	        println "calling tag '${tagName}'"
	        result = callable.call(go.getProperty(tagName))
		}
		return result
	}

    void onSetUp() {

    }
    
    void setUp() throws Exception {
        originalHandler = 	GroovySystem.metaClassRegistry.metaClassCreationHandle

		GroovySystem.metaClassRegistry.metaClassCreationHandle = new ExpandoMetaClassCreationHandle();
        onSetUp()
        grailsApplication = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        ga = grailsApplication
        grailsApplication.initialise()
        ApplicationHolder.application = ga
        mockManager = new MockGrailsPluginManager(grailsApplication)
        mockManager.registerProvidedArtefacts(grailsApplication)
        onInit()



		def mockControllerClass = gcl.parseClass("class MockController {  def index = {} } ")
        ctx = new MockApplicationContext();


        grailsApplication.setApplicationContext(ctx);

        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, grailsApplication);
        ctx.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager(new Resource[0]));
                


        grailsApplication.addArtefact(ControllerArtefactHandler.TYPE, mockControllerClass)

		
		messageSource = new StaticMessageSource()
		ctx.registerMockBean("manager", mockManager )
		ctx.registerMockBean("messageSource", messageSource )
		ctx.registerMockBean("grailsApplication",grailsApplication)
				
		def dependantPluginClasses = []
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.CodecsGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ServletsGrailsPlugin")
	    dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.mapping.UrlMappingsGrailsPlugin")		
		dependantPluginClasses << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin")


		
		def dependentPlugins = dependantPluginClasses.collect { new DefaultGrailsPlugin(it, grailsApplication)}

		dependentPlugins.each{ mockManager.registerMockPlugin(it); it.manager = mockManager }
		mockManager.registerProvidedArtefacts(grailsApplication)
		def springConfig = new WebRuntimeSpringConfiguration(ctx)
        webRequest = GrailsWebUtil.bindMockWebRequest()

        servletContext =  webRequest.servletContext

		springConfig.servletContext = servletContext

		dependentPlugins*.doWithRuntimeConfiguration(springConfig)

		appCtx = springConfig.getApplicationContext()

		servletContext.setAttribute(WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE, appCtx)
		mockManager.applicationContext = appCtx
		servletContext.setAttribute( GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx)
		GroovySystem.metaClassRegistry.removeMetaClass(String.class)
		GroovySystem.metaClassRegistry.removeMetaClass(Object.class)
	    grailsApplication.tagLibClasses.each { tc -> GroovySystem.metaClassRegistry.removeMetaClass(tc.clazz)}
		mockManager.doDynamicMethods()
        request = webRequest.currentRequest
        request.characterEncoding = "utf-8"
        response = webRequest.currentResponse
		
		assert appCtx.grailsUrlMappingsHolder
    }
    
    void tearDown() {
        // Clear the page cache in the template engine since it's
        // static and likely to cause tests to interfere with each
        // other.
        appCtx.groovyPagesTemplateEngine.clearPageCache()

        RequestContextHolder.setRequestAttributes(null)
		GroovySystem.metaClassRegistry.setMetaClassCreationHandle(originalHandler);
    	
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

    void printCompiledSource(template, params = [:]) {
        def text =  getCompiledSource(template, params)
        println "----- GSP SOURCE -----"
        println text

    }

    def getCompiledSource(template, params = [:]) {
        def engine = appCtx.groovyPagesTemplateEngine

        assert engine
        def t = engine.createTemplate(template, "test_"+ System.currentTimeMillis())

        def w = t.make(params)
        w.showSource = true

        def sw = new StringWriter()
        def out = new PrintWriter(sw)
        webRequest.out = out
        w.writeTo(out)

        String text = sw.toString()
    }

    def assertCompiledSourceContains(expected, template, params = [:]) {
        def text =  getCompiledSource(template, params)
        return text.indexOf(expected) > -1        
    }

    void assertOutputContains(expected, template, params = [:]) {
        def result = applyTemplate(template, params)
        assert result.indexOf(expected) > -1
    }

    /**
     * Compares the output generated by a template with a string.
     * @param expected The string that the template output is expected
     * to match.
     * @param template The template to run.
     * @param params A map of variables to pass to the template - by
     * default an empty map is used.
     * @param transform A closure that is passed a StringWriter instance
     * containing the output generated by the template. It is the result
     * of this transformation that is actually compared with the expected
     * string. The default transform simply converts the contents of the
     * writer to a string.
     */
    void assertOutputEquals(expected, template, params = [:], Closure transform = { it.toString() }) {

        def engine = appCtx.groovyPagesTemplateEngine

        assert engine
        def t = engine.createTemplate(template, "test_"+ System.currentTimeMillis())

        def w = t.make(params)

        def sw = new StringWriter()
        def out = new PrintWriter(sw)
        webRequest.out = out
        w.writeTo(out)

        assertEquals expected, transform(sw)
    }	

	def applyTemplate(template, params = [:] ) {

        def engine = appCtx.groovyPagesTemplateEngine

        assert engine
        def t = engine.createTemplate(template, "test_"+ System.currentTimeMillis())

        def w = t.make(params)

        def sw = new StringWriter()
        def out = new PrintWriter(sw)
        webRequest.out = out
        w.writeTo(out)

        return sw.toString()
    }
}