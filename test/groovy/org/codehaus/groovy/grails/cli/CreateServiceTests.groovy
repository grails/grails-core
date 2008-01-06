package org.codehaus.groovy.grails.cli

class CreateServiceTests extends AbstractCliTests {
    def appDir

    void testCreateService() {
        appDir = createTestApp()

        tryService('Book')
        tryService('org.example.Author')
        tryService('project-item', 'ProjectItem')
    }

    void tryService(String className) {
        tryService(className, className)
    }

    void tryService(String scriptArg, String className) {
        // Run the create service script with a single argument.
        System.setProperty("grails.cli.args", scriptArg)
        gantRun( ["-f", "scripts/CreateService.groovy"] as String[])

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

        // Check that the service has been created.
        def dcFile = new File("${appDir}/grails-app/services/${pkgPath}${className}Service.groovy")
        assert dcFile.exists()
        assert dcFile.text =~ "^${pkg ? 'package ' + pkg : ''}\\s*class ${className}Service \\{"

        // Now check that the associated test has also been created.
        def testFile = new File("${appDir}/test/integration/${pkgPath}${className}ServiceTests.groovy")
        assert testFile.exists()
        assert testFile.text =~ "^${pkg ? 'package ' + pkg : ''}\\s*class ${className}ServiceTests extends GroovyTestCase \\{"
    }
}