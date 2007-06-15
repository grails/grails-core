package org.codehaus.groovy.grails.commons;

/**
 * Tests for the GrailsUtil class
 */
class GrailsUtilTests extends GroovyTestCase {

    void testGrailsVersion() {

        
        assertEquals "0.6-SNAPSHOT", grails.util.GrailsUtil.getGrailsVersion()
    }

}