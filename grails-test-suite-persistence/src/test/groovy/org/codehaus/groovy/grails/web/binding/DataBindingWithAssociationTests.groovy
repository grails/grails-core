package org.codehaus.groovy.grails.web.binding

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests
import org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import grails.persistence.Entity

/**
 * @author Graeme Rocher
 * @since 1.1
 */
class DataBindingWithAssociationTests extends AbstractGrailsHibernateTests {


    @Override protected void onTearDown() {
        RequestContextHolder.setRequestAttributes(null)
    }

    @Override
    protected getDomainClasses() {
        [DataBindingWithAssociationBook, DataBindingWithAssociationAuthor,DataBindingWithAssociationShip, DataBindingWithAssociationCaptain]
    }




    void testDataBindingViaConstructorCall() {
        def Author = ga.getDomainClass("databindingwithassociationtests.Author").clazz
        def Book = ga.getDomainClass("databindingwithassociationtests.Book3").clazz

        def a = Author.newInstance(name:"Stephen").save(flush:true)

        assert a != null : "should have saved the Author"

        def b = Book.newInstance(author: a, title:"Book 1", isbn: "308943")
        assert b.author == a
        assert b.title == "Book 1"
    }

    void testDataBindingWithAssociation() {

        def a = new DataBindingWithAssociationAuthor(name:"Stephen").save(flush:true)

        assert a != null : "should have saved the Author"

        def request = new MockHttpServletRequest()
        request.addParameter("title","Book 1")
        request.addParameter("isbn", "938739")
        request.addParameter("author.id", "1")
        def params = new GrailsParameterMap(request)

        def b2 = new DataBindingWithAssociationBook()
        b2.properties['title', 'isbn', 'author'] = params

        assert b2.save(flush:true) : "should have saved book"

        // test traditional binding

        request = new MockHttpServletRequest()
        request.addParameter("title","Book 2")
        request.addParameter("isbn", "5645674")
        request.addParameter("author.id", "1")
        params = new GrailsParameterMap(request)

        def b = new DataBindingWithAssociationBook(params)

        assert b.save(flush:true) : "should have saved book"
    }

    void testBindToSetCollection() {
        def a = new DataBindingWithAssociationAuthor(name:"Stephen King")
                    .addToBooks(title:"The Stand", isbn:"983479")
                    .addToBooks(title:"The Shining", isbn:"232309")
                    .save(flush:true)


        assert a != null : "should have saved the Author"

        DataBindingWithAssociationAuthor.withSession { session -> session.clear() }

        a = DataBindingWithAssociationAuthor.findByName("Stephen King")

        def request = new MockHttpServletRequest()
        request.addParameter("books[0].isbn","12345")
        request.addParameter("books[1].isbn","54321")
        def params = new GrailsParameterMap(request)

        a.properties = params

        assert a.books.find { it.isbn == "12345" } != null
        assert a.books.find { it.isbn == "54321" } != null
    }

    void testBindToNewInstance() {
        super.buildMockRequest()
        def a = new DataBindingWithAssociationAuthor(name:"Stephen King")



        def request = new MockHttpServletRequest()
        request.addParameter("books[0].isbn","12345")
        request.addParameter("books[0].title","The Shining")

        request.addParameter("books[1].isbn","54321")
        request.addParameter("books[1].title","The Stand")

        def params = new GrailsParameterMap(request)

        a.properties = params

        assert a.books.find { it.isbn == "12345" }?.title == "The Shining"
        assert a.books.find { it.isbn == "54321" }?.title == "The Stand"
    }

    void testBindingToSetOfString() {
        def ship = new DataBindingWithAssociationShip()

        def request = new MockHttpServletRequest()
        request.addParameter 'name', 'Ship Name'
        request.addParameter 'captain.name', 'Captain Name'
        request.addParameter 'captain.weapons', 'sword'
        request.addParameter 'captain.weapons', 'pistol'

        def params = new GrailsParameterMap(request)

        ship.properties = params

        assertEquals 'Ship Name', ship.name
        assertEquals 'Captain Name', ship.captain?.name

        assertEquals 2, ship.captain.weapons?.size()
        assertTrue ship.captain.weapons.contains('sword')
        assertTrue ship.captain.weapons.contains('pistol')
    }
}



@Entity
class DataBindingWithAssociationBook {
    static belongsTo = [author: DataBindingWithAssociationAuthor]
    String isbn
    String title
    static constraints = {
        isbn(unique: true, size: 1..10)
        title(size: 1..40)
    }
}

@Entity
class DataBindingWithAssociationAuthor {
    static hasMany = [books: DataBindingWithAssociationBook]
    String name
    static constraints = {
        name(size: 1..40)
        books sort:"title"
    }
}

@Entity
class DataBindingWithAssociationShip {
    String name
    DataBindingWithAssociationCaptain captain
}

@Entity
class DataBindingWithAssociationCaptain {
    String name
    static hasMany = [weapons: String]
}
