package org.grails.reload

import org.grails.plugins.web.AbstractGrailsPluginTests

/**
 * Tests for auto-reloading of services
 *
 * @author Graeme Rocher
 */
class ServiceReloadTests extends AbstractGrailsPluginTests {

    def service1 = '''
class NonTransactionalService {
    def transactional = false

    def myMethod() {
        "foo"
    }
}    '''

    void testReloadTransactionalService() {
        def testService = appCtx.getBean("nonTransactionalService")
        assertEquals "bar", testService.myMethod()

        testService = ga.getServiceClass("NonTransactionalService").newInstance()
        assertEquals "bar", testService.myMethod()

        def newGcl = new GroovyClassLoader()
        def event = [source:newGcl.parseClass(service1), ctx:appCtx]
        def plugin = mockManager.getGrailsPlugin("services")
        plugin.instance.onChange(event)

        def newService = ga.getServiceClass("NonTransactionalService").newInstance()
        assertEquals "foo", newService.myMethod()

        newService = appCtx.getBean("nonTransactionalService")
        assertEquals "foo", newService.myMethod()
    }

    void onSetUp() {
        gcl.parseClass '''
class NonTransactionalService {
    def transactional = false

    def myMethod() {
        "bar"
    }
}
'''
        pluginsToLoad << gcl.loadClass("org.grails.plugins.datasource.DataSourceGrailsPlugin")
        pluginsToLoad << gcl.loadClass("org.grails.plugins.services.ServicesGrailsPlugin")
    }
}
