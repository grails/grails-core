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

import junit.framework.AssertionFailedError

/**
 * Test case for {@link GrailsMock}.
 */
class GrailsMockTests extends GroovyTestCase {

    def savedMetaClass

    protected void setUp() {
        super.setUp()
        savedMetaClass = GrailsMockCollaborator.metaClass

        // Create a new EMC for the class and attach it.
        def emc = new ExpandoMetaClass(GrailsMockCollaborator, true, true)
        emc.initialize()
        GroovySystem.metaClassRegistry.setMetaClass(GrailsMockCollaborator, emc)
    }

    protected void tearDown() {
        super.tearDown()

        // Restore the saved meta class.
        GroovySystem.metaClassRegistry.setMetaClass(GrailsMockCollaborator, savedMetaClass)
    }

    void testMethod() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.save(1..1) { -> false }

        def testClass = new GrailsMockTestClass()
        testClass.collaborator = mockControl.createMock()

        assertFalse testClass.testMethod()

        mockControl.verify()
    }

    void testVerifyFails() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.save(2..2) { -> false }

        def testClass = new GrailsMockTestClass()
        testClass.collaborator = mockControl.createMock()

        assertFalse testClass.testMethod()

        shouldFail(AssertionFailedError) {
            mockControl.verify()
        }
    }

    void testTooManyCalls() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.save(1..1) { -> false }

        def testClass = new GrailsMockTestClass()
        testClass.collaborator = mockControl.createMock()

        shouldFail(AssertionFailedError) {
            testClass.testMethod2()
        }
    }

    void testMissingMethod() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.merge(1..1) { -> false }

        def testClass = new GrailsMockTestClass()
        testClass.collaborator = mockControl.createMock()

        shouldFail(AssertionFailedError) {
            testClass.testDynamicMethod()
        }
    }

    void testOverridingMetaClassMethod() {
        GrailsMockCollaborator.metaClass.update = { -> "Failed!"}

        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.update() { -> "Success!"}

        def testClass = new GrailsMockTestClass()
        testClass.collaborator = mockControl.createMock()

        assertEquals "Success!", testClass.testDynamicMethod()
    }

    void testStaticMethod() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.static.get(1..1) { assertEquals 5, it; "Success!" }

        def testClass = new GrailsMockTestClass()
        assertEquals "Success!", testClass.testStaticMethod()

        mockControl.verify()
    }

    void testStaticVerifyFails() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.static.get(2..2) { assertEquals 5, it; "Success!" }

        def testClass = new GrailsMockTestClass()
        assertEquals "Success!", testClass.testStaticMethod()

        shouldFail(AssertionFailedError) {
            mockControl.verify()
        }
    }

    void testStaticTooManyCalls() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.static.get(1..1) { assertEquals 5, it; "Success!" }

        def testClass = new GrailsMockTestClass()
        shouldFail(AssertionFailedError) {
            testClass.testStaticMethod2()
        }
    }

    void testStaticMissingMethod() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.static.find(1..1) { assertEquals 5, it; "Success!" }

        def testClass = new GrailsMockTestClass()
        shouldFail(MissingMethodException) {
            testClass.testStaticMethod()
        }
    }

    void testOverridingStaticMetaClassMethod() {
        GrailsMockCollaborator.metaClass.static.findByNothing = { -> "Failed!"}

        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.static.findByNothing() { -> "Success!"}

        def testClass = new GrailsMockTestClass()
        assertEquals "Success!", testClass.testDynamicStaticMethod()
    }

    void testStrictOrdering() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.static.get(1..1) { "OK" }
        mockControl.demand.update(1..1) { "OK" }

        def testClass = new GrailsMockTestClass()
        testClass.collaborator = mockControl.createMock()
        testClass.testCorrectOrder()
        mockControl.verify()

        mockControl.demand.static.get(1..1) { "OK" }
        mockControl.demand.update(1..1) { "OK" }

        shouldFail(AssertionFailedError) {
            testClass.testWrongOrder()
        }
    }

    void testLooseOrdering() {
        def mockControl = new GrailsMock(GrailsMockCollaborator, true)
        mockControl.demand.static.get(1..1) { "OK" }
        mockControl.demand.update(1..1) { "OK" }

        def testClass = new GrailsMockTestClass()
        testClass.collaborator = mockControl.createMock()
        testClass.testCorrectOrder()
        mockControl.verify()

        mockControl.demand.static.get(1..1) { "OK" }
        mockControl.demand.update(1..1) { "OK" }

        testClass.testWrongOrder()
        mockControl.verify()
    }

    /**
     * Tests that the argument matching for demanded methods works
     * properly.
     */
    void testArgumentMatching() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.multiMethod(1..1) { -> "dynamic" }
        mockControl.demand.multiMethod(1..1) { String str ->
            assertEquals "Test string", str
            "dynamic"
        }
        mockControl.demand.multiMethod(1..1) { String str, Map map ->
            assertEquals "Test string", str
            assertNotNull map
            assertEquals 1, map["arg1"]
            assertEquals 2, map["arg2"]
            "dynamic"
        }

        def testClass = new GrailsMockTestClass()
        testClass.collaborator = mockControl.createMock()

        def retval = testClass.testMultiMethod()

        // Check that the dynamic methods were called rather than the
        // ones that are statically defined on the collaborator.
        assertTrue retval.every { it == "dynamic" }
    }

    /**
     * GRAILS-3508
     *
     * Tests that the mock works OK if the mocked method is called with
     * any <ocde>null</code> arguments.
     */
    void testNullArguments() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.multiMethod(1..1) { String str ->
            assertNull str
            "dynamic"
        }

        def testClass = new GrailsMockTestClass()
        testClass.collaborator = mockControl.createMock()

        assertEquals "dynamic", testClass.testNullArgument()
    }

    void testEmptyArrayArguments() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.testEmptyArrayArguments(1..1) { String str1, Object[] args, String str2 ->
            "dynamic"
        }

        def testClass = new GrailsMockTestClass()
        testClass.collaborator = mockControl.createMock()

        assertEquals "dynamic", testClass.testEmptyArrayArguments()
    }

    /**
     * Tests that mocking an interface works.
     */
    void testInterface() {
        def mockControl = new GrailsMock(GrailsMockInterface)
        mockControl.demand.testMethod(1..1) { String name, int qty ->
            assertEquals "brussels", name
            assertEquals 5, qty
            "brussels-5"
        }

        def testClass = new GrailsMockTestClass()
        testClass.gmi = mockControl.createMock()

        assertEquals "brussels-5", testClass.testInterfaceCollaborator()
    }
}

