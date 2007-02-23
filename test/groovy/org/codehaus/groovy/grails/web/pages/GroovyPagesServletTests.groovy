package org.codehaus.groovy.grails.web.pages

import org.springframework.mock.web.*
import org.springframework.web.context.request.*
import org.codehaus.groovy.grails.web.servlet.mvc.*
import org.codehaus.groovy.grails.web.errors.*

class GroovyPagesServletTests extends GroovyTestCase {


    void testCreateResponseWriter() {
        shouldFail {
            def gps = new GroovyPagesServlet()
            def writer = gps.createResponseWriter(new MockHttpServletResponse())
        }

        def webRequest = new GrailsWebRequest(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockServletContext())
        RequestContextHolder.setRequestAttributes(webRequest)
        
        def gps = new GroovyPagesServlet()
        def writer = gps.createResponseWriter(new MockHttpServletResponse())

        assert writer != null
    }

    void testHandleException() {
         def gps = new GroovyPagesServlet()
         gps.init(new MockServletConfig(new MockServletContext()))
         def e = new Exception()
         def request = new MockHttpServletRequest()
         gps.handleException(e,request, new MockHttpServletResponse())



         def error = request.getAttribute(GroovyPagesServlet.EXCEPTION_MODEL_KEY)
         assertNotNull(error)
         assertTrue(error instanceof GrailsWrappedRuntimeException)
    }

    void tearDown() {
        RequestContextHolder.setRequestAttributes(null)
    }
    void setUp() {
        RequestContextHolder.setRequestAttributes(null)    
    }
}