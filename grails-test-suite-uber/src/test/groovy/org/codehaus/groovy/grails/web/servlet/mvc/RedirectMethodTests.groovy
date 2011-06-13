package org.codehaus.groovy.grails.web.servlet.mvc

import java.util.Collection;

import org.codehaus.groovy.grails.plugins.web.api.ControllersApi
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.CannotRedirectException
import org.springframework.beans.MutablePropertyValues
import org.codehaus.groovy.grails.web.servlet.HttpHeaders

/**
 * Tests the behaviour of the redirect method.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class RedirectMethodTests extends AbstractGrailsControllerTests {

    protected void onSetUp() {
        gcl.parseClass('''
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
    
    @Override
    protected Collection<Class> getControllerClasses() {
        [NewsSignupController, RedirectController, AController, ABCController]
    }

    void testRedirectToDefaultActionOfAnotherController() {
        def c = new NewsSignupController()
        webRequest.controllerName = 'newsSignup'
        c.redirectToDefaultAction.call()
        assertEquals "/redirect/toAction", response.redirectedUrl
    }

    void testRedirectEventListeners() {
        def fired = false
        def callable = { fired = true }

        ctx.registerMockBean("testRedirect", callable)
        def pv = new MutablePropertyValues()
        pv.addPropertyValue("callable", callable)
        ControllersApi api = appCtx.getBean("instanceControllersApi")

        api.setRedirectListeners([new TestRedirectListener(callable: callable)])

        def c = new RedirectController()
        webRequest.controllerName = 'redirect'

        c.toAction.call()

        assert fired : "redirect event should have been fired"
    }

    void testRedirectAlreadyCalledException() {

        def c = new RedirectController()
        webRequest.controllerName = 'redirect'

        String message = shouldFail(CannotRedirectException) {
            c.redirectTwice.call()
        }
        assertEquals "incorrect error message for response redirect",
            "Cannot issue a redirect(..) here. A previous call to redirect(..) has already redirected the response.",
            message
    }

    void testRedirectWhenResponseCommitted() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'

        String message = shouldFail(CannotRedirectException) {
            c.responseCommitted.call()
        }
        assertEquals "incorrect error message for response redirect when already written to",
            "Cannot issue a redirect(..) here. The response has already been committed either by another redirect or by directly writing to the response.",
            message
    }

    void testRedirectToRoot() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        c.toRoot.call()
        assertEquals "/", response.redirectedUrl
    }

    void testRedirectWithFragment() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        c.toControllerAndActionWithFragment.call()
        assertEquals "/test/foo#frag", response.redirectedUrl
    }

    void testRedirectInControllerWithOneLetterClassName() {
        def c = new AController()
        webRequest.controllerName = 'a'
        c.index.call()
        assertEquals "/a/list", response.redirectedUrl
    }

    void testRedirectInControllerWithAllUpperCaseClassName() {
        def c = new ABCController()
        webRequest.controllerName = 'ABC'
        c.index.call()
        assertEquals "/ABC/list", response.redirectedUrl
    }

    void testRedirectToAction() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        c.toAction.call()
        assertEquals "/redirect/foo", response.redirectedUrl
    }

    void testRedirectToActionWithGstring() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        c.toActionWithGstring.call()
        assertEquals "/redirect/foo", response.redirectedUrl
    }

    void testRedirectToController() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        c.toController.call()
        assertEquals "/test", response.redirectedUrl
    }

    void testRedirectToControllerAndAction() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        c.toControllerAndAction.call()
        assertEquals "/test/foo", response.redirectedUrl
    }

    void testRedirectToControllerWithParams() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        c.toControllerWithParams.call()
        assertEquals "/test/foo?one=two&two=three", response.redirectedUrl
    }

    void testRedirectToControllerWithDuplicateParams() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        c.toControllerWithDuplicateParams.call()
        assertEquals "/test/foo?one=two&one=three", response.redirectedUrl
    }

    void testRedirectToControllerWithDuplicateArrayParams() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        c.toControllerWithDuplicateArrayParams.call()
        assertEquals "/test/foo?one=two&one=three", response.redirectedUrl
    }

    void testRedirectToActionWithMapping() {
        def c = new NewsSignupController()
        c = new NewsSignupController()
        webRequest.controllerName = 'newsSignup'
        c.testNoController.call()
        assertEquals "/little-brown-bottle/thankyou", response.redirectedUrl
    }

    void testPermanentRedirect() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        webRequest.currentRequest.serverPort = 8080
        c.toActionPermanent.call()

        // location header should be absolute
        assertEquals "http://localhost:8080/redirect/foo", response.getHeader(HttpHeaders.LOCATION)
        assertEquals 301, response.status
    }

}

class TestRedirectListener implements RedirectEventListener {

    def callable

    void responseRedirected(String url) {
        callable(url)
    }
}

class ABCController {
    def index = { redirect action: 'list' }
}
class AController {
    def index = { redirect action: 'list' }
}
class RedirectController {

    static defaultAction = 'toAction'

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

    def toActionPermanent = {
        redirect(action:'foo', permanent: true)
    }

    def toActionWithGstring = {
        def prefix = 'f'
        redirect(action:"${prefix}oo")
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

    static defaultAction = "thankyou"

    def testNoController = {
        redirect(action: 'thankyou')
    }

    def redirectToDefaultAction = {
        redirect(controller:"redirect")
    }

    def thankyou = {
    }
}
