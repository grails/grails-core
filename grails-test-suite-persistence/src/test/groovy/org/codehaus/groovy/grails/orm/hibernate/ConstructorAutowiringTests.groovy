package org.codehaus.groovy.grails.orm.hibernate

class ConstructorAutowiringTests extends AbstractGrailsHibernateTests {

    void testDomainClassAutowiring() {
        def factory = ga.classLoader.loadClass("BookFactory").newInstance()

        def b = factory.newBook()
        assertNotNull "Service autowiring failed", b.bookService
        assertEquals("foo", b.talkToService())

        b = factory.newBook(title:"gina")
        assertNotNull b.bookService
        assertEquals "gina", b.title
        assertEquals "foo", b.talkToService()
    }

    void testAutowiringFromGet() {
        def bookClass = ga.getDomainClass("Book").clazz

        assertNotNull bookClass.newInstance(title:"The Stand").save(flush:true)

        assertEquals 1, bookClass.count()
        session.clear()

        def b = bookClass.get(1)
        assertNotNull "Service autowiring failed", b.bookService
        assertEquals "The Stand", b.title
        assertEquals "foo", b.talkToService()
    }

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class Book {
    String title

    def bookService
    def talkToService() {
        bookService.testMethod()
    }
}

class BookService {
    def testMethod() { "foo" }
}

class BookFactory {
    Book newBook() {
        new Book()
    }

    Book newBook(args) {
        new Book(args)
    }
}
'''
    }
}
