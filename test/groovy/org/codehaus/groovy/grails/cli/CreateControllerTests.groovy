package org.codehaus.groovy.grails.cli;

class CreateControllerTests extends AbstractCliTests {
    def appDir

    void testCreateController() {
        appDir = createTestApp()

        tryController('Book')
        tryController('org.example.Author')
        tryController('project-item', 'ProjectItem')
    }
	
	void testCreateControllerCreatesViewDirectory() {
        appDir = createTestApp()

        def bookViewDirectory = "${appDir}/grails-app/views/book/"
        
		assertFalse "${bookViewDirectory} exists, but should not", new File(bookViewDirectory).exists()

		System.setProperty("grails.cli.args", "Book")
		gantRun( ["-f", "scripts/CreateController.groovy"] as String[])

		assertTrue "${bookViewDirectory} does not exist", new File(bookViewDirectory).exists()
	}

    void tryController(String className) {
        tryController(className, className)
    }

    void tryController(String scriptArg, String className) {
        // Run the create controller script with a single argument.
        System.setProperty("grails.cli.args", scriptArg)
        gantRun( ["-f", "scripts/CreateController.groovy"] as String[])

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

        // Check that the controller has been created.
        def controllerFile = new File("${appDir}/grails-app/controllers/${pkgPath}${className}Controller.groovy")
        assert controllerFile.exists()
        assert controllerFile.text =~ "^${pkg ? 'package ' + pkg : ''}\\s*class ${className}Controller \\{"

        // Now check that the associated test has also been created.
        def testFile = new File("${appDir}/test/integration/${pkgPath}${className}ControllerTests.groovy")
        assert testFile.exists()
        assert testFile.text =~ "^${pkg ? 'package ' + pkg : ''}\\s*class ${className}ControllerTests extends GroovyTestCase \\{"
    }
}
