package org.codehaus.groovy.grails.plugins

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*
import org.codehaus.groovy.runtime.*

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
        def registry = InvokerHelper.getInstance().getMetaRegistry()

        registry.removeMetaClass(String.class)                
	    
		def someString = 'some string'
		
		assert someString.encodeAsFirst() == 'found first encode method for string: some string'
		assert someString.decodeFirst() == 'found first decode method for string: some string'
		assert someString.encodeAsSecond() == 'found second encode method for string: some string'
		assert someString.decodeThird() == 'found third decode method for string: some string'
			
		shouldFail(MissingMethodException) {
			someString.decodeSecond()
		}
		
		shouldFail(MissingMethodException) {
			someString.encodeAsThird()
		}
	}
}