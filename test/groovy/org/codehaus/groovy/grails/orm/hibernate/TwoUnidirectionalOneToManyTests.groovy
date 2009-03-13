package org.codehaus.groovy.grails.orm.hibernate;

import org.codehaus.groovy.grails.commons.*

class TwoUnidirectionalOneToManyTests extends AbstractGrailsHibernateTests {

	void testTwoUniOneToManys() {
		def mailing = ga.getDomainClass("Mailing").newInstance()
		def recipient = ga.getDomainClass("Recipient").newInstance()
		def doc1 = ga.getDomainClass("Document").newInstance()
		def doc2 = ga.getDomainClass("Document").newInstance()
		
		doc1.filename = "file1.txt"
		doc2.filename = "file2.txt"
		
		mailing.addToDocuments(doc1)
		mailing.save(true)
		
		recipient.addToDocuments(doc2)
		recipient.save(true)
	}
	

	
	void onSetUp() {
		this.gcl.parseClass('''
class Mailing {
	Long id
	Long version
	Set documents
	static hasMany = [documents:Document]
}
class Recipient {
	Long id
	Long version
	Set documents
	static hasMany = [documents:Document]
}
class Document {
	Long id
	Long version
	String filename
}
'''
		)
	}
	
	void onTearDown() {
		
	}
}
