package org.codehaus.groovy.grails.web.mapping

import org.codehaus.groovy.grails.validation.ConstrainedProperty;

class RegexUrlMappingTests extends GroovyTestCase {

    void testMatchUriWithConstraints() {

        def cp = new ConstrainedProperty(RegexUrlMappingTests.class, "hello", String.class)
        cp.nullable = false

        // mapping would be "/foo/$hello/bar
        def m = new RegexUrlMapping("/foo/([^/]+?)/bar", "test", "action", [cp] as ConstrainedProperty[])

        def info = m.match("/foo/world/bar")
        assert info
        assertEquals "test", info.controllerName
        assertEquals "action", info.actionName
        println info.parameters
        assertEquals "world", info.parameters.hello

    }

    void testMatchUriWithMatchesConstraints() {

        def cp = new ConstrainedProperty(RegexUrlMappingTests.class, "year", String.class)
        cp.matches = /\d{4}/

        // mapping would be "/foo/$hello/bar
        def m = new RegexUrlMapping("/foo/([^/]+?)/bar", "test", "action", [cp] as ConstrainedProperty[])

        def info = m.match("/foo/2007/bar")
        assert info
        assertEquals "test", info.controllerName
        assertEquals "action", info.actionName
        println info.parameters
        assertEquals "2007", info.parameters.year

        info = m.match("/foo/blah/bar")
        assertNull info
    }


    void testInit() {
        def m = new RegexUrlMapping("/(\\w+)/hello", "test", [] as ConstrainedProperty[])

        shouldFail {
           m = new RegexUrlMapping(null, "test", [] as ConstrainedProperty[])
        }
        shouldFail {
           m = new RegexUrlMapping("/(\\w+)/hello/", null, [] as ConstrainedProperty[])
        }
    }

    void testMatchUriNoConstraints() {
        def m = new RegexUrlMapping("/foo/(\\w+?)/bar", "test", [] as ConstrainedProperty[])

        def info = m.match("/foo/test/bar")
        assert info
        assertEquals "test", info.controllerName

        info = m.match("/foo/bar/test")
        assertNull info
    }

}

