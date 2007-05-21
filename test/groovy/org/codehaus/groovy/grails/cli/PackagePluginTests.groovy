package org.codehaus.groovy.grails.cli;

import gant.Gant

class PackagePluginTests extends AbstractCliTests {

	void testPackagePlugin() {
        System.setProperty("grails.cli.args", "MyTest")
		Gant.main(["-f", "scripts/CreatePlugin.groovy"] as String[])
        def appDir = appBase + File.separatorChar + System.getProperty("grails.cli.args")
		System.setProperty("base.dir", appDir)
        Gant.main(["-f", "scripts/PackagePlugin.groovy"] as String[])
        assertTrue new File("${appDir}/grails-my-test-0.1.zip").exists()
        ant.unzip(src:"${appDir}/grails-my-test-0.1.zip", dest:"${appBase}/unzipped")

        assertTrue new File("${appBase}/unzipped").exists()
        assertTrue new File("${appBase}/unzipped/hibernate").exists()
        assertTrue new File("${appBase}/unzipped/lib").exists()
        assertTrue new File("${appBase}/unzipped/src").exists()
        assertTrue new File("${appBase}/unzipped/web-app").exists()
        assertTrue new File("${appBase}/unzipped/grails-app").exists()

        // test critical files
        assertTrue new File("${appBase}/unzipped/MyTestGrailsPlugin.groovy").exists()
    }

}
