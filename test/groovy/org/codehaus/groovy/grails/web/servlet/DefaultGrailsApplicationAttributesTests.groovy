package org.codehaus.groovy.grails.web.servlet

import org.springframework.mock.web.MockHttpServletRequest
import org.codehaus.groovy.grails.web.metaclass.ControllerDynamicMethods

class DefaultGrailsApplicationAttributesTests extends GroovyTestCase {

    void testGetTemplateUriWithNestedRelativePath() {
        def grailsApplicationAttributes = new DefaultGrailsApplicationAttributes(null)

        def controller = new Expando()
        controller."${ControllerDynamicMethods.CONTROLLER_URI_PROPERTY}" = 'foo'

        def request = new MockHttpServletRequest()
        request.setAttribute(GrailsApplicationAttributes.CONTROLLER, controller)

        String templateUri = grailsApplicationAttributes.getTemplateUri('one/two/three', request)

        assertEquals 'wrong template uri', 'foo/one/two/_three.gsp', templateUri
    }
    
}