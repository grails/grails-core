package org.codehaus.groovy.grails.cli;

class CreateAppTests extends AbstractCliTests {
	
	
	void testCreateApp() {
	    
	    gantRun(['-f', 'scripts/CreateApp.groovy'] as String[])

	    // test basic structure
		assert new File("${appBase}/testapp").exists()
		assert new File("${appBase}/testapp/grails-app/conf/hibernate").exists()
		assert new File("${appBase}/testapp/grails-app/conf/spring").exists()
		assert new File("${appBase}/testapp/lib").exists()
		assert new File("${appBase}/testapp/src/java").exists()
		assert new File("${appBase}/testapp/src/groovy").exists()
		assert new File("${appBase}/testapp/web-app").exists()
		assert new File("${appBase}/testapp/web-app/WEB-INF").exists()
		
		assert new File("${appBase}/testapp/web-app/css").exists()
		assert new File("${appBase}/testapp/web-app/js").exists()
		
		assert new File("${appBase}/testapp/grails-app/controllers").exists()
		assert new File("${appBase}/testapp/grails-app/domain").exists()
		assert new File("${appBase}/testapp/grails-app/conf").exists()
		assert new File("${appBase}/testapp/grails-app/services").exists()
		assert new File("${appBase}/testapp/grails-app/views").exists()
		assert new File("${appBase}/testapp/grails-app/taglib").exists()
		
		// test critical files
		assert new File("${appBase}/testapp/web-app/WEB-INF/applicationContext.xml").exists()
		assert new File("${appBase}/testapp/web-app/WEB-INF/sitemesh.xml").exists()
		assert new File("${appBase}/testapp/web-app/css/main.css").exists()
		assert new File("${appBase}/testapp/web-app/js/application.js").exists()
		assert new File("${appBase}/testapp/grails-app/conf/BootStrap.groovy").exists()
		assert new File("${appBase}/testapp/grails-app/conf/DataSource.groovy").exists()
		assert new File("${appBase}/testapp/grails-app/conf/Config.groovy").exists()
		assert new File("${appBase}/testapp/grails-app/i18n/messages.properties").exists()
		assert new File("${appBase}/testapp/grails-app/views/error.gsp").exists()
		assert new File("${appBase}/testapp/grails-app/views/layouts/main.gsp").exists()
		
	}
}
