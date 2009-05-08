package grails.test
/**
 * @author Graeme Rocher
 * @since 1.0
 * 
 * Created: May 8, 2009
 */

import grails.persistence.*

public class MockDomainWithInheritanceTests extends GroovyTestCase{

    void testMockDomainWithInheritance() {
        def test = new PersonTests()
        test.setUp()

        test.testLoadingPirateInstance()
    }
}
class PersonTests extends GrailsUnitTestCase {
    void setUp() {
        super.setUp()
		def aPerson = new Person(name: "Rob Fletcher")
		def aPirate = new Pirate(name: "Edward Teach", nickname: "Blackbeard")
		mockDomain(Person, [aPerson, aPirate])
    }

    void tearDown() {
        super.tearDown()
    }

    void testLoadingPersonInstance() {
		def person = Person.findByName("Rob Fletcher")
		assertTrue person instanceof Person
		assertFalse person instanceof Pirate
    }

	void testLoadingPirateInstance() {
		def person = Person.findByName("Edward Teach")
		assertTrue person instanceof Person
		assertTrue person instanceof Pirate
		assertEquals("Blackbeard", person.nickname)

        person = Pirate.findByName("Edward Teach")

        assertNotNull "should have found a pirate", person
        assertTrue person instanceof Person
        assertTrue person instanceof Pirate
        assertEquals("Blackbeard", person.nickname)

        assertNull "That's not a pirate!", Pirate.findByName("Rob Fletcher") 
	}
}

@Entity
class Person {
	String name
}
@Entity
class Pirate extends Person {
	String nickname
}
