package org.codehaus.groovy.grails.orm.hibernate.binding

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder

/**
 * @author Graeme Rocher
 * @since 1.0
 *
 * Created: Dec 8, 2008
 */
class AssociationDataBindingTests extends AbstractGrailsHibernateTests {

    protected void onSetUp() {
        gcl.parseClass '''
import grails.persistence.*

@Entity
class AssociationBindingReviewer {

    String name
    Integer age = 25
}
@Entity
class AssociationBindingPage {
    Integer number
}
@Entity
class AssociationBindingBook {

    String title
    List pages
    Map  reviewers
    static belongsTo = [author: AssociationBindingAuthor]
    static hasMany = [pages:AssociationBindingPage, reviewers:AssociationBindingReviewer]
}
@Entity
class AssociationBindingBook2 {

    String title
}

@Entity
class AssociationBindingAuthor {

    String name
    static hasMany = [books: AssociationBindingBook, moreBooks:AssociationBindingBook2]
}
'''
    }

    @Override protected void onTearDown() {
        RequestContextHolder.setRequestAttributes(null)
    }



    void testManyToOneBinding() {
        super.buildMockRequest()
        def Book = ga.getDomainClass("AssociationBindingBook").clazz
        def Author = ga.getDomainClass("AssociationBindingAuthor").clazz

        assertNotNull Author.newInstance(name:"Stephen King").save(flush:true)

        def book = Book.newInstance()
        def params = ['author.id':1, title:'The Shining']
        book.properties = params

        assertNotNull "The author should have been bound", book.author
        assertEquals "The Shining", book.title
    }

    void testManyToOneUnBinding() {
        super.buildMockRequest()
        def Book = ga.getDomainClass("AssociationBindingBook").clazz
        def Author = ga.getDomainClass("AssociationBindingAuthor").clazz

        def author = Author.newInstance(name:"Stephen King").save(flush:true, failOnError: true)
        def book = Book.newInstance(title: "The Shining", author: author).save(flush:true, failOnError: true)

        def params = ['author.id': "null"]
        book.properties = params
        assertNull "The author should have been unbound", book.author
    }

    void testOneToManyListBindingWithSubscriptOperator() {
        super.buildMockRequest()
        def Book = ga.getDomainClass("AssociationBindingBook").clazz
        def Author = ga.getDomainClass("AssociationBindingAuthor").clazz
        def Page = ga.getDomainClass("AssociationBindingPage").clazz

        def author = Author.newInstance(name: "William Gibson").save(flush: true, failOnError: true)
        def page1 = Page.newInstance(number: 1).save(flush: true, failOnError: true)
        def page2 = Page.newInstance(number: 2).save(flush: true, failOnError: true)
        def book = Book.newInstance(title: "Pattern Recognition", author: author).save(flush: true, failOnError: true)
        session.clear()
        book = book.refresh()

        def params = ["pages[0].id": "$page1.id", "pages[1].id": "$page2.id"]

        book.properties = params

        assertEquals 2, book.pages.size()
        assertEquals 1, book.pages[0].number
        assertEquals 2, book.pages[1].number
    }

    void testOneToManyListBindingWithSubscriptOperatorCanExtendExistingList() {
        super.buildMockRequest()
        def Book = ga.getDomainClass("AssociationBindingBook").clazz
        def Author = ga.getDomainClass("AssociationBindingAuthor").clazz
        def Page = ga.getDomainClass("AssociationBindingPage").clazz

        def author = Author.newInstance(name: "William Gibson").save(flush: true, failOnError: true)
        def page1 = Page.newInstance(number: 1).save(flush: true, failOnError: true)
        def page2 = Page.newInstance(number: 2).save(flush: true, failOnError: true)
        def book = Book.newInstance(title: "Pattern Recognition", author: author, pages: [page1]).save(flush: true, failOnError: true)
        session.clear()
        book = book.refresh()

        def params = ["pages[1].id": "$page2.id"]

        book.properties = params

        assertEquals 2, book.pages.size()
        assertEquals 1, book.pages[0].number
        assertEquals 2, book.pages[1].number
    }

    void testOneToManyListBindingWithSubscriptOperatorCanInsertToEmptyIndexOfList() {
        super.buildMockRequest()
        def Book = ga.getDomainClass("AssociationBindingBook").clazz
        def Author = ga.getDomainClass("AssociationBindingAuthor").clazz
        def Page = ga.getDomainClass("AssociationBindingPage").clazz

        def author = Author.newInstance(name: "William Gibson").save(flush: true, failOnError: true)
        def page1 = Page.newInstance(number: 1).save(flush: true, failOnError: true)
        def page2 = Page.newInstance(number: 2).save(flush: true, failOnError: true)
        def book = Book.newInstance(title: "Pattern Recognition", author: author, pages: [null, page2]).save(flush: true, failOnError: true)
        session.clear()
        book = book.refresh()

        def params = ["pages[0].id": "$page1.id"]

        book.properties = params

        assertEquals 2, book.pages.size()
        assertEquals 1, book.pages[0].number
        assertEquals 2, book.pages[1].number
    }

