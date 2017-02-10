package org.grails.web.json

import spock.lang.Issue

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class JSONObjectTests extends GroovyTestCase {

    void testContainsKey() {
        JSONObject j = new JSONObject()
        j.put('test', 1)
        assert j.containsKey('test')
    }

    void testContainsValue() {
        JSONObject j = new JSONObject()
        j.put('test', 1)
        assert j.containsValue(1)
    }

    @Issue("GRAILS-10412")
    void testToStringWithArray() {
        JSONObject j = new JSONObject([id:1, tags:['tag13333', 'tag2231']])

        JSONObject json = grails.converters.JSON.parse(j.toString())
        assertEquals(json.id, 1)
        assertEquals(json.tags[0], "tag13333")
        assertEquals(json.tags[1], "tag2231")
    }

    void testEqualityOfJSONObjectsReturnedFromConverter() {
        // GRAILS-7417

        def input = '''{"message":"mockMessage","errors":[{"field":"name","errorMessage":"mockMessage","errorType":"validation","fieldValue":null}],
        "object":{"nest2":{"p2":"val2","p1":"val1"},"nest1":{"p2":"val2","p1":"val1"},"name":null},"success":false}'''

        JSONObject j1 = grails.converters.JSON.parse(input)
        JSONObject j2 = grails.converters.JSON.parse(input)

        assertNotSame j1, j2
        assertEquals j1, j2
        assertTrue j1 == j2
    }
}
