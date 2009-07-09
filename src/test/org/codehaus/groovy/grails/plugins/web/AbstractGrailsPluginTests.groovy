package org.codehaus.groovy.grails.plugins.web;

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
import org.springframework.context.ApplicationContext
import javax.servlet.ServletContext
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest

abstract class AbstractGrailsPluginTests extends GroovyTestCase {

	ServletContext servletContext
	GrailsWebRequest webRequest
	MockHttpServletRequest request
	MockHttpServletResponse response
	def gcl = new GroovyClassLoader()
    def ga;
	def mockManager
    def ctx;	
	def originalHandler
	def springConfig
	ApplicationContext appCtx
	def pluginsToLoad = []
	def resolver = new PathMatchingResourcePatternResolver()
	
	void onSetUp() {
	}

	final void setUp() {		
		
        super.setUp();
        
		ExpandoMetaClass.enableGlobally()
        

        ctx = new MockApplicationContext();
        onSetUp();
        ga = new DefaultGrailsApplication(gcl.getLoadedClasses(),gcl);
        mockManager = new MockGrailsPluginManager(ga)
        def dependentPlugins = pluginsToLoad.collect { new DefaultGrailsPlugin(it, ga)}
        dependentPlugins.each{ mockManager.registerMockPlugin(it); it.manager = mockManager }
        mockManager.doArtefactConfiguration();
        ga.initialise()
        ApplicationHolder.application = ga
        ga.setApplicationContext(ctx);
        ctx.registerMockBean(GrailsApplication.APPLICATION_ID, ga);
        ctx.registerMockBean(GrailsRuntimeConfigurator.CLASS_LOADER_BEAN, gcl)
        ctx.registerMockBean(PluginMetaManager.BEAN_ID, new DefaultPluginMetaManager());

        ctx.registerMockBean("manager", mockManager )

        def configurator = new GrailsRuntimeConfigurator(ga)
        configurator.pluginManager = mockManager
        ctx.registerMockBean(GrailsRuntimeConfigurator.BEAN_ID, configurator )

        springConfig = new WebRuntimeSpringConfiguration(ctx)
        servletContext = new MockServletContext(new MockResourceLoader())
        springConfig.servletContext = servletContext
        mockManager.registerProvidedArtefacts(ga) 
        dependentPlugins*.doWithRuntimeConfiguration(springConfig)


		appCtx = springConfig.getApplicationContext()
		mockManager.applicationContext = appCtx
		servletContext.setAttribute( GrailsApplicationAttributes.APPLICATION_CONTEXT, appCtx)
		dependentPlugins*.doWithDynamicMethods(appCtx)
		dependentPlugins*.doWithApplicationContext(appCtx)

	}
	
	final void tearDown() {
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
    	springConfig = null
    	resolver = null
		
		ExpandoMetaClass.disableGlobally()

    	originalHandler = null

	}
}