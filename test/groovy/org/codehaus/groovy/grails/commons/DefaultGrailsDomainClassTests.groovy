package org.codehaus.groovy.grails.commons

import org.codehaus.groovy.grails.exceptions.InvalidPropertyException;

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
                    static mappedBy = [routes:"airport"]
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


	public void testOneToOneRelationships() {

		this.gcl.parseClass('''
class RelationshipsTest1 {
    Long id;
    Long version;
    OneToOneTest1 one; // uni-directional one-to-one
}

class OneToOneTest1 {

    Long id;
    Long version;

    RelationshipsTest1 other
}

				'''	)
		def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
		ga.initialise()
		def c1dc = ga.getDomainClass("RelationshipsTest1")
		def c2dc = ga.getDomainClass("OneToOneTest1")


		// test relationships
		assertTrue( c1dc.getPropertyByName( "one" ).isPersistent() );
		assertTrue( c1dc.getPropertyByName( "one" ).isOneToOne() );
		assertFalse( c1dc.getPropertyByName( "one" ).isManyToMany() );
		assertFalse( c1dc.getPropertyByName( "one" ).isManyToOne() );
		assertFalse( c1dc.getPropertyByName( "one" ).isOneToMany() );

		assertTrue( c2dc.getPropertyByName( "other" ).isPersistent() );
		assertTrue( c2dc.getPropertyByName( "other" ).isOneToOne() );
		assertFalse( c2dc.getPropertyByName( "other" ).isManyToMany() );
		assertFalse( c2dc.getPropertyByName( "other" ).isManyToOne() );
		assertFalse( c2dc.getPropertyByName( "other" ).isOneToMany() );
	}


	public void testCircularOneToManyRelationship() throws Exception {
		GroovyClassLoader gcl = new GroovyClassLoader();
		Class a = gcl.parseClass("class A { \n" +
									" Long id\n" +
									" Long version\n" +
									" def hasMany = [ children : A]\n" +
									" A parent\n" +
									" Set children\n" +
									"}");
		GrailsDomainClass dc = new DefaultGrailsDomainClass(a);
		GrailsDomainClass[] dcs = new GrailsDomainClass[1];
		dcs[0] =dc;
		Map domainMap = new HashMap();
		domainMap.put(dc.getFullName(),dc);
		GrailsDomainConfigurationUtil.configureDomainClassRelationships(dcs,domainMap);

		assertTrue(dc.getPropertyByName("children").isAssociation());
		assertTrue(dc.getPropertyByName("children").isOneToMany());
		assertTrue(dc.getPropertyByName("parent").isAssociation());
		assertTrue(dc.getPropertyByName("parent").isManyToOne());
		assertTrue(dc.getPropertyByName("children").getOtherSide().equals(dc.getPropertyByName("parent")));
		assertTrue(dc.getPropertyByName("parent").getOtherSide().equals(dc.getPropertyByName("children")));

	}


	public void testOneToManyRelationships(){


        this.gcl.parseClass('''
class RelationshipsTest2 {
   def hasMany = [ 	"ones" : OneToManyTest2.class,
  								"manys" : ManyToManyTest2.class,
  								"uniones" : UniOneToManyTest2.class ];

    Long id;
    Long version;

    Set manys; // many-to-many relationship
    OneToOneTest2 one; // uni-directional one-to-one
    Set ones; // bi-directional one-to-many relationship
    Set uniones; // uni-directional one-to-many relationship
}
class OneToManyTest2 {

    Long id;
    Long version;
    RelationshipsTest2 other; // many-to-one relationship

}
class UniOneToManyTest2 {

    Long id;
    Long version;
}
class ManyToManyTest2 {
//    Map relationships = [ "manys" : RelationshipsTest2.class ];
    Long id;
    Long version;
   // Set manys;
   // many-to-many relationship
}
class OneToOneTest2 {

    Long id;
    Long version;

    RelationshipsTest2 other
}

                '''	)
        def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        ga.initialise()


 		def c1dc = ga.getDomainClass("RelationshipsTest2")
		def c2dc = ga.getDomainClass("OneToManyTest2")

		// test relationship property
		assertEquals( c1dc.getPropertyByName("ones").getOtherSide(), c2dc.getPropertyByName("other") );
		assertTrue( c1dc.getPropertyByName( "ones" ).isOneToMany() );
		assertTrue( c1dc.getPropertyByName( "ones" ).isPersistent() );
		assertFalse( c1dc.getPropertyByName( "ones" ).isManyToMany() );
		assertFalse( c1dc.getPropertyByName( "ones" ).isManyToOne() );
		assertFalse( c1dc.getPropertyByName( "ones" ).isOneToOne() );

		assertEquals( c2dc.getPropertyByName("other").getOtherSide(), c1dc.getPropertyByName("ones") );
		assertTrue( c2dc.getPropertyByName( "other" ).isPersistent() );
		assertTrue( c2dc.getPropertyByName( "other" ).isManyToOne() );
		assertFalse( c2dc.getPropertyByName( "other" ).isManyToMany() );
		assertFalse( c2dc.getPropertyByName( "other" ).isOneToOne() );
		assertFalse( c2dc.getPropertyByName( "other" ).isOneToMany() );
	}


    public void testPersistentPropertyInheritance() {
        Class topClass = gcl.parseClass("class Top {\n" +
                "int id\n" +
                "int version\n" +
                "String topString\n" +
                "}");
        Class middleClass = gcl.parseClass("class Middle extends Top {\n" +
                "String middleString\n" +
        "}");
        Class bottomClass = gcl.parseClass("class Bottom extends Middle {\n" +
                "String bottomString\n" +
        "}");

        DefaultGrailsDomainClass topDomainClass = new DefaultGrailsDomainClass(topClass);
        DefaultGrailsDomainClass middleDomainClass = new DefaultGrailsDomainClass(middleClass);
        DefaultGrailsDomainClass bottomDomainClass = new DefaultGrailsDomainClass(bottomClass);

        assertEquals("bottom class had wrong number of persistent properties", 3, bottomDomainClass.getPersistentProperties().length);
        assertEquals("middle class had wrong number of persistent properties", 2, middleDomainClass.getPersistentProperties().length);
        assertEquals("top class had wrong number of persistent properties", 1, topDomainClass.getPersistentProperties().length);

        GrailsDomainClassProperty topStringProperty = topDomainClass.getPropertyByName("topString");
        assertNotNull("topString property not found in topDomainClass", topStringProperty);
        assertTrue("topString property was not persistent in topDomainClass", topStringProperty.isPersistent());

        topStringProperty = middleDomainClass.getPropertyByName("topString");
        assertNotNull("topString property not found in middleDomainClass", topStringProperty);
        assertTrue("topString property was not persistent in middleDomainClass", topStringProperty.isPersistent());

        GrailsDomainClassProperty middleStringProperty = middleDomainClass.getPropertyByName("middleString");
        assertNotNull("middleString property not found in middleDomainClass", middleStringProperty);
        assertTrue("middleString property was not persistent in middleDomainClass", middleStringProperty.isPersistent());

        topStringProperty = bottomDomainClass.getPropertyByName("topString");
        assertNotNull("topString property not found in bottomDomainClass", topStringProperty);
        assertTrue("topString property was not persistent in bottomDomainClass", topStringProperty.isPersistent());

        middleStringProperty = bottomDomainClass.getPropertyByName("middleString");
        assertNotNull("middleString property not found in bottomDomainClass", middleStringProperty);
        assertTrue("middleString property was not persistent in bottomDomainClass", middleStringProperty.isPersistent());

        GrailsDomainClassProperty bottomStringProperty = bottomDomainClass.getPropertyByName("bottomString");
        assertNotNull("bottomString property not found in bottomDomainClass", bottomStringProperty);
        assertTrue("bottomString property was not persistent in bottomDomainClass", bottomStringProperty.isPersistent());
    }

	public void testDefaultGrailsDomainClass() throws Exception {

		Class clazz = gcl.parseClass("class UserTest { " +
                " int id; " +
                " int version; " +
                " List transients = [ \"age\" ]; " +
                " String firstName; " +
                " String lastName; " +
                " java.util.Date age; " +
                "}");

		GrailsDomainClass domainClass = new DefaultGrailsDomainClass(clazz);

		assertEquals("UserTest",domainClass.getName());

		assertNotNull(domainClass.getIdentifier());
		assertNotNull(domainClass.getVersion());
		assertTrue(domainClass.getIdentifier().isIdentity());

		try {
			domainClass.getPropertyByName("rubbish");
			fail("should throw exception");
		}
		catch(InvalidPropertyException ipe) {
			// expected
		}

		GrailsDomainClassProperty age = domainClass.getPropertyByName( "age" );
		assertNotNull(age);
		assertFalse(age.isPersistent());

		GrailsDomainClassProperty lastName = domainClass.getPropertyByName( "lastName" );
		assertNotNull(lastName);
		assertFalse(lastName.isOptional());

		GrailsDomainClassProperty firstName = domainClass.getPropertyByName( "firstName" );
		assertNotNull(firstName);
		assertFalse(firstName.isOptional());
		assertTrue(firstName.isPersistent());


		GrailsDomainClassProperty[] persistantProperties = domainClass.getPersistentProperties();
		for(int i = 0; i < persistantProperties.length;i++) {
			assertTrue(persistantProperties[i].isPersistent());
		}
	}

    public void testManyToManyInSubclass() throws Exception {
        this.gcl.parseClass('''
class Bookmark {
    Long id;
    Long version;
    Set tags

    static hasMany = ["tags" : Tag]
    static belongsTo = [Tag]
}

class BookmarkSubclass extends Bookmark {
    Long id;
    Long version;
}

class Tag {
    Long id;
    Long version;
    Set bookmarks

    static hasMany = ["bookmarks": Bookmark]
}
''')
        def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        ga.initialise()


        def bookmarkClass = ga.getDomainClass("Bookmark")
        def bookmarkSubclassClass = ga.getDomainClass("BookmarkSubclass")
        def tagClass = ga.getDomainClass("Tag")

        GrailsDomainClassProperty tagBookmarks = tagClass.getPropertyByName("bookmarks")
        GrailsDomainClassProperty bookmarkTags = bookmarkClass.getPropertyByName("tags")
        GrailsDomainClassProperty bookmarkSubclassTags = bookmarkSubclassClass.getPropertyByName("tags")

        assertNotNull "Property 'bookmarks' should exist in Tag class", tagBookmarks
        assertNotNull "Property 'tags' should exist in Bookmark class", bookmarkTags
        assertNotNull "Property 'tags' should exist in BookmarkSubclass class", bookmarkSubclassTags

        assertEquals "Property 'bookmarks' of class Tag should relate to property 'tags' of class Bookmark", tagBookmarks.otherSide, bookmarkTags
        assertEquals "Property 'tags' of class Bookmark should relate to property 'bookmarks' of class Tag", bookmarkTags.otherSide, tagBookmarks
        assertEquals "Property 'tags' of class BookmarkSubclass should relate to property 'bookmarks' of class Tag",  bookmarkSubclassTags.otherSide, tagBookmarks

        assertEquals "Inherited property 'tags' in class Bookmark should be the same as parent property in class Bookmark", bookmarkSubclassTags, bookmarkTags

        assertTrue "Property 'bookmarks' of class Tag should have type many-to-many", tagBookmarks.isManyToMany()
        assertTrue "Property 'tags' of class Bookmark should have type many-to-many", bookmarkTags.isManyToMany()
        assertTrue "Property 'tags' of class BookmarkSubclass should have type many-to-many", bookmarkSubclassTags.isManyToMany()
    }
}
