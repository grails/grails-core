package org.codehaus.groovy.grails.cli;

class PackagePluginTests extends AbstractCliTests {

	void testPackagePlugin() {
        System.setProperty("grails.cli.args", "MyTest")
		gantRun( ["-f", "scripts/CreatePlugin.groovy"] as String[])
        def appDir = appBase + File.separatorChar + System.getProperty("grails.cli.args")

		System.setProperty("base.dir", appDir)
        gantRun( ["-f", "scripts/PackagePlugin.groovy"] as String[])
        assertTrue new File("${appDir}/grails-my-test-0.1.zip").exists()
        assertTrue new File("${appDir}/plugin.xml").exists()
        ant.unzip(src:"${appDir}/grails-my-test-0.1.zip", dest:"${appBase}/unzipped")

        assertTrue new File("${appBase}/unzipped").exists()
        // test critical files
        assertTrue new File("${appBase}/unzipped/MyTestGrailsPlugin.groovy").exists()
        assertTrue new File("${appBase}/unzipped/plugin.xml").exists()
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
