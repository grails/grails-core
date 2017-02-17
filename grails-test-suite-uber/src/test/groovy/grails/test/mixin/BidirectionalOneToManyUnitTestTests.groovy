package grails.test.mixin

import grails.persistence.Entity
import grails.testing.gorm.DataTest
import org.junit.Test

class BidirectionalOneToManyUnitTestTests implements DataTest {

    Class[] getDomainClassesToMock() {
        [Parent, Child]
    }

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
