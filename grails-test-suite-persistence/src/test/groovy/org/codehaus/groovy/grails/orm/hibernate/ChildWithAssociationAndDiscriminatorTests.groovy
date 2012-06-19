package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class ChildWithAssociationAndDiscriminatorTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class ChildWithAssociationAndDiscriminatorParent {

    static mapping = {
        discriminator column:[name:"test_discriminator",sqlType:"varchar"], value:"custom_parent"
    }
}

@Entity
class ChildWithAssociationAndDiscriminatorChild extends ChildWithAssociationAndDiscriminatorParent {

    static mapping = {
        discriminator "custom_child"
    }

    static belongsTo = [myObject:ChildWithAssociationAndDiscriminatorAssociated]
    ChildWithAssociationAndDiscriminatorAssociated myObject
}

@Entity
class ChildWithAssociationAndDiscriminatorChild2 {
    static mapping = {
        discriminator "custom_child"
    }

    String name
}

@Entity
class ChildWithAssociationAndDiscriminatorAssociated {

    static hasMany = [childList:ChildWithAssociationAndDiscriminatorChild]
}
'''
    }

    void testChildObjectWithAssociationAndCustomDiscriminator() {
        def Child = ga.getDomainClass("ChildWithAssociationAndDiscriminatorChild").clazz
        def Child2 = ga.getDomainClass("ChildWithAssociationAndDiscriminatorChild2").clazz
        def Associated = ga.getDomainClass("ChildWithAssociationAndDiscriminatorAssociated").clazz
        def associatedInstance = Associated.newInstance()

        associatedInstance.addToChildList(Child.newInstance())

        assertNotNull "should have saved instance", associatedInstance.save(flush:true)
        session.clear()

        def a = Associated.get(1)
        def children = a.childList
        assertNotNull "should have had some children", children
        assertTrue "should have had some children", children.size() > 0

        def child2 = Child2.newInstance(name:"bob")

        assertNotNull "should be able to save second child", child2.save(flush:true)?.id
    }
}
