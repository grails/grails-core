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
		}
		""")
	}
	
	
	void testConstructHelper() {
		runTest {
			def webRequest = RequestContextHolder.currentRequestAttributes()
			def helper = new SimpleGrailsControllerHelper(ga, ctx, servletContext)
		}
	}
	
	void testConfigureStateForParameterMapUri() {
		def helper = new SimpleGrailsControllerHelper(null, null, null)
		helper.configureStateForUri("/controller/action/id/test/param/one/two/three/four")
		
		assertEquals "controller", helper.@controllerName
		assertEquals "action", helper.@actionName
		assertEquals "id", helper.@id
		
		def extraParams = helper.@extraParams
		assertEquals "param", extraParams["test"]
		assertEquals "two", extraParams["one"]
		assertEquals "four", extraParams["three"]			                                
	}

	void testConfigureStateForUri2() {
		def helper = new SimpleGrailsControllerHelper(null, null, null)
		helper.configureStateForUri("/location/show/united-kingdom")

		assertEquals "location", helper.@controllerName
		assertEquals "show", helper.@actionName
		assertEquals "united-kingdom", helper.@id

    }

	void testConfigureStateForUri3() {
		def helper = new SimpleGrailsControllerHelper(null, null, null)
		helper.configureStateForUri("/location/show/united-kingdom/london")

		assertEquals "location", helper.@controllerName
		assertEquals "show", helper.@actionName
		assertEquals "united-kingdom", helper.@id

		assertTrue helper.@extraParams.containsKey("london")

    }

	void testCommandObjectArg() {
/*		runTest {
		  Class commandObject = gcl.parseClass(
				"""
				class MyFooObject {
				   def someAttribute	
                   static def validate = {}		
				}
				""")
		  Class controllerClass = gcl.parseClass(
				"""
				class MyController {
                   def errors
                   def theCommandObj
				   def list = { MyFooObject cmo ->
						theCommandObj = cmo
                   }			
				}
				""")
		  def helper = new SimpleGrailsControllerHelper(null, null, null)
		  def controller = controllerClass.newInstance()
          helper.handleAction(controller, controller.list, request, null, null)
		  assertNotNull controller.theCommandObj
		  assertNull controller.errors
		} */
    }

	void testConfigureStateForUri() {
		def helper = new SimpleGrailsControllerHelper(null, null, null)
		helper.configureStateForUri("/controller/action/id")

		assertEquals "controller", helper.@controllerName
		assertEquals "action", helper.@actionName
		assertEquals "id", helper.@id	
	}

	void testCheckDispatchAction() {
		runTest { 
			request.addParameter("_action_Edit", "Some label for editing")
			
			def helper = new SimpleGrailsControllerHelper(null, null, null)
			helper.@controllerName = "controller"
			
			def uri = helper.checkDispatchAction(request, "/controller/book")
			
			assertEquals "edit", helper.@actionName
			assertEquals "/controller/edit", uri
		}
	}
	
	// in case of submit image, .x is automatically appended by browser
	void testCheckDispatchActionX() {
		runTest { 
			request.addParameter("_action_Edit.x", "Some label for editing")
			
			def helper = new SimpleGrailsControllerHelper(null, null, null)
			helper.@controllerName = "controller"
			
			def uri = helper.checkDispatchAction(request, "/controller/book")
			
			assertEquals "edit", helper.@actionName
			assertEquals "/controller/edit", uri
		}
	}
	
	void testCheckDispatchActionWithoutActionParamater() {
		runTest { 
			def helper = new SimpleGrailsControllerHelper(null, null, null)
			def uri = helper.checkDispatchAction(request, "/controller/book")
			
			assertNull helper.@actionName
			assertEquals "/controller/book", uri
		}
	}
}