/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 29, 2007
 */
package org.codehaus.groovy.grails.plugins
class GrailsPluginUtilsTests extends GroovyTestCase {


    void testVersionValidity() {
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion(  "1.0","1.0 > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion(  "0.7","0.6 > 1.0")
        assertFalse "version should be outside range", GrailsPluginUtils.isValidVersion(  "1.1","0.6 > 1.0")
        assertTrue "versions should match", GrailsPluginUtils.isValidVersion( "1.0", "1.0")
        assertTrue "version with SNAPSHOT tag should match", GrailsPluginUtils.isValidVersion( "1.0-SNAPSHOT", "1.0")
        assertTrue "version with SNAPSHOT tag should match", GrailsPluginUtils.isValidVersion( "1.0-RC2-SNAPSHOT", "1.0")
        assertTrue "version with SNAPSHOT tag should match", GrailsPluginUtils.isValidVersion( "1.0-RC2-SNAPSHOT", "1.0-RC2-SNAPSHOT")

        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion(  "1.0","0.6 > 1.0")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion(  "1.6","1.0 > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion(  "1.0.7","1.0 > *")
        assertFalse "version should be outside range", GrailsPluginUtils.isValidVersion( "0.9", "1.0 > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion( "1.0.7", "1.0 > 2.7")
        assertTrue "version with RC tag should be within range",GrailsPluginUtils.isValidVersion( "1.0.7-RC1", "1.0 > 2.7")
        assertTrue "version with SNAPSHOT tag should be within range", GrailsPluginUtils.isValidVersion( "1.0-SNAPSHOT", "1.0 > 2.7" )
        assertFalse "version should be outside range", GrailsPluginUtils.isValidVersion( "0.9","1.0 > 2.7" )
        assertFalse "version should be outside range",GrailsPluginUtils.isValidVersion(  "2.8", "1.0 > 2.7")
    }
}