package org.codehaus.groovy.grails.orm.hibernate

import org.apache.tools.ant.util.XMLFragment.Child

/**
 * @author Graeme Rocher
 * @since 1.1
 */

public class ChildWithAssociationAndDiscriminatorTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''
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

    static constraints = {
    }

    static hasMany = [childList:ChildWithAssociationAndDiscriminatorChild]

}

''')
    }


    void testChildObjectWithAssociationAndCustomDiscriminator() {
        def Child = ga.getDomainClass("ChildWithAssociationAndDiscriminatorChild").clazz
        def Child2 = ga.getDomainClass("ChildWithAssociationAndDiscriminatorChild2").clazz
        def Associated = ga.getDomainClass("ChildWithAssociationAndDiscriminatorAssociated").clazz
        def associatedInstance = Associated.newInstance()

         associatedInstance.addToChildList(Child.newInstance());

        assert associatedInstance.save(flush:true) : "should have saved instance"

        session.clear()

        def a = Associated.get(1)

        def children = a.childList

        assert children : "should have had some children"

        def child2 = Child2.newInstance(name:"bob")

        assert child2.save(flush:true)?.id : "should be able to save second child"
    }
}