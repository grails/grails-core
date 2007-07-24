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

class CascadingDeleteBehaviourTests extends AbstractGrailsHibernateTests {

    /*void testSomething() {
        fail('irk!!!!')
                               
    } */
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

        assertEquals 1, companyClass.clazz.list().size()
        assertEquals 0, memberClass.clazz.list().size()

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

        assertEquals 1, companyClass.clazz.list().size()
         assertEquals 1, locationClass.clazz.list().size()
        assertEquals 1, personClass.clazz.list().size()

        c.delete()
        session.flush()

        assertEquals 0, companyClass.clazz.list().size()
        assertEquals 1, locationClass.clazz.list().size()
        assertEquals 0, personClass.clazz.list().size()

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