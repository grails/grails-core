package org.codehaus.groovy.grails.orm.hibernate

/**
 * Tests that finders like Foo.findByFooOrBar(x,y) work.
 */
class DynamicFinderOrTests extends AbstractGrailsHibernateTests {

    void testFindAllByOr() {
        def bookClass = ga.getDomainClass("Book")

        def b = bookClass.newInstance()
        b.title = "Groovy in Action"
        b.publisher = "Manning"
        b.save(flush:true)

        def b2 = bookClass.newInstance()
        b2.title = "Ajax in Action"
        b2.publisher = "Manning"
        b2.save(flush:true)

        assertEquals 1, bookClass.clazz.findAllByTitleAndPublisher("Groovy in Action", "Manning").size()
        assertEquals 1, bookClass.clazz.findAllByTitleAndPublisher("Ajax in Action", "Manning").size()
        assertEquals 2, bookClass.clazz.findAllByTitleOrPublisher("Groovy in Action", "Manning").size()
    }

    void testCountByOr() {
        def bookClass = ga.getDomainClass("Book")

        def b = bookClass.newInstance()
        b.title = "Groovy in Action"
        b.publisher = "Manning"
        b.save(flush:true)

        def b2 = bookClass.newInstance()
        b2.title = "Ajax in Action"
        b2.publisher = "Manning"
        b2.save(flush:true)

        assertEquals 1, bookClass.clazz.countByTitleAndPublisher("Groovy in Action", "Manning")
        assertEquals 1, bookClass.clazz.countByTitleAndPublisher("Ajax in Action", "Manning")
        assertEquals 2, bookClass.clazz.countByTitleOrPublisher("Groovy in Action", "Manning")
    }

    protected void onSetUp() {
        gcl.parseClass '''
class Book {
    Long id
    Long version
    String title
    String publisher
}
'''
    }
}
