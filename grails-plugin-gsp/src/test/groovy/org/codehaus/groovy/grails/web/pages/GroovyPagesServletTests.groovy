package org.codehaus.groovy.grails.web.pages

import grails.util.GrailsWebUtil
import grails.util.MockHttpServletResponse

import org.codehaus.groovy.grails.support.MockApplicationContext
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockServletConfig
import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder

class GroovyPagesServletTests extends GroovyTestCase {

    void testCreateResponseWriter() {
        shouldFail {
            def gps = new GroovyPagesServlet()
            def writer = gps.createResponseWriter(new MockHttpServletResponse())
        }

        RequestContextHolder.setRequestAttributes new GrailsWebRequest(
            new MockHttpServletRequest(), new MockHttpServletResponse(), new MockServletContext())

        def gps = new GroovyPagesServlet()
        def writer = gps.createResponseWriter(new MockHttpServletResponse())

        assert writer != null
    }

    void testHandleException() {

        def webRequest = GrailsWebUtil.bindMockWebRequest()
        def request = webRequest.currentRequest

        MockServletContext servletContext = new MockServletContext()
        MockApplicationContext ctx = new MockApplicationContext()

        servletContext.setAttribute("app.ctx", ctx)
        def gpte = new GroovyPagesTemplateEngine(servletContext)
        ctx.registerMockBean(GroovyPagesTemplateEngine.BEAN_ID, gpte)
        def gps = new GroovyPagesServlet()
        gps.contextAttribute = "app.ctx"
        gps.init(new MockServletConfig(servletContext))
        def e = new Exception()

        def response = new MockHttpServletResponse()

        gps.handleException(request, response,e,response.getWriter(),gpte)
    }

    void tearDown() {
        RequestContextHolder.setRequestAttributes(null)
    }

    void setUp() {
        RequestContextHolder.setRequestAttributes(null)
    }
}
