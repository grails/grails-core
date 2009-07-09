package org.codehaus.groovy.grails.plugins.web

import org.codehaus.groovy.grails.plugins.*
import org.codehaus.groovy.grails.commons.GrailsApplication
import org.apache.commons.logging.Log
import org.codehaus.groovy.grails.commons.DefaultGrailsApplication

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

    void testLoggingPluginBeforeCore() {
        def pluginManager = new DefaultGrailsPluginManager([] as Class[], new DefaultGrailsApplication())

        pluginManager.loadPlugins()

        def core =  pluginManager.getGrailsPlugin("core")
        def logging =  pluginManager.getGrailsPlugin("logging")

        assertTrue "logging plugin should have loaded before core",pluginManager.pluginList.indexOf(core) > pluginManager.pluginList.indexOf(logging)
    }

	void testLogAvailableToController() {
        def registry = GroovySystem.metaClassRegistry

//        registry.removeMetaClass(controllerClass)

        def controller = controllerClass.newInstance()
        assertTrue controller.log instanceof Log
	}

	void testLogAvailableToService() {
        def registry = GroovySystem.metaClassRegistry

//        registry.removeMetaClass(serviceClass)

        def service = serviceClass.newInstance()
        assertTrue service.log instanceof Log
	}

	void testLogAvailableToTagLib() {
        def registry = GroovySystem.metaClassRegistry

//        registry.removeMetaClass(serviceClass)

        def taglib = taglibClass.newInstance()
        assertTrue taglib.log instanceof Log
	}
}