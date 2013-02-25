package grails.test.mixin

import grails.persistence.Entity

import org.junit.Test

@Mock([Parent, Child])
class BidirectionalOneToManyUnitTestTests {

    // test for GRAILS-8030
    @Test
    void testRelationship() {
        def parent = new Parent()
        def child = new Child()

        parent.addToChildren child
        parent.save()
    }
}

@Entity
class Parent {

    List children
    String name

    static hasMany = [ children: Child ]

    static constraints = {
        name nullable: true
    }
}

@Entity
class Child {
    static belongsTo = [ parent: Parent ]
}
