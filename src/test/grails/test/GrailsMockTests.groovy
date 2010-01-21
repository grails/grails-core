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

    void setUp() {
        super.setUp()
        this.savedMetaClass = GrailsMockCollaborator.metaClass

        // Create a new EMC for the class and attach it.
        def emc = new ExpandoMetaClass(GrailsMockCollaborator, true, true)
        emc.initialize()
        GroovySystem.metaClassRegistry.setMetaClass(GrailsMockCollaborator, emc)
    }

    void tearDown() {
        super.tearDown()

        // Restore the saved meta class.
        GroovySystem.metaClassRegistry.setMetaClass(GrailsMockCollaborator, this.savedMetaClass)
    }

    void testMethod() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.save(1..1) {-> return false }

        def testClass = new GrailsMockTestClass()
        testClass.collaborator = mockControl.createMock()

        assertEquals false, testClass.testMethod()

        mockControl.verify()
    }

    void testVerifyFails() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.save(2..2) {-> return false }

        def testClass = new GrailsMockTestClass()
        testClass.collaborator = mockControl.createMock()

        assertEquals false, testClass.testMethod()

        shouldFail(AssertionFailedError) {
            mockControl.verify()
        }
    }

    void testTooManyCalls() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.save(1..1) {->
            return false
        }

        def testClass = new GrailsMockTestClass()
        testClass.collaborator = mockControl.createMock()

        shouldFail(AssertionFailedError) {
            testClass.testMethod2()
        }
    }

    void testMissingMethod() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.merge(1..1) {->
            return false
        }

        def testClass = new GrailsMockTestClass()
        testClass.collaborator = mockControl.createMock()

        shouldFail(AssertionFailedError) {
            testClass.testDynamicMethod()
        }
    }

    void testOverridingMetaClassMethod() {
        GrailsMockCollaborator.metaClass.update = {-> return "Failed!"}

        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.update() {-> return "Success!"}

        def testClass = new GrailsMockTestClass()
        testClass.collaborator = mockControl.createMock()

        assertEquals "Success!", testClass.testDynamicMethod()
    }

    void testStaticMethod() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.static.get(1..1) { assert it == 5; return "Success!" }

        def testClass = new GrailsMockTestClass()
        assertEquals "Success!", testClass.testStaticMethod()

        mockControl.verify()
    }

    void testStaticVerifyFails() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.static.get(2..2) { assert it == 5; return "Success!" }

        def testClass = new GrailsMockTestClass()
        assertEquals "Success!", testClass.testStaticMethod()

        shouldFail(AssertionFailedError) {
            mockControl.verify()
        }
    }

    void testStaticTooManyCalls() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.static.get(1..1) { assert it == 5; return "Success!" }

        def testClass = new GrailsMockTestClass()
        shouldFail(AssertionFailedError) {
            testClass.testStaticMethod2()
        }
    }

    void testStaticMissingMethod() {
        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.static.find(1..1) { assert it == 5; return "Success!" }

        def testClass = new GrailsMockTestClass()
        shouldFail(MissingMethodException) {
            testClass.testStaticMethod()
        }
    }

    void testOverridingStaticMetaClassMethod() {
        GrailsMockCollaborator.metaClass.static.findByNothing = {-> return "Failed!"}

        def mockControl = new GrailsMock(GrailsMockCollaborator)
        mockControl.demand.static.findByNothing() {-> return "Success!"}


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
        mockControl.demand.multiMethod(1..1) {-> return "dynamic" }
        mockControl.demand.multiMethod(1..1) { String str ->
            assertEquals "Test string", str
            return "dynamic"
        }
        mockControl.demand.multiMethod(1..1) { String str, Map map ->
            assertEquals "Test string", str
            assertNotNull map
            assertEquals 1, map["arg1"]
            assertEquals 2, map["arg2"]
            return "dynamic"
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
            return "dynamic"
        }

        def testClass = new GrailsMockTestClass()
        testClass.collaborator = mockControl.createMock()

        assertEquals "dynamic", testClass.testNullArgument()
    }
}

class GrailsMockTestClass {
    GrailsMockCollaborator collaborator

    boolean testMethod() {
        return this.collaborator.save()
    }

    String testDynamicMethod() {
        return this.collaborator.update()
    }

    boolean testMethod2() {
        this.testMethod()
        return this.testMethod()
    }

    String testStaticMethod() {
        return GrailsMockCollaborator.get(5)
    }

    String testStaticMethod2() {
        testStaticMethod()
        return testStaticMethod()
    }

    String testDynamicStaticMethod() {
        return GrailsMockCollaborator.findByNothing()
    }

    void testCorrectOrder() {
        GrailsMockCollaborator.get(5)
        this.collaborator.update()
    }

    void testWrongOrder() {
        this.collaborator.update()
        GrailsMockCollaborator.get(5)
    }

    String testNullArgument() {
        return this.collaborator.multiMethod(null)
    }

    List testMultiMethod() {
        List methodReturns = []
        methodReturns << this.collaborator.multiMethod()
        methodReturns << this.collaborator.multiMethod("Test string")
        methodReturns << this.collaborator.multiMethod("Test string", [arg1: 1, arg2: 2])
        return methodReturns
    }
}

class GrailsMockCollaborator {
    def save() {
        return true
    }

    String multiMethod() {
        return "static"
    }

    String multiMethod(String str) {
        return "static"
    }

    String multiMethod(String str, Map map) {
        return "static"
    }
}
