/* Copyright 2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package grails.test

import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.mock.web.MockHttpSession
import org.springframework.validation.BeanPropertyBindingResult
import org.springframework.validation.Errors

/**
 * Test case for {@link MockUtils}.
 *
 * @author Peter Ledbrook
 */
class MockUtilsTests extends GroovyTestCase {
    private Map savedMetaClasses
    private Map errorsMap

    void setUp() {
        savedMetaClasses = [
            (TestDomain): TestDomain.metaClass,
            (TestController): TestController.metaClass,
            (TestCommand): TestCommand.metaClass,
            (A): A.metaClass,
            (B): B.metaClass ]
        errorsMap = new IdentityHashMap()
    }

    void tearDown() {
        // Restore all the saved meta classes.
        savedMetaClasses.each { Class clazz, MetaClass metaClass ->
            GroovySystem.metaClassRegistry.setMetaClass(clazz, metaClass)
        }

        // If we don't remove the meta-classes from these classes, later
        // tests will fail because they can't register new methods and
        // properties.
        GroovySystem.metaClassRegistry.removeMetaClass(MockHttpServletRequest)
        GroovySystem.metaClassRegistry.removeMetaClass(MockHttpServletResponse)
        GroovySystem.metaClassRegistry.removeMetaClass(BeanPropertyBindingResult)
        GroovySystem.metaClassRegistry.removeMetaClass(Errors)
    }

    /**
     * Tests that the regular expression for dynamic finders works for
     * various different finder method names.
     */
    void testRegex() {
        // The regular expression only works within a '^$' pair.
        def regex = /^findBy${MockUtils.DYNAMIC_FINDER_RE}$/

        def m = "findByName" =~ regex
        assertEquals "Name", m[0][1]
        assertNull   m[0][2]
        assertNull   m[0][4]
        assertNull   m[0][5]
        assertNull   m[0][6]

        m = "findByNameAndTitle" =~ regex
        assertEquals "Name", m[0][1]
        assertNull   m[0][2]
        assertEquals "And", m[0][4]
        assertEquals "Title", m[0][5]
        assertNull   m[0][6]

        m = "findByIdGreaterThanOrTitle" =~ regex
        assertEquals "Id", m[0][1]
        assertEquals "GreaterThan", m[0][2]
        assertEquals "Or", m[0][4]
        assertEquals "Title", m[0][5]
        assertNull   m[0][6]

        m = "findByIdGreaterThanAndTitleNotEqual" =~ regex
        assertEquals "Id", m[0][1]
        assertEquals "GreaterThan", m[0][2]
        assertEquals "And", m[0][4]
        assertEquals "Title", m[0][5]
        assertEquals "NotEqual", m[0][6]

        m = "findByNameOrTitleNotEqual" =~ regex
        assertEquals "Name", m[0][1]
        assertNull   m[0][2]
        assertEquals "Or", m[0][4]
        assertEquals "Title", m[0][5]
        assertEquals "NotEqual", m[0][6]
    }

    /**
     * Tests that calling a missing static method throws MissingMethodException
     */
    void testMissingMethod() {
        MockUtils.mockDomain(TestDomain)

        def msg = shouldFail(MissingMethodException) {
            TestDomain.noSuchMethod()
        }
        assertEquals 'No signature of method: grails.test.TestDomain.noSuchMethod() is applicable for argument types: () values: []', msg
    }

    /**
     * Tests the {@link MockUtils#mockDomain(Class, Map, List)} method.
     */
    void testDynamicFinders() {
        def aliceDoeUS = new TestDomain(name: "Alice Doe", country: "US", age: 35)
        def aliceSmithOz = new TestDomain(name: "Alice Smith", country: "Australia", age: 34)
        def chrisJonesCA = new TestDomain(name: "Chris Jones", country: "Canada", age: 16)
        def chrisJonesOz = new TestDomain(name: "Chris Jones", country: "Australia", age: 29)
        def chrisPanNull = new TestDomain(name: "Chris Pan", country: null, age: 21)
        def chrisPanOz = new TestDomain(name: "Chris Pan", country: "Australia", age: 52)
        def janeDoeNull = new TestDomain(name: "Jane Doe", country: null, age: 42)
        def janeDoeUK = new TestDomain(name: "Jane Doe", country: "UK", age: 18)
        def johnPanUS = new TestDomain(name: "John Pan", country: "US", age: 13)
        def johnSmithOz = new TestDomain(name: "John Smith", country: "Australia", age: 45)
        def johnSmithUS = new TestDomain(name: "John Smith", country: "US", age: 45)
        def peterPan = new TestDomain(name: "Peter Pan", country: "UK", age: 27)

        MockUtils.mockDomain(TestDomain, errorsMap, [
                johnSmithUS,
                janeDoeUK,
                peterPan,
                aliceSmithOz,
                johnPanUS,
                chrisJonesOz,
                chrisPanOz,
                aliceDoeUS,
                chrisJonesCA,
                johnSmithOz,
                janeDoeNull,
                chrisPanNull
        ])

        def result = TestDomain.findByName("Peter Pan")
        assertEquals  peterPan, result

        result = TestDomain.findByName("Peter Parker")
        assertNull    result

        result = TestDomain.findByNameAndCountry("Chris Jones", "Australia")
        assertEquals  chrisJonesOz, result

        result = TestDomain.findByNameLikeAndCountry("%Smith", "Australia")
        assertEquals  aliceSmithOz, result

        result = TestDomain.findByNameLikeOrCountry("%Smith", "Australia")
        assertEquals  johnSmithUS, result

        result = TestDomain.findByNameLikeOrCountry("%Jones", "UK")
        assertEquals  janeDoeUK, result
        
        result = TestDomain.findAll()
        assertEquals  12, result.size()

        result = TestDomain.findAllByName("Peter Pan")
        assertEquals( [ peterPan ], result )

        result = TestDomain.findAllWhere(country: 'US')
        assertEquals( [ johnSmithUS, johnPanUS, aliceDoeUS ], result )

        result = TestDomain.findAllWhere(country: 'US', age: 35)
        assertEquals( [ aliceDoeUS ], result )

        result = TestDomain.findAllWhere(name: 'John Smith')
        assertEquals( [ johnSmithUS, johnSmithOz ], result )

        result = TestDomain.findAllWhere(country: 'US', name: 99)
        assertEquals( [], result )


        result = TestDomain.findAllByName("Peter Parker")
        assertEquals( [], result )

        result = TestDomain.findAllByName("Chris Jones")
        assertEquals( [ chrisJonesOz, chrisJonesCA ], result )

        result = TestDomain.findAllByNameAndCountry("Chris Jones", "Australia")
        assertEquals( [ chrisJonesOz ], result )

        result = TestDomain.findAllByNameLikeAndCountry("%Smith", "Australia")
        assertEquals( [ aliceSmithOz, johnSmithOz ], result )

        result = TestDomain.findAllByNameIlikeOrCountry("%smith", "Australia")
        assertEquals( [ johnSmithUS, aliceSmithOz, chrisJonesOz, chrisPanOz, johnSmithOz ], result )

        result = TestDomain.findAllByAgeBetween(18, 35)
        assertEquals( [ janeDoeUK, peterPan, aliceSmithOz, chrisJonesOz, aliceDoeUS, chrisPanNull ], result )
        assertEquals 6, TestDomain.countByAgeBetween(18,35) 

        result = TestDomain.findAllByCountryIsNull()
        assertEquals( [ janeDoeNull, chrisPanNull ], result )
        assertEquals 2, TestDomain.countByCountryIsNull()

        result = TestDomain.findAllByAgeLessThanAndCountryIsNotNull(22)
        assertEquals( [ janeDoeUK, johnPanUS, chrisJonesCA ], result )

        result = TestDomain.findAllByAgeGreaterThanEqualsOrAgeBetween(45, 17, 22)
        assertEquals( [ johnSmithUS, janeDoeUK, chrisPanOz, johnSmithOz, chrisPanNull ], result )

        result = TestDomain.findAllByCountryNotEqualAndNameLike("Australia", "John%")
        assertEquals( [ johnSmithUS, johnPanUS ], result )

        // Test with a sort field specified.
        result = TestDomain.findAllByAgeBetween(18, 35, [ sort: "name" ])
        assertEquals( [ aliceDoeUS, aliceSmithOz, chrisJonesOz, chrisPanNull, janeDoeUK, peterPan ], result )

        result = TestDomain.findAllByAgeBetween(18, 35, [ sort: "name", order: "desc", max: 3, offset: 1 ])
        assertEquals( [ janeDoeUK, chrisPanNull, chrisJonesOz ], result )
    }

