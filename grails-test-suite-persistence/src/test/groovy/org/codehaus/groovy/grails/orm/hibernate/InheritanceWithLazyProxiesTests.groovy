package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.hibernate.Hibernate
import org.hibernate.proxy.HibernateProxy

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Feb 27, 2009
 */
class InheritanceWithLazyProxiesTests extends AbstractGrailsHibernateTests {

    void testLazyAssociationsWithInheritance() {

        def attrb = new InheritanceWithLazyProxiesAttributeB()
        attrb.save()

        def b = new InheritanceWithLazyProxiesB(type:attrb, attr:attrb).save(flush:true)

        assertNotNull "subclass should have been saved", b

        session.clear()

        b = InheritanceWithLazyProxiesB.get(1)
        def type = GrailsHibernateUtil.getAssociationProxy(b, "type")

        assertFalse "should not have been initialized",GrailsHibernateUtil.isInitialized(b, "type")
        b.discard()

        assertNotNull "dynamic finder should have worked with proxy", InheritanceWithLazyProxiesB.findByType(type)
        session.clear()
        assertFalse "should not have been initialized",Hibernate.isInitialized(type)
        assertNotNull "dynamic finder should have worked with proxy", InheritanceWithLazyProxiesA.findByAttr(type)
    }

    void testInstanceOfMethod() {

        def attrb = new InheritanceWithLazyProxiesAttributeB()
        attrb.save()

        def b = new InheritanceWithLazyProxiesB(type:attrb, attr:attrb).save(flush:true)

        assertNotNull "subclass should have been saved", b
        session.clear()

        b = InheritanceWithLazyProxiesB.get(1)

        def type = GrailsHibernateUtil.getAssociationProxy(b, "type")

        assertFalse "should not have been initialized",GrailsHibernateUtil.isInitialized(b, "type")
        assertTrue "should be a hibernate proxy", (type instanceof HibernateProxy)

        assertTrue "instanceOf method should have returned true",type.instanceOf(InheritanceWithLazyProxiesAttributeA)
        assertTrue "instanceOf method should have returned true",type.instanceOf(InheritanceWithLazyProxiesAttributeB)
    }

    @Override
    protected getDomainClasses() {
        [InheritanceWithLazyProxiesA,InheritanceWithLazyProxiesB, InheritanceWithLazyProxiesAttributeA, InheritanceWithLazyProxiesAttributeB]
    }
}

@Entity
class InheritanceWithLazyProxiesA { static belongsTo = [attr:InheritanceWithLazyProxiesAttributeB] }

@Entity
class InheritanceWithLazyProxiesB extends InheritanceWithLazyProxiesA { static belongsTo = [type:InheritanceWithLazyProxiesAttributeA] }

@Entity
class InheritanceWithLazyProxiesAttributeA {}

@Entity
class InheritanceWithLazyProxiesAttributeB extends InheritanceWithLazyProxiesAttributeA {}
