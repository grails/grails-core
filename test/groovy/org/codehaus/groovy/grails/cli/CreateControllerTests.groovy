package org.codehaus.groovy.grails.cli;

class CreateControllerTests extends AbstractCliTests {
	
	
	void testCreateControllerCreatesViewDirectory() {
	    println "CREATE APP"

		gantRun( ["-f", "scripts/CreateApp.groovy"] as String[])

        // Correct the base dir now
        def appDir = appBase + File.separatorChar + System.getProperty("grails.cli.args")
		System.setProperty("base.dir", appDir)

        def bookViewDirectory = "${appDir}/grails-app/views/book/"
        
		assertFalse "${bookViewDirectory} exists, but should not", new File(bookViewDirectory).exists()

	    println "CREATE CONTROLLER"

		System.setProperty("grails.cli.args", "Book")
		gantRun( ["-f", "scripts/CreateController.groovy"] as String[])

		assertTrue "${bookViewDirectory} does not exist", new File(bookViewDirectory).exists()
	}

}
