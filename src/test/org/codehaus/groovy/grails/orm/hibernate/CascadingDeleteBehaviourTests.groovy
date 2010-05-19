package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.GrailsDomainClass

/**
 * @author Graeme Rocher
 * @since 0.4
 */
class CascadingDeleteBehaviourTests extends AbstractGrailsHibernateTests {

    void testDeleteToOne() {
        def companyClass = ga.getDomainClass("Company")
        def projectClass = ga.getDomainClass("Project")
        def memberClass = ga.getDomainClass("Member")
        def c = companyClass.newInstance()
        def p = projectClass.newInstance()
        def m = memberClass.newInstance()
        c.save()
        p.company = c
        p.member = m
        p.save()

        session.flush()

        p.delete()
        session.flush()                                     \

        assertEquals 1, companyClass.clazz.count()
        assertEquals 0, memberClass.clazz.count()
        assertEquals 0, projectClass.clazz.count()
    }

    void testDeleteToManyUnidirectional() {
        def companyClass = ga.getDomainClass("Company")
        def locationClass = ga.getDomainClass("Location")
        def personClass = ga.getDomainClass("Person")

        def c = companyClass.newInstance()

        c.addToLocations(locationClass.newInstance())
        c.addToPeople(personClass.newInstance())
        c.save()
        session.flush()

        assertEquals 1, companyClass.clazz.count()
        assertEquals 1, locationClass.clazz.count()
        assertEquals 1, personClass.clazz.count()

        c.delete()
        session.flush()

        assertEquals 0, companyClass.clazz.count()
        assertEquals 1, locationClass.clazz.count()
        assertEquals 0, personClass.clazz.count()
    }

    void testDomainModel() {
        GrailsDomainClass companyClass = ga.getDomainClass("Company")
        GrailsDomainClass memberClass = ga.getDomainClass("Member")
        GrailsDomainClass projectClass = ga.getDomainClass("Project")
        GrailsDomainClass locationClass = ga.getDomainClass("Location")
        GrailsDomainClass personClass = ga.getDomainClass("Person")

        assertFalse companyClass.isOwningClass(memberClass.clazz)
        assertFalse companyClass.isOwningClass(projectClass.clazz)
        assertFalse companyClass.isOwningClass(locationClass.clazz)
        assertFalse companyClass.isOwningClass(personClass.clazz)

        assertFalse projectClass.isOwningClass(companyClass.clazz)
        assertFalse projectClass.isOwningClass(memberClass.clazz)
        assertFalse projectClass.isOwningClass(locationClass.clazz)
        assertFalse projectClass.isOwningClass(personClass.clazz)

        assertTrue memberClass.isOwningClass(projectClass.clazz)
        assertFalse memberClass.isOwningClass(companyClass.clazz)
        assertFalse memberClass.isOwningClass(personClass.clazz)
        assertFalse memberClass.isOwningClass(locationClass.clazz)

        assertTrue personClass.isOwningClass(companyClass.clazz)
        assertFalse personClass.isOwningClass(locationClass.clazz)
        assertFalse personClass.isOwningClass(memberClass.clazz)
        assertFalse personClass.isOwningClass(projectClass.clazz)
    }

    void onSetUp() {
        gcl.parseClass '''
class Company {
    Long id
    Long version

    static hasMany = [locations:Location, people:Person]
    Set locations
    Set people
}
class Person {
    Long id
    Long version
    static belongsTo = Company
}
class Location {
    Long id
    Long version
}
class Project {
    Long id
    Long version

    Company company
    Member member
}
class Member {
    Long id
    Long version

    static belongsTo = Project
}
'''
    }
}
