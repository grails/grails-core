package org.codehaus.groovy.grails.web.plugins

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*

class EncodersGrailsPluginTests extends AbstractGrailsMockTests {
	
	
	void onSetUp() {
		gcl.parseClass(
				"""
				class FirstEncoder {
				   def encode = { str -> 'found first encode method' }
				   def decode = { str -> 'found first decode method' }			
				}
				""")
		gcl.parseClass(
				"""
				class SecondEncoder {
				   def encode = { str -> 'found second encode method' }
				}
				""")
		gcl.parseClass(
				"""
				class ThirdEncoder {
				   def decode = { str -> 'found third decode method' }			
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
		
		assert someString.encodeAsFirst() == 'found first encode method'
		assert someString.decodeAsFirst() == 'found first decode method'
		assert someString.encodeAsSecond() == 'found second encode method'
		assert someString.decodeAsThird() == 'found third decode method'
	}
}