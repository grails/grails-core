package org.codehaus.groovy.grails.web.servlet.mvc

import grails.artefact.Artefact
import grails.test.mixin.Mock
import grails.web.Action

import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.UnknownControllerException
import org.junit.Test
import org.springframework.web.context.request.RequestContextHolder


@Mock([Test1Controller, Test2Controller, Test3Controller, Test4Controller])
class SimpleGrailsControllerHelperTests  {

    @Test
    void testConstructHelper() {
        def webRequest = RequestContextHolder.currentRequestAttributes()
        def helper = new MixedGrailsControllerHelper(application:grailsApplication, applicationContext: mainContext, servletContext: servletContext)
    }

    @Test
    void testAmbiguousGetterNameForAction() {
        def helper = new MixedGrailsControllerHelper(application:grailsApplication, applicationContext: mainContext, servletContext: servletContext)
        def mv = helper.handleURI("/test3/list", webRequest)
        assert !mv.getModel()["someAmbiguousActionName"]
    }

    @Test
    void testCallsAfterInterceptorWithModel() {
        def helper = new MixedGrailsControllerHelper(application:grailsApplication, applicationContext: mainContext, servletContext: servletContext)
        def mv = helper.handleURI("/test1/list", webRequest)
        assert mv.getModel()["after"] == "value"
    }

    @Test
    void testCallsAfterInterceptorWithModelAndExplicitParam() {
        def helper = new MixedGrailsControllerHelper(application:grailsApplication, applicationContext: mainContext, servletContext: servletContext)
        def mv = helper.handleURI("/test2/list", webRequest)
        assert mv.getModel()["after"] == "value"
    }

    @Test
    void testCallsAfterInterceptorWithModelAndViewExplicitParams() {
        def helper = new MixedGrailsControllerHelper(application:grailsApplication, applicationContext: mainContext, servletContext: servletContext)
        def mv = helper.handleURI("/test3/list", webRequest)
        assert mv.getModel()["after"] == "/test3/list"
    }

    @Test
    void testReturnsNullIfAfterInterceptorReturnsFalse() {
        def helper = new MixedGrailsControllerHelper(application:grailsApplication, applicationContext: mainContext, servletContext: servletContext)
        def mv = helper.handleURI("/test4/list", webRequest)
        assert mv == null
    }

    @Test
    void testDontHandleFlowAction() {
        shouldFail(UnknownControllerException) {
            def helper = new MixedGrailsControllerHelper(application:grailsApplication, applicationContext: mainContext, servletContext: servletContext)
            def mv = helper.handleURI("/test1/testFlow", webRequest)
        }
    }
}

@Artefact("Controller")
class Test1Controller {
    @Action def list() {

    }

    def afterInterceptor = {
        it.put("after", "value")
    }

    def testFlow = {
        startFlow { on "foo" to "bar" }
    }
}

@Artefact("Controller")
class Test2Controller {
    @Action def list() {

    }

    def afterInterceptor = { model ->
        model.put("after", "value")
        return "not a boolean"
    }
}

@Artefact("Controller")
class Test3Controller {
    @Action def list() {

    }
    @Action def getSomeAmbiguousActionName() {
        [a:true]
    }
    def afterInterceptor = { model, modelAndView ->
        model.put("after", modelAndView.getViewName())
        return true
    }
}

@Artefact("Controller")
class Test4Controller {
    @Action def list() {

    }

    def afterInterceptor = { model, modelAndView -> return false }
}
