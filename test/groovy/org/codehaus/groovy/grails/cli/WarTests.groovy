package org.codehaus.groovy.grails.cli;
 
class WarTests  extends AbstractCliTests {
	void testWAR() {
        createTestApp()

        // Call the War script.
        gantRun( ["-f", "scripts/War.groovy"] as String[])
		
		checkWarFile("testapp-0.1.war")
	}

    /**
     * Test the War script with a single command line argument that is
     * the name of the WAR file to create.
     */
    void testWARWithArg() {
        createTestApp()

		// Pass the name of the WAR file to the script.
        def warName = "myapp.war"
        System.setProperty("grails.cli.args", warName)
        gantRun( ["-f", "scripts/War.groovy"] as String[])

		checkWarFile(warName)
    }

    /**
     * Test the configuration property 'grails.war.destFile'.
     */
    void testWARWithConfigOption() {
        createTestApp()

		// Add the 'grails.war.destFile' configuration option to the
        // test application's Config.
        def warName = "config.war"
        new File("${appBase}/testapp/grails-app/conf", "Config.groovy") << "\ngrails.war.destFile = '${warName}'\n"

        // Clear the command-line arguments before calling the War script.
        gantRun( ["-f", "scripts/War.groovy"] as String[])

        checkWarFile(warName)
    }

    /**
     * Test an absolute path passed to the script.
     */
    void testWARWithAbsolutePath() {
        createTestApp()

		// Pass the name of the WAR file to the script.
        def warPath = new File(File.tempDir, "myapp.war").absolutePath
        try{
            System.setProperty("grails.cli.args", warPath)
            gantRun( ["-f", "scripts/War.groovy"] as String[])

            checkWarFile(warPath)
        }
        finally {
            new File(warPath).delete()
        }
    }

    void testCustomCopyStep() {
        createTestApp()

        // Add the 'grails.war.copyToWebApp' configuration option to
        // the test application's Config.
        new File("${appBase}/testapp/grails-app/conf", "Config.groovy") << '''
grails.war.copyToWebApp = {
    fileset(dir: "${basedir}/web-app") {
        include(name: "css/main.css")
        include(name: "js/application.js")
        include(name: "WEB-INF/**")
    }
    fileset(dir: basedir, includes: "dummy.txt")
}
'''

        // Create the dummy file that is included in the copy.
        new File("$appBase/testapp/dummy.txt").createNewFile()

        // Run the war script and do the standard checks.
        gantRun( ["-f", "scripts/War.groovy"] as String[])

        def unzippedFile = checkWarFile("testapp-0.1.war")

        // Check that the dummy file was copied across, but none of the
        // unspecified files in "web-app".
        assertTrue new File(unzippedFile, "dummy.txt").exists()
        assertFalse new File(unzippedFile, "images").exists()
        assertFalse new File(unzippedFile, "index.gsp").exists()
    }

    void testArtefactDescriptor() {
        def appDir = createTestApp()

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


        // Now create the WAR file and check it.
        gantRun( ["-f", "scripts/War.groovy"] as String[])
		checkWarFile("testapp-0.1.war")

        // The 'grails.xml' descriptor has now been unpacked, so check
        // that it contains the expected entries.
        def descriptor = new XmlSlurper().parse(new File("${appBase}/unzipped/WEB-INF/grails.xml"))
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
    
    /**
     * Checks that the WAR file with the given filename exists in the
     * expected location and contains all the required files.
     */
    File checkWarFile(String warPath) {
        // First check that the WAR file exists.
        def warFile = new File(warPath)
        if (!warFile.absolute) {
            warFile = new File("${appBase}/testapp", warPath)
        }
        warFile.parentFile.eachFile {
            println ">>> $it"
        }
        assert warFile.exists()

        // Check that the staging directory has been removed.
        assert !new File(warFile.parentFile, "staging").exists()

        // Now unpack the WAR and check that the critical files that
        // must be there actually are.
        ant.unzip(src:warFile.path, dest:"${appBase}/unzipped")

        def unzippedDir = new File("$appBase/unzipped")
        assert new File(unzippedDir, "WEB-INF/applicationContext.xml").exists()
		assert new File(unzippedDir, "WEB-INF/sitemesh.xml").exists()
		assert new File(unzippedDir, "WEB-INF/grails.xml").exists()
		assert new File(unzippedDir, "WEB-INF/web.xml").exists()
		assert new File(unzippedDir, "css/main.css").exists()
		assert new File(unzippedDir, "js/application.js").exists()
		assert new File(unzippedDir, "WEB-INF/grails-app/i18n/messages.properties").exists()

        return unzippedDir
    }
}