    /**
     * Tests that the mock dynamic finder methods work when there are
     * no test instances.
     */
    void testDynamicFindersWithNoResults() {
        MockUtils.mockDomain(TestDomain, errorsMap, [])

        def result = TestDomain.findAllByCountryNotEqualAndNameLike("Australia", "John%")
        assertEquals 0, result.size()

        // Test with a sort field specified.
        result = TestDomain.findAllByAgeBetween(18, 35, [ sort: "name" ])
        assertEquals 0, result.size()

        result = TestDomain.findAllByAgeBetween(18, 35, [ sort: "name", order: "desc", max: 3, offset: 1 ])
        assertEquals 0, result.size()
    }

    /**
     * Tests the dynamically added <code>get()</code> method.
     */
    void testGet() {
        def aliceDoeUS = new TestDomain(name: "Alice Doe", country: "US", age: 35)
        def aliceSmithOz = new TestDomain(name: "Alice Smith", country: "Australia", age: 34)
        def chrisJonesCA = new TestDomain(name: "Chris Jones", country: "Canada", age: 16)
        def chrisJonesOz = new TestDomain(name: "Chris Jones", country: "Australia", age: 29)

        MockUtils.mockDomain(TestDomain, errorsMap, [
                aliceDoeUS,
                aliceSmithOz,
                chrisJonesCA,
                chrisJonesOz ])

        assertEquals aliceDoeUS, TestDomain.get("1")
        assertEquals chrisJonesCA, TestDomain.get(3)
        assertEquals chrisJonesOz, TestDomain.get(4L)
        assertNull   TestDomain.get(10)
        assertNull   TestDomain.get(-1)
        assertNull   TestDomain.get(null)
    }

    /**
     * Tests the dynamically added <code>get()</code> method where the
     * test instances have been given explicit ids.
     */
    void testGetWithExplicitIds() {
        def aliceDoeUS = new TestDomain(id: 235, name: "Alice Doe", country: "US", age: 35)
        def aliceSmithOz = new TestDomain(id: 43, name: "Alice Smith", country: "Australia", age: 34)
        def chrisJonesCA = new TestDomain(id: 9, name: "Chris Jones", country: "Canada", age: 16)
        def chrisJonesOz = new TestDomain(id: 192, name: "Chris Jones", country: "Australia", age: 29)

        MockUtils.mockDomain(TestDomain, errorsMap, [
                aliceDoeUS,
                aliceSmithOz,
                chrisJonesCA,
                chrisJonesOz ])

        assertEquals aliceDoeUS, TestDomain.get("235")
        assertEquals chrisJonesCA, TestDomain.get(9)
        assertEquals chrisJonesOz, TestDomain.get(192)
        assertNull   TestDomain.get(1)
        assertNull   TestDomain.get(2)
        assertNull   TestDomain.get(null)
    }

    /**
     * Tests the dynamically added <code>read()</code> method.
     */
    void testRead() {
        def aliceDoeUS = new TestDomain(name: "Alice Doe", country: "US", age: 35)
        def aliceSmithOz = new TestDomain(name: "Alice Smith", country: "Australia", age: 34)
        def chrisJonesCA = new TestDomain(name: "Chris Jones", country: "Canada", age: 16)
        def chrisJonesOz = new TestDomain(name: "Chris Jones", country: "Australia", age: 29)

        MockUtils.mockDomain(TestDomain, errorsMap, [
                aliceDoeUS,
                aliceSmithOz,
                chrisJonesCA,
                chrisJonesOz ])

        assertEquals aliceDoeUS, TestDomain.read("1")
        assertEquals chrisJonesCA, TestDomain.read(3)
        assertEquals chrisJonesOz, TestDomain.read(4L)
        assertNull   TestDomain.read(10)
        assertNull   TestDomain.read(-1)
        assertNull   TestDomain.read(null)
    }

    /**
     * Tests the dynamically added <code>read()</code> method where the
     * test instances have been given explicit ids.
     */
    void testReadWithExplicitIds() {
        def aliceDoeUS = new TestDomain(id: 235, name: "Alice Doe", country: "US", age: 35)
        def aliceSmithOz = new TestDomain(id: 43, name: "Alice Smith", country: "Australia", age: 34)
        def chrisJonesCA = new TestDomain(id: 9, name: "Chris Jones", country: "Canada", age: 16)
        def chrisJonesOz = new TestDomain(id: 192, name: "Chris Jones", country: "Australia", age: 29)

        MockUtils.mockDomain(TestDomain, errorsMap, [
                aliceDoeUS,
                aliceSmithOz,
                chrisJonesCA,
                chrisJonesOz ])

        assertEquals aliceDoeUS, TestDomain.read("235")
        assertEquals chrisJonesCA, TestDomain.read(9)
        assertEquals chrisJonesOz, TestDomain.read(192)
        assertNull   TestDomain.read(1)
        assertNull   TestDomain.read(2)
        assertNull   TestDomain.read(null)
    }

