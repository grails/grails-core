package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*

class MapMappingTests extends AbstractGrailsHibernateTests {

    void testAssociationMapMapping() {
		def bookClass = ga.getDomainClass("MapBook")
        def authorClass = ga.getDomainClass("MapAuthor")

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



	void onSetUp() {
		this.gcl.parseClass('''
class MapBook {
	Long id
	Long version
	Map authorNameSurname
	Map authors
	static hasMany = [authors:MapAuthor]
}
class MapAuthor {
    Long id
    Long version
    String name
}
class ApplicationDataSource {
	   boolean pooling = true
	   boolean logSql = true
	   String dbCreate = "create-drop" // one of 'create', 'create-drop','update'
	   String url = "jdbc:hsqldb:mem:testDB"
	   String driverClassName = "org.hsqldb.jdbcDriver"
	   String username = "sa"
	   String password = ""  
}
'''
		)
	}
	
	void onTearDown() {
		
	}
}
