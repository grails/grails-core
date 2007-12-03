package org.codehaus.groovy.grails.orm.hibernate;


import org.hibernate.FlushMode;

class ValidationFailureTests extends AbstractGrailsHibernateTests {

	void onSetUp() {
		gcl.parseClass(
"""
class ValidationFailureBook {
  Long id
  Long version
  String title
}
class ValidationFailureAuthor {
    Long id
    Long version
    String name
    Set books
    static hasMany = [books: ValidationFailureBook]
    static constraints = {
        name(size:8..16)
    }
}
"""
		)
	}


	void testValidationFailure() {
	    def authorClass = ga.getDomainClass("ValidationFailureAuthor")
	    def bookClass = ga.getDomainClass("ValidationFailureBook")

	    def a = authorClass.newInstance()
	    a.name = "123456789"

        def b1 = bookClass.newInstance()
        b1.title = "foo"
	    a.addToBooks(b1)
        def b2 = bookClass.newInstance()
        b2.title = "bar"
	    a.addToBooks(b2)

	    a.save(true)

	    assert session.contains(a)
	    session.flush()

	    session.evict(a)
	    session.evict(b1)
	    session.evict(b2)
	    a = null
	    b1 = null
	    b2 = null

	    a = authorClass.clazz.get(1)

	    // now invalidate a
	    a.name = "bad"
	    a.save()

	    assertTrue FlushMode.isManualFlushMode(session.getFlushMode())

    }
	
	void onTearDown() {
		
	}
}