    /**
     * Tests the dynamically added <code>getAll()</code> method.
     */
    void testGetAll() {
        def aliceDoeUS = new TestDomain(name: "Alice Doe", country: "US", age: 35)
        def aliceSmithOz = new TestDomain(name: "Alice Smith", country: "Australia", age: 34)
        def chrisJonesCA = new TestDomain(name: "Chris Jones", country: "Canada", age: 16)
        def chrisJonesOz = new TestDomain(name: "Chris Jones", country: "Australia", age: 29)

        MockUtils.mockDomain(TestDomain, errorsMap, [
                aliceDoeUS,
                aliceSmithOz,
                chrisJonesCA,
                chrisJonesOz ])

        assertEquals( [ aliceDoeUS, chrisJonesCA, aliceSmithOz ], TestDomain.getAll("1", "3", "2") )
        assertEquals( [ chrisJonesCA, aliceDoeUS ], TestDomain.getAll(3, 1) )
        assertEquals( [ aliceSmithOz ], TestDomain.getAll(2L) )
        assertEquals( [ aliceSmithOz, aliceDoeUS ], TestDomain.getAll([ 10, 2, 6, 1 ]) )
        assertEquals( [], TestDomain.getAll(10, 5) )
        assertEquals( [], TestDomain.getAll([]) )
        assertNull   TestDomain.getAll(null)
    }

    /**
     * Tests the dynamically added <code>exists()</code> method.
     */
    void testExists() {
        def aliceDoeUS = new TestDomain(name: "Alice Doe", country: "US", age: 35)
        def aliceSmithOz = new TestDomain(name: "Alice Smith", country: "Australia", age: 34)
        def chrisJonesCA = new TestDomain(name: "Chris Jones", country: "Canada", age: 16)
        def chrisJonesOz = new TestDomain(name: "Chris Jones", country: "Australia", age: 29)

        MockUtils.mockDomain(TestDomain, errorsMap, [
                aliceDoeUS,
                aliceSmithOz,
                chrisJonesCA,
                chrisJonesOz ])

        assertTrue  TestDomain.exists("1")
        assertTrue  TestDomain.exists(3)
        assertTrue  TestDomain.exists(4L)
        assertFalse TestDomain.exists(10)
        assertFalse TestDomain.exists(-1)
        assertFalse TestDomain.exists(null)
    }

    /**
     * Tests the dynamically added <code>list()</code> method.
     */
    void testList() {
        def aliceDoeUS = new TestDomain(name: "Alice Doe", country: "US", age: 35)
        def aliceSmithOz = new TestDomain(name: "Alice Smith", country: "Australia", age: 34)
        def chrisJonesCA = new TestDomain(name: "Chris Jones", country: "Canada", age: 16)
        def chrisJonesOz = new TestDomain(name: "Chris Jones", country: "Australia", age: 29)

        MockUtils.mockDomain(TestDomain, errorsMap, [
                aliceDoeUS,
                chrisJonesCA,
                aliceSmithOz,
                chrisJonesOz ])

        assertEquals( [ aliceDoeUS, chrisJonesCA, aliceSmithOz, chrisJonesOz ], TestDomain.list() )
    }

    /**
     * Tests the dynamically added <code>list()</code> method with
     * arguments ("sort", "max", etc.).
     */
    void testListWithArgs() {
        def aliceDoeUS = new TestDomain(name: "Alice Doe", country: "US", age: 35)
        def aliceSmithOz = new TestDomain(name: "Alice Smith", country: "Australia", age: 34)
        def chrisJonesCA = new TestDomain(name: "Chris Jones", country: "Canada", age: 16)
        def chrisJonesOz = new TestDomain(name: "Chris Jones", country: "Australia", age: 29)
        def jimBondCA = new TestDomain(name: "Jim Bond", country: "canada", age: 18)

        List fullList = [ aliceDoeUS, chrisJonesCA, aliceSmithOz, chrisJonesOz, jimBondCA ]

        MockUtils.mockDomain(TestDomain, errorsMap, fullList)

        assertEquals "max", [ aliceDoeUS, chrisJonesCA ], TestDomain.list(max:2)
        assertEquals "max out of bounds", fullList, TestDomain.list(max:20)
        assertEquals "max negative", fullList, TestDomain.list(max:-5)

        assertEquals "offset", fullList[2..-1], TestDomain.list(offset:2)
        assertEquals "offset out of bounds", [ ], TestDomain.list(offset:20)
        assertEquals "offset negative", fullList , TestDomain.list(offset:-5)

        assertEquals "max and offset", fullList[1..2], TestDomain.list(offset:1,max:2)
        assertEquals "max (out of bounds) and offset", fullList[2..-1], TestDomain.list(offset:2,max:20)
        assertEquals "max and offset (out of bounds)", [ ], TestDomain.list(offset:200,max:20)
        assertEquals "max (negative) and offset", fullList[1..-1], TestDomain.list(offset:1,max:-5)
        assertEquals "max and offset (negative)", fullList[0..1], TestDomain.list(offset:-5,max:2)
        assertEquals "max (negative) and offset (negative)", fullList, TestDomain.list(offset:-2,max:-5)

        assertEquals "sort", [ aliceSmithOz, chrisJonesOz, chrisJonesCA, jimBondCA, aliceDoeUS ], TestDomain.list(sort:"country")
        assertEquals "sort and max", [ aliceSmithOz, chrisJonesOz ], TestDomain.list(sort:"country", max:2)

        assertEquals(
                "sorting in descending order",
                [ aliceDoeUS, chrisJonesCA, jimBondCA, aliceSmithOz, chrisJonesOz ],
                TestDomain.list(sort:"country", order:"desc") )
        assertEquals(
                "sorting in ascending order",
                [ aliceSmithOz, chrisJonesOz, chrisJonesCA, jimBondCA, aliceDoeUS ],
                TestDomain.list(sort:"country", order:"asc") )
        assertEquals(
                "sorting (ignoring case)",
                [ aliceSmithOz, chrisJonesOz, chrisJonesCA, aliceDoeUS, jimBondCA ],
                TestDomain.list(sort:"country", ignoreCase: false) )

        assertEquals(
                "sorting by number",
                [ chrisJonesCA, jimBondCA, chrisJonesOz, aliceSmithOz, aliceDoeUS ],
                TestDomain.list(sort:"age") )
    }

