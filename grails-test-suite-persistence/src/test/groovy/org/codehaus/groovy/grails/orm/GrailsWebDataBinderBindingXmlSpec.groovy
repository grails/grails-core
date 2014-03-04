package org.codehaus.groovy.grails.orm

import grails.persistence.Entity
import grails.test.mixin.Mock
import grails.test.mixin.TestMixin
import grails.test.mixin.domain.DomainClassUnitTestMixin
import grails.validation.Validateable

import org.codehaus.groovy.grails.web.binding.GrailsWebDataBinder

import spock.lang.Issue
import spock.lang.Specification

@TestMixin(DomainClassUnitTestMixin)
@Mock([Writer, Book])
class GrailsWebDataBinderBindingXmlSpec extends Specification {

    @Issue('GRAILS-10868')
    void 'Test binding a single XML child element to a List'() {
        given:
        def binder = new GrailsWebDataBinder(grailsApplication)
        def writer = new Writer(name: 'Writer One')
        
        when:
        def xml = new XmlSlurper().parseText("""
  <writer>
    <name>Writer One</name>
    <books>
        <book><title>Book One</title><publisher>Publisher One</publisher></book>
    </books>
  </writer>
""")
        binder.bind writer, xml
        
        then:
        writer.name == 'Writer One'
        writer.books.size() == 1
        writer.books[0].title == 'Book One'
        writer.books[0].publisher == 'Publisher One'
    }
    
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
    
    @Issue('GRAILS-11175')
    void 'Test binding a single XML child element to a List in a non domain class'() {
        given:
        def binder = new GrailsWebDataBinder(grailsApplication)
        def obj = new CommandObject()
        
        when:
        def xml = new XmlSlurper().parseText("""
  <commandObject>
    <somethings>
        <something><name>One</name></something>
    </somethings>
  </commandObject>
""")
        binder.bind obj, xml
        
        then:
        println obj.errors
        !obj.hasErrors()
        obj.somethings?.size() == 1
        obj.somethings[0].name == 'One'
    }
    

}

@Validateable
class CommandObject {
    List<Something> somethings
}

class Something {
    String name
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
