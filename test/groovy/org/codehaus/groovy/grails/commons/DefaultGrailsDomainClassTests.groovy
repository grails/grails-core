package org.codehaus.groovy.grails.commons;

/**
 * Note there are more tests for DefaultGrailsDomainClass in test/persistence written in Java
 */
class DefaultGrailsDomainClassTests extends GroovyTestCase {

	def gcl 
	
	void setUp() {
		gcl = new GroovyClassLoader()
	}
	 
	void testFetchMode() {
		gcl.parseClass(
				"""
				class Test {
					Long id
					Long version
					Set others
					def hasMany = [others:Other]
					def fetchMode = [others:'eager']
				}
				class Other {
					Long id
					Long version
					Set anothers
					def hasMany = [anothers:Another]
				}
				class Another {
					Long id
					Long version
				}
				"""
						)
		
		def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
		
		def testDomain = ga.getGrailsDomainClass("Test")
		assertEquals(GrailsDomainClassProperty.FETCH_EAGER, testDomain.getPropertyByName('others').getFetchMode())
		
		def otherDomain = ga.getGrailsDomainClass("Other")
		assertEquals(GrailsDomainClassProperty.FETCH_LAZY, otherDomain.getPropertyByName('anothers').getFetchMode())
	}
	
	void testManyToManyIntegrity() {
		gcl.parseClass(
				"""
				class Test {
					Long id
					Long version
					Set others
					def hasMany = [others:Other]
				}
				class Other {
					Long id
					Long version
					Set tests
					def belongsTo = Test
					def hasMany = [tests:Test]
				}
			"""
			
	    )						
		
		def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
		
		def testDomain = ga.getGrailsDomainClass("Test")
		def otherDomain = ga.getGrailsDomainClass("Other")
		
		def others = testDomain?.getPropertyByName("others")
		def tests = otherDomain?.getPropertyByName("tests")
				
		assert others?.isManyToMany()
		assert tests?.isManyToMany()
		assert !others?.isOneToMany()
		assert !tests?.isOneToMany()
				
				
		assertEquals(others, tests.otherSide)
		assertEquals(tests, others.otherSide)
		assertTrue(others.owningSide)
		assertFalse(tests.owningSide)
	}
	
}
