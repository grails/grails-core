package org.codehaus.groovy.grails.cli

class CreateIntegrationTestTests extends AbstractCliTests {
    def appDir

    void testCreateIntegrationTest() {
        appDir = createTestApp()

        tryIntegrationTest('Book')
        tryIntegrationTest('org.example.Author')
        tryIntegrationTest('project-item', 'ProjectItem')
    }

    void tryIntegrationTest(String className) {
        tryIntegrationTest(className, className)
    }

    void tryIntegrationTest(String scriptArg, String className) {
        // Run the create integration test script with a single argument.
        System.setProperty("grails.cli.args", scriptArg)
        gantRun( ["-f", "scripts/CreateIntegrationTest.groovy"] as String[])

        // Extract any package from the class name.
        def pkg = null
        def pos = className.lastIndexOf('.')
        if (pos != -1) {
            pkg = className[0..<pos]
            className = className[(pos + 1)..-1]
        }

        def pkgPath = ''
        if (pkg) {
            pkgPath = pkg.replace('.' as char, '/' as char) + '/'
        }

        // Check that the integration test has been created.
        def testFile = new File("${appDir}/test/integration/${pkgPath}${className}Tests.groovy")
        assert testFile.exists()
        assert testFile.text =~ "^${pkg ? 'package ' + pkg : ''}\\s*class ${className}Tests extends GroovyTestCase \\{"
    }
}
