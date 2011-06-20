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

    void tearDown() {
        RequestContextHolder.setRequestAttributes(null)
    }

    void setUp() {
        RequestContextHolder.setRequestAttributes(null)
    }
}
