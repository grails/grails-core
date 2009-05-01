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

import org.codehaus.groovy.grails.plugins.codecs.HTMLCodec
import grails.converters.JSON
import grails.converters.XML
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import junit.framework.AssertionFailedError

/**
 * Test case for {@link GrailsUnitTestCase}.
 */
class GrailsUnitTestCaseTests extends GroovyTestCase {
    protected void setUp() {
        super.setUp()
    }

    void testMockConfig() {
        def testCase = new TestUnitTestCase()
        testCase.setUp()
        testCase.mockConfig '''
            foo.bar = "good"
        '''

        new GrailsUnitTestClass().testConfig()

        testCase.tearDown()

    }
    void testMockLogging() {
        def testCase = new TestUnitTestCase()
        testCase.setUp()
        testCase.mockLogging GrailsUnitTestClass

        new GrailsUnitTestClass().testLogging()

        testCase.tearDown()
    }

    void testMockLoggingWithDebugEnabled() {
        def testCase = new TestUnitTestCase()
        testCase.setUp()
        testCase.mockLogging GrailsUnitTestClass, true

        new GrailsUnitTestClass().testLogging()

        testCase.tearDown()
    }

    void testMockDomainErrors() {
        def testCase = new TestUnitTestCase()
        testCase.setUp()
        testCase.testFailingCase()
        testCase.testSuccessCase()
        testCase.tearDown()
    }

    /**
     * Tests that the {@link GrailsUnitTestCase#mockForConstraintsTests(Class, List)}
     * method adds the <code>validate()</code> method to the given
     * class. Since most of the work is done by a MockUtils method, the
     * test is very lightweight.
     */
    void testMockForConstraintsTests() {
        def testCase = new TestUnitTestCase()
        testCase.setUp()
        testCase.testConstraints()
        testCase.tearDown()
    }

    /**
     * Tests that the {@link GrailsUnitTestCase#mockFor(Class, boolean)}
     * method does not leak between tests - see
     * <a href="http://jira.codehaus.org/browse/GRAILS-3615">GRAILS-3615</a>.
     */
    void testMockFor() {
        def testCase = new TestUnitTestCase()
        testCase.setUp()
        testCase.testMockGroovy1()
        testCase.tearDown()

        testCase.setUp()
        testCase.testMockGroovy2()
        testCase.tearDown()

        testCase.setUp()
        testCase.testMockInterface1()
        testCase.tearDown()

        testCase.setUp()
        testCase.testMockInterface2()
        testCase.tearDown()
    }

    /**
     * Tests that the deep validation works on mocked domain classes.
     * Effectively tests {@link GrailsUnitTestCase#enableCascadingValidation()}.
     */
    void testCascadingValidation() {
        def testCase = new TestUnitTestCase()
        testCase.setUp()
        testCase.testCascadingValidation()
        testCase.tearDown()
    }

    void testMockDynamicMethodsWithInstanceList() {
        def testCase = new TestUnitTestCase()
        testCase.setUp()

        testCase.testMockInstances1()

        testCase.tearDown()

        testCase.testMockInstances2()
    }

    /**
     * Tests that the HTML codec can be loaded using the "loadCodec()"
     * method such that the "encodeAsHTML()" and "decodeHTML()" are
     * available on strings.
     */
    void testLoadCodec() {
        def testCase = new TestUnitTestCase()
        testCase.setUp()
        testCase.testLoadHtmlCodec()
        testCase.tearDown()
    }

    void testConverters() {
        def testCase = new TestUnitTestCase()
        testCase.setUp()
        testCase.testAsJson()
        testCase.tearDown()

        testCase.setUp()
        testCase.testAsXml()
        testCase.tearDown()
    }
}

class TestUnitTestCase extends GrailsUnitTestCase {

    void testMockInstances1() {
        mockDomain(GrailsUnitTestClass)

        assertEquals 0, GrailsUnitTestClass.list().size()
        assertNull GrailsUnitTestClass.findByName("foo")
    }
    void testMockInstances2() {
        mockDomain(GrailsUnitTestClass, [new GrailsUnitTestClass(id:1, name:"foo")])

        assertEquals 1, GrailsUnitTestClass.list().size()
        assertNotNull GrailsUnitTestClass.findByName("foo")
    }
    void testConstraints() {
        mockForConstraintsTests(GrailsUnitTestClass)

        def obj = new GrailsUnitTestClass()
        obj.validate()
        assertEquals 3, obj.errors.errorCount

        obj.child = new GrailsUnitChildClass(memo: "Test")
        obj.age = 22
        obj.validate()
        assertEquals 1, obj.errors.errorCount
    }

    void testMockGroovy1() {
        def mocker = mockFor(ClassToMock)
        mocker.demand.foo() {s -> 1 }
        def obj = mocker.createMock()
        obj.foo("")
        mocker.verify()
    }

    void testMockGroovy2() {
        def mocker = mockFor(ClassToMock)
        mocker.demand.foo() {s -> 1 }
        def obj = mocker.createMock()
        obj.foo("")
        mocker.verify()
    }

