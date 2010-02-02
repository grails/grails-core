package org.codehaus.groovy.grails.web.servlet.mvc

import org.springframework.mock.web.MockHttpServletRequest

class GrailsParameterMapTests extends GroovyTestCase {

    GrailsParameterMap theMap
    MockHttpServletRequest mockRequest

    void setUp() {
        mockRequest = new MockHttpServletRequest();
    }

	void testPlusOperator() {
		mockRequest.addParameter("album", "Foxtrot")

		def originalMap = new GrailsParameterMap(mockRequest)

		def newMap = originalMap + [vocalist: 'Peter']
		assertTrue originalMap.containsKey('album')
		assertFalse originalMap.containsKey('vocalist')
		assertTrue newMap.containsKey('album')
		assertTrue newMap.containsKey('vocalist')
	}

    void testConversionHelperMethods() {
        def map = new GrailsParameterMap(mockRequest)

        map.one = "1"
        map.bad = "foo"
        map.decimals = "1.4"
        map.bool = "true"
        map.aList = [1,2]
        map.array = ["one", "two" ] as String[]
        map.longNumber = 1234567890

        assertEquals( ["1"], map.list("one") )
        assertEquals( [1,2], map.list("aList") )
        assertEquals( ["one","two"], map.list("array") )
        assertEquals( [], map.list("nonexistant") )

        assertEquals 1, map.byte('one')
        assertEquals( -46,map.byte('longNumber') ) // overflows
        assertNull map.byte("test")
        assertNull map.byte("bad")
        assertNull map.byte("nonexistant")

        assertEquals 1, map.int('one')
        assertNull map.int("test")
        assertNull map.int("bad")
        assertNull map.int("nonexistant")
        
        assertEquals 1L, map.long('one')
        assertNull map.long("test")
        assertNull map.long("bad")
        assertNull map.long("nonexistant")

        assertEquals 1, map.short('one')
        assertNull map.short("test")
        assertNull map.short("bad")
        assertNull map.short("nonexistant")

        assertEquals 1.0, map.double('one')
        assertEquals 1.4, map.double('decimals')
        assertNull map.double("bad")
        assertNull map.double("nonexistant")

        assertEquals 1.0, map.float('one')
        assertEquals 1.399999976158142, map.float('decimals')
        assertNull map.float("bad")
        assertNull map.float("nonexistant")

        assertEquals false, map.boolean('one')
        assertEquals true, map.boolean('bool')
        assertNull map.boolean("nonexistant")

    }

    void testAutoEvaluateBlankDates() {
        mockRequest.addParameter("foo", "date.struct")
        mockRequest.addParameter("foo_year", "")
        mockRequest.addParameter("foo_month", "")


        theMap = new GrailsParameterMap(mockRequest);
        assert theMap['foo'] == null : "should be null"
    }
    void testAutoEvaluateDates() {
        mockRequest.addParameter("foo", "date.struct")
        mockRequest.addParameter("foo_year", "2007")
        mockRequest.addParameter("foo_month", "07")


        theMap = new GrailsParameterMap(mockRequest);

        assert theMap['foo'] instanceof Date : "Should have returned a date but was a ${theMap['foo']}!"
        def cal = new GregorianCalendar()
        cal.setTime(theMap['foo'])

        assert 2007 == cal.get(Calendar.YEAR) : "Year should be 2007"
    }

    void testIterateOverMapContainingDate() {
        mockRequest.addParameter("stuff", "07")
        mockRequest.addParameter("foo", "date.struct")
        mockRequest.addParameter("foo_year", "2007")
        mockRequest.addParameter("foo_month", "07")
        mockRequest.addParameter("bar", "07")


        theMap = new GrailsParameterMap(mockRequest);

        def params = new GrailsParameterMap(mockRequest);
        for (Object o : theMap.keySet()) {
            String name = (String) o;
            params.put(name, theMap.get(name));
        }

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