package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.test.*

class MapDomainTests extends AbstractGrailsMockTests {

	void testMapDomain() {
		def authorClass = ga.getDomainClass("Author")
		def bookClass = ga.getDomainClass("Book")


        def simpleAuthors = bookClass.getPropertyByName("simpleAuthors")

        assert !simpleAuthors.association
        assert !simpleAuthors.oneToMany
        assert simpleAuthors.persistent
        
        def authorsProp = bookClass.getPropertyByName("authors")
        assert simpleAuthors.persistent
        assert authorsProp.oneToMany
        assert authorsProp.bidirectional
        assert authorsProp.association
        assertEquals "book", authorsProp.referencedPropertyName
        assertEquals authorClass, authorsProp.referencedDomainClass
        assertEquals authorClass.clazz, authorsProp.referencedPropertyType

	}

	void onSetUp() {
		this.gcl.parseClass('''
class Book {
	Long id
	Long version
    Map simpleAuthors
	Map authors 
	def hasMany = [authors:Author]
}
class Author {
	Long id
	Long version
	Book book
}
'''
		)
	}
	
	void onTearDown() {
		
	}
}
