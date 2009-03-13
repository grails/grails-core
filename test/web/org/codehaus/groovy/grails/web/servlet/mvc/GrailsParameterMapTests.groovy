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
        mockRequest.addParameter("a.bc", "bcValue")
        mockRequest.addParameter("a.b.d", "dValue")
        mockRequest.addParameter("a.e.f", "fValue")
        mockRequest.addParameter("a.e.g", "gValue")
        theMap = new GrailsParameterMap(mockRequest);
        assert theMap['a'] instanceof Map
        assert theMap.a.b == "bValue"
        assert theMap.a.'b.c' == "cValue"
        assert theMap.a.'bc' == "bcValue"
        assert theMap.a.'b.d' == "dValue"

        assert theMap.a['e'] instanceof Map
        assert theMap.a.e.f == "fValue"
        assert theMap.a.e.g == "gValue"
    }

    void testToQueryString() {
        mockRequest.addParameter("name", "Dierk Koenig")
        mockRequest.addParameter("dob", "01/01/1970")
        theMap = new GrailsParameterMap(mockRequest);

        def queryString = theMap.toQueryString()

        assertTrue queryString.startsWith('?')
        queryString = queryString[1..-1].split('&')

        assert queryString.find { it == 'name=Dierk+Koenig' }
        assert queryString.find { it == 'dob=01%2F01%2F1970' }
    }

    void testSimpleMappings() {
        mockRequest.addParameter("test", "1")
        theMap = new GrailsParameterMap(mockRequest);

        assertEquals "1", theMap['test']
    }

    void testToQueryStringWithMultiD() {
        mockRequest.addParameter("name", "Dierk Koenig")
        mockRequest.addParameter("dob", "01/01/1970")
        mockRequest.addParameter("address.postCode", "345435")
        theMap = new GrailsParameterMap(mockRequest);

        def queryString = theMap.toQueryString()

        assertTrue queryString.startsWith('?')
        queryString = queryString[1..-1].split('&')


        assert queryString.find { it == 'name=Dierk+Koenig' }
        assert queryString.find { it == 'dob=01%2F01%2F1970' }
        assert queryString.find { it == 'address.postCode=345435' }
    }

}