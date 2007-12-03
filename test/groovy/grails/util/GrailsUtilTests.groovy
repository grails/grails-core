package org.codehaus.groovy.grails.commons;

/**
 * Tests for the GrailsUtil class
 */
class GrailsUtilTests extends GroovyTestCase {

    void testGrailsVersion() {

        assertEquals "1.0-RC2", grails.util.GrailsUtil.getGrailsVersion()
    }

}