package org.codehaus.groovy.grails.web.servlet.mvc

import org.springframework.mock.web.MockHttpServletRequest

class GrailsParameterMapTests extends GroovyTestCase {

    GrailsParameterMap theMap
    MockHttpServletRequest mockRequest

    void setUp() {
        mockRequest = new MockHttpServletRequest();
    }

    void testMultiDimensionParams() {
        mockRequest.addParameter("a.b.c", "cValue")
        mockRequest.addParameter("a.b", "bValue")
        mockRequest.addParameter("a", "aValue")
        mockRequest.addParameter("a.bc", "bcValue")
        theMap = new GrailsParameterMap(mockRequest);
        assert theMap['a'] instanceof Map
    }

}