package org.codehaus.groovy.grails.reload;

 import org.codehaus.groovy.grails.web.servlet.mvc.*
 import org.codehaus.groovy.grails.commons.*
 import org.apache.commons.logging.*
 import org.codehaus.groovy.grails.web.plugins.*

/**
 * Tests for auto-reloading of transactional services
 *
 * @author Graeme Rocher
 **/

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


            def newGcl = new GroovyClassLoader()

            def event = [source:newGcl.parseClass(service1),
                         ctx:appCtx]

            def plugin = mockManager.getGrailsPlugin("services")

            def eventHandler = plugin.instance.onChange
            eventHandler.delegate = plugin
            eventHandler.call(event)


            def newService = ga.getServiceClass("TransactionalService").newInstance()

            assertEquals "bar", newService.myMethod()

            newService = appCtx.getBean("transactionalService")

            assertEquals "bar", newService.myMethod()
            
    }

	void onSetUp() {
		gcl.parseClass(
'''
class TransactionalService {
    def transactional = true

    def myMethod() {
        "foo"
    }
}
class ApplicationDataSource {
	   boolean pooling = true
	   boolean logSql = true
	   String dbCreate = "create-drop" // one of 'create', 'create-drop','update'
	   String url = "jdbc:hsqldb:mem:testDB"
	   String driverClassName = "org.hsqldb.jdbcDriver"
	   String username = "sa"
	   String password = ""
}

'''
        )

       pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.CoreGrailsPlugin")
       pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.i18n.plugins.I18nGrailsPlugin")
       pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin")
       pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.datasource.plugins.DataSourceGrailsPlugin")
       pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.orm.hibernate.plugins.HibernateGrailsPlugin")
       pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.services.plugins.ServicesGrailsPlugin")
    }

}