package org.codehaus.groovy.grails.metaclass;

import org.codehaus.groovy.grails.orm.hibernate.*

class AddRelationshipTests extends AbstractGrailsHibernateTests {

	void testAddRelationship() {
		def bookClass = ga.getGrailsDomainClass("Book")
		def authorClass = ga.getGrailsDomainClass("Author")
		
		def book = bookClass.newInstance()		

		// test injected addBook method
		def author = authorClass.newInstance()
		author.name = "Stephen King"
		
		book.addAuthor(author)
		
		assertEquals 1, book.authors?.size()
		assertTrue book.authors.contains(author)
		
		// test user provided
		book.addAuthor("Rudyard Kipling")
		assertEquals 2, book.authors?.size()		
		
	}

	void onSetUp() {
		this.gcl.parseClass('''
class Book {
	Long id
	Long version
	Set authors

	def addAuthor(String name) {
        if(!authors)authors = new HashSet()
		authors.add(new Author(name:name, book:this))
	}
	def hasMany = [authors:Author]
}
class Author {
	Long id
	Long version
	String name
	Book book
}
'''
		)
	}
	
	void onTearDown() {
		
	}


}