    /**
     * Tests the dynamically added <code>list()</code> method with
     * arguments ("sort", "max", etc.) when no results are returned.
     */
    void testListWithArgsAndNoResults() {
        MockUtils.mockDomain(TestDomain, errorsMap, [])
        assertEquals "max and offset", [], TestDomain.list(max:4)
        assertEquals "max and offset", [], TestDomain.list(offset:1, max:2)
        assertEquals "max and offset", [], TestDomain.list(sort:"country", max:4)
    }

    /**
     * Tests the dynamically added <code>save()</code> method.
     */
    void testSave() {
        MockUtils.mockDomain(TestDomain, errorsMap)

        def domain = new TestDomain(name: "Alice Doe", country: "US", age: 35, title: "Ms.")
        assertEquals domain, domain.save()
    }

    /**
     * Tests the dynamically added <code>save()</code> method.
     */
    void testSaveWithArgs() {
        MockUtils.mockDomain(TestDomain, errorsMap)

        def domain = new TestDomain(name: "Alice Doe", country: "US", age: 35, title: "Ms.")
        assertEquals domain, domain.save(flush: true)
    }

    /**
     * Tests that the <code>save()</code> method does not add an existing
     * test instance to the original list of test instances. In other
     * words, if one of the original test instances is saved, the list
     * should <i>not</i> change in size.
     */
    void testSaveWithExistingInstance() {
        def testInstances = [ new TestDomain(name: "Alice Doe", country: "US", age: 35) ]
        MockUtils.mockDomain(TestDomain, errorsMap, testInstances)

        testInstances[0].save()
        assertEquals 1, testInstances.size()
    }

    /**
     * Tests the dynamically added <code>delete()</code> method.
     */
    void testDelete() {
        def aliceDoeUS = new TestDomain(name: "Alice Doe", country: "US", age: 35)
        def aliceSmithOz = new TestDomain(name: "Alice Smith", country: "Australia", age: 34)
        def chrisJonesCA = new TestDomain(name: "Chris Jones", country: "Canada", age: 16)
        def chrisJonesOz = new TestDomain(name: "Chris Jones", country: "Australia", age: 29)
        def testInstances = [
                aliceDoeUS,
                chrisJonesCA,
                aliceSmithOz,
                chrisJonesOz ]

        MockUtils.mockDomain(TestDomain, errorsMap, testInstances)

        aliceSmithOz.delete()
        assertNull TestDomain.get(3)
        assertEquals 3, TestDomain.count()
    }

    /**
     * Tests the dynamically added <code>delete()</code> method.
     */
    void testDeleteWithArgs() {
        def aliceDoeUS = new TestDomain(name: "Alice Doe", country: "US", age: 35)
        def aliceSmithOz = new TestDomain(name: "Alice Smith", country: "Australia", age: 34)
        def chrisJonesCA = new TestDomain(name: "Chris Jones", country: "Canada", age: 16)
        def chrisJonesOz = new TestDomain(name: "Chris Jones", country: "Australia", age: 29)

        MockUtils.mockDomain(TestDomain, errorsMap, [
                aliceDoeUS,
                chrisJonesCA,
                aliceSmithOz,
                chrisJonesOz ])

        aliceSmithOz.delete(flush: true)
        assertNull TestDomain.get(3)
    }

    /**
     * Tests that the <code>discard()</code> method is mocked.
     */
    void testDiscard() {
        MockUtils.mockDomain(TestDomain, errorsMap)

        def domain = new TestDomain(name: "Alice Doe", country: "US", age: 35)
        assertEquals domain, domain.discard()
    }

    /**
     * Tests the dynamically added <code>addTo*()</code> method.
     */
    void testAddTo() {
        def aliceDoeUS = new TestDomain(name: "Alice Doe", country: "US", age: 35)

        MockUtils.mockDomain(TestDomain, errorsMap, [ aliceDoeUS ])

        // Make sure we start with no relations.
        assertNull "Alice US's relations set should be null.", aliceDoeUS.relations

        // Now add a relation or two.
        assertEquals aliceDoeUS, aliceDoeUS.addToRelations("Auntie Miriam")
        assertEquals aliceDoeUS, aliceDoeUS.addToRelations("Uncle Jack")

        // Check that they are in the set.
        assertEquals( [ "Auntie Miriam", "Uncle Jack" ] as Set, aliceDoeUS.relations )
    }

    /**
     * Tests the dynamically added <code>removeFrom*()</code> method.
     */
    void testRemoveFrom() {
        def aliceDoeUS = new TestDomain(
                name: "Alice Doe",
                country: "US",
                age: 35,
                relations: [ "Auntie Miriam", "Uncle Jack" ] as Set)

        MockUtils.mockDomain(TestDomain, errorsMap, [ aliceDoeUS ])

        assertEquals 2, aliceDoeUS.relations?.size()

        // Now remove a relation.
        assertEquals aliceDoeUS, aliceDoeUS.removeFromRelations("Auntie Miriam")

        // Check that only Uncle Jack is left.
        assertEquals( [ "Uncle Jack" ] as Set, aliceDoeUS.relations )
    }

    /**
     * Tests that a domain class with a string ID can be mocked.
     */
    void testStringId() {
        def aliceDoeUS = new TestDomainWithUUID(id: "lemon", name: "Alice Doe", country: "US", age: 35)
        def aliceSmithOz = new TestDomainWithUUID(id: "orange", name: "Alice Smith", country: "Australia", age: 34)
        def chrisJonesCA = new TestDomainWithUUID(id: "apple", name: "Chris Jones", country: "Canada", age: 16)
        def chrisJonesOz = new TestDomainWithUUID(id: "cherry", name: "Chris Jones", country: "Australia", age: 29)

        MockUtils.mockDomain(TestDomainWithUUID, errorsMap, [ aliceDoeUS, aliceSmithOz, chrisJonesCA, chrisJonesOz ])
        def d = TestDomainWithUUID.get("apple")
        assertEquals chrisJonesCA, d
    }

