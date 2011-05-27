package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.GrailsDomainClass

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Dec 5, 2007
 */
class TwoCircularUnidirectionalOneToManyTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class TwoCircularUnidirectionalOneToManyUser {

    Long id
    Long version
    Set children
    Set parents

    static hasMany = [parents: TwoCircularUnidirectionalOneToManyUser,children: TwoCircularUnidirectionalOneToManyUser]
    String name

    def addChild(u) {
        u.addToParents(this)
        addToChildren(u)
    }

    def addParent(u) {
        u.addToChildren(this)
        addToParents(u)
    }

    String toString() { return name }
}
'''
    }

    void testAssociation() {
        GrailsDomainClass userDomainClass = ga.getDomainClass("TwoCircularUnidirectionalOneToManyUser")

        assertTrue userDomainClass.getPropertyByName("children").isOneToMany()
        assertTrue userDomainClass.getPropertyByName("children").isAssociation()
        assertFalse userDomainClass.getPropertyByName("children").isBidirectional()
        assertFalse userDomainClass.getPropertyByName("children").isManyToMany()
        assertTrue userDomainClass.getPropertyByName("parents").isOneToMany()
        assertTrue userDomainClass.getPropertyByName("parents").isAssociation()
        assertFalse userDomainClass.getPropertyByName("parents").isBidirectional()
        assertFalse userDomainClass.getPropertyByName("parents").isManyToMany()


        def userClass = userDomainClass.clazz
        def user1 = userClass.newInstance(name:'A')
        def user2 = userClass.newInstance(name:'B')
        def user3 = userClass.newInstance(name:'C')
        def user4 = userClass.newInstance(name:'D')
        def user5 = userClass.newInstance(name:'E')

        user1.addChild(user3)
        user1.addChild(user4)
        user1.addChild(user5)

        user2.addChild(user3)
        user2.addChild(user4)
        user2.addChild(user5)

        assertTrue user1.children.contains(user3)
        assertTrue user1.children.contains(user4)
        assertTrue user1.children.contains(user5)

        assertTrue user2.children.contains(user3)
        assertTrue user2.children.contains(user4)
        assertTrue user2.children.contains(user5)

        user1.save()
        user2.save()
        user3.save()
        user4.save(flush:true)

        session.clear()

        def userA = userClass.findByName("A")
        def userB = userClass.findByName("B")
        def userC = userClass.findByName("C")
        def userD = userClass.findByName("D")
        def userE = userClass.findByName("E")

        assertTrue userA.children.contains(userE) // success
        assertTrue userA.children.contains(userD) //success
        assertTrue userB.children.contains(userC) //success
        assertTrue userB.children.contains(userE) //success
        assertTrue userB.children.contains(userD) //fails
        assertTrue userA.children.contains(userC) //fails
    }
}
