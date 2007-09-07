package org.codehaus.groovy.grails.commons;

/**
 * Note there are more tests for DefaultGrailsDomainClass in test/persistence written in Java
 */
class DefaultGrailsDomainClassTests extends GroovyTestCase {

	def gcl 
	
	void setUp() {
		gcl = new GroovyClassLoader()
	}

    void tearDown() {
        gcl = null
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
		ga.initialise()
		
		def testDomain = ga.getDomainClass("Test")
		assertEquals(GrailsDomainClassProperty.FETCH_EAGER, testDomain.getPropertyByName('others').getFetchMode())
		
		def otherDomain = ga.getDomainClass("Other")
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
		ga.initialise()
		def testDomain = ga.getDomainClass("Test")
		def otherDomain = ga.getDomainClass("Other")
		
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
	
	
	void testTwoManyToOneIntegrity() {
		this.gcl.parseClass('''
				class Airport {
					Long id
					Long version
					Set routes

					static hasMany = [routes:Route]
				}
				class Route {
					Long id
					Long version
					
					Airport airport
					Airport destination
				}
				'''	)
		def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
		ga.initialise()		
		def airportClass = ga.getDomainClass("Airport")
		def routeClass = ga.getDomainClass("Route")
		
		def routes = airportClass.getPropertyByName("routes")
		
		assertTrue routes.bidirectional
		assertTrue routes.oneToMany
		assertNotNull routes.otherSide
		assertEquals "airport", routes.otherSide.name
		
		def airport = routeClass.getPropertyByName("airport")
		
		assertTrue airport.bidirectional
		assertTrue airport.manyToOne
		assertNotNull airport.otherSide
		assertEquals "routes", airport.otherSide.name 
		
		def destination = routeClass.getPropertyByName("destination")
		
		assertFalse destination.bidirectional
		assertTrue destination.oneToOne
	}

	void testTableName() {
        this.gcl.parseClass('''
				class Person {
				    Long id
				    Long version
				    String firstName
				    String lastName

				    static mapping = {
				        table 'people'
				    }
				}
				'''	)
		def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
		ga.initialise()
		def personClass = ga.getDomainClass('Person')
		assertNotNull 'person class was null', personClass
		assertNotNull 'ormMapping was null', personClass.ormMapping
		assertEquals 'person was mapped to the wrong table', 'people', personClass.ormMapping.tableName
    }
	void testColumnName() {
        this.gcl.parseClass('''
				class Person {
				    Long id
				    Long version
				    String firstName
				    String lastName

				    static mapping = {
				        firstName(column:'fname')
				    }
				}
				'''	)
		def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
		ga.initialise()
		def personClass = ga.getDomainClass('Person')
		assertNotNull 'person class was null', personClass
		def ormMapping = personClass.ormMapping
		assertNotNull 'ormMapping was null', ormMapping

		def firstNameProperty = personClass.getPropertyByName('firstName')
		assertNotNull ' firstName property not found', firstNameProperty
		assertEquals 'firstName property had wrong column name', 'fname', ormMapping.getColumnName(firstNameProperty)

		def lastNameProperty = personClass.getPropertyByName('lastName')
		assertNotNull ' lastName property not found', lastNameProperty
		assertNull 'lastName property had wrong column name', ormMapping.getColumnName(lastNameProperty)
    }
}
