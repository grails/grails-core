package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity

/**
 * Tests that bidirectional one-to-one associations cascade correctly with foreign key in parent cascades correctly
 */
class BidirectionalOneToOneBelongsToCascadeSpec extends GormSpec {

    void "Test that child is saved correctly when associating only the owning side"() {
        when:"An owner is saved by the inverse child is not associated"
            def book = new BidirectionalOneToOneBelongsToCascadeBook()
            book.name = "A new book"
            book.author = new BidirectionalOneToOneBelongsToCascadeAuthor(name:"Grails")
            book.save(flush:true)
            session.clear()

            book = BidirectionalOneToOneBelongsToCascadeBook.get(1)

        then:"Both sides are correctly associated"
            book.author != null
            book.author.book != null
    }
    @Override
    List getDomainClasses() {
        [BidirectionalOneToOneBelongsToCascadeBook,BidirectionalOneToOneBelongsToCascadeAuthor]
    }
}

@Entity
class BidirectionalOneToOneBelongsToCascadeBook {

    String name
    BidirectionalOneToOneBelongsToCascadeAuthor author

    static constraints = {
    }
}
@Entity
class BidirectionalOneToOneBelongsToCascadeAuthor {
    String name
    static belongsTo = [book:BidirectionalOneToOneBelongsToCascadeBook]

}
