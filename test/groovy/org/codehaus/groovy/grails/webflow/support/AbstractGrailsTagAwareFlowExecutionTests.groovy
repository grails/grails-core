/**
 * Class description here.
 
 * @author Graeme Rocher
 * @since 0.4
  *
 * Created: Sep 7, 2007
 * Time: 8:26:46 AM
 * 
 */
package org.codehaus.groovy.grails.webflow.support

import grails.util.GrailsWebUtil
import org.codehaus.groovy.grails.commons.ControllerArtefactHandler
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.codehaus.groovy.grails.commons.spring.DefaultRuntimeSpringConfiguration
import org.codehaus.groovy.grails.plugins.DefaultGrailsPlugin
import org.codehaus.groovy.grails.plugins.DefaultPluginMetaManager
import org.codehaus.groovy.grails.plugins.MockGrailsPluginManager
import org.codehaus.groovy.grails.plugins.PluginMetaManager
import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes
import org.codehaus.groovy.runtime.InvokerHelper
import org.springframework.context.support.StaticMessageSource
import org.springframework.core.io.Resource
import org.springframework.web.context.WebApplicationContext
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.webflow.test.execution.AbstractFlowExecutionTests
import org.codehaus.groovy.grails.commons.spring.WebRuntimeSpringConfiguration

abstract class AbstractGrailsTagAwareFlowExecutionTests extends AbstractFlowExecutionTests {
    
    def servletContext
    def webRequest
    def request
    def response
    def ctx
    def originalHandler
    def appCtx
    def ga
    def mockManager
    def gcl = new GroovyClassLoader()

    GrailsApplication grailsApplication;
    StaticMessageSource messageSource;


    final void setUp() throws Exception {
        originalHandler = 	GroovySystem.metaClassRegistry.metaClassCreationHandle

		GroovySystem.metaClassRegistry.metaClassCreationHandle = new ExpandoMetaClassCreationHandle();

        grailsApplication = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        ga = grailsApplication
        grailsApplication.initialise()
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

    final void tearDown() {
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
    
}