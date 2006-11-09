package org.codehaus.groovy.grails.orm.hibernate;


class AlternateTableMappingTests extends AbstractGrailsHibernateTests {

	void testAlternateTable() {
		def bookClass = ga.getGrailsDomainClass("Book")
		
		def book = bookClass.newInstance()
		book.title = "The Stand"
		book.save(true)
	}

	void onSetUp() {
		this.gcl.parseClass('''
class Book {
	Long id
	Long version
	String title
	static withTable = "my_books"
}
'''
		)
	}	
	
	void onTearDown() {
		
	}	
}
