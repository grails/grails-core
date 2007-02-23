package org.codehaus.groovy.grails.web.pages

import org.springframework.mock.web.*
import org.springframework.web.context.request.*
import org.codehaus.groovy.grails.web.servlet.mvc.*
import org.codehaus.groovy.grails.web.errors.*
import grails.util.*

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

        def webRequest = GrailsWebUtil.bindMockWebRequest()
        def request = webRequest.currentRequest
        
        def gpte = new GroovyPagesTemplateEngine(new MockServletContext())

         def gps = new GroovyPagesServlet()
         gps.init(new MockServletConfig(new MockServletContext()))
         def e = new Exception()

         def response = new MockHttpServletResponse()



         gps.handleException(e,response.getWriter(),gpte)

    }

    void tearDown() {
        RequestContextHolder.setRequestAttributes(null)
    }
    void setUp() {
        RequestContextHolder.setRequestAttributes(null)    
    }
}