package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*
import org.hibernate.LazyInitializationException

class ManyToManyLazinessTests extends AbstractGrailsHibernateTests {

	void testManyToManyLazyLoading() {
		def authorClass = ga.getDomainClass("M2MLAuthor")
		def bookClass = ga.getDomainClass("M2MLBook")
		def a = authorClass.newInstance()
		
		a.addToBooks(bookClass.newInstance())
		a.save(true)
		this.session.flush()
		
		this.session.evict(a)

		a = authorClass.clazz.get(1)
		this.session.evict(a)
		assertFalse(this.session.contains(a))
		
		try {
			def books = a.books
			assertEquals 1, books.size()
			fail("Should have thrown lazy load exception")
		}
		catch(LazyInitializationException lie) {
			// expected
		}
		
	}
	

	
	void onSetUp() {
		this.gcl.parseClass('''
import grails.persistence.*

@Entity
class M2MLBook {
	static belongsTo = M2MLAuthor
	static hasMany = [authors:M2MLAuthor]
}

@Entity
class M2MLAuthor {

	static hasMany = [books:M2MLBook]
}
'''
		)
	}
	
	void onTearDown() {
		
	}
}
