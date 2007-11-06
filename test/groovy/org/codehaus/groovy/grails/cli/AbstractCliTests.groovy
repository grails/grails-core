package org.codehaus.groovy.grails.cli;

import org.codehaus.groovy.tools.RootLoader
import org.codehaus.groovy.tools.LoaderConfiguration

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
		ant.delete(dir:appBase, failonerror:false)
		ant = null
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

	    gant.process(args)
	}
}
