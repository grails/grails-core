package org.codehaus.groovy.grails.cli;

import gant.Gant

class WarTests  extends AbstractCliTests {

	void testWAR() {
		Gant.main(["-f", "scripts/CreateApp.groovy"] as String[])
		
		
		System.setProperty("base.dir", "${appBase}/testapp")
		Gant.main(["-f", "scripts/War.groovy"] as String[])
		
		assert new File("${appBase}/testapp/testapp.war").exists()
		
		ant.unzip(src:"${appBase}/testapp/testapp.war", dest:"${appBase}/unzipped")
		
		// test critical files
		assert new File("${appBase}/unzipped/WEB-INF/applicationContext.xml").exists()
		assert new File("${appBase}/unzipped/WEB-INF/sitemesh.xml").exists()
		assert new File("${appBase}/unzipped/WEB-INF/web.template.xml").exists()
		assert new File("${appBase}/unzipped/css/main.css").exists()
		assert new File("${appBase}/unzipped/js/application.js").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/conf/ApplicationBootStrap.groovy").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/conf/DevelopmentDataSource.groovy").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/conf/ProductionDataSource.groovy").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/conf/TestDataSource.groovy").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/conf/log4j.development.properties").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/conf/log4j.production.properties").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/conf/log4j.test.properties").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/i18n/messages.properties").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/views/error.gsp").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/views/layouts/main.gsp").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/taglib/ApplicationTagLib.groovy").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/taglib/FormTagLib.groovy").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/taglib/JavascriptTagLib.groovy").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/taglib/RenderTagLib.groovy").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/taglib/UITagLib.groovy").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/taglib/ValidationTagLib.groovy").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/controllers").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/domain").exists()
		assert new File("${appBase}/unzipped/WEB-INF/grails-app/services").exists()
		
		
	}

}
