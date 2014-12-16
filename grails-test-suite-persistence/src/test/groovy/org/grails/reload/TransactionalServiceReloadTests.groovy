package org.grails.reload

import org.grails.plugins.MockHibernateGrailsPlugin
import org.grails.plugins.web.AbstractGrailsPluginTests

/**
 * Tests for auto-reloading of transactional services.
 *
 * @author Graeme Rocher
 */
class TransactionalServiceReloadTests extends AbstractGrailsPluginTests {

    def service1 = '''
class TransactionalService {
    def transactional = true

    def myMethod() {
        "bar"
    }
}
    '''

    void testReloadTransactionalService() {
        def testService = appCtx.getBean("transactionalService")

        assertEquals "foo", testService.myMethod()

        testService = ga.getServiceClass("TransactionalService").newInstance()

        assertEquals "foo", testService.myMethod()

        def event = [source:gcl.parseClass(service1), ctx:appCtx]

        def plugin = mockManager.getGrailsPlugin("services")

        plugin.instance.onChange(event)

        def newService = ga.getServiceClass("TransactionalService").newInstance()

        assertEquals "bar", newService.myMethod()

        newService = appCtx.getBean("transactionalService")

        assertEquals "bar", newService.myMethod()
    }

    protected void onSetUp() {
        gcl.parseClass '''
class TransactionalService {
    def transactional = true

    def myMethod() {
        "foo"
    }
}
'''

    gcl.parseClass '''\
dataSource {
    pooling = true
    logSql = true
    dbCreate = "create-drop" // one of 'create', 'create-drop','update'
    url = "jdbc:h2:mem:testDB"
    driverClassName = "org.h2.Driver"
    username = "sa"
    password = ""
}
''', "DataSource"

       pluginsToLoad << gcl.loadClass("org.grails.plugins.CoreGrailsPlugin")
       pluginsToLoad << gcl.loadClass("org.grails.plugins.i18n.I18nGrailsPlugin")
       pluginsToLoad << gcl.loadClass("org.grails.plugins.domain.DomainClassGrailsPlugin")
       pluginsToLoad << gcl.loadClass("org.grails.plugins.datasource.DataSourceGrailsPlugin")
       pluginsToLoad << MockHibernateGrailsPlugin
       pluginsToLoad << gcl.loadClass("org.grails.plugins.services.ServicesGrailsPlugin")
    }
}
