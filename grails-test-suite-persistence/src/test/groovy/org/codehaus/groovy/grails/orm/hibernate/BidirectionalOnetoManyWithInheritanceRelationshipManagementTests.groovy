package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.GrailsDomainClass

/**
 * Longest class name in history!
 *
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Mar 13, 2008
 */
class BidirectionalOnetoManyWithInheritanceRelationshipManagementTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
class BidirectionalOnetoManyManySide {
    Long id
    Long version
    Set oneSides
    static hasMany = [oneSides:BidirectionalOnetoManyOneSide]
}

class BidirectionalOnetoManySubManySide extends BidirectionalOnetoManyManySide {}

class BidirectionalOnetoManyOneSide {
    Long id
    Long version
    BidirectionalOnetoManyManySide manySide
    static belongsTo = [manySide:BidirectionalOnetoManyManySide]
}
'''
    }

    void testRelationshipManagementMethods() {
        GrailsDomainClass manySideClass = ga.getDomainClass("BidirectionalOnetoManyManySide")
        GrailsDomainClass oneSideClass = ga.getDomainClass("BidirectionalOnetoManyOneSide")
        GrailsDomainClass subManySideClass = ga.getDomainClass("BidirectionalOnetoManySubManySide")

        def collection = subManySideClass.getPropertyByName("oneSides")
        assert collection

        assertTrue collection.isBidirectional()
        def otherSide = collection.getOtherSide()
        assert otherSide

        def manySide = manySideClass.newInstance()
        def oneSide1 = oneSideClass.newInstance()

        manySide.addToOneSides(oneSide1)
        manySide.save(flush:true) // OK
        assertEquals "1", 1, manySide.oneSides?.size() // OK
        assertNotNull "2", oneSide1.manySide // OK
        assertEquals "3", oneSide1.manySide?.id, manySide.id // OK

        def subManySide = subManySideClass.newInstance()
        def oneSide2 = oneSideClass.newInstance()

        subManySide.addToOneSides(oneSide2)
        assertEquals "4", 1, subManySide.oneSides?.size() // OK
        assertNotNull "5", oneSide2.manySide // NG
        assertEquals "6", oneSide2.manySide?.id, subManySide.id
        subManySide.save(flush:true)
    }
}
