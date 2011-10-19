package org.codehaus.groovy.grails.web.context

import spock.lang.Specification
import org.springframework.mock.web.MockServletContext
import grails.util.GrailsWebUtil
import org.springframework.web.context.request.RequestContextHolder

/**
 * Created by IntelliJ IDEA.
 * User: graemerocher
 * Date: 10/19/11
 * Time: 4:42 PM
 * To change this template use File | Settings | File Templates.
 */
class ServletContextHolderSpec extends Specification{

    void cleanup() {
        ServletContextHolder.clearServletContext()
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
