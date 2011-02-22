package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class HasPropertyAndRespondsToWithProxyTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class HasProperty {
    RespondsTo one
}

@Entity
class RespondsTo {

}

@Entity
class SubclassRespondsTo extends RespondsTo {
    String name
    def foo() { "good" }
    def bar(String i) { i }
}
'''
    }

    void testHasPropertyWithProxy() {
        def HasProperty = ga.getDomainClass("HasProperty").clazz
        def RespondsTo = ga.getDomainClass("SubclassRespondsTo").clazz

        def rt = RespondsTo.newInstance(name:"Bob")
        def hp = HasProperty.newInstance(one:rt)
        assertNotNull "should have saved", rt.save()
        assertNotNull "should have saved", hp.save()

        session.clear()

        hp = HasProperty.get(1)
        assertFalse 'should be a proxy!', GrailsHibernateUtil.isInitialized(hp, "one")

        def proxy = GrailsHibernateUtil.getAssociationProxy(hp, "one")
        assertNotNull "should have a name property!", proxy.hasProperty("name")
    }

    void testRespondsToWithProxy() {
        def HasProperty = ga.getDomainClass("HasProperty").clazz
        def RespondsTo = ga.getDomainClass("SubclassRespondsTo").clazz

        def rt = RespondsTo.newInstance(name:"Bob")
        def hp = HasProperty.newInstance(one:rt)
        assertNotNull "should have saved", rt.save()
        assertNotNull "should have saved", hp.save()

        session.clear()

        hp = HasProperty.get(1)
        assertFalse 'should be a proxy!', GrailsHibernateUtil.isInitialized(hp, "one")

        def proxy = GrailsHibernateUtil.getAssociationProxy(hp, "one")

        assertNotNull "should have a foo method!", proxy.respondsTo("foo")
        assertNotNull "should have a bar method!", proxy.respondsTo("bar", String)
    }
}
