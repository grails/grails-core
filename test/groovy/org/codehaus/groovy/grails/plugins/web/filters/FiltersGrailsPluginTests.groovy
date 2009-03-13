/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Oct 11, 2007
 */
package org.codehaus.groovy.grails.plugins.web.filters

import org.codehaus.groovy.grails.plugins.web.AbstractGrailsPluginTests
import org.codehaus.groovy.grails.plugins.GrailsPlugin

class FiltersGrailsPluginTests extends AbstractGrailsPluginTests{
	void onSetUp() {
		gcl.parseClass(
"""
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
}""")

        pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.web.filters.FiltersGrailsPlugin")

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