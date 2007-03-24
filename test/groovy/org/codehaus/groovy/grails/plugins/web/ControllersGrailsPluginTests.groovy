package org.codehaus.groovy.grails.plugins.web

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*
import org.codehaus.groovy.grails.web.plugins.*

class ControllersGrailsPluginTests extends AbstractGrailsPluginTests {
	
	
	void onSetUp() {
		gcl.parseClass(
"""
class TestController {
   def list = {}			
}
""")

        pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
        pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")
        pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.ControllersGrailsPlugin")

	}
	
	void testControllersPlugin() {		
		assert appCtx.containsBean("TestControllerTargetSource")
		assert appCtx.containsBean("TestControllerProxy")
		assert appCtx.containsBean("TestControllerClass")
		assert appCtx.containsBean("TestController")
	}
}