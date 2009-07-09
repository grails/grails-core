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

   // test for GRAILS-4059
   void testLastUpdateDoesntChangeWhenNotDirty() {
       def personClass = ga.getDomainClass("PersonEvent").clazz
       def p = personClass.newInstance()


       p.name = "Fred"
       assertNotNull "person should have been saved",p.save()

       p.addToAddresses(postCode:"23209")
       assertNotNull "person should have been updated",p.save(flush:true)

       def address = p.addresses.iterator().next()

       assertTrue "address should have been saved", session.contains(address)
       def current = address.lastUpdated
       assertNotNull "should have created time sstamp",current

       session.flush()

       personClass.executeQuery("select f from PersonEvent f join fetch f.addresses") // cause auto-flush of session

       def now = address.lastUpdated

       assertEquals "The last updated date should not have been changed!", current, now
   }

   // test for GRAILS-4041
   void testNoModifyVersion() {

       def personClass = ga.getDomainClass("PersonEvent2").clazz
       def p = personClass.newInstance()

       p.name = "Fred"
       p.beforeUpdate = { name = "Wilma" }
       p.save(flush:true)

       p.name = "Body"
       p.save()


       personClass.findWhere(name:"Wilma")       
   }

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

        sleep(2000)

        assertEquals p.dateCreated, p.lastUpdated

        p.name = "Wilma"
        p.save()
        session.flush()

        assertTrue p.dateCreated.before(p.lastUpdated)        
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
        def personClass = ga.getDomainClass("PersonEvent2").clazz
        def p = personClass.newInstance()

        p.name = "Fred"
        p.save(flush:true)
        session.clear()

        p = personClass.get(1)
        assertEquals "Fred", p.name

        p.beforeUpdate = { name = "Wilma" }
        p.name = "Bob"
        assert p.save(flush:true)

        assertEquals "Wilma", p.name
        session.clear()

        p = personClass.get(1)
        assertEquals "Wilma", p.name
    }

    void testBeforeInsertEvent() {
        def personClass = ga.getDomainClass("PersonEvent").clazz
        def p = personClass.newInstance()

        p.name = "Fred"
        def success = false
        p.beforeInsert = {                
            name = "Bob"
        }
        p.save()
        session.flush()

        assertEquals "Bob", p.name

        session.clear()

        p = personClass.get(1)

        assertEquals "Bob", p.name
        p.beforeInsert = {
            name = "Marge"
        }
        p.save(flush:true)

        assertEquals "Bob", p.name

    }

    void onSetUp() {
		this.gcl.parseClass('''
import grails.persistence.*

@Entity
class PersonEvent {
	Long id
	Long version
	String name
	Date dateCreated
	Date lastUpdated

	def afterLoad = {
	    name = "Bob"
	}
	def beforeDelete = {}
	def beforeUpdate = {}
	def beforeInsert = {}

    static hasMany = [addresses:Address]
}
@Entity
class Address {
    String postCode
    Date lastUpdated
    static belongsTo = [person:PersonEvent]
}
class PersonEvent2 {
	Long id
	Long version
	String name
	Date dateCreated
	Date lastUpdated

    def beforeUpdate = {}
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