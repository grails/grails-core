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
class BidirectionalOnetoManySubManySide extends BidirectionalOnetoManyManySide {

}
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
        assertTrue "1", manySide.oneSides?.size() == 1 // OK
        assertTrue "2", oneSide1.manySide != null // OK
        assertTrue "3", oneSide1.manySide?.id == manySide.id // OK

        def subManySide = subManySideClass.newInstance()
        def oneSide2 = oneSideClass.newInstance()

        subManySide.addToOneSides(oneSide2)
        assertTrue "4", subManySide.oneSides?.size() == 1 // OK
        assertTrue "5", oneSide2.manySide != null // NG
        assertTrue "6", oneSide2.manySide?.id == subManySide.id
        subManySide.save(flush:true)
    }
}