/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Nov 29, 2007
 */
package org.codehaus.groovy.grails.plugins

import org.apache.commons.io.FileUtils
import grails.util.BuildSettingsHolder
import grails.util.BuildSettings
import org.springframework.core.io.Resource

class GrailsPluginUtilsTests extends GroovyTestCase {
    BuildSettings settings

    void setUp() {
        settings = new BuildSettings(new File("."))
        BuildSettingsHolder.settings = settings

//        new File("tmp/plugins/searchable-0.5").mkdirs()
//        new File("tmp/plugins/jsecurity-0.3").mkdirs()
//        new File("tmp/plugins/.hidden").mkdirs()
//        new File("tmp/global-plugins/logging-0.1").mkdirs()
//        new File("tmp/global-plugins/notAPlugin").mkdirs()
//        new File("tmp/grails-debug").mkdirs()
//        new File("tmp/grails-dummy").mkdirs()
        

        def resourceDir = "test/resources/grails-plugin-utils"
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
        settings.projectPluginsDir = new File("$resourceDir/plugins")
        settings.globalPluginsDir = new File("$resourceDir/global-plugins")
    }

    void tearDown() {
        BuildSettingsHolder.settings = null

//        new File("tmp").deleteDir()
    }

    void testVersionValidity() {
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion(  "1.1-SNAPSHOT","1.0 > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion(  "1.0","1.0 > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion(  "1.1.1","1.1 > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion(  "1.1.1-SNAPSHOT","1.1 > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion(  "1.1.1","1.1-SNAPSHOT > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion(  "1.0","1.0-SNAPSHOT > *")
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion(  "1.0","0.6 > 1.1-SNAPSHOT")
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
        assertTrue "version should be within range", GrailsPluginUtils.isValidVersion(  "1.0","0.6 > 1.0-SNAPSHOT")
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
        //assertNotNull pluginDirs.find { it.filename == "searchable-0.5" }
        assertNotNull pluginDirs.find { it.filename == "grails-debug" }
        assertNotNull pluginDirs.find { it.filename == "grails-dummy" }
    }

    void testGetImplicitPluginDirectories() {
        def pluginDirs = GrailsPluginUtils.getImplicitPluginDirectories()
        assertEquals 2, pluginDirs.size()
        assertNotNull pluginDirs.find { it.filename == "jsecurity-0.3" }
        assertNotNull pluginDirs.find { it.filename == "logging-0.1" }
        //assertNotNull pluginDirs.find { it.filename == "searchable-0.5" }
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
