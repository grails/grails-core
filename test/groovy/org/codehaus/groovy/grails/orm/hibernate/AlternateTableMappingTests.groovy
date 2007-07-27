package org.codehaus.groovy.grails.orm.hibernate;


class AlternateTableMappingTests extends AbstractGrailsHibernateTests {

	void testAlternateTable() {
        println "INVOKING TEST!!!"
        def bookClass = ga.getDomainClass("Book")
        println "CREATING NEW INSTANCE"
		def book = bookClass.newInstance()
		println "SETTING TITLE"
		book.title = "The Stand"
		println "SAVING BOOK"
		book.save(true)
		println "DONE"
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
