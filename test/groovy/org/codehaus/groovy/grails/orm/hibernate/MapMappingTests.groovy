package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*

class MapMappingTests extends AbstractGrailsHibernateTests {

	void testBasicMapMapping() {
		def bookClass = ga.getDomainClass("Book")

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
class Book {
	Long id
	Long version
	Map authorNameSurname 
}
/*class Book2 {
	Long id
	Long version
	Map authors

	static hasMany = [authors:Author]
}
class Author {
    Long id
    Long version
}*/
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
