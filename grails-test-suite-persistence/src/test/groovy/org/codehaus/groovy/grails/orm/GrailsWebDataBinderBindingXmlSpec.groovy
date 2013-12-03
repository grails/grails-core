package org.codehaus.groovy.grails.orm

import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin

import org.codehaus.groovy.grails.web.binding.GrailsWebDataBinder

import spock.lang.Issue
import spock.lang.Specification

@TestMixin(DomainClassUnitTestMixin)
@Mock([Writer, Book])
class GrailsWebDataBinderBindingXmlSpec extends Specification {

    @Issue('GRAILS-10868')
    void 'Test adding an existing element to a List by id'() {
        given:
        def binder = new GrailsWebDataBinder(grailsApplication)
        def writer = new Writer(name: 'Writer One')
        def originalBook = new Book(title: 'Book One', publisher: 'Publisher One')
        
        when:
        originalBook = originalBook.save()
        
        then:
        originalBook
        
        when:
        def xml = new XmlSlurper().parseText("""
  <writer>
    <name>Writer Two</name>
    <books>
        <book><id>${originalBook.id}</id><title>Updated Book One</title></book>
        <book><id>2</id><title>Book Two</title><publisher>Publisher Two</publisher></book>
    </books>
  </writer>
""")
        binder.bind writer, xml
        
        then:
        writer.name == 'Writer Two'
        writer.books.size() == 2
        writer.books[0].title == 'Updated Book One'
        writer.books[0].publisher == 'Publisher One'
        writer.books[1].publisher == 'Publisher Two'
        writer.books[1].title == 'Book Two'
    }
}

@Entity
class Writer {
    String name
    List books
    static hasMany = [books: Book]
}

@Entity
class Book {
    String publisher
    String title
}
