package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.test.*

class ListDomainTests extends AbstractGrailsMockTests {

    void testListDomain() {
        def authorClass = ga.getDomainClass("Author")
        def bookClass = ga.getDomainClass("Book")

        def authorsProp = bookClass.getPropertyByName("authors")
        assertTrue authorsProp.oneToMany
        assertTrue authorsProp.bidirectional
        assertTrue authorsProp.association
        assertEquals "book", authorsProp.referencedPropertyName

        def otherSide = authorsProp.otherSide
        assertNotNull otherSide
        assertEquals "book", otherSide.name
    }

    void onSetUp() {
        gcl.parseClass '''
class Book {
    Long id
    Long version
    List authors
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
