package org.codehaus.groovy.grails.cli;

import gant.Gant

class CreateControllerTests extends AbstractCliTests {
	
	
	void testCreateControllerCreatesViewDirectory() {				
		Gant.main(["-f", "scripts/CreateApp.groovy"] as String[])

        def bookViewDirectory = "${appBase}/grails-app/views/book/"
        
		assertFalse "${bookViewDirectory} exists, but should not", new File(bookViewDirectory).exists()

		System.setProperty("grails.cli.args", "Book")
		Gant.main(["-f", "scripts/CreateController.groovy"] as String[])

		assertTrue "${bookViewDirectory} does not exist", new File(bookViewDirectory).exists()
	}

}
