package org.codehaus.groovy.grails.cli;

import gant.Gant

class InstallTemplatesTests extends AbstractCliTests {
	
	
	void testInstallTemplatesCreatesTemplates() {				
		Gant.main(["-f", "scripts/CreateApp.groovy"] as String[])

        def appDir = appBase + File.separatorChar + System.getProperty("grails.cli.args")
		System.setProperty("base.dir", appDir)
		def templatesDirectory = "${appDir}/src/templates/"
        
		assertFalse "${templatesDirectory} exists, but should not", new File(templatesDirectory).exists()

		Gant.main(["-f", "scripts/InstallTemplates.groovy"] as String[])

		assertTrue "${templatesDirectory} does not exist", new File(templatesDirectory).exists()
		
		// expected templates to be installed
		def templates = [
		  "/artifacts/BootStrap.groovy",
		  "/artifacts/Controller.groovy",
		  "/artifacts/DataSource.groovy",
		  "/artifacts/DomainClass.groovy",
		  "/artifacts/Service.groovy",
		  "/artifacts/TagLib.groovy",
		  "/artifacts/Tests.groovy",
		  "/artifacts/WebTest.groovy",
		  "/scaffolding/Controller.groovy",
		  "/scaffolding/create.gsp",
		  "/scaffolding/edit.gsp",
		  "/scaffolding/list.gsp",
		  "/scaffolding/show.gsp",
		  "/scaffolding/renderEditor.template"
		]
		
		for (t in templates) {
      		assertTrue "${templatesDirectory}${t} does not exist", new File(templatesDirectory + t).exists()
    	}
		
	}

}
