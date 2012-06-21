package org.codehaus.groovy.grails.reload;


import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests

/**
 * Tests for auto-reloading of domain classes
 *
 * @author Aaron Long
 */

class DomainReloadTests extends AbstractGrailsHibernateTests {

    def domain1 = '''
@grails.persistence.Entity
class Face {
    String name
    String description
    static hasMany = [eyes: Eye]
}

@grails.persistence.Entity
class Eye {
    static belongsTo = [face: Face]
}
'''

    void testReloadDomainClass() {
        def testDomain = ga.getDomainClass("Face").newInstance()
        assert testDomain.respondsTo("getName")
        assert !testDomain.respondsTo("getDescription")
        assert !testDomain.respondsTo("addToEyes")

        def newGcl = new GroovyClassLoader()
        def event = [source: newGcl.parseClass(domain1), ctx: appCtx, application: ga]

        def plugin = mockManager.getGrailsPlugin("domainClass")
        def eventHandler = plugin.instance.onChange
        eventHandler.delegate = plugin
        eventHandler.call(event)

        plugin = mockManager.getGrailsPlugin("mockHibernate")
        eventHandler = plugin.instance.onChange
        eventHandler.delegate = plugin
        eventHandler.call(event)

        def newDomain = ga.getDomainClass("Face").newInstance()
        assert newDomain.respondsTo("getName")
        assert newDomain.respondsTo("getDescription")
        assert newDomain.respondsTo("save")
        assert newDomain.respondsTo("addToEyes")
    }

    void onSetUp() {
        gcl.parseClass '''
@grails.persistence.Entity
class Face {
    String name
}
'''
    }
}

