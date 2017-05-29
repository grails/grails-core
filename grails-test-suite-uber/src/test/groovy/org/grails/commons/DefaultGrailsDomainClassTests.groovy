package org.grails.commons

import grails.core.DefaultGrailsApplication
import grails.core.GrailsDomainClass
import grails.core.GrailsDomainClassProperty
import org.grails.core.DefaultGrailsDomainClass
import org.grails.core.exceptions.InvalidPropertyException
import org.grails.core.support.GrailsDomainConfigurationUtil

/**
 * Note there are more tests for DefaultGrailsDomainClass in test/persistence written in Java
 */
class DefaultGrailsDomainClassTests extends GroovyTestCase {

    def gcl = new GroovyClassLoader()

    void testFetchMode() {
        gcl.parseClass """
                @grails.persistence.Entity
                class Test {
                    Long id
                    Long version
                    Set others
                    def hasMany = [others:Other]
                    def fetchMode = [others:'eager']
                }
                @grails.persistence.Entity
                class Other {
                    Long id
                    Long version
                    Set anothers
                    def hasMany = [anothers:Another]
                }
                @grails.persistence.Entity
                class Another {
                    Long id
                    Long version
                }
                """

        def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        ga.initialise()

        def testDomain = ga.getDomainClass("Test")
        assertEquals(GrailsDomainClassProperty.FETCH_EAGER, testDomain.getPropertyByName('others').getFetchMode())

        def otherDomain = ga.getDomainClass("Other")
        assertEquals(GrailsDomainClassProperty.FETCH_LAZY, otherDomain.getPropertyByName('anothers').getFetchMode())
    }

    void testPersistentProperties() {
        def cls = gcl.parseClass('''
@grails.persistence.Entity
class Book {
    Long id
    Long version
    String title
    String author

    static constraints = {
        title blank: false
        author blank: false
    }

    String getAuteur() {
        return author
    }
}
''')

        def dc = new DefaultGrailsDomainClass(cls)

        assert dc.persistentProperties.size() == 2
        assert dc.properties.size() == 5
        assert dc.properties.find { it.name == 'auteur'} != null
    }

    void testListPersistentProperties() {
        def cls = gcl.parseClass('''
@grails.persistence.Entity
class Book {
    Long id
    Long version
    String title
    List<String> authors
}
''')

        def dc = new DefaultGrailsDomainClass(cls)

        assert dc.persistentProperties.size() == 2
        assert dc.properties.size() == 4
    }