    /**
     * Tests the <code>validate()</code> method added by {@link
     * MockUtils#prepareForConstraintsTests(Class, List)}.
     */
    void testConstraintValidation() {
        def testInstances = [
                new TestDomain(id: 5L, name: "Test", country: "US", age: 21, other: "Complex", title: "Prof.") ]
        MockUtils.prepareForConstraintsTests(TestDomain, errorsMap, testInstances)

        def dc = new TestDomain()
        dc.validate()
        assertEquals "nullable", dc.errors["name"]
        assertEquals "nullable", dc.errors["title"]
        assertEquals "min", dc.errors["age"]
        assertNull   dc.errors["id"]
        assertNull   dc.errors["country"]
        assertNull   dc.errors["email"]
        assertNull   dc.errors["cardNumber"]
        assertNull   dc.errors["item"]

        dc = new TestDomain(
                id: 5,
                name: "",
                country: "",
                age: 10,
                email: "someone@nowhere.net",
                other: "Antidisestablishmentarianism",
                number: 342L,
                title: "Ms.")
        dc.validate()
        assertEquals "unique", dc.errors["id"]
        assertEquals "blank", dc.errors["name"]

        assertNull dc.errors["country"] // blank values bound to null when nullable:true
        assertEquals "matches", dc.errors["email"]
        assertEquals "size", dc.errors["other"]
        assertEquals "range", dc.errors["number"]
        assertNull   dc.errors["age"]
        assertNull   dc.errors["title"]

        dc = new TestDomain(
                name: "A long name",
                country: "F",
                age: 110,
                email: "notsomewhere.org",
                homePage: "mypage",
                cardNumber: "49357",
                item: "five",
                other: "Test",
                number: 5L,
                notOdd: 53L,
                title: "Mr.")
        dc.validate()
        assertEquals "maxSize", dc.errors["name"]
        assertEquals "minSize", dc.errors["country"]
        assertEquals "max", dc.errors["age"]
        assertEquals "email", dc.errors["email"]
        assertEquals "url", dc.errors["homePage"]
        assertEquals "creditCard", dc.errors["cardNumber"]
        assertEquals "inList", dc.errors["item"]
        assertEquals "notEqual", dc.errors["other"]
        assertEquals "range", dc.errors["number"]
        assertEquals "odd", dc.errors["notOdd"]

        // Testing the remaining unique constraint types.
        dc = new TestDomain(
                name: "Test",
                country: "US",
                age: 21,
                other: "Complex",
                number: 21L,
                title: "Mrs.")
        dc.validate()
        assertEquals "unique", dc.errors["name"]
        assertEquals "unique", dc.errors["country"]
        assertEquals "validator", dc.errors["number"]

        // Test that the "unique" constraint does not fire on objects
        // that are in the list of test instances.
        dc = testInstances[0]
        assertTrue dc.validate()

        dc = new TestDomain(
                name: "Test",
                country: "UK",
                age: 21,
                email: "someone@somewhere.org",
                homePage: "http://www.mypage.org/",
                cardNumber: "4417123456789113",
                item: "two",
                other: "Simple",
                number: 13L,
                notOdd: 14L,
                title: "Dr.")
        dc.validate()
        assertTrue dc.errors.isEmpty()

        // Test that class hierarchies with multiple "constraints" blocks
        // work ok.
        MockUtils.prepareForConstraintsTests(B, errorsMap)
        dc = new B(name: "Bee", country: "US", age: 5, b: "supercallifragilistic")
        assertFalse  dc.validate()
        assertEquals "nullable", dc.errors["title"]
        assertEquals "min", dc.errors["age"]
        assertEquals "maxSize", dc.errors["b"]
        assertNull   dc.errors["country"]
        assertNull   dc.errors["email"]
        assertNull   dc.errors["cardNumber"]
        assertNull   dc.errors["item"]

        dc = new B(name: "Bee", country: "US", age: 21, b: "simple", title: "Mr.")
        assertTrue dc.validate()
    }

    /**
     * Tests that the dynamic methods can be called on the test instances
     * passed into the {@link MockUtils#mockDomain(Class, Map, List)} method.
     */
    void testDynamicMethodsOnTestInstances() {
        def aliceDoeUS = new TestDomain(name: "Alice Doe", country: "US", age: 35)
        def aliceSmithOz = new TestDomain(name: "Alice Smith", country: "Australia", age: 34)
        def chrisJonesCA = new TestDomain(name: "Chris Jones", country: "Canada", age: 16)
        def chrisJonesOz = new TestDomain(name: "Chris Jones", country: "Australia", age: 29)

        def testInstances = [
                aliceDoeUS,
                aliceSmithOz,
                chrisJonesCA,
                chrisJonesOz ]
        MockUtils.mockDomain(TestDomain, errorsMap, testInstances)

        def domain = TestDomain.get(1)
        assertEquals domain, domain.addToRelations("test")

        // Also check that the "id" attribute has been set correctly
        // on all of the test instances.
        testInstances.eachWithIndex { obj, i ->
            assertEquals i + 1, obj.id
        }
    }

    /**
     * Tests that the mock "render()" method writes a simple string
     * argument to the response without modification.
     */
    void testMockControllerRenderText() {
        MockUtils.mockController(TestController)

        def controller = new TestController()
        controller.index()

        assertEquals "hello", controller.response.contentAsString
    }

    /**
     * Tests that the render method for views populates the "renderArgs"
     * map with the given values.
     */
    void testMockControllerRenderView() {
        MockUtils.mockController(TestController)

        def controller = new TestController()

        // Test simple view and model.
        controller.render(view: "list", model: [count: 101])

        assertEquals "list", controller.renderArgs["view"]
        assertEquals( [count: 101], controller.renderArgs["model"] )

        // "view" and "text" arguments are mutually exclusive.
        shouldFail(AssertionError) {
            controller.render(view: "list", text: "This should fail!")
        }
    }

