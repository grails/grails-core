package org.codehaus.groovy.grails.cli

class CreateDomainClassTests extends AbstractCliTests {
    def appDir

    void testCreateDomainClass() {
        appDir = createTestApp()

        tryDomain('Book')
        tryDomain('org.example.Author')
        tryDomain('project-item', 'ProjectItem')
    }

    void tryDomain(String className) {
        tryDomain(className, className)
    }

    void tryDomain(String scriptArg, String className) {
        // Run the create domain class script with a single argument.
        System.setProperty("grails.cli.args", scriptArg)
        gantRun( ["-f", "scripts/CreateDomainClass.groovy"] as String[])

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

        // Check that the domain class has been created.
        def dcFile = new File("${appDir}/grails-app/domain/${pkgPath}${className}.groovy")
        assert dcFile.exists()
        assert dcFile.text =~ "^${pkg ? 'package ' + pkg : ''}\\s*class ${className} \\{"

        // Now check that the associated test has also been created.
        def testFile = new File("${appDir}/test/integration/${pkgPath}${className}Tests.groovy")
        assert testFile.exists()
        assert testFile.text =~ "^${pkg ? 'package ' + pkg : ''}\\s*class ${className}Tests extends GroovyTestCase \\{"
    }
}
