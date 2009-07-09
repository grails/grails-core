package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*
import org.hibernate.LazyInitializationException

class URLMappingTests extends AbstractGrailsHibernateTests {

	void testURLMapping() {
		def b = ga.getDomainClass("testUniListMappingBookmark").newInstance()
		
		b.url = new URL("http://grails.org")
		b.publisherSite = new URI("http://apress.com")
		b.title = "TDGTG"
		b.notes = "some notes"
		
		b.save(true)
		
		b.discard()
		b=null
		
		b = ga.getDomainClass("testUniListMappingBookmark").clazz.get(1)
		
		
		assertEquals "http://grails.org", b.url.toString()
	}
	

	
	void onSetUp() {
		this.gcl.parseClass('''
class testUniListMappingBookmark {
	Long id
	Long version
	URL url 
	URI publisherSite
	String title 
	String notes 
	Date dateCreated = new Date() 
}
'''
		)
	}
	
	void onTearDown() {
		
	}
}
