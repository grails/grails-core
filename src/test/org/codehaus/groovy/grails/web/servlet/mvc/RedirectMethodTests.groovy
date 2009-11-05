/**
 * Tests the behaviour of the redirect method
 
 * @author Graeme Rocher
 * @since 1.0
  *
 * Created: Sep 13, 2007
 * Time: 10:10:45 AM
 * 
 */
package org.codehaus.groovy.grails.web.servlet.mvc

import org.springframework.beans.MutablePropertyValues

class RedirectMethodTests extends AbstractGrailsControllerTests {

    void onSetUp() {
        gcl.parseClass('''
class RedirectController {

    def redirectTwice = {

        redirect(action:'one')
        redirect(action:'two')
    }
    def responseCommitted = {
        response.outputStream << "write data"
        response.outputStream.flush()
        redirect(action:'one')
    }
    def toAction = {
        redirect(action:'foo')
    }
    def toRoot = {
        redirect(controller:'default')
    }
    def toController = {
        redirect(controller:'test')
    }
    def toControllerAndAction = {
        redirect(controller:'test', action:'foo')
    }

    def toControllerAndActionWithFragment = {
        redirect(controller:'test', action:'foo', fragment:"frag")
    }

    def toControllerWithParams = {
        redirect(controller:'test',action:'foo', params:[one:'two', two:'three'])
    }
    def toControllerWithDuplicateParams = {
        redirect(controller:'test',action:'foo', params:[one:['two','three']])
    }
    def toControllerWithDuplicateArrayParams = {
        redirect(controller:'test',action:'foo', params:[one:['two','three'] as String[]])
    }

}

class NewsSignupController {
    def testNoController = {
        redirect(action: 'thankyou')
    }

    def thankyou = {
    }
}

class UrlMappings {
    static mappings = {
        "/"(controller:'default')
        "/little-brown-bottle/$action?" {
        	controller = "newsSignup"
        }

        "/$controller/$action?/$id?"{
	        constraints {
		        // apply constraints here
		    }
	    }
	}
}
        ''')
    }

    void testRedirectEventListeners() {
        def fired = false
        def callable = { fired = true }

        ctx.registerMockBean("testRedirect", callable)
        def pv = new MutablePropertyValues()
        pv.addPropertyValue("callable", callable)
        appCtx.registerSingleton("testDirect",TestRedirectListener, pv )

        def c = ga.getControllerClass("RedirectController").newInstance()
        webRequest.controllerName = 'redirect'

        c.toAction.call()

        assert fired : "redirect event should have been fired"
    }
    void testRedirectAlreadyCalledException() {

        def c = ga.getControllerClass("RedirectController").newInstance()
        webRequest.controllerName = 'redirect'


        try {
            c.redirectTwice.call()
            fail "should have thrown an exception"
        }
        catch (org.codehaus.groovy.grails.web.servlet.mvc.exceptions.CannotRedirectException e) {
            assert e.message == "Cannot issue a redirect(..) here. A previous call to redirect(..) has already redirected the response." : "incorrect error message for response redirect"
        }

    }

    void testRedirectWhenResponseCommitted() {
        def c = ga.getControllerClass("RedirectController").newInstance()
        webRequest.controllerName = 'redirect'


        try {
            c.responseCommitted.call()
            fail "should have thrown an exception"
        }
        catch (org.codehaus.groovy.grails.web.servlet.mvc.exceptions.CannotRedirectException e) {
            assert e.message == "Cannot issue a redirect(..) here. The response has already been committed either by another redirect or by directly writing to the response." : "incorrect error message for response redirect when already written to"
        }

    }

    void testRedirectToRoot() {

       def c = ga.getControllerClass("RedirectController").newInstance()
        webRequest.controllerName = 'redirect'
        c.toRoot.call()
        assertEquals "/", response.redirectedUrl
    }
    void testRedirectWithFragment() {
       def c = ga.getControllerClass("RedirectController").newInstance()
        webRequest.controllerName = 'redirect'
        c.toControllerAndActionWithFragment.call()
        assertEquals "/test/foo#frag", response.redirectedUrl
    }

    void testRedirectToAction() {
        def c = ga.getControllerClass("RedirectController").newInstance()
        webRequest.controllerName = 'redirect'
        c.toAction.call()
        assertEquals "/redirect/foo", response.redirectedUrl
    }

    void testRedirectToController() {
        def c = ga.getControllerClass("RedirectController").newInstance()
        webRequest.controllerName = 'redirect'
        c.toController.call()
        assertEquals "/test", response.redirectedUrl
    }

    void testRedirectToControllerAndAction() {
        def c = ga.getControllerClass("RedirectController").newInstance()
        webRequest.controllerName = 'redirect'
        c.toControllerAndAction.call()
        assertEquals "/test/foo", response.redirectedUrl

    }

    void testRedirectToControllerWithParams() {
        def c = ga.getControllerClass("RedirectController").newInstance()
        webRequest.controllerName = 'redirect'
        c.toControllerWithParams.call()
        assertEquals "/test/foo?one=two&two=three", response.redirectedUrl

    }

    void testRedirectToControllerWithDuplicateParams() {
        def c = ga.getControllerClass("RedirectController").newInstance()
        webRequest.controllerName = 'redirect'
        c.toControllerWithDuplicateParams.call()
        assertEquals "/test/foo?one=two&one=three", response.redirectedUrl

    }

    void testRedirectToControllerWithDuplicateArrayParams() {
        def c = ga.getControllerClass("RedirectController").newInstance()
        webRequest.controllerName = 'redirect'
        c.toControllerWithDuplicateArrayParams.call()
        assertEquals "/test/foo?one=two&one=three", response.redirectedUrl

    }

    void testRedirectToActionWithMapping() {
        def c = ga.getControllerClass("NewsSignupController").newInstance()
        webRequest.controllerName = 'newsSignup'
        c.testNoController.call()
        assertEquals "/little-brown-bottle/thankyou", response.redirectedUrl
    }
}
class TestRedirectListener implements RedirectEventListener  {

    def callable
    public void responseRedirected(String url) {
        callable(url)
    }
}