    void testMockInterface1() {
        def mocker = mockFor(InterfaceToMock)
        mocker.demand.foo() {s -> 1 }
        def obj = mocker.createMock()
        obj.foo("")
        mocker.verify()
    }

    void testMockInterface2() {
        def mocker = mockFor(InterfaceToMock)
        mocker.demand.foo() {s -> 1 }
        def obj = mocker.createMock()
        obj.foo("")
        mocker.verify()
    }

    void testCascadingValidation() {
        mockDomain(GrailsUnitTestClass)
        mockDomain(GrailsUnitChildClass)
        enableCascadingValidation()

        def d = new GrailsUnitTestClass()
        assertFalse d.validate()

        d.name = "Peter"
        d.age = 22
        assertFalse d.validate()

        def child = new GrailsUnitChildClass()
        d.child = child
        assertFalse d.validate()
        assertTrue d.validate(deepValidate: false)

        d.child.memo = "Something"
        assertTrue d.validate()
        assertTrue d.validate(deepValidate: false)
    }

    void testLoadHtmlCodec() {
        loadCodec(HTMLCodec)
        assertEquals "&lt;p&gt;Q &amp; A&lt;/p&gt;", "<p>Q & A</p>".encodeAsHTML()
        assertEquals "<p>Q & A</p>", "&lt;p&gt;Q &amp; A&lt;/p&gt;".decodeHTML()

        // Do a test with GStrings.
        def tag = "div"
        assertEquals "&lt;div&gt;Q &amp; A&lt;/div&gt;", "<${tag}>Q & A</${tag}>".encodeAsHTML()
        assertEquals "<div>Q & A</div>", "&lt;${tag}&gt;Q &amp; A&lt;/${tag}&gt;".decodeHTML()
    }

    void testAsJson() {
        mockController(GrailsUnitController)

        def controller = new GrailsUnitController()
        controller.listJson()

        def json = JSON.parse(controller.response.contentAsString)
        assertEquals 3, json.size()
        assertEquals 6, json[1].qty
        assertEquals "Chair", json[2].name
    }

    void testAsXml() {
        mockDomain(GrailsUnitTestClass, [
                new GrailsUnitTestClass(
                        id: 555,
                        name: "The Parent",
                        age: 46,
                        child: new GrailsUnitChildClass(id: 673, memo: "Test"))
        ])
        mockController(GrailsUnitController)

        def controller = new GrailsUnitController()
        controller.showXml()

        def xml = XML.parse(controller.response.contentAsString)
        assertEquals "46", xml.age.text()
        assertEquals "673", xml.child.id.text()
        assertEquals "The Parent", xml.name.text() 
    }

    void testFailingCase() {
        mockDomain(GrailsUnitTestPerson)

        def testDomain = new GrailsUnitTestPerson()
        testDomain.name = "Rob"

        // validation should fail as mandatory field is missing
        assertFalse testDomain.hasErrors()
        assertFalse testDomain.validate()
        assertTrue testDomain.hasErrors()
    }

    void testSuccessCase() {
        mockDomain(GrailsUnitTestPerson)

        def testDomain = new GrailsUnitTestPerson()
        testDomain.name = "Rob"
        testDomain.city = "London"

        // validation should pass
        assertFalse testDomain.hasErrors()
        assertTrue testDomain.validate()
        assertFalse testDomain.hasErrors()
    }
}

class GrailsUnitTestClass {
    Long id
    Long version
    String name
    int age
    GrailsUnitChildClass child

    void testLogging() {
        log.fatal "Test fatal"
        log.fatal "Test fatal with exception", new Exception("something went seriously wrong!")
        log.error "Test error"
        log.error "Test error with exception", new Exception("something went wrong!")
        log.warn "Test warning"
        log.warn "Test warning with exception", new Exception("something went wrong!")
        log.info "Test info message"
        log.info "Test info message with exception", new Exception("something went wrong!")
        log.debug "Test debug"
        log.debug "Test debug with exception", new Exception("something went wrong!")
        log.trace "Test trace"
        log.trace "Test trace with exception", new Exception("something went wrong!")
    }

    void testConfig() {
        assert ConfigurationHolder.config.foo.bar == "good"
    }

    static constraints = {
        name(nullable: false, blank: false)
        age(range: 10..50)
    }
}

class GrailsUnitChildClass {
    Long id
    Long version
    String memo

    static belongsTo = GrailsUnitTestClass
}

class GrailsUnitTestPerson {
    Long id
    Long version
    String name
    String city

    int hashCode() {
        return name.hashCode()
    }

    boolean equals(Object obj) {
        return obj instanceof GrailsUnitTestPerson && name == obj?.name
    }
}

class ClassToMock {
  def foo(s) { 1 }
}

interface InterfaceToMock {
  int foo(String s)
}

class GrailsUnitController {
    def listJson = {
        def items = [
                [ qty: 10, name: "Orange", type: "Fruit" ],
                [ qty: 6, name: "Apple", type: "Fruit" ],
                [ qty: 2, name: "Chair", type: "Furniture" ]
        ]
        render items as JSON
    }

    def showXml = {
        render GrailsUnitTestClass.get(555) as XML
    }
}