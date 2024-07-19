package org.grails.web.servlet.mvc

import grails.artefact.Artefact
import grails.testing.web.taglib.TagLibUnitTest
import spock.lang.Ignore
import spock.lang.Specification

@Ignore('grails-gsp is not on jakarta.servlet yet')
class TagLibDynamicMethodsTests extends Specification implements TagLibUnitTest<TestTagLib> {

    void testFlashObject() {
        when:
        tagLib.flash.test = "hello"

        then:
        tagLib.flash.test == "hello"
    }

    void testParamsObject() {
        when:
        tagLib.params.test = "hello"

        then:
        tagLib.params.test == "hello"
    }

    void testSessionObject() {
        when:
        tagLib.session.test = "hello"

        then:
        tagLib.session.test == "hello"
    }

    void testGrailsAttributesObject() {
        expect:
        tagLib.grailsAttributes != null
    }

    void testRequestObjects() {
        expect:
        tagLib.request != null

        tagLib.response != null
        tagLib.servletContext != null
    }
}

@Artefact("TagLibrary")
class TestTagLib {
    def myTag = {attrs, body -> body() }
 }

