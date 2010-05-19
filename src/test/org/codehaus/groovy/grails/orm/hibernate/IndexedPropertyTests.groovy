package org.codehaus.groovy.grails.orm.hibernate

/**
 * @author Burt Beckwith
 */
class IndexedPropertyTests extends AbstractGrailsHibernateTests {

	protected void onSetUp() {
		gcl.parseClass '''
class Eyjafjallajokul {
	Long id
	Long version
	String name
	int getFoo(int bar) { 0 }
}
'''
	}

	// test for GRAILS-5999
	void testIndexedProperty() {
		def clazz = ga.getDomainClass('Eyjafjallajokul').clazz

		assertNotNull clazz.newInstance(name: 'volcano').save(flush: true)
		session.clear()

		assertEquals 1, clazz.count()
	}
}

