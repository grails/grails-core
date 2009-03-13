package org.codehaus.groovy.grails.test

import grails.util.BuildSettings
import org.springframework.core.io.Resource

/**
 * Test case for {@link DefaultGrailsTestHelper}.
 */
class DefaultGrailsTestHelperTests extends GroovyTestCase {
    BuildSettings buildSettings
    DefaultGrailsTestHelper testHelper

    protected void setUp() {
        this.buildSettings = new BuildSettings()
        this.testHelper = new DefaultGrailsTestHelper(this.buildSettings, getClass().classLoader) {
            [] as Resource[]
        }
    }
    void testFileToClassName() {
        // Basic test to ensure that the file path is correctly converted
        // to a fully qualified class name.
        String name = testHelper.fileToClassName(new File("src/groovy/org/example/Test.groovy"), new File("src/groovy"))
        assertEquals "org.example.Test", name

        // Test a class that's in the default package.
        name = testHelper.fileToClassName(new File("src/groovy/Test.groovy"), new File("src/groovy"))
        assertEquals "Test", name

        // Test a file that's relative to the current working dir.
        name = testHelper.fileToClassName(new File("org/example/Test.groovy"), new File(""))
        assertEquals "org.example.Test", name

        // An exception should be thrown if the target file is not
        // a descendent of the base directory.
        shouldFail(IllegalArgumentException) {
            testHelper.fileToClassName(new File("src/groovy/org/example/Test.groovy"), new File("src/java"))
        }
    }
}
