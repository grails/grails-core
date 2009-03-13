package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.test.*
                                                                    
class DeepHeirarchyTests extends AbstractGrailsHibernateTests {

	void testCountInDeepHeirarchy() {
		def p = ga.getDomainClass("Personnel")
		def a = ga.getDomainClass("Approver")
		def h = ga.getDomainClass("Handler")


        def p1 = p.newInstance()

        p1.name = "Joe Bloggs"
        p1.save()
        session.flush()


        def a1 = a.newInstance()
        a1.name = "Fred Flinstone"
        a1.status = "active"
        a1.save()
        session.flush()

        def h1 = h.newInstance()
        h1.name = "Barney Rubble"
        h1.status = "dormant"
        h1.strength = 10
        h1.save()
        session.flush()

        session.clear()

        assertEquals 3, p.clazz.count()
        assertEquals 2, a.clazz.count()
        assertEquals 1, h.clazz.count()
	}

	void testPersistentValuesInDeepHeirarchy() {
		def p = ga.getDomainClass("Personnel")
		def a = ga.getDomainClass("Approver")
		def h = ga.getDomainClass("Handler")


        def p1 = p.newInstance()

        p1.name = "Joe Bloggs"
        p1.save()
        session.flush()


        def a1 = a.newInstance()
        a1.name = "Fred Flinstone"
        a1.status = "active"
        a1.save()
        session.flush()

        def h1 = h.newInstance()
        h1.name = "Barney Rubble"
        h1.status = "dormant"
        h1.strength = 10
        h1.save()
        session.flush()

        def aId = a1.id
        def pId = p1.id
        def hId = h1.id

        session.clear()


        def p2 = p.clazz.get(pId)
        assertEquals "Joe Bloggs", p2.name

        def a2 = a.clazz.get(aId)
        assertEquals "Fred Flinstone", a2.name
        assertEquals "active", a2.status

        def h2 = h.clazz.get(hId)
        assertEquals "Barney Rubble", h2.name
        assertEquals "dormant", h2.status
        assertEquals 10, h2.strength

    }

	void onSetUp() {
		this.gcl.parseClass('''
class Personnel{
    Long id
    Long version
    String name
}

class Approver extends Personnel{
    Long id
    Long version
    String status
}

class Handler extends Approver{
    Long id
    Long version
    int strength
}

'''
		)
	}
	
	void onTearDown() {
		
	}
}
