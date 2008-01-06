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

    /**
     * Checks that the WAR file with the given filename exists in the
     * expected location and contains all the required files.
     */
    void checkWarFile(String warPath) {
        // First check that the WAR file exists.
        def warFile = new File(warPath)
        if (!warFile.absolute) {
            warFile = new File("${appBase}/testapp", warPath)
        }
        assert warFile.exists()

        // Check that the staging directory has been removed.
        assert !new File(warFile.parentFile, "staging").exists()

        // Now unpack the WAR and check that the critical files that
        // must be there actually are.
        ant.unzip(src:warFile.path, dest:"${appBase}/unzipped")
		assert new File("${appBase}/unzipped/WEB-INF/applicationContext.xml").exists()
		assert new File("${appBase}/unzipped/WEB-INF/sitemesh.xml").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails.xml").exists()
		assert new File("${appBase}/unzipped/WEB-INF/web.xml").exists()
		assert new File("${appBase}/unzipped/css/main.css").exists()
		assert new File("${appBase}/unzipped/js/application.js").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/i18n/messages.properties").exists()
    }
}
