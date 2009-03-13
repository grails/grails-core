package org.codehaus.groovy.grails.web.servlet.mvc

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*
import org.springframework.web.context.request.*
import org.codehaus.groovy.grails.web.servlet.mvc.*
import org.codehaus.groovy.grails.web.servlet.*
import org.springframework.mock.web.*
import org.springframework.validation.*
import org.springframework.web.servlet.*

class TagLibDynamicMethodsTests extends AbstractGrailsControllerTests {
	
	
	void onSetUp() {
				gcl.parseClass(
		"""
		class TestTagLib {
		   def myTag = {attrs, body -> body() }			
		}
		""")
	}
	
	void testFlashObject() {
		runTest {
			def testTagLib = ga.getTagLibClass("TestTagLib").newInstance()
			testTagLib.flash.test = "hello"
			
			assertEquals "hello", testTagLib.flash.test			
		}		
	}
	
	void testParamsObject() {
		runTest {
			def testTagLib = ga.getTagLibClass("TestTagLib").newInstance()
			testTagLib.params.test = "hello"
			
			assertEquals "hello", testTagLib.params.test
						
		}		
	}
	
	void testSessionObject() {
		runTest {
			def testTagLib = ga.getTagLibClass("TestTagLib").newInstance()
			testTagLib.session.test = "hello"
			
			assertEquals "hello", testTagLib.session.test			
		}
	}
	
	void testGrailsAttributesObject() {
		runTest {
			def testTagLib = ga.getTagLibClass("TestTagLib").newInstance()
		 	assertNotNull(testTagLib.grailsAttributes)			
		}		
	}
	
	void testRequestObjects() {
		runTest {
			def testTagLib = ga.getTagLibClass("TestTagLib").newInstance()
			
			assertNotNull(testTagLib.request)
			
			assertNotNull(testTagLib.response)		
			assertNotNull(testTagLib.servletContext)
		}
	}

}