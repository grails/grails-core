package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

/**
 * Tests that bidirectional one-to-one associations cascade correctly with foreign key in parent cascades correctly
 */
class BidirectionalOneToOneBelongsToCascadeSpec extends GormSpec {

    void "Test that child is saved correctly when associating only the owning side"() {
        when:"An owner is saved by the inverse child is not associated"
            def book = new Book()
            book.name = "A new book"
            book.author = new Author(name:"Grails")
            book.save(flush:true)
            session.clear()

            book = Book.get(1)

        then:"Both sides are correctly associated"
            book.author != null
            book.author.book != null
    }
    @Override
    List getDomainClasses() {
        [Book,Author]
    }
}

@Entity
class Book {

    String name
    Author author

    static constraints = {
    }
}
@Entity
class Author {
    String name
    static belongsTo = [book:Book]

}
