package org.codehaus.groovy.grails.orm.hibernate

import spock.lang.Specification
import spock.lang.Issue
import grails.persistence.Entity
import grails.test.mixin.TestFor

/**
 */
class AutoLinkListAssociationSpec extends GormSpec{

    @Issue('GRAILS-8815')
    void "Test that associations are linked automatically when saving"() {
        given:"A new domain class with a one-to-many association"
            def author = new AutoLinkListAuthor(firstName:'foo', lastName: 'bar')
        when:"The domain is saved"
            author.save()
        then:"The association is intially empty"
            author.id != null
            author.books == null

        when:"An associated object is added"
            def book1 = new AutoLinkListBook(title: 'grails', price: 43, published: new Date(), author: author)

            // add the book to the author to complete the other side
            author.addToBooks(book1)
        then:"The relationship size is correct"
            author.books.size() == 1

        when:"The domain is saved"
            author.save()
        then:"The relationship size is still correct"
            author.books.size() == 1
    }

    @Override
    List getDomainClasses() {
        [AutoLinkListAuthor, AutoLinkListBook]
    }
}

@Entity
class AutoLinkListAuthor {
    String firstName
    String lastName

    // Hi, see here!
    List books

    static hasMany = [books: AutoLinkListBook]
}

@Entity
class AutoLinkListBook {
    String title
    Date published
    BigDecimal price

    static belongsTo = [author: AutoLinkListAuthor]
}