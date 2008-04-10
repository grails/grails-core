package org.codehaus.groovy.grails.plugins.web

import org.codehaus.groovy.grails.commons.test.*
import org.codehaus.groovy.grails.commons.spring.*
import org.codehaus.groovy.grails.plugins.*
import org.codehaus.groovy.runtime.*
import groovy.mock.interceptor.*
import org.apache.commons.logging.LogFactory
import org.apache.commons.logging.impl.NoOpLog
import org.codehaus.groovy.grails.commons.GrailsApplication

class LoggingGrailsPluginTests extends AbstractGrailsPluginTests {   

    def controllerClass
    def serviceClass
    def taglibClass

    void onTearDown() {
        controllerClass = null
        serviceClass = null
        taglibClass = null
    }

	void onSetUp() {
		controllerClass = gcl.parseClass(
				"""
				class TestController {
				}
"""
        )

		serviceClass = gcl.parseClass(
				"""
				class TestService {
				}
"""
        )

		taglibClass = gcl.parseClass(
				"""
				class TestTagLib {
				}
"""
        )

		pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
		pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.LoggingGrailsPlugin")
	}

    void testDoWithWebDescriptor() {
        String xmlText = '''
<web-app>

	<context-param>
		<param-name>log4jConfigLocation</param-name>
		<param-value>/WEB-INF/classes/log4j.properties</param-value>
	</context-param>
</web-app>
'''
        System.setProperty('current.gant.script','')

        def xml = new XmlSlurper().parseText( xmlText  )
        System.setProperty(GrailsApplication.PROJECT_RESOURCES_DIR, "/test")
        def plugin = new LoggingGrailsPlugin()
        plugin.doWithWebDescriptor.delegate = [application:ga]
        plugin.doWithWebDescriptor(xml)

        assertEquals 'file:/test/log4j.properties',xml.'context-param'.'param-value'.text()

        println xml

        System.setProperty('current.gant.script','war')

        xml = new XmlSlurper().parseText( xmlText  )
        plugin.doWithWebDescriptor(xml)

        assertEquals '/WEB-INF/classes/log4j.properties',xml.'context-param'.'param-value'.text()
        
    }

	void testLogAvailableToController() {
        def registry = GroovySystem.metaClassRegistry

        registry.removeMetaClass(controllerClass)

        def controller = controllerClass.newInstance()
        assertEquals "grails.app.controller.TestController", controller.log?.name
	}

	void testLogAvailableToService() {
        def registry = GroovySystem.metaClassRegistry

        registry.removeMetaClass(serviceClass)

        def service = serviceClass.newInstance()
        assertEquals "grails.app.service.TestService", service.log?.name
	}

	void testLogAvailableToTagLib() {
        def registry = GroovySystem.metaClassRegistry

        registry.removeMetaClass(serviceClass)

        def taglib = taglibClass.newInstance()
        assertEquals "grails.app.tagLib.TestTagLib", taglib.log?.name
	}
}