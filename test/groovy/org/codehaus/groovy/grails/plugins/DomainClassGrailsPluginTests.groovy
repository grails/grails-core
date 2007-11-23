package org.codehaus.groovy.grails.plugins;

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*


class DomainClassGrailsPluginTests extends AbstractGrailsMockTests {

	
	void onSetUp() {
		gcl.parseClass(
"""
class Test {
   Long id
   Long version			
}
""")
	}
	
	void testDomainClassesPlugin() {
		def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin")
		
		def plugin = new DefaultGrailsPlugin(pluginClass, ga)
		
		def springConfig = new WebRuntimeSpringConfiguration(ctx)
		springConfig.servletContext = createMockServletContext()
		
		plugin.doWithRuntimeConfiguration(springConfig)
		
		def appCtx = springConfig.getApplicationContext()
		
		assert appCtx.containsBean("TestDomainClass")
	}
}