package org.codehaus.groovy.grails.cli;

import gant.Gant

class InstallTemplatesTests extends AbstractCliTests {
	
	
	void testInstallTemplatesCreatesTemplates() {				
		Gant.main(["-f", "scripts/CreateApp.groovy"] as String[])

		def templatesDirectory = "${appBase}/src/templates/"
        
		assertFalse "${templatesDirectory} exists, but should not", new File(templatesDirectory).exists()

		Gant.main(["-f", "scripts/InstallTemplates.groovy"] as String[])

		assertTrue "${templatesDirectory} does not exist", new File(templatesDirectory).exists()
		
		// expected templates to be installed
		def templates = [
		  "/artifacts/BootStrap.groovy"
		  /*"/artifacts/Controller.groovy",
		  "/artifacts/DataSource.groovy",
		  "/artifacts/DomainClass.groovy",
		  "/artifacts/Job.groovy",
		  "/artifacts/Service.groovy",
		  "/artifacts/TagLib.groovy",
		  "/artifacts/Tests.groovy",
		  "/artifacts/WebTest.groovy",
		  "/scaffolding/Controller.groovy",
		  "/scaffolding/create.gsp",
		  "/scaffolding/edit.gsp",
		  "/scaffolding/list.gsp",
		  "/scaffolding/show.gsp"*/
		]
		
		for (t in templates) {
      		assertTrue "${templatesDirectory}${t} does not exist", new File(templatesDirectory + t).exists()
    	}
		
	}

}
