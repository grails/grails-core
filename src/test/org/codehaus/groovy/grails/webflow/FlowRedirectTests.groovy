package org.codehaus.groovy.grails.webflow

import grails.util.MockHttpServletResponse 
import org.codehaus.groovy.grails.web.servlet.GrailsApplicationAttributes 
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest 
import org.codehaus.groovy.grails.webflow.support.AbstractGrailsTagAwareFlowExecutionTests
import org.springframework.mock.web.MockHttpServletRequest 
import org.springframework.mock.web.MockServletContext 
import org.springframework.web.context.request.RequestContextHolder 
import org.springframework.webflow.definition.FlowDefinition

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class FlowRedirectTests extends AbstractGrailsTagAwareFlowExecutionTests {

    void testRedirectToControllerAndAction() {
        startFlow()

        def context = signalEvent("test1")
        assertFlowExecutionEnded()
        assertEquals "contextRelative:/test/foo",context.getExternalRedirectUrl()
    }

    void testRedirectToControllerAndActionWithParamsObjectAccess() {

        webRequest.params.id = "1"
        startFlow()
        def context = signalEvent("test2")
        assertFlowExecutionEnded()
        assertEquals "contextRelative:/test/foo/1",context.getExternalRedirectUrl()
    }
	
	void testRedirectToActionWithoutSpecifyingController() {
		def webRequest = new GrailsWebRequest(
				new MockHttpServletRequest(), new MockHttpServletResponse(), new MockServletContext())
		webRequest.currentRequest.setAttribute GrailsApplicationAttributes.CONTROLLER_NAME_ATTRIBUTE, 'mycontroller'
		RequestContextHolder.requestAttributes = webRequest
		try {
			startFlow()
			def context = signalEvent("test3")
			assertFlowExecutionEnded()
			assertEquals "contextRelative:/mycontroller/foo",context.getExternalRedirectUrl()
		} finally {
			RequestContextHolder.requestAttributes = null
		}
	}
	
    Map params = [id:10] // this should not be resolved

    Closure getFlowClosure() {
        return {
            one {
                on("test1").to "test1"
                on("test2").to "test2"
                on("test3").to "test3"
            }
            test1 {
                redirect(controller:"test", action:"foo")
            }
            test2 {
                redirect(controller:"test", action:"foo", id:params.id)
            }
            test3 {
            	redirect(action:"foo")
            }
        }
    }
}
