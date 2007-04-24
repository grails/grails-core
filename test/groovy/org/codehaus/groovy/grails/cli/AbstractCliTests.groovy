package org.codehaus.groovy.grails.cli;


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

}
