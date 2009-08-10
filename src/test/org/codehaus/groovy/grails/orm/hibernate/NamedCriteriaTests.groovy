package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Jeff Brown
 */
class NamedCriteriaTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass('''

class Book {
   Long id
   Long version
   String title
   Date datePublished

   static namedQueries = {
       recentBooks {
           def now = new Date()
           gt 'datePublished', now - 365
       }

       booksWithBookInTitle {
           like 'title', '%Book%'
       }
   }

}
''')
    }

    void testList() {
        def bookClass = ga.getDomainClass("Book").clazz

        def now = new Date()
        assert bookClass.newInstance(title: "Some New Book",
                datePublished: now - 10).save(flush: true)
        assert bookClass.newInstance(title: "Some Old Book",
                datePublished: now - 900).save(flush: true)

        session.clear()

        def books = bookClass.recentBooks.list()

        assertEquals 1, books?.size()
        assertEquals 'Some New Book', books[0].title
    }

    void testInvokingDirectly() {
        def bookClass = ga.getDomainClass("Book").clazz

        def now = new Date()
        assert bookClass.newInstance(title: "Some New Book",
                datePublished: now - 10).save(flush: true)
        assert bookClass.newInstance(title: "Some Old Book",
                datePublished: now - 900).save(flush: true)

        session.clear()

        def books = bookClass.recentBooks()

        assertEquals 1, books?.size()
        assertEquals 'Some New Book', books[0].title
    }

    void testGetReturnsCorrectObject() {
        def bookClass = ga.getDomainClass("Book").clazz

        def now = new Date()
        def newBook = bookClass.newInstance(title: "Some New Book",
                datePublished: now - 10).save(flush: true)
        assert newBook
        def oldBook = bookClass.newInstance(title: "Some Old Book",
                datePublished: now - 900).save(flush: true)
        assert oldBook

        session.clear()

        def book = bookClass.recentBooks.get(newBook.id)

        assert book
        assertEquals 'Some New Book', book.title
    }

    void testGetReturnsNull() {
        def bookClass = ga.getDomainClass("Book").clazz

        def now = new Date()
        def newBook = bookClass.newInstance(title: "Some New Book",
                datePublished: now - 10).save(flush: true)
        assert newBook
        def oldBook = bookClass.newInstance(title: "Some Old Book",
                datePublished: now - 900).save(flush: true)
        assert oldBook

        session.clear()

        def book = bookClass.recentBooks.get(42 + oldBook.id)

        assert !book
    }

    void testCount() {
        def bookClass = ga.getDomainClass("Book").clazz

        def now = new Date()
        def newBook = bookClass.newInstance(title: "Some New Book",
                datePublished: now - 10).save(flush: true)
        assert newBook
        def oldBook = bookClass.newInstance(title: "Some Old Book",
                datePublished: now - 900).save(flush: true)
        assert oldBook

        session.clear()
        assertEquals 2, bookClass.booksWithBookInTitle.count()
        assertEquals 1, bookClass.recentBooks.count()
    }

}