    void testManyToManyIntegrity() {
        gcl.parseClass """
                @grails.persistence.Entity
                class Test {
                    Long id
                    Long version
                    Set others
                    static hasMany = [others:Other]
                }
                @grails.persistence.Entity
                class Other {
                    Long id
                    Long version
                    Set tests
                    static belongsTo = Test
                    static hasMany = [tests:Test]
                }
            """

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
        gcl.parseClass '''
            @grails.persistence.Entity
            class Airport {
                Long id
                Long version
                Set routes

                static hasMany = [routes:Route]
                static mappedBy = [routes:"airport"]
            }
            @grails.persistence.Entity
            class Route {
                Long id
                Long version

                Airport airport
                Airport destination
            }
        '''
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

    void testOneToOneRelationships() {

        gcl.parseClass '''
@grails.persistence.Entity
class RelationshipsTest1 {
    Long id
    Long version
    OneToOneTest1 one // uni-directional one-to-one
}

@grails.persistence.Entity
class OneToOneTest1 {

    Long id
    Long version

    RelationshipsTest1 other
}
'''

        def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        ga.initialise()
        def c1dc = ga.getDomainClass("RelationshipsTest1")
        def c2dc = ga.getDomainClass("OneToOneTest1")

        // test relationships
        assertTrue(c1dc.getPropertyByName("one").isPersistent())
        assertTrue(c1dc.getPropertyByName("one").isOneToOne())
        assertFalse(c1dc.getPropertyByName("one").isManyToMany())
        assertFalse(c1dc.getPropertyByName("one").isManyToOne())
        assertFalse(c1dc.getPropertyByName("one").isOneToMany())

        assertTrue(c2dc.getPropertyByName("other").isPersistent())
        assertTrue(c2dc.getPropertyByName("other").isOneToOne())
        assertFalse(c2dc.getPropertyByName("other").isManyToMany())
        assertFalse(c2dc.getPropertyByName("other").isManyToOne())
        assertFalse(c2dc.getPropertyByName("other").isOneToMany())
    }

    void testCircularOneToManyRelationship() throws Exception {
        GroovyClassLoader gcl = new GroovyClassLoader()
        Class a = gcl.parseClass("@grails.persistence.Entity\nclass A { \n" +
                                    " Long id\n" +
                                    " Long version\n" +
                                    " static hasMany = [ children : A]\n" +
                                    " A parent\n" +
                                    " Set children\n" +
                                    "}")
        GrailsDomainClass dc = new DefaultGrailsDomainClass(a)
        GrailsDomainClass[] dcs = new GrailsDomainClass[1]
        dcs[0] =dc
        Map domainMap = new HashMap()
        domainMap.put(dc.getFullName(),dc)
        GrailsDomainConfigurationUtil.configureDomainClassRelationships(dcs,domainMap)

        assertTrue(dc.getPropertyByName("children").isAssociation())
        assertTrue(dc.getPropertyByName("children").isOneToMany())
        assertTrue(dc.getPropertyByName("parent").isAssociation())
        assertTrue(dc.getPropertyByName("parent").isManyToOne())
        assertTrue(dc.getPropertyByName("children").getOtherSide().equals(dc.getPropertyByName("parent")))
        assertTrue(dc.getPropertyByName("parent").getOtherSide().equals(dc.getPropertyByName("children")))
    }

    void testOneToManyRelationships() {

        gcl.parseClass '''
@grails.persistence.Entity
class RelationshipsTest2 {
   static hasMany = [     "ones" : OneToManyTest2.class,
                                  "manys" : ManyToManyTest2.class,
                                  "uniones" : UniOneToManyTest2.class ]

    Long id
    Long version

    Set manys // many-to-many relationship
    OneToOneTest2 one // uni-directional one-to-one
    Set ones // bi-directional one-to-many relationship
    Set uniones // uni-directional one-to-many relationship
}
@grails.persistence.Entity
class OneToManyTest2 {
    Long id
    Long version
    RelationshipsTest2 other // many-to-one relationship
}
@grails.persistence.Entity
class UniOneToManyTest2 {
    Long id
    Long version
}
@grails.persistence.Entity
class ManyToManyTest2 {
    Long id
    Long version
}
@grails.persistence.Entity
class OneToOneTest2 {
    Long id
    Long version

    RelationshipsTest2 other
}'''
        def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        ga.initialise()

        def c1dc = ga.getDomainClass("RelationshipsTest2")
        def c2dc = ga.getDomainClass("OneToManyTest2")

        // test relationship property
        assertEquals(c1dc.getPropertyByName("ones").getOtherSide(), c2dc.getPropertyByName("other"))
        assertTrue(c1dc.getPropertyByName("ones").isOneToMany())
        assertTrue(c1dc.getPropertyByName("ones").isPersistent())
        assertFalse(c1dc.getPropertyByName("ones").isManyToMany())
        assertFalse(c1dc.getPropertyByName("ones").isManyToOne())
        assertFalse(c1dc.getPropertyByName("ones").isOneToOne())

        assertEquals(c2dc.getPropertyByName("other").getOtherSide(), c1dc.getPropertyByName("ones"))
        assertTrue(c2dc.getPropertyByName("other").isPersistent())
        assertTrue(c2dc.getPropertyByName("other").isManyToOne())
        assertFalse(c2dc.getPropertyByName("other").isManyToMany())
        assertFalse(c2dc.getPropertyByName("other").isOneToOne())
        assertFalse(c2dc.getPropertyByName("other").isOneToMany())
    }

    void testPersistentPropertyInheritance() {
        Class topClass = gcl.parseClass("@grails.persistence.Entity\nclass Top {\n" +
                "int id\n" +
                "int version\n" +
                "String topString\n" +
                "String transientString\n" +
                "static transients=['transientString']\n"+
                "}")
        Class middleClass = gcl.parseClass("@grails.persistence.Entity\nclass Middle extends Top {\n" +
                "String middleString\n" +
                "String transientString2\n" +
                "static transients=['transientString2']\n"+
        "}")
        Class bottomClass = gcl.parseClass("@grails.persistence.Entity\nclass Bottom extends Middle {\n" +
                "String bottomString\n" +
                "String transientString3\n" +
                "static transients=['transientString3']\n"+
        "}")

        def ga = new DefaultGrailsApplication(gcl.loadedClasses, gcl)
        ga.initialise()

        def topDomainClass = ga.getDomainClass('Top')
        def middleDomainClass = ga.getDomainClass('Middle')
        def bottomDomainClass = ga.getDomainClass('Bottom')

        // topDomainClass
        GrailsDomainClassProperty topStringProperty = topDomainClass.getPropertyByName("topString")
        assertNotNull("topString property not found in topDomainClass", topStringProperty)
        assertTrue("topString property was not persistent in topDomainClass", topStringProperty.isPersistent())
        GrailsDomainClassProperty transientStringProperty = topDomainClass.getPropertyByName("transientString")
        assertNotNull("transientString property not found in topDomainClass", transientStringProperty)
        assertFalse("transientString property should have been transient in topDomainClass", transientStringProperty.isPersistent())

        // middleDomainClass
        topStringProperty = middleDomainClass.getPropertyByName("topString")
        assertNotNull("topString property not found in middleDomainClass", topStringProperty)
        assertTrue("topString property was not persistent in middleDomainClass", topStringProperty.isPersistent())

        GrailsDomainClassProperty middleStringProperty = middleDomainClass.getPropertyByName("middleString")
        assertNotNull("middleString property not found in middleDomainClass", middleStringProperty)
        assertTrue("middleString property was not persistent in middleDomainClass", middleStringProperty.isPersistent())

        GrailsDomainClassProperty transientString2Property = middleDomainClass.getPropertyByName("transientString2")
        assertNotNull("transientString2 property not found in middleDomainClass", transientString2Property)
        assertFalse("transientString2 property should have been transient in middleDomainClass", transientString2Property.isPersistent())

        transientStringProperty = middleDomainClass.getPropertyByName("transientString")
        assertNotNull("transientString property not found in middleDomainClass", transientStringProperty)
        assertFalse("transientString property should have been transient in middleDomainClass", transientStringProperty.isPersistent())

        // bottomDomainClass
        topStringProperty = bottomDomainClass.getPropertyByName("topString")
        assertNotNull("topString property not found in bottomDomainClass", topStringProperty)
        assertTrue("topString property was not persistent in bottomDomainClass", topStringProperty.isPersistent())
        assertTrue("topString property was not inherited in bottomDomainClass", topStringProperty.isInherited())

        middleStringProperty = bottomDomainClass.getPropertyByName("middleString")
        assertNotNull("middleString property not found in bottomDomainClass", middleStringProperty)
        assertTrue("middleString property was not persistent in bottomDomainClass", middleStringProperty.isPersistent())

        GrailsDomainClassProperty bottomStringProperty = bottomDomainClass.getPropertyByName("bottomString")
        assertNotNull("bottomString property not found in bottomDomainClass", bottomStringProperty)
        assertTrue("bottomString property was not persistent in bottomDomainClass", bottomStringProperty.isPersistent())

        GrailsDomainClassProperty transientString3Property = bottomDomainClass.getPropertyByName("transientString3")
        assertNotNull("transientString3 property not found in bottomDomainClass", transientString3Property)
        assertFalse("transientString3 property should have been transient in bottomDomainClass", transientString3Property.isPersistent())

        transientString2Property = bottomDomainClass.getPropertyByName("transientString2")
        assertNotNull("transientString2 property not found in bottomDomainClass", transientString2Property)
        assertFalse("transientString2 property should have been transient in bottomDomainClass", transientString2Property.isPersistent())

        transientStringProperty = bottomDomainClass.getPropertyByName("transientString")
        assertNotNull("transientString property not found in bottomDomainClass", transientStringProperty)
        assertFalse("transientString property should have been transient in bottomDomainClass", transientStringProperty.isPersistent())
    }

    void testDefaultGrailsDomainClass() throws Exception {

        Class clazz = gcl.parseClass("class UserTest { " +
                " int id\n" +
                " int version\n" +
                " List transients = [ \"age\" ]\n" +
                " String firstName\n" +
                " String lastName\n" +
                " Date age\n" +
                "}")

        GrailsDomainClass domainClass = new DefaultGrailsDomainClass(clazz)

        assertEquals("UserTest",domainClass.getName())

        assertNotNull(domainClass.getIdentifier())
        assertNotNull(domainClass.getVersion())
        assertTrue(domainClass.getIdentifier().isIdentity())

        try {
            domainClass.getPropertyByName("rubbish")
            fail("should throw exception")
        }
        catch(InvalidPropertyException ipe) {
            // expected
        }

        GrailsDomainClassProperty age = domainClass.getPropertyByName("age")
        assertNotNull(age)
        assertFalse(age.isPersistent())

        GrailsDomainClassProperty lastName = domainClass.getPropertyByName("lastName")
        assertNotNull(lastName)
        assertFalse(lastName.isOptional())

        GrailsDomainClassProperty firstName = domainClass.getPropertyByName("firstName")
        assertNotNull(firstName)
        assertFalse(firstName.isOptional())
        assertTrue(firstName.isPersistent())

        GrailsDomainClassProperty[] persistantProperties = domainClass.getPersistentProperties()
        for (int i = 0; i < persistantProperties.length; i++) {
            assertTrue(persistantProperties[i].isPersistent())
        }
    }

    void testManyToManyInSubclass() throws Exception {
        gcl.parseClass('''
@grails.persistence.Entity
class Bookmark {
    Long id
    Long version
    Set tags

    static hasMany = ["tags" : Tag]
    static belongsTo = [Tag]
}

@grails.persistence.Entity
class BookmarkSubclass extends Bookmark {
    Long id
    Long version
}

@grails.persistence.Entity
class Tag {
    Long id
    Long version
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
