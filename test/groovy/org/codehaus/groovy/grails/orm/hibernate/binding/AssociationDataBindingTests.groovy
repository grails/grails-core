package org.codehaus.groovy.grails.orm.hibernate.binding

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests
import org.springframework.mock.web.MockHttpServletRequest

/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: Dec 8, 2008
 */

public class AssociationDataBindingTests extends AbstractGrailsHibernateTests{

    protected void onSetUp() {
        gcl.parseClass('''

class AssociationBindingReviewer {
    Long id
    Long version

    String name
    Integer age = 25
}
class AssociationBindingPage {
    Long id
    Long version

    Integer number
}
class AssociationBindingBook {
    Long id
    Long version

    String title
    AssociationBindingAuthor author
    List pages
    Map  reviewers
    static belongsTo = [author: AssociationBindingAuthor]
    static hasMany = [pages:AssociationBindingPage, reviewers:AssociationBindingReviewer]
}

class AssociationBindingBook2 {
    Long id
    Long version

    String title
}

class AssociationBindingAuthor {
    Long id
    Long version

    String name
    Set books
    Set moreBooks
    static hasMany = [books: AssociationBindingBook, moreBooks:AssociationBindingBook2]
}
''')

    }



    void testManyToOneBinding() {
        def Book = ga.getDomainClass("AssociationBindingBook").clazz
        def Author = ga.getDomainClass("AssociationBindingAuthor").clazz

        assert Author.newInstance(name:"Stephen King").save(flush:true)

        def book = Book.newInstance()

        def params = ['author.id':1, title:'The Shining']

        book.properties = params

        assertNotNull "The author should have been bound", book.author
        assertEquals "The Shining", book.title
    }

    void testOneToManyBindingWithSubscriptOperatorAndExistingInstance() {
        def Book = ga.getDomainClass("AssociationBindingBook").clazz
        def Author = ga.getDomainClass("AssociationBindingAuthor").clazz
        def Reviewer = ga.getDomainClass("AssociationBindingReviewer").clazz
        def Page = ga.getDomainClass("AssociationBindingPage").clazz

        def author = Author.newInstance(name: "Stephen King").save(flush: true)
        assert author
        def book = Book.newInstance(title: "The Shining", author: author).save(flush:true)
        assert book


        assert Reviewer.newInstance(name:"Joe Bloggs", age:41).save(flush:true)
        assert Page.newInstance(number:11).save(flush:true)

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
        def Book = ga.getDomainClass("AssociationBindingBook2").clazz
        def Author = ga.getDomainClass("AssociationBindingAuthor").clazz

        def author = Author.newInstance(name:"Stephen King")

        assert Book.newInstance(title:"The Shining").save(flush:true)
        assert Book.newInstance(title:"The Stand").save(flush:true)

        def params = ['moreBooks':['1','2'] as String[], name:'Stephen King']

        author.properties = params

        assertNotNull "The books association should have been bound", author.moreBooks

        assertEquals 2, author.moreBooks.size()
        assertTrue "element is not an instance of a book, no binding occured!", Book.isInstance(author.moreBooks.iterator().next())
        assertEquals "Stephen King", author.name

    }

    void testOneToManyWithAString() {

        def Book = ga.getDomainClass("AssociationBindingBook2").clazz
        def Author = ga.getDomainClass("AssociationBindingAuthor").clazz        

        assert Book.newInstance(title:"The Shining").save(flush:true)
        assert Book.newInstance(title:"The Stand").save(flush:true)

        def params = ['moreBooks':'1', name:'Stephen King']

        def author = Author.newInstance()

        author.properties = params

        assertNotNull "The books association should have been bound", author.moreBooks

        assertEquals 1, author.moreBooks.size()
        assertTrue "element is not an instance of a book, no binding occured!", Book.isInstance(author.moreBooks.iterator().next())
        assertEquals "Stephen King", author.name
    }
}