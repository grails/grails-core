package org.codehaus.groovy.grails.plugins;

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.support.*
import org.codehaus.groovy.grails.commons.metaclass.*
import org.codehaus.groovy.grails.plugins.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.springframework.mock.web.*
import org.springframework.core.io.support.PathMatchingResourcePatternResolver
import org.springframework.core.io.Resource
import org.codehaus.groovy.runtime.*
import org.codehaus.groovy.grails.web.servlet.*

abstract class AbstractGrailsPluginTests extends GroovyTestCase {

	def servletContext
	def webRequest
	def request
	def response
	GroovyClassLoader gcl = new GroovyClassLoader()
    def ga;
	def mockManager
    MockApplicationContext ctx;	
	def originalHandler
	def springConfig
	def appCtx
	def pluginsToLoad = []
	def resolver = new PathMatchingResourcePatternResolver()
	
	void onSetup() {
	}

	final void setUp() {		
		
        super.setUp();
        
        originalHandler = InvokerHelper.getInstance()
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

        def configurator = new GrailsRuntimeConfigurator(ga)
        configurator.pluginManager = mockManager
        ctx.registerMockBean(GrailsRuntimeConfigurator.BEAN_ID, configurator )

		
		def dependentPlugins = pluginsToLoad.collect { new DefaultGrailsPlugin(it, ga)}
		springConfig = new DefaultRuntimeSpringConfiguration(ctx)
		servletContext = new MockServletContext(new MockResourceLoader())
		springConfig.servletContext = servletContext		
		
		dependentPlugins*.doWithRuntimeConfiguration(springConfig)
		dependentPlugins.each{ mockManager.registerMockPlugin(it); it.manager = mockManager }
			
		appCtx = springConfig.getApplicationContext()
		mockManager.applicationContext = appCtx
		servletContext.setAttribute( GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx)
		dependentPlugins*.doWithApplicationContext(appCtx)
		dependentPlugins*.doWithDynamicMethods(appCtx)		
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
		pluginsToLoad = []
		appCtx = null
		
		InvokerHelper.getInstance()
		.getMetaRegistry()
		.setMetaClassCreationHandle(originalHandler);
	}
}