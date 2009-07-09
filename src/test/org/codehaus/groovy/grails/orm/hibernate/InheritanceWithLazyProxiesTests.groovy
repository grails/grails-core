package org.codehaus.groovy.grails.orm.hibernate
import org.hibernate.Hibernate
import org.hibernate.proxy.HibernateProxy
import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Feb 27, 2009
 */

public class InheritanceWithLazyProxiesTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''

import grails.persistence.*

@Entity
class A { static belongsTo = [attr:AttributeB] }

@Entity
class B extends A { static belongsTo = [type:AttributeA] }

@Entity
class AttributeA {
}

@Entity
class AttributeB extends AttributeA {
}
''')
    }


    void testLazyAssociationsWithInheritance() {
        Class AttributeA = ga.getDomainClass("AttributeA").clazz
        Class AttributeB = ga.getDomainClass("AttributeB").clazz
        Class A = ga.getDomainClass("A").clazz
        Class B = ga.getDomainClass("B").clazz

        def attrb = AttributeB.newInstance()
        attrb.save()

        def b = B.newInstance(type:attrb, attr:attrb).save(flush:true)

        assertNotNull "subclass should have been saved", b

        session.clear()

        b = B.get(1)

        def type = GrailsHibernateUtil.getAssociationProxy(b, "type")

        assertFalse "should not have been initialized",GrailsHibernateUtil.isInitialized(b, "type")


        b.discard()


        assertNotNull "dynamic finder should have worked with proxy", B.findByType(type)
        session.clear()
        assertFalse "should not have been initialized",Hibernate.isInitialized(type)
        assertNotNull "dynamic finder should have worked with proxy", A.findByAttr(type)

    }

    void testInstanceOfMethod() {
        Class AttributeA = ga.getDomainClass("AttributeA").clazz
        Class AttributeB = ga.getDomainClass("AttributeB").clazz
        Class A = ga.getDomainClass("A").clazz
        Class B = ga.getDomainClass("B").clazz

        def attrb = AttributeB.newInstance()
        attrb.save()

        def b = B.newInstance(type:attrb, attr:attrb).save(flush:true)

        assertNotNull "subclass should have been saved", b
        session.clear()

        b = B.get(1)

        def type = GrailsHibernateUtil.getAssociationProxy(b, "type")

        assertFalse "should not have been initialized",GrailsHibernateUtil.isInitialized(b, "type")
        assertTrue "should be a hibernate proxy", (type instanceof HibernateProxy)

        assertTrue "instanceOf method should have returned true",type.instanceOf(AttributeA)
        assertTrue "instanceOf method should have returned true",type.instanceOf(AttributeB)


    }

}