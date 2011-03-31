package org.codehaus.groovy.grails.orm.hibernate

/**
 * Tests the delete method.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class DeleteMethodTests extends AbstractGrailsHibernateTests {

    void testDeleteAndFlush() {
        def domainClass = ga.getDomainClass("Book2")

        def book = domainClass.newInstance()
        book.title = "The Stand"
        book.save()

        book = domainClass.clazz.get(1)
        book.delete(flush:true)

        book = domainClass.clazz.get(1)
        assertNull book
    }

    void onSetUp() {
        gcl.parseClass """
class Book2 {
    Long id
    Long version
    String title
}
"""
    }
}
