package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.orm.hibernate.AbstractGrailsHibernateTests

/**
 * @author Burt Beckwith
 */
class ExistsTests extends AbstractGrailsHibernateTests {

	void testExistsLongPk() {
		def fooClass = ga.getDomainClass('ExistsFoo').clazz
		def foo = fooClass.newInstance()
		foo.name = 'foo 1';
		foo.save()

		assertNotNull foo
		assertEquals 1, fooClass.count()
		long fooId = foo.id
		assertTrue fooClass.exists(fooId)
		assertFalse fooClass.exists(fooId + 1)
	}

	void testExistsCompositePk() {
		def fooClass = ga.getDomainClass('ExistsFoo').clazz
		def foo = fooClass.newInstance()
		foo.name = 'foo 1';
		foo.save()
		assertNotNull foo

		def barClass = ga.getDomainClass('ExistsBar').clazz
		def bar = barClass.newInstance()
		bar.name = 'bar 1';
		bar.save()
		assertNotNull bar

		def foobarClass = ga.getDomainClass('ExistsFooBar').clazz
		def foobar = foobarClass.newInstance()
		foobar.foo = foo
		foobar.bar = bar
		foobar.d = new Date()
		foobar.save()
		assertNotNull foobar
		
		def foobarPk = foobarClass.newInstance()
		foobarPk.foo = foo
		foobarPk.bar = bar

		assertTrue fooClass.exists(foobarPk)

		def bar2 = barClass.newInstance()
		bar2.name = 'bar 2';
		bar2.save()
		assertNotNull bar2
		def foobarPk2 = foobarClass.newInstance()
		foobarPk.foo = foo
		foobarPk.bar = bar2

		assertFalse fooClass.exists(foobarPk2)
	}
	
	protected void onSetUp() {
		gcl.parseClass '''
class ExistsFoo {
	Long id
	Long version
	String name
}

class ExistsBar {
	Long id
	Long version
	String name
}

class ExistsFooBar implements Serializable {
	Long id
	Long version
	ExistsFoo foo
	ExistsBar bar
	Date d
	
	static mapping = {
		id composite: ['foo', 'bar']
	}
}
'''
	}
}
