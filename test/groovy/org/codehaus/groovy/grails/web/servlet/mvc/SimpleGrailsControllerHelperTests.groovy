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

class SimpleGrailsControllerHelperTests extends AbstractGrailsControllerTests {
	
	
	void onSetUp() {
				gcl.parseClass(
		"""
		class TestController {
		   def list = {}

		   def afterInterceptor = {
		        it.put("after", "value")
		   }
		}
		""")
		gcl.parseClass(
		"""
		class Test2Controller {
		   def list = {}

		   def afterInterceptor = { model ->
		        model.put("after", "value")
		        return "not a boolean"
		   }
		}
		""")
		gcl.parseClass(
		"""
		class Test3Controller {
		   def list = {}

		   def afterInterceptor = { model, modelAndView ->
		        model.put("after", modelAndView.getViewName())
		        return true
		   }
		}
		""")
		gcl.parseClass(
		"""
		class Test4Controller {
		   def list = {}

		   def afterInterceptor = { model, modelAndView ->
		        return false
		   }
		}
		""")
	}
	
	
	void testConstructHelper() {
		runTest {
			def webRequest = RequestContextHolder.currentRequestAttributes()
			def helper = new SimpleGrailsControllerHelper(ga, ctx, servletContext)
		}
	}
	

	void testCallsAfterInterceptorWithModel(){
        runTest {
			def helper = new SimpleGrailsControllerHelper(ga, appCtx , servletContext)
			def mv = helper.handleURI("/test/list", webRequest)
			assert mv.getModel()["after"] == "value"
		}
    }

    void testCallsAfterInterceptorWithModelAndExplicitParam(){
        runTest {
			def helper = new SimpleGrailsControllerHelper(ga, appCtx , servletContext)
			def mv = helper.handleURI("/test2/list", webRequest)
			assert mv.getModel()["after"] == "value"
		}
    }

     void testCallsAfterInterceptorWithModelAndViewExplicitParams(){
        runTest {
			def helper = new SimpleGrailsControllerHelper(ga, appCtx , servletContext)
			def mv = helper.handleURI("/test3/list", webRequest)
			assert mv.getModel()["after"] == "/test3/list"
		}
    }

    void testReturnsNullIfAfterInterceptorReturnsFalse(){
          runTest {
			def helper = new SimpleGrailsControllerHelper(ga, appCtx , servletContext)
			def mv = helper.handleURI("/test4/list", webRequest)
			assert mv == null
		}
    }
}