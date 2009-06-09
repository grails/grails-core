package org.codehaus.groovy.grails.orm.hibernate

import org.hibernate.Hibernate

import org.codehaus.groovy.grails.orm.hibernate.cfg.GrailsHibernateUtil
import org.apache.commons.beanutils.PropertyUtils
import org.hibernate.proxy.HibernateProxy

/**
* @author Graeme Rocher
* @since 1.0
*
* Created: Mar 14, 2008
*/
class LazyLoadedOneToOneIdentifierTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class LazyLoadedUserIdentifier {
    Long id
    Long version
    LazyLoadedUser user
    static belongsTo = [user:LazyLoadedUser]
    static mapping = { user lazy:true }
}
class LazyLoadedUser {
    Long id
    Long version
    String name
}
'''
    }

    void testDynamicMethodOnProxiedObject() {
        def userClass = ga.getDomainClass("LazyLoadedUser").clazz
        def identifierClass = ga.getDomainClass("LazyLoadedUserIdentifier").clazz

        def user = userClass.newInstance(name:"Fred")

        assert user.save(flush:true)

        def id = identifierClass.newInstance(user:user)
        assert id.save(flush:true)

        session.clear()


        id = identifierClass.get(1)

        def proxy = PropertyUtils.getProperty(id, "user")
        assertTrue "should be a hibernate proxy", (proxy instanceof HibernateProxy)
        assertFalse "proxy should not be initialized", Hibernate.isInitialized(proxy)

        assertNotNull "calling save() on the proxy should have worked",proxy.save()
        


    }
    void testMethodCallsOnProxiedObjects() {

        def userClass = ga.getDomainClass("LazyLoadedUser").clazz
        def identifierClass = ga.getDomainClass("LazyLoadedUserIdentifier").clazz

        def user = userClass.newInstance(name:"Fred")

        assert user.save(flush:true)

        def id = identifierClass.newInstance(user:user)
        assert id.save(flush:true)

        session.clear()


        id = identifierClass.get(1)

        def proxy = PropertyUtils.getProperty(id, "user")
        assertTrue "should be a hibernate proxy", (proxy instanceof HibernateProxy)
        assertFalse "proxy should not be initialized", Hibernate.isInitialized(proxy)

        assertEquals "Fred", proxy.name
    }


    void testObtainIdFromLazyLoadedObject() {
        def userClass = ga.getDomainClass("LazyLoadedUser").clazz
        def identifierClass = ga.getDomainClass("LazyLoadedUserIdentifier").clazz

        def user = userClass.newInstance(name:"Fred")

        assert user.save(flush:true)

        def id = identifierClass.newInstance(user:user)
        assert id.save(flush:true)

        session.clear()


        id = identifierClass.get(1)



        println "one"
        assertFalse "user should have been lazy loaded", GrailsHibernateUtil.isInitialized(id, "user")
        println "two"

        def dbId = id.userId

        assertEquals 1, dbId

        assertFalse "accessed identifier, but lazy association should not have been initialized", GrailsHibernateUtil.isInitialized(id, "user") 
    }

}