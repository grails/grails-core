package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.test.*

class MapDomainTests extends AbstractGrailsMockTests {

    void testMapDomain() {
        def authorClass = ga.getDomainClass("Author")
        def bookClass = ga.getDomainClass("Book")

        def simpleAuthors = bookClass.getPropertyByName("simpleAuthors")

        assertFalse simpleAuthors.association
        assertFalse simpleAuthors.oneToMany
        assertTrue simpleAuthors.persistent

        def authorsProp = bookClass.getPropertyByName("authors")
        assertTrue simpleAuthors.persistent
        assertTrue authorsProp.oneToMany
        assertTrue authorsProp.bidirectional
        assertTrue authorsProp.association
        assertEquals "book", authorsProp.referencedPropertyName
        assertEquals authorClass, authorsProp.referencedDomainClass
        assertEquals authorClass.clazz, authorsProp.referencedPropertyType
    }

    void onSetUp() {
        gcl.parseClass '''
class Book {
    Long id
    Long version
    Map simpleAuthors
    Map authors
    static hasMany = [authors:Author]
}

class Author {
    Long id
    Long version
    Book book
}
'''
    }
}
