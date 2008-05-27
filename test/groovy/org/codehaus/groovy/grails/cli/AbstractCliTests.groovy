package org.codehaus.groovy.grails.cli;

import org.codehaus.groovy.tools.RootLoader
import org.codehaus.groovy.tools.LoaderConfiguration
import org.codehaus.groovy.grails.plugins.PluginManagerHolder

abstract class AbstractCliTests extends GroovyTestCase {

	protected appBase = "test/cliTestApp"
	protected ant = new AntBuilder()

	void setUp() {
		ant.delete(dir:appBase, failonerror:false)
		System.setProperty("base.dir", appBase)
		System.setProperty("grails.cli.args", "testapp")
		System.setProperty("grails.cli.testing", "true")
		System.setProperty("env.GRAILS_HOME", new File("").absolutePath)
	}
	
	void tearDown() {
        PluginManagerHolder.pluginManager = null
        ant.delete(dir:appBase, failonerror:false)
		ant = null
	}

	protected String createTestApp() {
        // Pass the name of the test project to the create-app script.
        def appName = "testapp"
        System.setProperty("grails.cli.args", appName)

        // Create the application.
	    gantRun( ["-f", "scripts/CreateApp.groovy"] as String[])

	    // Update the base directory to the application dir.
        def appDir = appBase + File.separator + appName
        System.setProperty("base.dir", appDir)

		// Finally, clear the CLI arguments.
        System.setProperty("grails.cli.args", "")

        // Return the path to the new app.
        return appDir
    }

	protected void gantRun(final String[] args) {

	    LoaderConfiguration loaderConfig = new LoaderConfiguration()
	    loaderConfig.setRequireMain(false);
	    
	    def libDir = new File('lib')
	    assert libDir.exists()
	    assert libDir.isDirectory()

	    libDir.eachFileMatch(~/gant.*\.jar/) {jarFile ->
	        loaderConfig.addFile(jarFile)
	    }

	    def rootLoader = new RootLoader(loaderConfig)
	    def gant = rootLoader.loadClass('gant.Gant', false).newInstance()

	    gant.processArgs(args)
	}
}