    void testOneToManyListBindingWithNestedSubscriptOperatorCanInsertToEmptyIndexOfList() {
        super.buildMockRequest()
        def Book = ga.getDomainClass("AssociationBindingBook").clazz
        def Author = ga.getDomainClass("AssociationBindingAuthor").clazz
        def Page = ga.getDomainClass("AssociationBindingPage").clazz

        def author = Author.newInstance(name: "William Gibson").save(flush: true, failOnError: true)
        def page1 = Page.newInstance(number: 1).save(flush: true, failOnError: true)
        def page2 = Page.newInstance(number: 2).save(flush: true, failOnError: true)
        def book = Book.newInstance(title: "Pattern Recognition", author: author, pages: [null, page2]).save(flush: true, failOnError: true)
        session.clear()
        author = author.refresh()

        def params = ["books[0].pages[0].id": "$page1.id"]

        author.properties = params

        assertEquals 2, author.books.sum { it.pages.size() }
        assertFalse author.books.any { it.pages.contains(null) }
    }

    void testOneToManyListUnBindingWithSubscriptOperator() {
        super.buildMockRequest()
        def Book = ga.getDomainClass("AssociationBindingBook").clazz
        def Author = ga.getDomainClass("AssociationBindingAuthor").clazz
        def Page = ga.getDomainClass("AssociationBindingPage").clazz

        def author = Author.newInstance(name: "William Gibson").save(flush: true, failOnError: true)
        def page = Page.newInstance(number: 1).save(flush: true, failOnError: true)
        def book = Book.newInstance(title: "Pattern Recognition", author: author, pages: [page]).save(flush: true, failOnError: true)
        session.clear()
        book = book.refresh()

        def params = ["pages[0].id": "null"]
        book.properties = params

        assertNull "Should have removed pages[0] but it is ${book.pages[0]}", book.pages[0]
    }

    void testOneToManyListBindingNewInstance() {
        super.buildMockRequest()
        def Book = ga.getDomainClass("AssociationBindingBook").clazz
        def Author = ga.getDomainClass("AssociationBindingAuthor").clazz
        def Page = ga.getDomainClass("AssociationBindingPage").clazz

        def author = Author.newInstance(name: "William Gibson").save(flush: true, failOnError: true)
        def book = Book.newInstance(title: "Pattern Recognition", author: author, pages: []).save(flush: true, failOnError: true)
        session.clear()
        book = book.refresh()

        def params = ["pages[0].number": "1", "pages[1].number": "2", "pages[2].number": "3"]
        book.properties = params
        assertEquals "Should bound new pages to book", 3, book.pages.size()

        book.save(flush: true, failOnError: true)
        assertEquals "Should have created new pages", 3, Page.count()
    }

    void testOneToManyMapUnBindingWithSubscriptOperator() {
        super.buildMockRequest()
        def Book = ga.getDomainClass("AssociationBindingBook").clazz
        def Author = ga.getDomainClass("AssociationBindingAuthor").clazz
        def Reviewer = ga.getDomainClass("AssociationBindingReviewer").clazz

        def author = Author.newInstance(name: "Peter Ledbrook").save(flush: true, failOnError: true)
        def reviewer = Reviewer.newInstance(name: "Rob Fletcher").save(flush: true, failOnError: true)
        def book = Book.newInstance(title: "Grails In Action", author: author, reviewers: [rob: reviewer]).save(flush: true, failOnError: true)
        session.clear()
        book = book.refresh()

        def params = ["reviewers[rob].id": "null"]
        book.properties = params

        assertNull "Should have removed reviewers[rob] but it is ${book.reviewers['rob']}", book.reviewers["rob"]
    }

    void testOneToManyBindingWithSubscriptOperatorAndExistingInstance() {
        super.buildMockRequest()
        def Book = ga.getDomainClass("AssociationBindingBook").clazz
        def Author = ga.getDomainClass("AssociationBindingAuthor").clazz
        def Reviewer = ga.getDomainClass("AssociationBindingReviewer").clazz
        def Page = ga.getDomainClass("AssociationBindingPage").clazz

        def author = Author.newInstance(name: "Stephen King").save(flush: true)
        assertNotNull author
        def book = Book.newInstance(title: "The Shining", author: author).save(flush:true)
        assertNotNull book

        assertNotNull Reviewer.newInstance(name:"Joe Bloggs", age:41).save(flush:true)
        assertNotNull Page.newInstance(number:11).save(flush:true)

        session.clear()

        author = Author.get(1)
        assertEquals 1, author.books.size()

        def request = new MockHttpServletRequest()
        request.addParameter("books[1].title", "The Stand")
        request.addParameter("books[1].reviewers['joe'].id", "1")
        request.addParameter("books[1].reviewers['joe'].name", "Joseph Bloggs")
        request.addParameter("books[1].pages[0].id", "1")

        author.properties = request
        assertEquals 2, author.books.size()

        def b2 = author.books.find { it.title == "The Stand" }
        assertNotNull b2

        assertNotNull b2.reviewers['joe']
        assertEquals 41, b2.reviewers['joe'].age
        assertEquals "Joseph Bloggs", b2.reviewers['joe'].name

        assertNotNull b2.pages[0]
        assertEquals 11, b2.pages[0].number

        author.save(flush:true)

        session.clear()

        author = Author.get(1)
        assertEquals 2, author.books.size()

        b2 = author.books.find { it.title == "The Stand" }
        assertNotNull b2
        assertNotNull b2.reviewers['joe']
        assertEquals 41, b2.reviewers['joe'].age
        assertEquals "Joseph Bloggs", b2.reviewers['joe'].name

        assertNotNull b2.pages[0]
        assertEquals 11, b2.pages[0].number
    }

