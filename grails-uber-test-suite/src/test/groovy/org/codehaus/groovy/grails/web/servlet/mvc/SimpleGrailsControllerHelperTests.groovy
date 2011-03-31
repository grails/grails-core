package org.codehaus.groovy.grails.web.servlet.mvc

import org.springframework.web.context.request.RequestContextHolder

class SimpleGrailsControllerHelperTests extends AbstractGrailsControllerTests {

    protected void onSetUp() {
        gcl.parseClass """
        class TestController {
           def list = {}

           def afterInterceptor = {
                it.put("after", "value")
           }
        }
        """

        gcl.parseClass """
        class Test2Controller {
           def list = {}

           def afterInterceptor = { model ->
                model.put("after", "value")
                return "not a boolean"
           }
        }
        """

        gcl.parseClass """
        class Test3Controller {
           def list = {}

           def afterInterceptor = { model, modelAndView ->
                model.put("after", modelAndView.getViewName())
                return true
           }
        }
        """

        gcl.parseClass """
        class Test4Controller {
           def list = {}

           def afterInterceptor = { model, modelAndView ->
                return false
           }
        }
        """
    }

    void testConstructHelper() {
        runTest {
            def webRequest = RequestContextHolder.currentRequestAttributes()
            def helper = new SimpleGrailsControllerHelper(ga, ctx, servletContext)
        }
    }

    void testCallsAfterInterceptorWithModel() {
        runTest {
            def helper = new SimpleGrailsControllerHelper(ga, appCtx , servletContext)
            def mv = helper.handleURI("/test/list", webRequest)
            assert mv.getModel()["after"] == "value"
        }
    }

    void testCallsAfterInterceptorWithModelAndExplicitParam() {
        runTest {
            def helper = new SimpleGrailsControllerHelper(ga, appCtx , servletContext)
            def mv = helper.handleURI("/test2/list", webRequest)
            assert mv.getModel()["after"] == "value"
        }
    }

    void testCallsAfterInterceptorWithModelAndViewExplicitParams() {
        runTest {
            def helper = new SimpleGrailsControllerHelper(ga, appCtx , servletContext)
            def mv = helper.handleURI("/test3/list", webRequest)
            assert mv.getModel()["after"] == "/test3/list"
        }
    }

    void testReturnsNullIfAfterInterceptorReturnsFalse() {
        runTest {
            def helper = new SimpleGrailsControllerHelper(ga, appCtx , servletContext)
            def mv = helper.handleURI("/test4/list", webRequest)
            assert mv == null
        }
    }
}
