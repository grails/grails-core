package org.codehaus.groovy.grails.web.servlet.mvc

import grails.artefact.Artefact
import grails.test.mixin.TestFor

import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.plugins.*
import org.codehaus.groovy.grails.web.servlet.*
import org.junit.Test
import static org.junit.Assert.*
import org.springframework.mock.web.*
import org.springframework.validation.*
import org.springframework.web.context.request.*
import org.springframework.web.servlet.*

@TestFor(TestTagLib)
class TagLibDynamicMethodsTests {

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

