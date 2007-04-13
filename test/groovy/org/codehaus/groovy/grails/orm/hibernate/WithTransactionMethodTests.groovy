package org.codehaus.groovy.grails.orm.hibernate;


class WithTransactionMethodTests extends AbstractGrailsHibernateTests {

	void testWithTransactionMethod() {
		def authors = []
		def domainClass = ga.getDomainClass("Author")
		authors << domainClass.newInstance()
		authors << domainClass.newInstance()
		authors << domainClass.newInstance()
		
		
		
		authors[0].name = "Stephen King"
	    authors[1].name = "John Grisham"
	    authors[2].name = "James Patterson"

		
		domainClass.clazz.withTransaction { status ->
			authors[0].save(true)
			authors[1].save(true)
		}
		
		def results = domainClass.clazz.list()
		assertEquals 2, results.size()
		
		domainClass.clazz.withTransaction { status ->
			authors[2].save(true)
			status.setRollbackOnly()
		}		
		
		results = domainClass.clazz.list()
		assertEquals 2, results.size()
		
	}

	void onSetUp() {
		gcl.parseClass(
"""
class Book {
  Long id
  Long version
  def belongsTo = Author
  Author author
  String title
  boolean equals(obj) { title == obj?.title }
  int hashCode() { title ? title.hashCode() : super.hashCode() }
  String toString() { title }

  static constraints = {
      author(nullable:true)
  }
}
class Author {
  Long id
  Long version
  String name
  Set books
  def relatesToMany = [books:Book]
  boolean equals(obj) { name == obj?.name }
  int hashCode() { name ? name.hashCode() : super.hashCode() }
  String toString() { name }
}
"""				
		)
	}
	
	void onTearDown() {
		
	}
}
