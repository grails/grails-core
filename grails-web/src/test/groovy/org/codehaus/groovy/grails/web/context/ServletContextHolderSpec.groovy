package org.codehaus.groovy.grails.web.context

import grails.util.GrailsWebUtil

import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder

import spock.lang.Specification

class ServletContextHolderSpec extends Specification {

    void cleanup() {
        ServletContextHolder.setServletContext(null)
        RequestContextHolder.setRequestAttributes(null)
    }

    def "Test get set servlet context"() {
        when:"the ServletContext is set"
            final context = new MockServletContext()
            ServletContextHolder.servletContext = context
        then:"It can be retrieved"
            ServletContextHolder.servletContext == context
    }

    def "Test fallback to mock web request"() {
        when:"the ServletContext is set"
            final request = GrailsWebUtil.bindMockWebRequest()
        then:"It can be retrieved"
            ServletContextHolder.servletContext == request.servletContext
    }
}
