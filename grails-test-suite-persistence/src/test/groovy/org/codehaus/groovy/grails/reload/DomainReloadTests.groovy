package org.codehaus.groovy.grails.reload;


import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests
import org.hibernate.SessionFactory;

/**
 * Tests for auto-reloading of domain classes
 *
 * @author Aaron Long
 */

class DomainReloadTests extends AbstractGrailsHibernateTests {

    def domain1 = '''
@grails.persistence.Entity
class DomainReloadFace {
    String name
    String description
    static hasMany = [eyes: DomainReloadEye]
}

@grails.persistence.Entity
class DomainReloadEye {
    static belongsTo = [face: DomainReloadFace]
}
'''

    void testReloadDomainClass() {
        def testDomain = ga.getDomainClass("DomainReloadFace").newInstance()
        assert testDomain.respondsTo("getName")
        assert !testDomain.respondsTo("getDescription")
        assert !testDomain.respondsTo("addToEyes")
        
        Map<String,Object> sessionFactoryBeans=ga.mainContext.getBeansOfType(SessionFactory.class);
        assertEquals 1, sessionFactoryBeans.size()

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

        def newDomain = ga.getDomainClass("DomainReloadFace").newInstance()
        assert newDomain.respondsTo("getName")
        assert newDomain.respondsTo("getDescription")
        assert newDomain.respondsTo("save")
        assert newDomain.respondsTo("addToEyes")
        
        sessionFactoryBeans=ga.mainContext.getBeansOfType(SessionFactory.class);
        assertEquals 1, sessionFactoryBeans.size()
    }

    void onSetUp() {
        gcl.parseClass '''
@grails.persistence.Entity
class DomainReloadFace {
    String name
}
'''
    }
}

