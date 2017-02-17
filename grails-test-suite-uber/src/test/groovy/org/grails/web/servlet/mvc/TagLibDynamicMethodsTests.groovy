package org.grails.web.servlet.mvc

import grails.artefact.Artefact
import grails.testing.web.taglib.TagLibUnitTest
import org.junit.Test
import static org.junit.Assert.*

class TagLibDynamicMethodsTests implements TagLibUnitTest<TestTagLib> {

    @Test
    void testFlashObject() {
        tagLib.flash.test = "hello"

        assertEquals "hello", tagLib.flash.test
    }

    @Test
    void testParamsObject() {
        tagLib.params.test = "hello"

        assertEquals "hello", tagLib.params.test
    }

    @Test
    void testSessionObject() {
        tagLib.session.test = "hello"

        assertEquals "hello", tagLib.session.test
    }

    @Test
    void testGrailsAttributesObject() {
        assertNotNull(tagLib.grailsAttributes)
    }

    @Test
    void testRequestObjects() {
        assertNotNull(tagLib.request)

        assertNotNull(tagLib.response)
        assertNotNull(tagLib.servletContext)
    }
}

@Artefact("TagLibrary")
class TestTagLib {
    def myTag = {attrs, body -> body() }
 }

