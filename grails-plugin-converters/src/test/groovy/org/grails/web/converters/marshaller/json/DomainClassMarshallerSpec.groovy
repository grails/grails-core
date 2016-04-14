package org.grails.web.converters.marshaller.json

import grails.converters.JSON
import grails.converters.XML
import grails.core.DefaultGrailsApplication
import grails.persistence.Entity
import org.grails.web.converters.configuration.ConvertersConfigurationInitializer
import spock.lang.Specification

class DomainClassMarshallerSpec extends Specification {
    void setup() {
        final initializer = new ConvertersConfigurationInitializer()
        def grailsApplication = new DefaultGrailsApplication(Author, Book)
        grailsApplication.initialise()
        initializer.grailsApplication = grailsApplication
        initializer.initialize()
    }

    void "Test DomainClassMarshaller's should maintain order of relations"() {
        def json, xml

        when:
        def book = new Book(
                id: 1, version: 2,
                authorsSet: authors as Set,
                authorsMap: authors.inject([:]) { acc, val ->
                    acc[val.name] = val
                    acc
                }
        )
        JSON.use('deep') {
            json = book as JSON
        }
        XML.use('deep') {
            xml = book as XML
        }

        then:
        json.toString() == expectedJson
        xml.toString() == expectedXml

        where:
        authors                                                      | expectedJson                                                                                                                     | expectedXml
        [new Author(id: 1, name: 'a'), new Author(id: 2, name: 'b')] | '{"id":1,"authorsMap":{"a":{"id":1,"name":"a"},"b":{"id":2,"name":"b"}},"authorsSet":[{"id":1,"name":"a"},{"id":2,"name":"b"}]}' | '<?xml version="1.0" encoding="UTF-8"?><book id="1"><authorsMap><entry key="a" id="1"><name>a</name></entry><entry key="b" id="2"><name>b</name></entry></authorsMap><authorsSet><author id="1"><name>a</name></author><author id="2"><name>b</name></author></authorsSet></book>'
        [new Author(id: 2, name: 'b'), new Author(id: 1, name: 'a')] | '{"id":1,"authorsMap":{"b":{"id":2,"name":"b"},"a":{"id":1,"name":"a"}},"authorsSet":[{"id":2,"name":"b"},{"id":1,"name":"a"}]}' | '<?xml version="1.0" encoding="UTF-8"?><book id="1"><authorsMap><entry key="b" id="2"><name>b</name></entry><entry key="a" id="1"><name>a</name></entry></authorsMap><authorsSet><author id="2"><name>b</name></author><author id="1"><name>a</name></author></authorsSet></book>'
    }
}

@Entity
class Author {
    Long id
    Long version
    String name
}

@Entity
class Book {
    static hasMany = [authorsSet: Author, authorsMap: Author]
    Long id
    Long version
    Set authorsSet
    Map authorsMap
}