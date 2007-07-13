package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*

class ManyToManyTests extends AbstractGrailsHibernateTests {

	void testManyToManyDomain() {
		def authorDomain = ga.getDomainClass("Author")
		def bookDomain = ga.getDomainClass("Book")

		
		def books = authorDomain?.getPropertyByName("books")
		def authors = bookDomain?.getPropertyByName("authors")
				
		assert books?.isManyToMany()
		assert authors?.isManyToMany()
		assert !books?.isOneToMany()
		assert !authors?.isOneToMany()				
	}
	void testManyToManyMapping() {
		def authorClass = ga.getDomainClass("Author")
		def bookClass = ga.getDomainClass("Book")
		def a = authorClass.newInstance()
		
		a.addToBooks(bookClass.newInstance())
		a.save(true)
		
		a = null
		
		assertEquals 1, bookClass.clazz.list().size()
		
		def b = bookClass.clazz.get(1)
		assert b
		assert b.authors
		
		a = authorClass.clazz.get(1)
		assert a
		assert a.books
		
		assertEquals b, a.books.find { it.id == 1}
		this.session.flush()
		
		this.session.evict(a)
		this.session.evict(b)
		
		a = authorClass.clazz.get(1)
		assert a
		assert a.books
		
		b = bookClass.clazz.get(1)
		assert b
		assert b.authors
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
