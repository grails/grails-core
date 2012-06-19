package org.codehaus.groovy.grails.plugins

import grails.util.BuildSettings
import grails.util.BuildSettingsHolder

import org.apache.commons.io.FileUtils
import org.codehaus.groovy.grails.plugins.metadata.GrailsPlugin
import org.springframework.core.io.Resource

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class GrailsPluginUtilsTests extends GroovyTestCase {
    BuildSettings settings

     protected void setUp() {
         GrailsPluginUtils.clearCaches()
         System.setProperty("disable.grails.plugin.transform","true")
         settings = new BuildSettings(new File("."))
         BuildSettingsHolder.settings = settings
         def resourceDir = "test/resources/grails-plugin-utils"
         settings.projectPluginsDir = new File("$resourceDir/plugins")
         settings.globalPluginsDir = new File("$resourceDir/global-plugins")
         settings.config = new ConfigSlurper().parse("""\
grails {
    plugin {
        location {
            debug = "$resourceDir/grails-debug"
            dummy = "$resourceDir/grails-dummy"
        }
    }
}
""")
    }

    void tearDown() {
        System.setProperty("disable.grails.plugin.transform","false")
        BuildSettingsHolder.settings = null
    }

    void testOsgiFormatVersionNumbers() {
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("1.0","0.6 > 1.1-SNAPSHOT")
        assertTrue "version with SNAPSHOT tag should match", GrailsPluginUtils.isValidVersion("1.2", "1.2.0.BUILD-SNAPSHOT > *")
        assertTrue "version with SNAPSHOT tag should match", GrailsPluginUtils.isValidVersion("1.2.0.BUILD-SNAPSHOT", "1.2 > *")
        assertFalse "version with SNAPSHOT should not match", GrailsPluginUtils.isValidVersion("1.2.0.BUILD-SNAPSHOT", "1.3 > *")
    }

    void testGetPluginName() {
        assertEquals "foo", GrailsPluginUtils.getPluginName(TestPluginAnnotation)
        assertNull GrailsPluginUtils.getPluginName(null)
        assertNull GrailsPluginUtils.getPluginName(String)
    }

    void testGetPluginVersion() {
        assertEquals "1.0", GrailsPluginUtils.getPluginVersion(TestPluginAnnotation)
        assertNull GrailsPluginUtils.getPluginVersion(null)
        assertNull GrailsPluginUtils.getPluginName(String)
    }

    void testIsVersionGreaterThan() {
        assertTrue "version should be greater than", GrailsPluginUtils.isVersionGreaterThan("0.5.5", "0.5.5.1")
        assertFalse "version should be less than", GrailsPluginUtils.isVersionGreaterThan("0.5.5.1", "0.5.5")
    }

    void testIsVersionGreaterThanWithSnapshots() {
        assertVersionIsGreaterThan "0.5.5-SNAPSHOT", "0.5.5.1"
        assertVersionIsGreaterThan "0.5.5-SNAPSHOT", "0.5.5"

        assertVersionIsNotGreaterThan "0.5.5.1", "0.5.5-SNAPSHOT"
        assertVersionIsNotGreaterThan "0.5.5", "0.5.5-SNAPSHOT"
    }

    protected assertVersionIsGreaterThan(lower, higher) {
        assertTrue "version '$higher' should be greater than '$lower'", GrailsPluginUtils.isVersionGreaterThan(lower, higher)
    }

    protected assertVersionIsNotGreaterThan(higher, lower) {
        assertFalse "version '$lower' should NOT be greater than '$higher'", GrailsPluginUtils.isVersionGreaterThan(higher, lower)
    }

    void testVersionValidity() {
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("0.5.5","0.5.5 > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("0.4.2","0.4.2 > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("1.1.1","1.1.1  > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("0.9.5","0.9.2 > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("1.5","1.5 > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("1.5","1.5 > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("1.0-RC3","1.0-RC3 > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("1.1-SNAPSHOT","1.0 > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("1.0","1.0 > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("1.1.1","1.1 > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("1.1.1-SNAPSHOT","1.1 > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("1.1.1","1.1-SNAPSHOT > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("1.0","1.0-SNAPSHOT > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("1.0","0.6 > 1.1-SNAPSHOT")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("0.7","0.6 > 1.0")
        assertFalse "version should be outside range", GrailsPluginUtils.isValidVersion("1.1","0.6 > 1.0")
        assertTrue "versions should match", GrailsPluginUtils.isValidVersion("1.0", "1.0")
        assertTrue "version with SNAPSHOT tag should match", GrailsPluginUtils.isValidVersion("1.0-SNAPSHOT", "1.0")
        assertTrue "version with SNAPSHOT tag should match", GrailsPluginUtils.isValidVersion("1.0-RC2-SNAPSHOT", "1.0")
        assertTrue "version with SNAPSHOT tag should match", GrailsPluginUtils.isValidVersion("1.0-RC2-SNAPSHOT", "1.0-RC2-SNAPSHOT")

        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("1.0","0.6 > 1.0")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("1.6","1.0 > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("1.0.7","1.0 > *")
        assertFalse "version should be outside range", GrailsPluginUtils.isValidVersion("0.9", "1.0 > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("1.0.7", "1.0 > 2.7")
        assertTrue "version with RC tag should be within range",GrailsPluginUtils.isValidVersion("1.0.7-RC1", "1.0 > 2.7")
        assertTrue "version with SNAPSHOT tag should be within range", GrailsPluginUtils.isValidVersion("1.0-SNAPSHOT", "1.0 > 2.7")
        assertFalse "version should be outside range", GrailsPluginUtils.isValidVersion("0.9","1.0 > 2.7")
        assertFalse "version should be outside range",GrailsPluginUtils.isValidVersion("2.8", "1.0 > 2.7")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion("1.0","0.6 > 1.0-SNAPSHOT")
    }

    void testIsAtLeastVersion() {
        assertFalse "should not support 1.1", GrailsPluginUtils.supportsAtLeastVersion("1.1 > *", "1.2")
        assertFalse "should not support 1.1", GrailsPluginUtils.supportsAtLeastVersion("1.1", "1.2")
        assertFalse "should not support anything", GrailsPluginUtils.supportsAtLeastVersion("*", "1.2")
        assertFalse "should not support anything", GrailsPluginUtils.supportsAtLeastVersion("* > 1.2", "1.2")
        assertTrue "should support 1.2 and above", GrailsPluginUtils.supportsAtLeastVersion("1.2 > *", "1.2")
        assertTrue "should support 1.2 and above", GrailsPluginUtils.supportsAtLeastVersion("1.2", "1.2")
    }

    void testGetUpperVersion() {
        assertEquals "*", GrailsPluginUtils.getUpperVersion("1.0 > * ")
        assertEquals "*", GrailsPluginUtils.getUpperVersion("1.0 >*")
        assertEquals "1.1", GrailsPluginUtils.getUpperVersion("* >1.1")
        assertEquals "1.1", GrailsPluginUtils.getUpperVersion("0.9 >1.1")
        assertEquals "1.1", GrailsPluginUtils.getUpperVersion("1.1")
    }

    void testGetPluginDirForName() {
        assertNotNull GrailsPluginUtils.getPluginDirForName("jsecurity")
        assertNotNull GrailsPluginUtils.getPluginDirForName("jsecurity-0.3")
        assertNotNull GrailsPluginUtils.getPluginDirForName("logging")
        assertNotNull GrailsPluginUtils.getPluginDirForName("logging-0.1")

        assertNull GrailsPluginUtils.getPluginDirForName("jsecurity-0.2.1")
        assertNull GrailsPluginUtils.getPluginDirForName("logging-0.1.1")
        assertNull GrailsPluginUtils.getPluginDirForName("remoting")
        assertNull GrailsPluginUtils.getPluginDirForName("remoting-0.3")
    }

    void testGetPluginDirectories() {
        def pluginDirs = GrailsPluginUtils.getPluginDirectories()
        assertEquals 4, pluginDirs.size()
        assertNotNull pluginDirs.find { it.filename == "jsecurity-0.3" }
        assertNotNull pluginDirs.find { it.filename == "logging-0.1" }
        assertNotNull pluginDirs.find { it.filename == "grails-debug" }
        assertNotNull pluginDirs.find { it.filename == "grails-dummy" }
    }

    void testGetImplicitPluginDirectories() {
        def pluginDirs = GrailsPluginUtils.getImplicitPluginDirectories()
        assertEquals 2, pluginDirs.size()
        assertNotNull pluginDirs.find { it.filename == "jsecurity-0.3" }
        assertNotNull pluginDirs.find { it.filename == "logging-0.1" }
    }

    void testGetPluginScripts() {
        Resource[] scripts = GrailsPluginUtils.getPluginScripts(settings.projectPluginsDir.path)
        assertEquals 5, scripts.size()
        assertNotNull scripts.find { it.filename == "CreateAuthController.groovy" }
        assertNotNull scripts.find { it.filename == "CreateDbRealm.groovy" }
        assertNotNull scripts.find { it.filename == "_Install.groovy" }
        assertNotNull scripts.find { it.filename == "DoSomething.groovy" }
        assertNotNull scripts.find { it.filename == "RunDebug.groovy" }
    }
}

@GrailsPlugin(name="foo", version="1.0")
class TestPluginAnnotation {}
