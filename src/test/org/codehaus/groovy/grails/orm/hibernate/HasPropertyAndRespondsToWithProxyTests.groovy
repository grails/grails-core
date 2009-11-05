package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class HasPropertyAndRespondsToWithProxyTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
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
''')
    }


    void testHasPropertyWithProxy() {
        def HasProperty = ga.getDomainClass("HasProperty").clazz
        def RespondsTo = ga.getDomainClass("SubclassRespondsTo").clazz

        def rt = RespondsTo.newInstance(name:"Bob")
        def hp = HasProperty.newInstance(one:rt)
        assert rt.save() : "should have saved"
        assert hp.save() : "should have saved"

        session.clear()

        hp = HasProperty.get(1)

        assert !GrailsHibernateUtil.isInitialized(hp, "one") : 'should be a proxy!'

        def proxy = GrailsHibernateUtil.getAssociationProxy(hp, "one")

        assert proxy.hasProperty("name") : "should have a name property!"
        
    }

    void testRespondsToWithProxy() {
        def HasProperty = ga.getDomainClass("HasProperty").clazz
        def RespondsTo = ga.getDomainClass("SubclassRespondsTo").clazz

        def rt = RespondsTo.newInstance(name:"Bob")
        def hp = HasProperty.newInstance(one:rt)
        assert rt.save() : "should have saved"
        assert hp.save() : "should have saved"

        session.clear()

        hp = HasProperty.get(1)

        assert !GrailsHibernateUtil.isInitialized(hp, "one") : 'should be a proxy!'

        def proxy = GrailsHibernateUtil.getAssociationProxy(hp, "one")

        assert proxy.respondsTo("foo") : "should have a foo method!"
        assert proxy.respondsTo("bar", String) : "should have a bar method!"
    }
}