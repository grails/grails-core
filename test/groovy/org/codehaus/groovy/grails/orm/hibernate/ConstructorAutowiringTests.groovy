package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*

class ConstructorAutowiringTests extends AbstractGrailsHibernateTests {

	void testDomainClassAutowiring() {
	    def factory = ga.classLoader.loadClass("BookFactory").newInstance()

	    def b = factory.newBook()
	    assert b.bookService
	    assertEquals( "foo", b.talkToService() )

	    b = factory.newBook(title:"gina")
	    assert b.bookService
	    assertEquals "gina", b.title
	    assertEquals "foo", b.talkToService()
	}

	void testAutowiringFromGet() {
        def bookClass = ga.getDomainClass("Book").clazz

        assert bookClass.newInstance(title:"The Stand").save(flush:true)

        assertEquals 1, bookClass.count()
        session.clear()

        def b = bookClass.get(1)
	    assert b.bookService
	    assertEquals "The Stand", b.title
	    assertEquals "foo", b.talkToService()        

    }

	void onSetUp() {
		this.gcl.parseClass('''
class Book {
	Long id
	Long version
	String title

	def bookService
	def talkToService() {
	    bookService.testMethod()
	}
}

class BookService {
    def testMethod() { "foo" }
}
class BookFactory {
    Book newBook() {
        return new Book()
    }
    Book newBook(args) {
        return new Book(args)
    }
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
