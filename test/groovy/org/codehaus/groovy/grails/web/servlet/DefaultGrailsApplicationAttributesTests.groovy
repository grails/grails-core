package org.codehaus.groovy.grails.web.servlet

import org.springframework.mock.web.MockHttpServletRequest
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods

class DefaultGrailsApplicationAttributesTests extends GroovyTestCase {

    def grailsApplicationAttributes
    def request

    void setUp() {
        grailsApplicationAttributes = new DefaultGrailsApplicationAttributes(null)
        def controller = new Expando()
        controller."${ControllerDynamicMethods.CONTROLLER_URI_PROPERTY}" = 'mycontroller'
        request = new MockHttpServletRequest()
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER, controller)
    }

    void testGetTemplateUriWithNestedRelativePath() {
        def templateUri = grailsApplicationAttributes.getTemplateUri('one/two/three', request)
        assertEquals 'wrong template uri', 'mycontroller/one/two/_three.gsp', templateUri
    }

    void testGetTemplateUriWithRelativePath() {
        def templateUri = grailsApplicationAttributes.getTemplateUri('bar', request)
        assertEquals 'wrong template uri', 'mycontroller/_bar.gsp', templateUri
    }
}