/**
 * Tests pessimistic locking "lock" method
 
 * @author Graeme Rocher
 * @since 0.4
  *
 * Created: Sep 5, 2007
 * Time: 6:57:09 PM
 * 
 */
package org.codehaus.groovy.grails.orm.hibernate
class PessimisticLockingTests extends AbstractGrailsHibernateTests {

	void testLockMethod() {
		def domainClass = ga.getDomainClass("Book2")

		def book = domainClass.newInstance()

		book.title = "The Stand"
		book.save()

        session.flush()
		session.clear()

        book = domainClass.clazz.get(1)

        book.lock()
        book.title = "The Shining"
        book.save()

	}

    void testLockStaticMethod() {
        def domainClass = ga.getDomainClass("Book2").clazz

        def book = domainClass.newInstance()

        book.title = "The Stand"
        book.save(flush:true)

        session.clear()

        book = domainClass.lock(1)

        assert book

    }

    void onSetUp() {
		gcl.parseClass(
"""
class Book2 {
  Long id
  Long version
  String title
 }
"""
		)
	}

	void onTearDown() {

	}
}
