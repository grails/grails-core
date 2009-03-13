package org.codehaus.groovy.grails.orm.hibernate;


class WithCriteriaMethodTests extends AbstractGrailsHibernateTests {

	void testWithCriteriaMethod() {
		def authors = []
		def domainClass = ga.getDomainClass("WithCriteriaMethodAuthor")
		authors << domainClass.newInstance()
		authors << domainClass.newInstance()
		authors << domainClass.newInstance()
		
		
		
		authors[0].name = "Stephen King"
	    authors[1].name = "John Grisham"
	    authors[2].name = "James Patterson"
		authors*.save(true)
		
		def results = domainClass.clazz.withCriteria {
			like('name','J%')
		}
		
		assertEquals 2, results.size()
	}

	void onSetUp() {
		gcl.parseClass(
"""
class WithCriteriaMethodBook {
  Long id
  Long version
  def belongsTo = WithCriteriaMethodAuthor
  WithCriteriaMethodAuthor author
  String title
  boolean equals(obj) { title == obj?.title }
  int hashCode() { title ? title.hashCode() : super.hashCode() }
  String toString() { title }

  static constraints = {
      author(nullable:true)
  }
}
class WithCriteriaMethodAuthor {
  Long id
  Long version
  String name
  Set books
  def relatesToMany = [books:WithCriteriaMethodBook]
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
