package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*

class MapMappingTests extends AbstractGrailsHibernateTests {

    void testAssociationMapMapping() {
		def bookClass = ga.getDomainClass("MapBook")
        def authorClass = ga.getDomainClass("MapPerson")

        def a1 = authorClass.newInstance()
        a1.name = "Stephen King"
        def a2 = authorClass.newInstance()
        a2.name = "James Patterson"
        def a3 = authorClass.newInstance()
        a3.name = "Joe Bloggs"

        def map = [Stephen:a1,
                   James:a2,
                   Joe:a3]

        def book = bookClass.newInstance()

        book.authors = map
        book.authorNameSurname = [:]
        book.save()


        assert !book.hasErrors()
        
        session.flush()

        assert book.id
        
        session.clear()

        book = null

        book = bookClass.clazz.get(1)

        assert book
        assertEquals 3, book.authors.size()
        assertEquals "Stephen King", book.authors.Stephen.name

    }

	void testBasicMapMapping() {
		def bookClass = ga.getDomainClass("MapBook")

        def map = ["Stephen":"King",
                   "James": "Patterson",
                   "Joe": "Bloggs"]

        def book = bookClass.newInstance()

        book.authorNameSurname = map
        book.save()
        session.flush()
        session.clear()    

        book = null

        book = bookClass.clazz.get(1)

        assertEquals 3, book.authorNameSurname.size()
        assertEquals "King", book.authorNameSurname.Stephen
        assertEquals "Patterson", book.authorNameSurname.James
        assertEquals "Bloggs", book.authorNameSurname.Joe
    }   


    void testTypeMappings() {
		def bookClass = ga.getDomainClass("MapBook")

        def map = [(1):30,
                   (2): 42,
                   (3): 23]

        def book = bookClass.newInstance()

        book.chapterPageCounts = map
        book.save(flush:true)

        session.clear()

        
        book = bookClass.clazz.get(1)

        assertEquals 3, book.chapterPageCounts.size()
        assertEquals 30, book.chapterPageCounts.get(1)

        def c = session.connection()

        def ps = c.prepareStatement("select * from  map_book_chapter_page_counts")

        def rs = ps.executeQuery()

        assert rs.next()

        assert rs.getInt("chapter_number")
        assert rs.getInt("page_count")
        assert rs.getLong("map_book_id")
    }

    void onSetUp() {
		this.gcl.parseClass('''
class MapBook {
	Long id
	Long version
	Map authorNameSurname

	Map authors
    Map chapterPageCounts

	static hasMany = [authors:MapPerson, chapterPageCounts:Integer]

    static mapping = {
        chapterPageCounts indexColumn:[name:"chapter_number", type:Integer],
                          joinTable:[column:"page_count"]

        authorNameSurname indexColumn:[length:50], length:100
    }
}
class MapPerson {
    Long id
    Long version
    String name
}
'''
		)
	}
	
	void onTearDown() {
		
	}
}
