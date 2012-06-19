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

import org.springframework.validation.Errors

class MockUtilsDeleteDomainTests extends GroovyTestCase {

    private @Lazy MetaTestHelper metaTestHelper = {
        MetaTestHelper result = new MetaTestHelper()
        result.classesUnderTest = [TestDomain]
        result.classesToReset = [Errors]
        return result
    }()

    private Map errorsMap

    void setUp() {
        metaTestHelper.setUp()
        errorsMap = new IdentityHashMap()
    }

    void tearDown() {
        metaTestHelper.tearDown()
        MockUtils.resetIds()
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

    void testDeleteEvents() {
        def domain = new TestDomain(name: "Alice Doe", country: "US", age: 35, title: "Ms.")

        MockUtils.mockDomain(TestDomain, errorsMap, [domain])

        assertEquals 0, domain.beforeDeleted
        assertEquals 0, domain.afterDeleted

        domain.delete()

        assertEquals 'beforeDeleted was not called', 1, domain.beforeDeleted
        assertEquals 'afterDeleted was not called', 1, domain.afterDeleted
    }

    void testDeleteEventsWithClosureHandlers() {
        def domain = new TestDomainWithClosureEventHandlers(name: "Alice Doe")

        MockUtils.mockDomain(TestDomainWithClosureEventHandlers, errorsMap, [domain])

        assertEquals 0, domain.beforeDeleted
        assertEquals 0, domain.afterDeleted

        domain.delete()

        assertEquals 'beforeDeleted was not called', 1, domain.beforeDeleted
        assertEquals 'afterDeleted was not called', 1, domain.afterDeleted
    }
}
