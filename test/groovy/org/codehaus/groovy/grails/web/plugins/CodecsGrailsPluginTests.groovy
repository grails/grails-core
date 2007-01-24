package org.codehaus.groovy.grails.web.plugins

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*

class CodecsGrailsPluginTests extends AbstractGrailsPluginTests {
	
	void onSetUp() {
		gcl.parseClass(
				"""
				class FirstCodec {
				   static def encode = { str -> \"found first encode method for string: \${str}\" }
				   static def decode = { str -> \"found first decode method for string: \${str}\" }			
				}
				""")
		gcl.parseClass(
				"""
				class SecondCodec {
				   static def encode = { str -> \"found second encode method for string: \${str}\" }
				}
				""")
		gcl.parseClass(
				"""
				class ThirdCodec {
				   static def decode = { str -> \"found third decode method for string: \${str}\" }			
				}
				""")

		pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")					
		pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.CodecsGrailsPlugin")					
	}
	
	void testCodecsPlugin() {

	    
		def someString = 'some string'
		
		assert someString.encodeAsFirst() == 'found first encode method for string: some string'
		assert someString.decodeFromFirst() == 'found first decode method for string: some string'
		assert someString.encodeAsSecond() == 'found second encode method for string: some string'
		assert someString.decodeFromThird() == 'found third decode method for string: some string'
			
		shouldFail(MissingMethodException) {
			someString.decodeFromSecond()
		}
		
		shouldFail(MissingMethodException) {
			someString.encodeAsThird()
		}
	}
}