package org.codehaus.groovy.grails.cli

class CreateUnitTestTests extends AbstractCliTests {
    def appDir

    void testCreateUnitTest() {
        appDir = createTestApp()

        tryUnitTest('Book')
        tryUnitTest('org.example.Author')
        tryUnitTest('project-item', 'ProjectItem')
    }

    void tryUnitTest(String className) {
        tryUnitTest(className, className)
    }

    void tryUnitTest(String scriptArg, String className) {
        // Run the create unit test script with a single argument.
        System.setProperty("grails.cli.args", scriptArg)
        gantRun( ["-f", "scripts/CreateUnitTest.groovy"] as String[])

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

        // Check that the unit test has been created.
        // Now check that the associated test has also been created.
        def testFile = new File("${appDir}/test/unit/${pkgPath}${className}Tests.groovy")
        assert testFile.exists()
        assert testFile.text =~ "^${pkg ? 'package ' + pkg : ''}\\s*class ${className}Tests extends GroovyTestCase \\{"
    }
}
