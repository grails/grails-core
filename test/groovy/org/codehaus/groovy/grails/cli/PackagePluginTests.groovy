package org.codehaus.groovy.grails.cli;

class PackagePluginTests extends AbstractCliTests {

	void testPackagePlugin() {
        System.setProperty("grails.cli.args", "MyTest")
		gantRun( ["-f", "scripts/CreatePlugin.groovy"] as String[])
        def appDir = appBase + File.separatorChar + System.getProperty("grails.cli.args")

        // Create some artefacts in packages.
        def pkgDir = new File("${appDir}/grails-app/domain/org/example/domain")
        pkgDir.mkdirs()
        new File(pkgDir, "Book.groovy").withWriter { writer ->
            writer << """\
package org.example.domain

class Book {
    String title
    String author
}
"""
        }

        pkgDir = new File("${appDir}/grails-app/controllers/org/example/controllers")
        pkgDir.mkdirs()
        new File(pkgDir, "BookController.groovy").withWriter { writer ->
            writer << """\
package org.example.controllers

class BookController {
    def scaffold = true
}
"""
        }

        // And a domain class without a package.
        new File(pkgDir, "Person.groovy").withWriter { writer ->
            writer << """\
class Person {
    String firstName
    String lastName
}
"""
        }

        // And finally, a domain class whose package includes "groovy"
        // (GRAILS-2846).
        pkgDir = new File("${appDir}/grails-app/domain/org/groovy/example")
        pkgDir.mkdirs()
        new File(pkgDir, "Item.groovy").withWriter { writer ->
            writer << """\
package org.groovy.example

class Item {
    String name
    String colour
}
"""
        }

        // Now do the packaging.
        System.setProperty("base.dir", appDir)
        gantRun( ["-f", "scripts/PackagePlugin.groovy"] as String[])
        assertTrue "Plugin package missing.", new File("${appDir}/grails-my-test-0.1.zip").exists()
        assertTrue "Plugin descriptor missing.", new File("${appDir}/plugin.xml").exists()
        ant.unzip(src:"${appDir}/grails-my-test-0.1.zip", dest:"${appBase}/unzipped")

        assertTrue new File("${appBase}/unzipped").exists()
        // test critical files
        assertTrue new File("${appBase}/unzipped/MyTestGrailsPlugin.groovy").exists()
        assertTrue new File("${appBase}/unzipped/plugin.xml").exists()

        // Check that the plugin descriptor contains the expected entries.
        def descriptor = new XmlSlurper().parse(new File("${appBase}/unzipped/plugin.xml"))
        assertNotNull(
                "Book domain resource missing.",
                descriptor.resources.resource.find { it.text() == "org.example.domain.Book" })
        assertNotNull(
                "Book controller resource missing.",
                descriptor.resources.resource.find { it.text() == "org.example.controllers.BookController" })
        assertNotNull(
                "Person domain resource missing.",
                descriptor.resources.resource.find { it.text() == "Person" })
        assertNotNull(
                "Item domain resource missing.",
                descriptor.resources.resource.find { it.text() == "org.groovy.example.Item" })
    }

    void testPackagePluginExcludesGroovyResources() {
        System.setProperty("grails.cli.args", "MyTest")
        gantRun( ["-f", "scripts/CreatePlugin.groovy"] as String[])
        def appDir = appBase + File.separatorChar + System.getProperty("grails.cli.args")

        new File( appDir + File.separatorChar + "grails-app" + File.separatorChar + "conf" +
                File.separatorChar + "spring", "resources.groovy").withPrintWriter {
            """
beans = {
}
""" }

        System.setProperty("base.dir", appDir)
        gantRun( ["-f", "scripts/PackagePlugin.groovy"] as String[])
        assertTrue new File("${appDir}/grails-my-test-0.1.zip").exists()
        ant.unzip(src:"${appDir}/grails-my-test-0.1.zip", dest:"${appBase}/unzipped")

        assertTrue new File("${appBase}/unzipped").exists()

        // Ensure the file was not put into plugin
        assertFalse new File("${appBase}/unzipped/grails-app/conf/spring/resources.groovy").exists()
    }

}
