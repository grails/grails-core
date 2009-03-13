/**
 * Class description here.
 
 * @author Graeme Rocher
 * @since 0.4
  *
 * Created: Jul 23, 2007
 * Time: 4:46:49 PM
 * 
 */

package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.GrailsDomainClass

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


        assert !companyClass.isOwningClass(memberClass.clazz)
        assert !companyClass.isOwningClass(projectClass.clazz)
        assert !companyClass.isOwningClass(locationClass.clazz)
        assert !companyClass.isOwningClass(personClass.clazz)

        assert !projectClass.isOwningClass(companyClass.clazz)
        assert !projectClass.isOwningClass(memberClass.clazz)
        assert !projectClass.isOwningClass(locationClass.clazz)
        assert !projectClass.isOwningClass(personClass.clazz)

        assert memberClass.isOwningClass(projectClass.clazz)
        assert !memberClass.isOwningClass(companyClass.clazz)
        assert !memberClass.isOwningClass(personClass.clazz)
        assert !memberClass.isOwningClass(locationClass.clazz)

        assert personClass.isOwningClass(companyClass.clazz)
        assert !personClass.isOwningClass(locationClass.clazz)
        assert !personClass.isOwningClass(memberClass.clazz)
        assert !personClass.isOwningClass(projectClass.clazz)


    }

    void onSetUp() {
		this.gcl.parseClass('''
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
		)
	}
}