package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*

/**
 * Tests that finders like Foo.findByFooOrBar(x,y) work
 */
class DynamicFinderOrTests extends AbstractGrailsHibernateTests {

	void testFindAllByOr() {
		def bookClass = ga.getDomainClass("Book")

		def b = bookClass.newInstance()

		b.title = "Groovy in Action"
		b.publisher = "Manning"
		b.save(flush:true)

		def b2 = bookClass.newInstance()

		b2.title = "Ajax in Action"
		b2.publisher = "Manning"
		b2.save(flush:true)


        assertEquals 1, bookClass.clazz.findAllByTitleAndPublisher("Groovy in Action", "Manning").size()
        assertEquals 1, bookClass.clazz.findAllByTitleAndPublisher("Ajax in Action", "Manning").size()
        assertEquals 2, bookClass.clazz.findAllByTitleOrPublisher("Groovy in Action", "Manning").size()
	}

	void testCountByOr() {
		def bookClass = ga.getDomainClass("Book")

		def b = bookClass.newInstance()

		b.title = "Groovy in Action"
		b.publisher = "Manning"
		b.save(flush:true)

		def b2 = bookClass.newInstance()

		b2.title = "Ajax in Action"
		b2.publisher = "Manning"
		b2.save(flush:true)


        assertEquals 1, bookClass.clazz.countByTitleAndPublisher("Groovy in Action", "Manning")
        assertEquals 1, bookClass.clazz.countByTitleAndPublisher("Ajax in Action", "Manning")
        assertEquals 2, bookClass.clazz.countByTitleOrPublisher("Groovy in Action", "Manning")
    }

	void onSetUp() {
		this.gcl.parseClass('''
class Book {
	Long id
	Long version
	String title
	String publisher
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
