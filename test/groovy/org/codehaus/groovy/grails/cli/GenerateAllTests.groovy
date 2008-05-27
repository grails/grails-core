package org.codehaus.groovy.grails.cli

import org.codehaus.groovy.grails.commons.GrailsClassUtils

class GenerateAllTests extends AbstractCliTests {
    def appDir

    void testGenerateAll() {
        appDir = createTestApp()

        // Create the domain class.
        new File("${appDir}/grails-app/domain/Book.groovy").withWriter { BufferedWriter writer ->
            writer << """\
class Book {
    String title
    String author
}
"""
        }

        // Now invoke the "generate-all" script for this domain class.
        tryGenerate("Book")

        // Create a domain class that has a package. The controller
        // should be put into the same package.
        def pkgDir = new File(appDir, "grails-app/domain/org/example")
        pkgDir.mkdirs()

        new File("${pkgDir}/Person.groovy").withWriter { BufferedWriter writer ->
            writer << """\
package org.example

class Person {
    String firstName
    String lastName
}
"""
        }

        tryGenerate("org.example.Person")
    }

    void tryGenerate(String className) {
        tryGenerate(className, className)
    }

    void tryGenerate(String scriptArg, String className) {
        // Run the create controller script with a single argument.
        System.setProperty("grails.cli.args", scriptArg)
        gantRun( ["-f", "scripts/GenerateAll.groovy"] as String[])

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

        // Now check that the associated views have also been created.
        def viewDir = "${appDir}/grails-app/views/${GrailsClassUtils.getPropertyName(className)}/"
		assertTrue "${viewDir} does not exist", new File(viewDir).exists()
        assertTrue "'list.gsp' is missing", new File(viewDir, "list.gsp").exists()
        assertTrue "'show.gsp' is missing", new File(viewDir, "show.gsp").exists()
        assertTrue "'create.gsp' is missing", new File(viewDir, "create.gsp").exists()
        assertTrue "'edit.gsp' is missing", new File(viewDir, "edit.gsp").exists()
    }
}