    /**
     * Tests that the render method used for templates populates the
     * "renderArgs" map with the given values.
     */
    void testMockControllerRenderTemplate() {
        MockUtils.mockController(TestController)

        def controller = new TestController()

        // Test template on its own.
        controller.render(template: "fragment")

        assertEquals "fragment", controller.renderArgs["template"]
        assertNull   "'bean' should not be in render args.", controller.renderArgs["bean"]
        assertNull   "'model' should not be in render args.", controller.renderArgs["model"]
        assertNull   "'collection' should not be in render args.", controller.renderArgs["collection"]

        // Test simple template and model.
        controller.renderArgs.clear()
        controller.render(template: "fragment", model: [count: 101])

        assertEquals "fragment", controller.renderArgs["template"]
        assertEquals 101, controller.renderArgs["model"]["count"]
        assertNull   "'bean' should not be in render args.", controller.renderArgs["bean"]
        assertNull   "'collection' should not be in render args.", controller.renderArgs["collection"]

        // Test template with bean.
        def testBean = new Expando(name: "My Bean")
        controller.renderArgs.clear()
        controller.render(template: "fragment", bean: testBean)

        assertEquals "fragment", controller.renderArgs["template"]
        assertEquals testBean, controller.renderArgs["bean"]
        assertNull   "'model' should not be in render args.", controller.renderArgs["model"]
        assertNull   "'collection' should not be in render args.", controller.renderArgs["collection"]

        // Test template with collection.
        def testCollection = [ "item" ]
        controller.renderArgs.clear()
        controller.render(template: "fragment", collection: testCollection)

        assertEquals "fragment", controller.renderArgs["template"]
        assertEquals testCollection, controller.renderArgs["collection"]
        assertNull   "'model' should not be in render args.", controller.renderArgs["model"]
        assertNull   "'bean' should not be in render args.", controller.renderArgs["bean"]

        // "template" and "text" arguments are mutually exclusive.
        shouldFail(AssertionError) {
            controller.render(template: "fragment", text: "This should fail!")
        }

        // "template" and "view" arguments are mutually exclusive.
        shouldFail(AssertionError) {
            controller.render(template: "fragment", view: "list")
        }

        // "bean" and "collection" arguments are mutually exclusive.
        shouldFail(AssertionError) {
            controller.render(template: "fragment", bean: [:], collection: [])
        }

        // "bean" and "model" arguments are mutually exclusive.
        shouldFail(AssertionError) {
            controller.render(template: "fragment", bean: [:], model: [:])
        }

        // "collection" and "model" arguments are mutually exclusive.
        shouldFail(AssertionError) {
            controller.render(template: "fragment", collection: [], model: [:])
        }
    }

    /**
     * Tests that the render method used for generating XML via a closure
     * writes the expected XML content to the response stream.
     */
    void testMockControllerRenderXml() {
        MockUtils.mockController(TestController)

        def controller = new TestController()

        // Test the render method with a simple block of markup.
        controller.render(contentType: "application/xml") {
            "shopping-list" {
                item(qty: 10, "Orange")
                item(qty: 6, "Apple")
                item(qty: 1, "Soap")
            }
        }

        assertEquals(
                "<shopping-list><item qty='10'>Orange</item><item qty='6'>Apple</item><item qty='1'>Soap</item></shopping-list>",
                controller.response.contentAsString)
    }

    /**
     * Tests that the mock "params" object on a controller works properly.
     */
    void testMockControllerParamsObject() {
        MockUtils.mockController(TestController)

        def controller = new TestController()
        controller.params.id = "John"
        controller.testParams()

        assertEquals "hello John", controller.response.contentAsString
    }

    /**
     * Tests that the mock "chainModel" object on a controller works properly.
     */
    void testMockControllerChainModelObject() {
        MockUtils.mockController(TestController)

        def controller = new TestController()
        controller.chainModel.key = "value"
        controller.testChainModel()

        assertEquals "chained with [key:value]", controller.response.contentAsString
    }

    /**
     * Tests that the mock "forward()" method with a controller and
     * action adds the values to the corresponding properties in the
     * "forwardArgs" map.
     */
    void testMockControllerForward() {
        MockUtils.mockController(TestController)

        def controller = new TestController()

        controller.testForward()

        assertEquals "car", controller.forwardArgs.controller
        assertEquals "list", controller.forwardArgs.action
    }

    /**
     * Tests that the mock "redirect()" method with a controller and
     * action adds the values to the corresponding properties in the
     * "redirectArgs" map.
     */
    void testMockControllerRedirect() {
        MockUtils.mockController(TestController)

        def controller = new TestController()

        controller.testRedirect()

        assertEquals "foo", controller.redirectArgs.controller
        assertEquals "bar", controller.redirectArgs.action
    }

    /**
     * Tests that the mock "chain()" method with a controller, action
     * and model adds the values to the corresponding properties in the
     * "chainArgs" map.
     */
    void testMockControllerChain() {
        MockUtils.mockController(TestController)

        def controller = new TestController()

        controller.testChain()

        assertEquals "foo", controller.chainArgs.controller
        assertEquals "bar", controller.chainArgs.action
        assertEquals "baz", controller.chainArgs.model.key
    }

    /**
     * Tests that the mock session added to controllers works OK, and
     * in particular that "session['attr']" and "session.attr" notation
     * works.
     */
    void testMockControllerSession() {
        MockUtils.mockController(TestController)

        def controller = new TestController()
        controller.session.setAttribute("attr1", "star")
        controller.session.setAttribute("attr2", "square")
        controller.testSession()

        assertEquals "star_suffix", controller.session.getAttribute("attr1")
        assertEquals "square_suffix", controller.session.getAttribute("attr2")
        assertEquals "Last attribute", controller.session.getAttribute("attr3")

        // Now a quick check that the session can be invalidated.
        controller.testSessionInvalidate()

        assertTrue controller.session.isInvalid()
        assertNull controller.session.getAttribute("attr1")
    }

    /**
     * Tests that the withFormat method of content negotiation is testable
     */
    void testWithFormat() {
        MockUtils.mockController(TestController)
        MockUtils.mockDomain(TestDomain, errorsMap)

        def controller = new TestController()
        controller.request.format = "html"

        def model = controller.testWithFormat()
        assertEquals "bar", model.foo

        controller.request.format = "json"

        controller.testWithFormat()

        assertEquals "someTemplate", controller.renderArgs.template

        controller.request.format = "xml"

        controller.testWithFormat()

        assertEquals "<root class='TestDomain'><foo>bar</foo></root>", controller.response.contentAsString
    }

    /**
     * Tests that the withForm double-submission handling is mocked
     * correctly.
     */
    void testWithForm() {
        MockUtils.mockController(TestController)
        MockUtils.mockDomain(TestDomain, errorsMap)

        def controller = new TestController()

        // Check that "single submission" works fine.
        def model = controller.testWithForm()
        assertEquals "bar", model.foo

        // A "double submission" should invoke the "invalid token"
        // handler.
        controller.request.invalidToken = true
        controller.testWithForm()

        assertEquals "Double submission!", controller.response.contentAsString
        assertNull controller.flash.invalidToken

        // Explicit "single submission".
        controller.request.invalidToken = false
        model = controller.testWithForm()
        assertEquals "bar", model.foo
    }

    /**
     * Tests that the withForm double-submission handling is mocked
     * correctly when the "invalid token" handler returns a model.
     */
    void testWithFormInvalidReturnsModel() {
        MockUtils.mockController(TestController)
        MockUtils.mockDomain(TestDomain, errorsMap)

        def controller = new TestController()

        // Make sure that the model returned by the "invalid token"
        // handler is also returned by the action.
        controller.request.invalidToken = true
        def model = controller.testWithFormInvalidReturnsModel()

        assertEquals "Double submission!", model.error
        assertNull controller.flash.invalidToken
    }

