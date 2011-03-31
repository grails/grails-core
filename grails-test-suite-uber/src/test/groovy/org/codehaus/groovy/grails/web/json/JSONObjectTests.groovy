package org.codehaus.groovy.grails.web.json

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class JSONObjectTests extends GroovyTestCase {


    void testContainsKey() {
        JSONObject j = new JSONObject();
        j.put('test', 1)
        assert j.containsKey('test')
    }

    void testContainsValue() {
        JSONObject j = new JSONObject();
        j.put('test', 1)
        assert j.containsValue(1)

    }
}
