package org.codehaus.groovy.grails.servlet.mvc

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

class SimpleGrailsControllerHelperTests extends AbstractGrailsControllerTests {
	
	
	void onSetUp() {
				gcl.parseClass(
		"""
		class TestController {
		   def list = {}			
		}
		""")
	}
	
	void testSomething() {
		runTest {
			def webRequest = RequestContextHolder.currentRequestAttributes()
			def helper = new SimpleGrailsControllerHelper(ga, ctx, servletContext)
		}
	}
}