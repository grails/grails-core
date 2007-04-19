package org.codehaus.groovy.grails.web.pages

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

class GroovyPageMethodDispatchTests extends AbstractGrailsControllerTests {
	
	void onSetUp() {
		gcl.parseClass(
"""
import org.codehaus.groovy.grails.web.taglib.*
class TestController {
	def index = {}
}
class Test1TagLib {
	def tag1 = { attrs, body ->
	   
		out << "print"
 
		def result = tag2(test:'blah')
		out << result

		result = body()

		out << result
	}
}
class Test2TagLib {
	def tag2 = { attrs, body -> out << attrs.test }
}
class MyPage extends org.codehaus.groovy.grails.web.pages.GroovyPage {
	def run() {
		invokeTag("tag1", [attr1:"test"]) {
		    out << "foo"
		    ""
		}
		out << "hello" + tag2(test:"test2", new GroovyPageTagBody(this, webRequest) {

        })
	}
}
""")
	}

	void testGroovyPage() {
		runTest {
			def webRequest = RequestContextHolder.currentRequestAttributes()
			def script = gcl.loadClass("MyPage").newInstance()
			def controller = ga.getControllerClass("TestController").newInstance()
			def sw = new StringWriter()
			webRequest.out =  new PrintWriter(sw)
			def b = new Binding(application:controller.servletContext,
								request:controller.request,
								response:controller.response,
								flash:controller.flash,
								out: webRequest.out ,
								webRequest:webRequest)			
			script.binding = b
			script.run()
			
			assertEquals "printblahfoohellotest2",sw.toString()
			
			
		}
	}
	
}

