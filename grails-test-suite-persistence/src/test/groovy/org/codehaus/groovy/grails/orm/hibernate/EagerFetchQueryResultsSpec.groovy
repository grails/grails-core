package org.codehaus.groovy.grails.orm.hibernate

import grails.persistence.Entity
import spock.lang.Issue

/**
 */
class EagerFetchQueryResultsSpec extends GormSpec {

    @Issue('GRAILS-8915')
    void "Test fetch eager association"() {
        given:"a one-to-many with an eager association"
            createSampleData()

        when:"The association is queried"
            def authors = EagerFetchQueryResultsBook.findByTitle('Book1').authors

        then:"The correct results are returend"
            ["Author1", "Author2"] as Set == authors.collect {it.name} as Set
    }

    public void createSampleData() {
        EagerFetchQueryResultsBook book1 = new EagerFetchQueryResultsBook(title: 'Book1')
        EagerFetchQueryResultsAuthor author1 = new EagerFetchQueryResultsAuthor(name: 'Author1')
        author1.save(flush: true, failOnError: true)
        book1.addToAuthors(author1)
        EagerFetchQueryResultsAuthor author2 = new EagerFetchQueryResultsAuthor(name: 'Author2')
        author2.save(flush: true, failOnError: true)
        book1.addToAuthors(author2)
        book1.save(flush: true, failOnError: true)
        session.clear()
    }

    @Override
    List getDomainClasses() {
        [EagerFetchQueryResultsAuthor, EagerFetchQueryResultsBook]
    }
}

@Entity
class EagerFetchQueryResultsAuthor {
    String name
    static hasMany = [books:EagerFetchQueryResultsBook]
}
@Entity
class EagerFetchQueryResultsBook {
    String title
    static hasMany = [authors:EagerFetchQueryResultsAuthor]
    static belongsTo = [EagerFetchQueryResultsAuthor]
    static fetchMode = [authors: 'eager']
}
