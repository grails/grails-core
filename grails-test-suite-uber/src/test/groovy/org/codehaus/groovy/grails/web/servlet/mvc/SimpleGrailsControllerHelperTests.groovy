package org.codehaus.groovy.grails.web.servlet.mvc

import grails.web.Action
import org.springframework.web.context.request.RequestContextHolder
import org.codehaus.groovy.grails.web.servlet.mvc.exceptions.UnknownControllerException
import grails.artefact.Artefact

class SimpleGrailsControllerHelperTests extends AbstractGrailsControllerTests {

    @Override
    protected Collection<Class> getControllerClasses() {
        [Test1Controller, Test2Controller, Test3Controller, Test4Controller]
    }

    void testConstructHelper() {
        runTest {
            def webRequest = RequestContextHolder.currentRequestAttributes()
            def helper = new MixedGrailsControllerHelper(application:ga, applicationContext: appCtx, servletContext: servletContext)
        }
    }

    void testAmbiguousGetterNameForAction() {
            runTest {
                def helper = new MixedGrailsControllerHelper(application:ga, applicationContext: appCtx, servletContext: servletContext)
                def mv = helper.handleURI("/test3/list", webRequest)
                assert !mv.getModel()["someAmbiguousActionName"]
            }
        }

    void testCallsAfterInterceptorWithModel() {
        runTest {
            def helper = new MixedGrailsControllerHelper(application:ga, applicationContext: appCtx, servletContext: servletContext)
            def mv = helper.handleURI("/test1/list", webRequest)
            assert mv.getModel()["after"] == "value"
        }
    }

    void testCallsAfterInterceptorWithModelAndExplicitParam() {
        runTest {
            def helper = new MixedGrailsControllerHelper(application:ga, applicationContext: appCtx, servletContext: servletContext)
            def mv = helper.handleURI("/test2/list", webRequest)
            assert mv.getModel()["after"] == "value"
        }
    }

    void testCallsAfterInterceptorWithModelAndViewExplicitParams() {
        runTest {
            def helper = new MixedGrailsControllerHelper(application:ga, applicationContext: appCtx, servletContext: servletContext)
            def mv = helper.handleURI("/test3/list", webRequest)
            assert mv.getModel()["after"] == "/test3/list"
        }
    }

    void testReturnsNullIfAfterInterceptorReturnsFalse() {
        runTest {
            def helper = new MixedGrailsControllerHelper(application:ga, applicationContext: appCtx, servletContext: servletContext)
            def mv = helper.handleURI("/test4/list", webRequest)
            assert mv == null
        }
    }

    void testDontHandleFlowAction() {
        runTest {
            shouldFail(UnknownControllerException) {
                def helper = new MixedGrailsControllerHelper(application:ga, applicationContext: appCtx, servletContext: servletContext)
                def mv = helper.handleURI("/test1/testFlow", webRequest)
            }
        }
    }
}

@Artefact("Controller")
class Test1Controller {
    @Action def list(){}

    def afterInterceptor = {
         it.put("after", "value")
    }

    def testFlow = {
         startFlow {
             on "foo" to "bar"
         }
    }
}

@Artefact("Controller")
class Test2Controller {
    @Action def list(){}

    def afterInterceptor = { model ->
         model.put("after", "value")
         return "not a boolean"
    }
}

@Artefact("Controller")
class Test3Controller {
    @Action def list(){}
    @Action def getSomeAmbiguousActionName(){[a:true]}
    def afterInterceptor = { model, modelAndView ->
         model.put("after", modelAndView.getViewName())
         return true
    }
}

@Artefact("Controller")
class Test4Controller {
    @Action def list(){}

    def afterInterceptor = { model, modelAndView ->
         return false
    }
}
