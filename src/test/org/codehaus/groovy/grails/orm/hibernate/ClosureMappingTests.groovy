package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*

class ClosureMappingTests extends AbstractGrailsHibernateTests {

	void testClosureMapping() {
		def thingClass = ga.getDomainClass("Thing")

        def thing = thingClass.newInstance()
        
        assertEquals "Hello, Fred!", thing.whoHello("Fred")

	}

	void onSetUp() {
		this.gcl.parseClass('''
class Thing {
Long id
Long version
String name

def whoHello = { who ->
return "Hello, ${who}!"
}
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