    void testOneToManyBindingWithSubscriptOperatorAndNewInstance() {
        super.buildMockRequest()
        def Author = ga.getDomainClass("AssociationBindingAuthor").clazz

        def author = Author.newInstance(name:"Stephen King")

        def request = new MockHttpServletRequest()
        request.addParameter("books[0].title", "The Shining")
        request.addParameter("books[0].pages[0].number", "1")
        request.addParameter("books[0].pages[1].number", "2")
        request.addParameter("books[0].pages[2].number", "3")
        request.addParameter("books[0].reviewers['fred'].name", "Fred Bloggs")
        request.addParameter("books[0].reviewers['bob'].name", "Bob Bloggs")
        request.addParameter("books[0].reviewers['chuck'].name", "Chuck Bloggs")
        request.addParameter("books[1].title", "The Stand")

        author.properties = request

        def books = author.books.iterator()
        def b1 = books.next()
        def b2 = books.next()

        assertEquals "The Shining", b1.title
        assertEquals 3, b1.pages.size()
        assertEquals 1, b1.pages[0].number
        assertEquals 2, b1.pages[1].number
        assertEquals 3, b1.pages[2].number

        assertEquals 3, b1.reviewers.size()
        assertEquals "Fred Bloggs", b1.reviewers['fred'].name
        assertEquals "The Stand", b2.title

        assertNotNull "author should have saved", author.save(flush:true)

        session.clear()

        author = Author.get(1)
        assertEquals 2, author.books.size()

        b1 = author.books.find { it.title == 'The Shining'}
        assertNotNull b1

        assertEquals "The Shining", b1.title
        assertEquals 3, b1.pages.size()
        assertEquals 1, b1.pages[0].number
        assertEquals 2, b1.pages[1].number
        assertEquals 3, b1.pages[2].number

        assertEquals 3, b1.reviewers.size()
        assertEquals "Fred Bloggs", b1.reviewers['fred'].name
        assertEquals "The Stand", b2.title
    }

    void testOneToManyBindingWithAnArrayOfStrings() {
        super.buildMockRequest()
        def Book = ga.getDomainClass("AssociationBindingBook2").clazz
        def Author = ga.getDomainClass("AssociationBindingAuthor").clazz

        def author = Author.newInstance(name:"Stephen King")

        assertNotNull Book.newInstance(title:"The Shining").save(flush:true)
        assertNotNull Book.newInstance(title:"The Stand").save(flush:true)

        def params = ['moreBooks':['1','2'] as String[], name:'Stephen King']
        author.properties = params

        assertNotNull "The books association should have been bound", author.moreBooks
        assertEquals 2, author.moreBooks.size()
        assertTrue "element is not an instance of a book, no binding occured!", Book.isInstance(author.moreBooks.iterator().next())
        assertEquals "Stephen King", author.name
    }

    void testOneToManyWithAString() {
        super.buildMockRequest()

        def Book = ga.getDomainClass("AssociationBindingBook2").clazz
        def Author = ga.getDomainClass("AssociationBindingAuthor").clazz

        assertNotNull Book.newInstance(title:"The Shining").save(flush:true)
        assertNotNull Book.newInstance(title:"The Stand").save(flush:true)

        def params = ['moreBooks':'1', name:'Stephen King']
        def author = Author.newInstance()
        author.properties = params
        assertNotNull "The books association should have been bound", author.moreBooks
        assertEquals 1, author.moreBooks.size()
        assertTrue "element is not an instance of a book, no binding occured!", Book.isInstance(author.moreBooks.iterator().next())
        assertEquals "Stephen King", author.name
    }

    void testOneToManyWithIndexedParams() {
        super.buildMockRequest()

        if (notYetImplemented()) return

        def Book = ga.getDomainClass("AssociationBindingBook2").clazz
        def Author = ga.getDomainClass("AssociationBindingAuthor").clazz

        assertNotNull Book.newInstance(title:"The Shining").save(flush:true)
        assertNotNull Book.newInstance(title:"The Stand").save(flush:true)

        def params = ['moreBooks[0]':'1', name:'Stephen King']
        def author = Author.newInstance()

        author.properties = params
        assertNotNull "The books association should have been bound", author.moreBooks

        assertEquals 1, author.moreBooks.size()
        assertTrue "element is not an instance of a book, no binding occured!", Book.isInstance(author.moreBooks.iterator().next())
        assertEquals "Stephen King", author.name
    }
}
