package org.codehaus.groovy.grails.web.plugins

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*

class EncodersGrailsPluginTests extends AbstractGrailsMockTests {
	
	
	void onSetUp() {
		gcl.parseClass(
				"""
				class FooEncoder {

				   def encode = { str -> 'found encode method' }
				   def decode = { str -> 'found decode method' }			
				}
				""")
	}
	
	void testEncodersPlugin() {
		def pluginClass = gcl.loadClass("org.codehaus.groovy.grails.plugins.EncodersGrailsPlugin")
		
		def plugin = new DefaultGrailsPlugin(pluginClass, ga)
		
		def springConfig = new DefaultRuntimeSpringConfiguration(ctx)
		springConfig.servletContext = createMockServletContext()

		def appCtx = springConfig.getApplicationContext()
		plugin.doWithDynamicMethods(appCtx)

		def someString = 'some string'
		def encoded = someString.encodeAsFoo()
		assert 'found encode method' == encoded
		def decoded = someString.decodeAsFoo()
		assert 'found decode method' == decoded
	}
}