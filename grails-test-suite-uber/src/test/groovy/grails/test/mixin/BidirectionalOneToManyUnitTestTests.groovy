package grails.test.mixin

import grails.persistence.Entity
import grails.testing.gorm.DataTest
import spock.lang.Specification

class BidirectionalOneToManyUnitTestTests extends Specification implements DataTest {

    Class[] getDomainClassesToMock() {
        [Parent, Child]
    }

    // test for GRAILS-8030
    void testRelationship() {
        when:
        def parent = new Parent()
        def child = new Child()
        parent.addToChildren child

        then:
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