    /**
     * Tests that the withForm double-submission handling is mocked
     * correctly when there is no "invalid token" handler.
     */
    void testWithFormNoInvalidTokenHandler() {
        MockUtils.mockController(TestController)
        MockUtils.mockDomain(TestDomain, errorsMap)

        def controller = new TestController()

        // Check that "single submission" works fine.
        def model = controller.testWithForm2()
        assertEquals "bar", model.foo

        // A "double submission" should add the value "token" to the
        // flash under the key "invalidToken". It should *not* return
        // a model containing anything other than the special "invalidToken"
        // entry.
        controller.request.invalidToken = true
        model = controller.testWithForm2()

        model.remove("invalidToken")
        assertTrue !model
        assertEquals "token", controller.flash.invalidToken

        // Explicit "single submission".
        controller.request.invalidToken = false
        model = controller.testWithForm2()
        assertEquals "bar", model.foo
    }

    /**
     * Tests that mocking of attribute access works
     */
    void testMockAttributeAccess() {
        MockUtils.mockAttributeAccess(MockHttpServletRequest)

        def request = new MockHttpServletRequest()
        request.setAttribute("attr1", "value1")
        request.setAttribute("attr2", "value2")

        assertEquals "value1", request["attr1"]
        assertEquals "value2", request["attr2"]
        assertEquals "value1", request.attr1
        assertEquals "value2", request.attr2

        request.foo = "bar"
        assertEquals "bar", request.getAttribute("foo")

        request["foo2"] = "bar2"
        assertEquals "bar2", request.getAttribute("foo2")

        // Make sure that the real properties on the request are still
        // accessible.
        assertEquals 80, request.localPort
    }

    /**
     * Tests that command objects are mocked properly, i.e. the
     * "validate()" method populates an Errors object correctly
     * and that the Errors object is accessible.
     */
    void testMockCommandObject() {
        MockUtils.mockCommandObject(TestCommand, errorsMap)

        // Create a command and check that we have access to the errors
        // object.
        def cmd = new TestCommand(rating: 0.5)

        assertFalse cmd.hasErrors()

        // Validate the command and check that the errors object is
        // correctly populated.
        cmd.validate()

        assertTrue cmd.hasErrors()
        assertTrue cmd.errors.hasFieldErrors("name")
        assertFalse cmd.errors.hasFieldErrors("rating")

        // Set the name field and revalidate. The errors should now be
        // clear.
        cmd.name = "dilbert"
        cmd.validate()

        assertFalse cmd.hasErrors()

        // Test a second object after adding errors to the previous one.
        cmd.name = ""
        cmd.rating = 1.5
        cmd.validate()
        assertTrue cmd.hasErrors()

        cmd = new TestCommand()
        assertFalse cmd.hasErrors()
    }

    /**
     * Tests that the data-binding added by the
     * {@link MockUtils#addValidateMethod(Class, List)} works as expected.
     */
    void testDataBinding() {
        MockUtils.prepareForConstraintsTests(TestDomain, errorsMap, [
                new TestDomain(id: 5L, name: "Test", country: "US", age: 21, other: "Complex") ])

        // First check that binding errors appear.
        def dc = new TestDomain(name: "Bad data", age: "Not a number")
        assertTrue dc.hasErrors()
        assertEquals 1, dc.errors.fieldErrorCount
        assertTrue dc.errors.hasFieldErrors("age")
        assertEquals "Bad data", dc.name
        assertEquals 0, dc.age

        // Clear the errors and try binding using ".properties".
        def params = [ name: "Properties", item: "Apple", age: "21", number: "sdjhfks", notOdd: "12eee345" ]
        dc.clearErrors()
        dc.properties = params

        // "age" should no longer be in errors, but "number" and "notOdd"
        // should be.
        assertTrue dc.hasErrors()
        assertEquals 2, dc.errors.fieldErrorCount
        assertFalse dc.errors.hasFieldErrors("age")
        assertTrue dc.errors.hasFieldErrors("number")
        assertTrue dc.errors.hasFieldErrors("notOdd")
        assertEquals "Properties", dc.name
        assertEquals 21, dc.age
        assertNull dc.number
        assertNull dc.notOdd
    }

    /**
     * Tests that the data-binding handles nested properties.
     */
    void testNestedDataBinding() {
        MockUtils.mockDomain(TestNestedParentDomain, errorsMap)
        MockUtils.mockDomain(TestNestedChildDomain, errorsMap, [ new TestNestedChildDomain(id: 42L, name: 'Apple') ])

        def params = [ name: 'Fruit basket', 'child.id': 42L ]
        def dc = new TestNestedParentDomain()
        dc.properties = params

        // no binding errors should occur, as the child exists
        assertFalse dc.hasErrors()
        assertNotNull dc.child
        assertEquals 42L, dc.child.id

        // re-bind with a non-existing child
        params.'child.id' = 12345L
        dc = new TestNestedParentDomain()
        dc.properties = params
        assertFalse dc.hasErrors()
        assertNotNull dc.child
        assertEquals 12345L, dc.child.id
        assertNull dc.child.name

        println ">>> Validation result: ${dc.validate(deep: true)}"
    }

    /**
     * Tests that the usual dynamic properties are available to mocked
     * tag libraries and that the tags themselves have access to "out",
     * the "render" tag, and the "throwTagError()" method.
     */
    void testMockTagLib() {
        MockUtils.mockTagLib(TestTagLib)

        // Create the tag lib and check that various dynamic properties
        // are available on it.
        def taglib = new TestTagLib()
        assertTrue taglib.request instanceof MockHttpServletRequest
        assertTrue taglib.response instanceof MockHttpServletResponse
        assertTrue taglib.session instanceof MockHttpSession

        // Test a simple tag that writes something to the output writer.
        taglib.myTag(attr1: "value1", attr2: "value2", null)
        assertEquals "Something", taglib.out.toString()

        // Now test a tag that uses the "render" tag as a method.
        taglib.myRenderTag([:])
        assertEquals "fragment", taglib.template.name
        assertEquals 10, taglib.template.model["count"]

        // Finally, test a tag that throws an error.
        shouldFail(GrailsTagException) {
            taglib.myErrorTag(attr1: "value1", null)
        }
    }

