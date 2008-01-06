package org.codehaus.groovy.grails.cli

class CreateTagLibTests extends AbstractCliTests {
    def appDir

    void testCreateTagLib() {
        appDir = createTestApp()

        tryTagLib('Book')
        tryTagLib('org.example.Author')
        tryTagLib('project-item', 'ProjectItem')
    }

    void tryTagLib(String className) {
        tryTagLib(className, className)
    }

    void tryTagLib(String scriptArg, String className) {
        // Run the create tag lib script with a single argument.
        System.setProperty("grails.cli.args", scriptArg)
        gantRun( ["-f", "scripts/CreateTagLib.groovy"] as String[])

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

        // Check that the tag lib has been created.
        def dcFile = new File("${appDir}/grails-app/taglib/${pkgPath}${className}TagLib.groovy")
        assert dcFile.exists()
        assert dcFile.text =~ "^${pkg ? 'package ' + pkg : ''}\\s*class ${className}TagLib \\{"

        // Now check that the associated test has also been created.
        def testFile = new File("${appDir}/test/integration/${pkgPath}${className}TagLibTests.groovy")
        assert testFile.exists()
        assert testFile.text =~ "^${pkg ? 'package ' + pkg : ''}\\s*class ${className}TagLibTests extends GroovyTestCase \\{"
    }
}
