package org.codehaus.groovy.grails.cli;

class CreatePluginTests extends AbstractCliTests {
	
	
	void testCreatePlugin() {
        System.setProperty("grails.cli.args", "MyTest")
		gantRun( ["-f", "scripts/CreatePlugin.groovy"] as String[])
		
		// test basic structure
		assertTrue new File("${appBase}/MyTest").exists()
		assertTrue new File("${appBase}/MyTest/grails-app/conf/hibernate").exists()
		assertTrue new File("${appBase}/MyTest/grails-app/conf/spring").exists()
		assertTrue new File("${appBase}/MyTest/lib").exists()
		assertTrue new File("${appBase}/MyTest/src/java").exists()
		assertTrue new File("${appBase}/MyTest/src/groovy").exists()
		assertTrue new File("${appBase}/MyTest/web-app").exists()
		assertTrue new File("${appBase}/MyTest/web-app/WEB-INF").exists()
		
		assertTrue new File("${appBase}/MyTest/web-app/css").exists()
		assertTrue new File("${appBase}/MyTest/web-app/js").exists()
		
		assertTrue new File("${appBase}/MyTest/grails-app/controllers").exists()
		assertTrue new File("${appBase}/MyTest/grails-app/domain").exists()
		assertTrue new File("${appBase}/MyTest/grails-app/conf").exists()
		assertTrue new File("${appBase}/MyTest/grails-app/services").exists()
		assertTrue new File("${appBase}/MyTest/grails-app/views").exists()
		assertTrue new File("${appBase}/MyTest/grails-app/taglib").exists()
		
		// test critical files
        assertTrue new File("${appBase}/MyTest/MyTestGrailsPlugin.groovy").exists()
		assertTrue new File("${appBase}/MyTest/web-app/WEB-INF/applicationContext.xml").exists()
		assertTrue new File("${appBase}/MyTest/web-app/WEB-INF/sitemesh.xml").exists()		
	}

}
