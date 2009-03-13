package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*

class ListMappingTests extends AbstractGrailsHibernateTests {

    void testAddPersistentPogoToList() {
		def authorClass = ga.getDomainClass("ListMappingAuthor")
		def bookClass = ga.getDomainClass("ListMappingBook")
        def b = bookClass.newInstance()
        def a = authorClass.newInstance()

        a.name = "Stephen King"
        a.save()

        b.addToAuthors(a)
        b.save()
        session.flush()
        session.clear()

        b = bookClass.clazz.get(1)

        assertEquals "Stephen King",b.authors[0].name
    }

	void testListMapping() {
		def authorClass = ga.getDomainClass("ListMappingAuthor")
		def bookClass = ga.getDomainClass("ListMappingBook")
		def a1 = authorClass.newInstance()
		def a2 = authorClass.newInstance()
		def a3 = authorClass.newInstance()

        a1.name = "Stephen King"
        a2.name = "James Patterson"
        a3.name = "Joe Bloggs"

        def book = bookClass.newInstance()

        book.addToAuthors(a1)
            .addToAuthors(a2)
            .addToAuthors(a3)
            .save(true)

        session.flush()
        println "Flushed session"
        session.clear()    

        def ids = [a1.id, a2.id, a2.id]

        book = null

        book = bookClass.clazz.get(1)

        assertEquals 3, book.authors.size()

        assertEquals a1.id, book.authors[0].id
        assertEquals a2.id, book.authors[1].id
        assertEquals a3.id, book.authors[2].id
	}

	void onSetUp() {
		this.gcl.parseClass('''
import grails.persistence.*

@Entity
class ListMappingBook {
	List authors
	def hasMany = [authors:ListMappingAuthor]

}

@Entity
class ListMappingAuthor {
	String name
	ListMappingBook book
}
'''
		)
	}
	
	void onTearDown() {
		
	}
}