class GrailsMockTestClass {
    GrailsMockCollaborator collaborator
    GrailsMockInterface gmi

    boolean testMethod() { collaborator.save() }

    String testDynamicMethod() { collaborator.update() }

    boolean testMethod2() { testMethod(); testMethod() }

    String testStaticMethod() { GrailsMockCollaborator.get(5) }

    String testStaticMethod2() { testStaticMethod(); testStaticMethod() }

    String testDynamicStaticMethod() {
        GrailsMockCollaborator.findByNothing()
    }

    void testCorrectOrder() {
        GrailsMockCollaborator.get(5)
        collaborator.update()
    }

    void testWrongOrder() {
        collaborator.update()
        GrailsMockCollaborator.get(5)
    }

    String testNullArgument() {
        collaborator.multiMethod(null)
    }

    String testEmptyArrayArguments() {
        collaborator.testEmptyArrayArguments('abc', [] as Object[], 'def')
    }

    String testInterfaceCollaborator() {
        gmi.testMethod("brussels", 5)
    }

    List testMultiMethod() {
        [collaborator.multiMethod(),
         collaborator.multiMethod("Test string"),
         collaborator.multiMethod("Test string", [arg1: 1, arg2: 2])]
    }
}

class GrailsMockCollaborator {
    def save() { true }

    String multiMethod() { "static" }

    String multiMethod(String str) { "static" }

    String multiMethod(String str, Map map) { "static" }

    String someMethod(String str1, Object[] args, String str2) { 'static' }
}

interface GrailsMockInterface {
    String testMethod(String name, int quantity)
}

class GrailsMockImpl implements GrailsMockInterface {
    String testMethod(String name, int quantity) { name * quantity }
}
