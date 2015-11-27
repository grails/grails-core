package org.grails.web.servlet.mvc

import org.grails.web.servlet.mvc.alpha.NamespacedController
import grails.web.http.HttpHeaders
import grails.web.mapping.mvc.exceptions.CannotRedirectException
import org.grails.web.util.GrailsApplicationAttributes
import org.springframework.beans.MutablePropertyValues
import grails.artefact.Artefact
import grails.web.mapping.mvc.RedirectEventListener

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
        "/noNamespace/$action?" {
            controller = 'namespaced'
        }
        "/anotherNoNamespace/$action?" {
            controller = 'anotherNamespaced'
        }

        "/secondaryNamespace/$action?" {
            controller = 'namespaced'
            namespace = 'secondary'
        }
        "/anotherSecondaryNamespace/$action?" {
            controller = 'anotherNamespaced'
            namespace = 'secondary'
        }
    }
}
''')
    }

    @Override
    protected Collection<Class> getControllerClasses() {
        [NewsSignupController,
         RedirectController,
         AController,
         ABCController,
         NamespacedController,
         org.grails.web.servlet.mvc.beta.NamespacedController]
    }

    void testRedirectsWithNamespacedControllers() {
        def primary = new NamespacedController()
        webRequest.controllerName = 'namespaced'
        primary.redirectToSelf()
        assertEquals '/noNamespace/demo', response.redirectedUrl

        request.removeAttribute(GrailsApplicationAttributes.REDIRECT_ISSUED)
        primary.redirectToSecondary()
        assertEquals '/secondaryNamespace/demo', response.redirectedUrl

        def secondary = new org.grails.web.servlet.mvc.beta.NamespacedController()
        request.removeAttribute(GrailsApplicationAttributes.REDIRECT_ISSUED)
        secondary.redirectToPrimary()
        assertEquals '/noNamespace/demo', response.redirectedUrl

        request.removeAttribute(GrailsApplicationAttributes.REDIRECT_ISSUED)
        secondary.redirectToSelfWithImplicitNamespace()
        assertEquals '/secondaryNamespace/demo', response.redirectedUrl

        request.removeAttribute(GrailsApplicationAttributes.REDIRECT_ISSUED)
        secondary.redirectToSelfWithExplicitNamespace()
        assertEquals '/secondaryNamespace/demo', response.redirectedUrl

        request.removeAttribute(GrailsApplicationAttributes.REDIRECT_ISSUED)
        secondary.redirectToAnotherPrimary()
        assertEquals '/anotherNoNamespace/demo', response.redirectedUrl

        request.removeAttribute(GrailsApplicationAttributes.REDIRECT_ISSUED)
        secondary.redirectToAnotherSecondaryWithImplicitNamespace()
        assertEquals '/anotherSecondaryNamespace/demo', response.redirectedUrl

        request.removeAttribute(GrailsApplicationAttributes.REDIRECT_ISSUED)
        secondary.redirectToAnotherSecondaryWithExplicitNamespace()
        assertEquals '/anotherSecondaryNamespace/demo', response.redirectedUrl
    }

    void testRedirectToDefaultActionOfAnotherController() {
        def c = new NewsSignupController()
        webRequest.controllerName = 'newsSignup'
        c.redirectToDefaultAction()
        assertEquals "/redirect/toAction", response.redirectedUrl
    }

    void testRedirectEventListeners() {
        def fired = false
        def callable = { fired = true }

        ctx.registerMockBean("testRedirect", callable)
        def pv = new MutablePropertyValues()
        pv.addPropertyValue("callable", callable)

        def c = new RedirectController()
        c.setRedirectListeners([new TestRedirectListener(callable: callable)])
        webRequest.controllerName = 'redirect'

        c.toAction()

        assert fired : "redirect event should have been fired"
    }

    void testRedirectAlreadyCalledException() {

        def c = new RedirectController()
        webRequest.controllerName = 'redirect'

        String message = shouldFail(CannotRedirectException) {
            c.redirectTwice()
        }
        assertEquals "incorrect error message for response redirect",
            "Cannot issue a redirect(..) here. A previous call to redirect(..) has already redirected the response.",
            message
    }

    void testRedirectWhenResponseCommitted() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'

        String message = shouldFail(CannotRedirectException) {
            c.responseCommitted()
        }
        assertEquals "incorrect error message for response redirect when already written to",
            "Cannot issue a redirect(..) here. The response has already been committed either by another redirect or by directly writing to the response.",
            message
    }

    void testRedirectToRoot() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        c.toRoot()
        assertEquals "/", response.redirectedUrl
    }

    void testRedirectToAbsoluteURL() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        c.toAbsolute()
        assertEquals "http://google.com", response.redirectedUrl

    }

    void testRedirectWithFragment() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        c.toControllerAndActionWithFragment()
        assertEquals "/test/foo#frag", response.redirectedUrl
    }

    void testRedirectInControllerWithOneLetterClassName() {
        def c = new AController()
        webRequest.controllerName = 'a'
        c.index()
        assertEquals "/a/list", response.redirectedUrl
    }

    void testRedirectInControllerWithAllUpperCaseClassName() {
        def c = new ABCController()
        webRequest.controllerName = 'ABC'
        c.index()
        assertEquals "/ABC/list", response.redirectedUrl
    }
    void testRedirectToAction() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        c.toAction()
        assertEquals "/redirect/foo", response.redirectedUrl
    }

    void testRedirectToActionWithGstring() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        c.toActionWithGstring()
        assertEquals "/redirect/foo", response.redirectedUrl
    }

    void testRedirectToController() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        c.toController()
        assertEquals "/test", response.redirectedUrl
    }

    void testRedirectToControllerAndAction() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        c.toControllerAndAction()
        assertEquals "/test/foo", response.redirectedUrl
    }

    void testRedirectToControllerWithParams() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        c.toControllerWithParams()
        assertEquals "/test/foo?one=two&two=three", response.redirectedUrl
    }

    void testRedirectToControllerWithDuplicateParams() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        c.toControllerWithDuplicateParams()
        assertEquals "/test/foo?one=two&one=three", response.redirectedUrl
    }

    void testRedirectToControllerWithDuplicateArrayParams() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        c.toControllerWithDuplicateArrayParams()
        assertEquals "/test/foo?one=two&one=three", response.redirectedUrl
    }

    void testRedirectToActionWithMapping() {
        def c = new NewsSignupController()
        c = new NewsSignupController()
        webRequest.controllerName = 'newsSignup'
        c.testNoController()
        assertEquals "/little-brown-bottle/thankyou", response.redirectedUrl
    }

    void testPermanentRedirect() {
        def c = new RedirectController()
        webRequest.controllerName = 'redirect'
        webRequest.currentRequest.serverPort = 8080
        c.toActionPermanent()

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

@Artefact('Controller')
class ABCController {
    def index = { redirect action: 'list' }
}

@Artefact('Controller')
class AController {
    def index = { redirect action: 'list' }
}

@Artefact('Controller')
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
