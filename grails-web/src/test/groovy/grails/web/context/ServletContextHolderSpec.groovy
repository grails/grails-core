package grails.web.context

import grails.util.GrailsWebMockUtil
import grails.web.context.ServletContextHolder

import org.springframework.mock.web.MockServletContext
import org.springframework.web.context.request.RequestContextHolder

import spock.lang.Specification

class ServletContextHolderSpec extends Specification {

    void cleanup() {
        ServletContextHolder.setServletContext(null)
        RequestContextHolder.resetRequestAttributes()
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
            final request = GrailsWebMockUtil.bindMockWebRequest()
        then:"It can be retrieved"
            ServletContextHolder.servletContext == request.servletContext
    }
}
