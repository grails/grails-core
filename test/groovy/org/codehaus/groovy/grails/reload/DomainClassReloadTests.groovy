package org.codehaus.groovy.grails.reload;

 import org.codehaus.groovy.grails.web.servlet.mvc.*
 import org.codehaus.groovy.grails.commons.*
 import org.apache.commons.logging.*
 import org.codehaus.groovy.grails.plugins.web.*

/**
 * Tests for auto-reloading of services
 *
 * @author Graeme Rocher
 **/

class DomainClassReloadTests extends AbstractGrailsPluginTests {

    def updatedClass = '''
class Book {
    Long id
    Long version
    String title
    Date dateCreated
}    '''
    void testReloadDomainClass() {
            def domainClass = ga.getDomainClass("Book").newInstance()

            domainClass.title = "foo"
            shouldFail {
                domainClass.dateCreated = new Date()
            }

            def newGcl = new GroovyClassLoader()

            def event = [source:newGcl.parseClass(updatedClass),
                         ctx:appCtx,
                         application:ga,
                         manager:mockManager]

            def plugin = mockManager.getGrailsPlugin("hibernate")

            def eventHandler = plugin.instance.onChange
            eventHandler.delegate = plugin
            shouldFail(IllegalStateException) {
                eventHandler.call(event)
            }
            System.setProperty("grails.env", "development")
            try {
                eventHandler.call(event)                            
            }
            finally {
                System.setProperty("grails.env", "")
            }

    }

	void onSetUp() {
		gcl.parseClass(
'''
class Book {
    Long id
    Long version
    String title
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
       pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.i18n.I18nGrailsPlugin")
       pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.DomainClassGrailsPlugin")
       pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.datasource.DataSourceGrailsPlugin")
       pluginsToLoad << gcl.loadClass("org.codehaus.groovy.grails.plugins.orm.hibernate.HibernateGrailsPlugin")

    }

}