package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*

class UnidirectionalListMappingTests extends AbstractGrailsHibernateTests {

	void testUniListMapping() {
		def personClass = ga.getDomainClass("UnidirectionalListMappingPerson")
		def emailClass = ga.getDomainClass("UnidirectionalListMappingEmailAddress")

		def p = personClass.newInstance()

		def e = emailClass.newInstance()

		p.firstName = "Fred"
		p.lastName = "Flintstone"

		e.email = "fred@flintstones.com"
		p.addToEmailAddresses(e)

		p.save()

        session.flush()
        println "Flushed session"
        session.clear()

        assert p.id
        assert e.id

        def e2 = emailClass.newInstance()
        e2.email = "foo@bar.com"
        e2.save()
        session.flush()

        assert e2.id
	}

	void onSetUp() {
		this.gcl.parseClass('''
class UnidirectionalListMappingEmailAddress {
    Long id
    Long version
        String email
}

class UnidirectionalListMappingPerson {
    Long id
    Long version
        String firstName
        String lastName
        List emailAddresses
        static hasMany = [emailAddresses:UnidirectionalListMappingEmailAddress]
}
class ApplicationDataSource {
	   boolean pooling = true
	   boolean logSql = true
	   String dbCreate = "create-drop" // one of 'create', 'create-drop','update'
	   String url = "jdbc:hsqldb:mem:testDB"
	   String driverClassName = "org.hsqldb.jdbcDriver"
	   String username = "sa"
	   String password = ""
}

'''
		)
	}
	
	void onTearDown() {
		
	}
}
