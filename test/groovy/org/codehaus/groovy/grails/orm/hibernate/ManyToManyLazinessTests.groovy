package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*
import org.hibernate.LazyInitializationException

class ManyToManyLazinessTests extends AbstractGrailsHibernateTests {

	void testManyToManyLazyLoading() {
		def authorClass = ga.getDomainClass("Author")
		def bookClass = ga.getDomainClass("Book")
		def a = authorClass.newInstance()
		
		a.addToBooks(bookClass.newInstance())
		a.save(true)
		this.session.flush()
		
		this.session.evict(a)

		a = authorClass.clazz.get(1)
		this.session.evict(a)
		assertFalse(this.session.contains(a))
		
		try {
			def books = a.books
			assertEquals 1, books.size()
			fail("Should have thrown lazy load exception")
		}
		catch(LazyInitializationException lie) {
			// expected
		}
		
	}
	

	
	void onSetUp() {
		this.gcl.parseClass('''
class Book {
	Long id
	Long version
	Set authors
	def belongsTo = Author 
	def hasMany = [authors:Author]
}
class Author {
	Long id
	Long version
	Set books	
	def hasMany = [books:Book]
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
