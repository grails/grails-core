package org.codehaus.groovy.grails.commons;

import grails.util.GrailsWebUtil

/**
 * Tests for the GrailsWebUtil class
 */
class GrailsWebUtilTests extends GroovyTestCase {

    void testMakeMockRequest() {

        def req = GrailsWebUtil.makeMockRequest()

        assertEquals "UTF-8", req.characterEncoding
    }

}