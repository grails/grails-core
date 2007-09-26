/**
 * Tests using domain events
 
 * @author Graeme Rocher
 * @since 1.0
  *
 * Created: Sep 26, 2007
 * Time: 1:09:07 AM
 * 
 */
package org.codehaus.groovy.grails.orm.hibernate
class DomainEventsTests extends AbstractGrailsHibernateTests{


    void testOnloadEvent() {
        def personClass = ga.getDomainClass("Person")
        def p = personClass.newInstance()

        p.name = "Fred"
        p.save()
        session.flush()
        session.clear()

        p = personClass.clazz.get(1)
        assertEquals "Bob", p.name
    }

    void testBeforeDeleteEvent() {
        def personClass = ga.getDomainClass("Person")
        def p = personClass.newInstance()

        p.name = "Fred"
        p.save()
        session.flush()

        def success = false
        p.beforeDelete = { success = true }
        p.delete()
        session.flush()
        assert success
    }

    void testBeforeUpdateEvent() {
        def personClass = ga.getDomainClass("Person")
        def p = personClass.newInstance()

        p.name = "Fred"
        p.save()
        session.flush()

        def success = false
        p.beforeUpdate = {
            println "CALLED BEFORE UPDATE!"
            success = true }
        p.name = "Bob"
        p.save()
        session.flush()
        assert success
    }

    void testBeforeInsertEvent() {
        def personClass = ga.getDomainClass("Person")
        def p = personClass.newInstance()

        p.name = "Fred"
        def success = false
        p.beforeInsert = {
            println "CALLED BEFORE INSERT!"
            success = true }
        p.save()
        session.flush()

        assert success
    }

    void onSetUp() {
		this.gcl.parseClass('''
class Person {
	Long id
	Long version
	String name

	def onLoad = {
	    name = "Bob"
	}
	def beforeDelete = {}
	def beforeUpdate = {}
	def beforeInsert = {}
}
'''
		)
	}
}