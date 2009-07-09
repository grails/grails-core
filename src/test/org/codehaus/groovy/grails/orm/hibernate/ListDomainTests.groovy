package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*
import org.codehaus.groovy.grails.commons.test.*

class ListDomainTests extends AbstractGrailsMockTests {

	void testListDomain() {
		def authorClass = ga.getDomainClass("Author")
		def bookClass = ga.getDomainClass("Book")


        def authorsProp = bookClass.getPropertyByName("authors")
        assert authorsProp.oneToMany
        assert authorsProp.bidirectional
        assert authorsProp.association
        assertEquals "book", authorsProp.referencedPropertyName

        def otherSide = authorsProp.otherSide
        assert otherSide
        assertEquals "book", otherSide.name
        
	}

	void onSetUp() {
		this.gcl.parseClass('''
class Book {
	Long id
	Long version
	List authors 
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
