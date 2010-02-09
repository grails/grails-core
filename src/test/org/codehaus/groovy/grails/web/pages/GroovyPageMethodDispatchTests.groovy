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
import org.codehaus.groovy.grails.web.util.GrailsPrintWriter

class GroovyPageMethodDispatchTests extends AbstractGrailsControllerTests {
	
	void onSetUp() {
		gcl.parseClass(
"""
import org.codehaus.groovy.grails.web.taglib.*
import org.codehaus.groovy.grails.web.pages.*

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
	def tag3 = { attrs, body ->
				out << body() }
}
class MyPage extends org.codehaus.groovy.grails.web.pages.GroovyPage {
    String getGroovyPageFileName() { "test" }
	def run() {
		invokeTag("tag1", [attr1:"test"]) {
		    out << "foo"
		    ""
		}
		def tagResult=tag3([:], new GroovyPage.ConstantClosure('TEST'))?.toString()
		if(tagResult != 'TEST') {
				out << '<ERROR in tag3 output>' << tagResult
		}
		out << "hello" + tag2(test:"test2", new GroovyPageTagBody(this, webRequest, {

        }))
	}
}
""")
	}

	void testGroovyPage() {
		runTest {
			def webRequest = RequestContextHolder.currentRequestAttributes()
			GroovyPage script = gcl.loadClass("MyPage").newInstance()
            script.setJspTagLibraryResolver(appCtx.getBean('jspTagLibraryResolver'))
            script.setGspTagLibraryLookup(appCtx.getBean('gspTagLibraryLookup'))
			def controller = ga.getControllerClass("TestController").newInstance()
			def sw = new StringWriter()
			webRequest.out =  new GrailsPrintWriter(sw)
			def b = new GroovyPageBinding(application:controller.servletContext,
								request:controller.request,
								response:controller.response,
								flash:controller.flash,
								out: webRequest.out ,
								webRequest:webRequest)			
			script.binding = b
			script.initRun(webRequest.out, webRequest)
			script.run()
			
			assertEquals "printblahfoohellotest2",sw.toString()
			
			
		}
	}
	
}