    void testMockLogging() {
        MockUtils.mockLogging(TestDomain)

        def obj = new TestDomain()
        obj.log.fatal("Fatal error")
        obj.log.error("Normal error")
        obj.log.warn("Warning")
        obj.log.info("Information message")
        obj.log.debug("Debug")
        obj.log.trace("Trace")

        assertTrue obj.log.isFatalEnabled()
        assertTrue obj.log.isErrorEnabled()
        assertTrue obj.log.isWarnEnabled()
        assertTrue obj.log.isInfoEnabled()
        assertFalse obj.log.isDebugEnabled()
        assertFalse obj.log.isTraceEnabled()

        assertTrue obj.log.fatalEnabled
        assertTrue obj.log.errorEnabled
        assertTrue obj.log.warnEnabled
        assertTrue obj.log.infoEnabled
        assertFalse obj.log.debugEnabled
        assertFalse obj.log.traceEnabled
    }

    void testMockLoggingDebugEnabled() {
        MockUtils.mockLogging(TestDomain, true)

        def obj = new TestDomain()
        obj.log.fatal("Fatal error")
        obj.log.error("Normal error")
        obj.log.warn("Warning")
        obj.log.info("Information message")
        obj.log.debug("Debug")
        obj.log.trace("Trace")

        assertTrue obj.log.isFatalEnabled()
        assertTrue obj.log.isErrorEnabled()
        assertTrue obj.log.isWarnEnabled()
        assertTrue obj.log.isInfoEnabled()
        assertTrue obj.log.isDebugEnabled()
        assertFalse obj.log.isTraceEnabled()

        assertTrue obj.log.fatalEnabled
        assertTrue obj.log.errorEnabled
        assertTrue obj.log.warnEnabled
        assertTrue obj.log.infoEnabled
        assertTrue obj.log.debugEnabled
        assertFalse obj.log.traceEnabled
    }

    /**
     * Tests that the <code>instanceOf()</code> method is mocked.
     */
    void testInstanceOf() {
        MockUtils.mockDomain(TestDomain)
        def domain = new TestDomain()
        assertTrue domain.instanceOf(TestDomain)
        assertFalse domain.instanceOf(A)

        MockUtils.mockDomain(A)
        def a = new A()
        assertTrue a.instanceOf(TestDomain)
        assertTrue a.instanceOf(A)
        assertFalse a.instanceOf(B)
    }
}

/**
 * Simple controller implementation to test the controller mocking.
 */
class TestController  {
    def index = {
        render "hello"
    }

    def testParams = {
        render "hello ${params.id}"
    }

    def testChainModel = {
        render "chained with ${chainModel}"
    }

    def testForward = {
        forward(controller: "car", action: "list")
    }

    def testRedirect = {
        redirect(controller: "foo", action: "bar")
    }

    def testChain = {
        chain(controller: "foo", action: "bar", model: [key: "baz"])
    }

    def testSession = {
        session["attr1"] = session["attr1"] + "_suffix"
        session.attr2 = session.attr2 + "_suffix"
        session.attr3 = "Last attribute"
    }

    def testSessionInvalidate = {
        session.invalidate()
    }

    def testWithFormat = {
        withFormat {
            html foo:"bar"
            xml {
                render(contentType: "application/xml") {
                    root("class": "TestDomain") {
                        foo("bar")
                    }
                }
            }
            json { render(template:"someTemplate")}
        }
    }

    def testWithForm = {
        withForm {
            [ foo: "bar" ]
        }.invalidToken {
            render "Double submission!"
        }
    }

    def testWithFormInvalidReturnsModel = {
        withForm {
            render "OK"
        }.invalidToken {
            [ error: "Double submission!" ]
        }
    }

    def testWithForm2 = {
        withForm {
            [ foo: "bar" ]
        }
    }
}

/**
 * Domain class used to test MockUtils.
 */
class TestDomain {
    Long id
    Long version
    String name
    String country
    int    age
    String email
    String homePage
    String cardNumber
    String item
    String other
    Long number
    Long notOdd
    String title
    Set relations

    static constraints = {
        id(nullable: true, unique: true)
        name(nullable: false, blank: false, maxSize: 10, unique: "country")
        country(nullable: true, blank: false, minSize: 2, unique: [ "age", "other" ])
        age(min: 8, max: 65)
        email(nullable: true, email: true, matches: /.+somewhere.org$/)
        homePage(nullable: true, url: true)
        cardNumber(nullable: true, creditCard: true)
        item(nullable: true, inList: [ "one", "two", "three" ])
        other(nullable: true, notEqual: "Test", size: 4..10)
        number(nullable: true, range: 10L..22L, validator: {val, obj -> val != obj.age})
        notOdd(nullable: true, validator: { val, obj ->
            if (val && val % 2 > 0) return "odd"
        })
    }

    boolean equals(Object obj) {
        if (!(obj instanceof TestDomain)) return null

        return this.name == obj.name &&
                this.country == obj.country &&
                this.age == obj.age
    }

    String toString() {
        "TestDomain(${this.id}, ${this.name}, ${this.country}, ${this.age})"
    }
}

class A extends TestDomain {
    String a
}

class B extends TestDomain {
    String b

    static constraints = {
        b(nullable: false, blank: false, maxSize: 10)
    }
}

/**
 * Test domain class to check that MockUtils works with string IDs.
 */
class TestDomainWithUUID {
    String id
    Long   version
    String name
    String country
    int    age

    static constraints = {
        name(nullable: false, blank: false, maxSize: 10, unique: "country")
        age(min: 8, max: 65)
    }

    boolean equals(Object obj) {
        if (!(obj instanceof TestDomainWithUUID)) return null

        return this.name == obj.name &&
                this.country == obj.country &&
                this.age == obj.age
    }

    String toString() {
        "TestDomainWithUUID(${this.id}, ${this.name}, ${this.country}, ${this.age})"
    }
}

class TestTagLib {
    def myTag = { attrs, body ->
        out << "Something"
    }

    def myErrorTag = { attrs, body ->
        throwTagError("Some error")
    }

    def myRenderTag = { attrs ->
        out << g.render(template: "fragment", model: [count: 10])
    }
}

/**
 * Command object used to test MockUtils.
 */
class TestCommand {
    String name
    BigDecimal rating
    List roles

    static constraints = {
        name(nullable: false, blank: false)
        rating(range: 0.0..1.0)
    }
}

/**
 * Parent class for testing data binding of nested domain classes.
 */
class TestNestedParentDomain {
    Long id
    Long version
    String name
    TestNestedChildDomain child

    String toString() {
        "TestNestedParentDomain (${this.id}, ${this.name}, ${this.child?.id})"
    }
}

/**
 * Child class for testing data binding of nested domain classes.
 */
class TestNestedChildDomain {
    Long id
    Long version
    String name
    TestNestedParentDomain parent

    static belongsTo = TestNestedParentDomain

    String toString() {
        "TestNestedChildDomain (${this.id}, ${this.name})"
    }
}
