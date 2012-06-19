package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.Hibernate
import org.hibernate.ObjectNotFoundException
import org.hibernate.proxy.HibernateProxy

import org.springframework.orm.hibernate3.HibernateObjectRetrievalFailureException

/**
 * @author Burt Beckwith
 */
class LoadMethodTests extends AbstractGrailsHibernateTests {

    protected getDomainClasses() {
        [LoadMethodTest, LoadMethodZeroIdTest]
    }

    void testNullId() {
        def clazz = ga.getDomainClass(LoadMethodTest.name).clazz
        assertNull 'null id should return null instance', clazz.load(null)
    }

    void testIntId() {
        String className = LoadMethodTest.name
        def clazz = ga.getDomainClass(className).clazz

        def instance = clazz.load(1)
        assertNotNull 'load never returns null for non-null ids', instance
        assertEquals "id is accessible even if object doesn't exist", 1, instance.getId()
        shouldFail(ObjectNotFoundException) {
            instance.name
        }

        String name = 'Foo'
        assertNotNull clazz.newInstance(name: name).save(flush: true)
        session.clear()

        instance = clazz.load(1)
        assertNotNull 'load never returns null for non-null ids', instance
        assertEquals 'calling non-id method on valid instance works', name, instance.name

        assertProxy instance, className
    }

    void testLongId() {
        String className = LoadMethodTest.name
        def clazz = ga.getDomainClass(className).clazz

        def instance = clazz.load(1L)
        assertNotNull 'load never returns null for non-null ids', instance
        assertEquals "id is accessible even if object doesn't exist", 1, instance.getId()
        shouldFail(ObjectNotFoundException) {
            instance.name
        }

        String name = 'Foo'
        assertNotNull clazz.newInstance(name: name).save(flush: true)
        session.clear()

        instance = clazz.load(1L)
        assertNotNull 'load never returns null for non-null ids', instance
        assertEquals 'calling non-id method on valid instance works', name, instance.name

        assertProxy instance, className
    }

    void testGetBeforeLoad() {
        String className = LoadMethodTest.name
        def clazz = ga.getDomainClass(className).clazz

        String name = 'Foo'
        assertNotNull clazz.newInstance(name: name).save(flush: true)
        session.clear()

        def getInstance = clazz.get(1L)
        def loadInstance = clazz.load(1L)
        assertTrue getInstance.is(loadInstance)

        assertFalse 'should not be a proxy', loadInstance instanceof HibernateProxy
        assertTrue 'should not be a proxy', loadInstance.getClass().name.equals(className)
    }

    void testLoadBeforeGet() {
        String className = LoadMethodTest.name
        def clazz = ga.getDomainClass(className).clazz

        String name = 'Foo'
        assertNotNull clazz.newInstance(name: name).save(flush: true)
        session.clear()

        def loadInstance = clazz.load(1L)
        assertProxy loadInstance, className

        // metaclass get() method un-proxies, unlike standard Hibernate
        def getInstance = clazz.get(1L)
        assertFalse getInstance.is(loadInstance)
        assertFalse 'should not be a proxy', getInstance instanceof HibernateProxy
        assertTrue 'should not be a proxy', getInstance.getClass().name.equals(className)

        assertEquals 'calling non-id method on valid instance works', name, loadInstance.name
    }

    void testZeroId() {
        String className = LoadMethodZeroIdTest.name
        def clazz = ga.getDomainClass(className).clazz

        def o = clazz.newInstance()

        def instance = clazz.load(null)
        assertNull 'null id should return null instance', instance

        instance = clazz.load(0)
        assertNotNull 'load never returns null for non-null ids', instance
        shouldFail(ObjectNotFoundException) {
            instance.name
        }

        instance = clazz.load(0L)
        assertNotNull 'load never returns null for non-null ids', instance
        shouldFail(ObjectNotFoundException) {
            instance.name
        }

        String name = 'Foo'
        instance = clazz.newInstance(name: name)
        instance.id = 0
        assertNotNull instance.save(flush: true)
        session.clear()

        instance = clazz.load(0)
        assertNotNull 'load never returns null for non-null ids', instance
        assertEquals 'calling non-id method on valid instance works', name, instance.name

        assertProxy instance, className
    }

    void testIdPropertyAccess() {
        String className = LoadMethodTest.name
        def clazz = ga.getDomainClass(className).clazz

        def instance = clazz.load(1L)
        assertEquals "id is accessible even if object doesn't exist", 1, instance.id

        shouldFail(ObjectNotFoundException) {
            instance.name
        }
    }

    private void assertProxy(instance, String className) {
        assertTrue 'should be a proxy', instance instanceof HibernateProxy
        assertFalse 'should be a proxy', instance.getClass().name.equals(className)
        assertEquals 'proxied class should be domain class', className, Hibernate.getClass(instance).name
    }
}
class LoadMethodTest {
    Long id
    Long version
    String name
}

class LoadMethodZeroIdTest {
    Long id
    Long version
    String name
    static mapping = {
        id generator:'assigned'
    }
}
