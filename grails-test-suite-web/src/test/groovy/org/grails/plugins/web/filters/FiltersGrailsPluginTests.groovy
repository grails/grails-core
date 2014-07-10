package org.grails.plugins.web.filters

import org.grails.plugins.web.AbstractGrailsPluginTests
import grails.plugins.GrailsPlugin
import org.grails.plugins.web.controllers.ControllersGrailsPlugin
import org.grails.plugins.web.mapping.UrlMappingsGrailsPlugin

/**
 * @author Graeme Rocher
 * @since 1.0
 */
class FiltersGrailsPluginTests extends AbstractGrailsPluginTests {

    protected void onSetUp() {
        gcl.parseClass """
class Filters {
    def filters = {
        all(controller:"*", action:"*") {
            before = {

            }
            after = {

            }
            afterView = {

            }
        }
    }
}"""
        pluginsToLoad << gcl.loadClass(UrlMappingsGrailsPlugin.name)
        pluginsToLoad << gcl.loadClass(ControllersGrailsPlugin.name)
        pluginsToLoad << gcl.loadClass("org.grails.plugins.web.filters.FiltersGrailsPlugin")
    }

    void testSpringConfig() {
        assertTrue appCtx.containsBean("filterInterceptor")
        assertTrue appCtx.containsBean("Filters")
        assertTrue appCtx.containsBean("FiltersClass")
    }

    void testOnChange() {
        def newFilter = gcl.parseClass('''
class Filters {
    def filters = {
        all(controller:"author", action:"list") {
            before = {
                println "different"
            }
            after = {

            }
            afterView = {

            }
        }
    }
}
        ''')

        mockManager.getGrailsPlugin("filters").notifyOfEvent(GrailsPlugin.EVENT_ON_CHANGE, newFilter)

        assertTrue appCtx.containsBean("filterInterceptor")
        assertTrue appCtx.containsBean("Filters")
        assertTrue appCtx.containsBean("FiltersClass")

        def configs = appCtx.getBean("FiltersClass").getConfigs(appCtx.getBean("Filters"))

        assertEquals "author", configs[0].scope.controller
        assertEquals "list", configs[0].scope.action
    }
}
