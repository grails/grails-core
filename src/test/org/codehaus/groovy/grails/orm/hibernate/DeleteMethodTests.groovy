/**
 * Tests the delete method
 
 * @author Graeme Rocher
 * @since 1.0
  *
 * Created: Sep 7, 2007
 * Time: 8:21:22 AM
 * 
 */
package org.codehaus.groovy.grails.orm.hibernate
class DeleteMethodTests extends AbstractGrailsHibernateTests {

	void testDeleteAndFlush() {
		def domainClass = ga.getDomainClass("Book2")

		def book = domainClass.newInstance()

		book.title = "The Stand"
		book.save()


        book = domainClass.clazz.get(1)
        book.delete(flush:true)

        book = domainClass.clazz.get(1)

        assert !book
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
