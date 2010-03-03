package org.codehaus.groovy.grails.orm.hibernate

import org.codehaus.groovy.grails.web.converters.ConverterUtil

/**
 * Persistence-related tests for ConverterUtil.
 *
 * @author Burt Beckwith
 */
class ConverterUtilPersistenceTests extends AbstractGrailsHibernateTests {

	protected void onSetUp() {
		gcl.parseClass('''
package com.foo.bar

class Flurg {
    Long id
    Long version

    String title
    static belongsTo = [bargle: Bargle]
}

class Bargle {
    Long id
    Long version

    String name
    Set flurgs
    static hasMany = [flurgs: Flurg]
}
''')
	}

	void testGetDomainClass() {

		String bargleClassName = 'com.foo.bar.Bargle'
		String flurgClassName = 'com.foo.bar.Flurg'

		def bargleClass = ga.getDomainClass(bargleClassName).clazz
		def flurgClass = ga.getDomainClass(flurgClassName).clazz

		assertNotNull bargleClass.newInstance(name: 'Stephen King')
			.addToFlurgs(title:'The Shining')
			.addToFlurgs(title:'The Stand')
			.addToFlurgs(title:'Rose Madder')
			.save(flush: true)
		session.clear()

		assertEquals bargleClassName, ConverterUtil.getDomainClass(bargleClassName).fullName
		assertEquals flurgClassName, ConverterUtil.getDomainClass(flurgClassName).fullName

		def bargle = bargleClass.findByName('Stephen King')
		assertNotNull 'should have found bargle', bargle
		assertEquals bargleClassName, ConverterUtil.getDomainClass(bargle.getClass().name).fullName

		def flurg = flurgClass.findByTitle('The Shining')
		assertNotNull 'should have found flurg', flurg
		assertEquals flurgClassName, ConverterUtil.getDomainClass(flurg.getClass().name).fullName		

		for (b in bargle.flurgs) {
			assertEquals flurgClassName, ConverterUtil.getDomainClass(b.getClass().name).fullName		
		}
	}
}
