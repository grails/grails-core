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

   void testDisabledAutoTimestamps() {
        def personClass = ga.getDomainClass("PersonEvent2")
        def p = personClass.newInstance()

        p.name = "Fred"
        p.save()
        session.flush()
        session.clear()

        p = personClass.clazz.get(1)

        assert !p.dateCreated
        assert !p.lastUpdated

        p.name = "Wilma"
        p.save()
        session.flush()

        assert !p.dateCreated
        assert !p.lastUpdated
    }

    void testAutoTimestamps() {
        def personClass = ga.getDomainClass("PersonEvent")
        def p = personClass.newInstance()

        p.name = "Fred"
        p.save()
        session.flush()
        session.clear()

        p = personClass.clazz.get(1)

        assertEquals p.dateCreated, p.lastUpdated

        p.name = "Wilma"
        p.save()
        session.flush()

        assert p.dateCreated.before(p.lastUpdated)        
    }

    void testOnloadEvent() {
        def personClass = ga.getDomainClass("PersonEvent")
        def p = personClass.newInstance()

        p.name = "Fred"
        p.save()
        session.flush()
        session.clear()

        p = personClass.clazz.get(1)
        assertEquals "Bob", p.name
    }

    void testBeforeDeleteEvent() {
        def personClass = ga.getDomainClass("PersonEvent")
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
        def personClass = ga.getDomainClass("PersonEvent")
        def p = personClass.newInstance()

        p.name = "Fred"
        p.save()
        session.flush()

        def success = false
        p.beforeUpdate = { name = "Wilma" }
        p.name = "Bob"
        p.save()
        session.flush()
        assertEquals "Wilma", p.name
    }

    void testBeforeInsertEvent() {
        def personClass = ga.getDomainClass("PersonEvent")
        def p = personClass.newInstance()

        p.name = "Fred"
        def success = false
        p.beforeInsert = { name = "Bob" }
        p.save()
        session.flush()

        assertEquals "Bob", p.name
    }

    void onSetUp() {
		this.gcl.parseClass('''
class PersonEvent {
	Long id
	Long version
	String name
	Date dateCreated
	Date lastUpdated

	def onLoad = {
	    name = "Bob"
	}
	def beforeDelete = {}
	def beforeUpdate = {}
	def beforeInsert = {}
}
class PersonEvent2 {
	Long id
	Long version
	String name
	Date dateCreated
	Date lastUpdated
	static mapping = {
	    autoTimestamp false
	}
	static constraints = {
	    dateCreated nullable:true
	    lastUpdated nullable:true
	}
}
'''
		)
	}
}