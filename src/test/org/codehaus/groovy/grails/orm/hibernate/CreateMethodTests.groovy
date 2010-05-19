package org.codehaus.groovy.grails.orm.hibernate

/**
 * Tests the create method.
 *
 * @author Graeme Rocher
 * @since 1.0
 */
class CreateMethodTests extends AbstractGrailsHibernateTests {

    void testCreateMethod() {
        def domainClass = ga.getDomainClass("Book2")

        def book = domainClass.clazz.create()
        assertNotNull book
        assertNotNull domainClass.clazz.isInstance(book)
    }

    void testCreateMethodOnAnnotatedEntity() {
        def domainClass = ga.getDomainClass("BookEntity")

        def book = domainClass.clazz.create()
        assertNotNull book
        assertNotNull domainClass.clazz.isInstance(book)
    }

    void onSetUp() {
        gcl.parseClass """
class Book2 {
    Long id
    Long version
    String title
}

@javax.persistence.Entity
class BookEntity {
    @javax.persistence.Id
    Long myId
    String title
}
"""
    }
}
