/*
 * Copyright 2008 the original author or authors.
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
package org.grails.plugins.web.filters
/**
 * Test case for {@link org.grails.plugins.web.filters.FilterConfig}.
 *
 * @author pledbrook
 */
class FilterConfigTests extends GroovyTestCase {
    private static final int INT_PROP_VALUE = 1000
    private static final String STRING_PROP_VALUE = 'Test property'

    void testPropertyMissing() {
        def mockDefinition = new MockFiltersDefinition()
        def testFilterConfig = new FilterConfig(name: 'Test filter', initialised: true, filtersDefinition: mockDefinition)

        // Try property 1 to start with.
        assert testFilterConfig.propOne == INT_PROP_VALUE

        // Now try it a couple more times to make sure that the metaclass
        // property has been registered correctly.
        assert testFilterConfig.propOne == INT_PROP_VALUE
        assert testFilterConfig.propOne == INT_PROP_VALUE

        // Now try with number 2.
        assert testFilterConfig.prop2 == STRING_PROP_VALUE
        assert testFilterConfig.prop2 == STRING_PROP_VALUE
        assert testFilterConfig.prop2 == STRING_PROP_VALUE

        // And now make sure we can still access property 1.
        assert testFilterConfig.propOne == INT_PROP_VALUE

        // Any other property access should result in this exception.
        shouldFail(MissingPropertyException) {
            testFilterConfig.unknownProperty
        }
    }

    void testMethodMissing() {
        def mockDefinition = new MockFiltersDefinition()
        def testFilterConfig = new MethodMissingCountingFilterConfig(name: 'Test filter', initialised: true, filtersDefinition: mockDefinition)

        // Try the 'run' method first.
        testFilterConfig.run()
        assert mockDefinition.runCalled

        assert testFilterConfig.methodMissingCounter == 1

        testFilterConfig.methodMissingCounter = 0

        // Now try it a couple more times to make sure that the metaclass
        // method has been registered correctly.
        mockDefinition.reset()
        testFilterConfig.run()
        assert mockDefinition.runCalled

        mockDefinition.reset()
        testFilterConfig.run()
        assert mockDefinition.runCalled

        assert testFilterConfig.methodMissingCounter == 0

        // Now try with the next method.
        mockDefinition.reset()
        mockDefinition.returnValue = 6342
        assert testFilterConfig.generateNumber() == 6342
        assert mockDefinition.generateNumberCalled == true

        testFilterConfig.methodMissingCounter = 0

        mockDefinition.reset()
        mockDefinition.returnValue = '101'
        assert testFilterConfig.generateNumber() == '101'
        assert mockDefinition.generateNumberCalled == true

        mockDefinition.reset()
        mockDefinition.returnValue = 10.232
        assert testFilterConfig.generateNumber() == 10.232
        assert mockDefinition.generateNumberCalled == true

        assert testFilterConfig.methodMissingCounter == 0

        // Now for a method with arguments.
        mockDefinition.reset()
        mockDefinition.expectedStringArg = 'Test'
        mockDefinition.expectedIntArg = 1000
        testFilterConfig.checkArgs('Test', 1000)
        assert mockDefinition.checkArgsCalled

        testFilterConfig.methodMissingCounter = 0

        mockDefinition.reset()
        mockDefinition.expectedStringArg = 'Test two'
        mockDefinition.expectedIntArg = 2000
        testFilterConfig.checkArgs('Test two', 2000)
        assert mockDefinition.checkArgsCalled

        mockDefinition.reset()
        mockDefinition.expectedStringArg = 'Apples'
        mockDefinition.expectedIntArg = -3423
        testFilterConfig.checkArgs('Apples', -3423)
        assert mockDefinition.checkArgsCalled

        assert testFilterConfig.methodMissingCounter == 0

        mockDefinition.reset()
        mockDefinition.expectedStringArg = 'Oranges'
        mockDefinition.expectedDoubleArg = 1.23d
        testFilterConfig.checkArgs('Oranges', 1.23d)
        assert mockDefinition.checkArgsCalled

        assert testFilterConfig.methodMissingCounter == 1

        testFilterConfig.methodMissingCounter = 0

        mockDefinition.reset()
        mockDefinition.expectedStringArg = 'Pears'
        mockDefinition.expectedDoubleArg = 2.56d
        testFilterConfig.checkArgs('Pears', 2.56d)
        assert mockDefinition.checkArgsCalled

        assert testFilterConfig.methodMissingCounter == 0

        // A method that takes a list as an argument.
        mockDefinition.reset()
        assert testFilterConfig.sum([1, 2, 3, 4]) == 10
        assert mockDefinition.sumCalled

        testFilterConfig.methodMissingCounter = 0

        mockDefinition.reset()
        assert testFilterConfig.sum([4, 5, 1, 10]) == 20
        assert mockDefinition.sumCalled

        mockDefinition.reset()
        assert testFilterConfig.sum([12, 26, 3, 41]) == 82
        assert mockDefinition.sumCalled

        assert testFilterConfig.methodMissingCounter == 0

        // And now make sure the 'run' method is still available.
        mockDefinition.reset()
        testFilterConfig.run()
        assert mockDefinition.runCalled

        // Any other property access should result in this exception.
        shouldFail(MissingMethodException) {
            testFilterConfig.unknownMethod(23)
        }
    }

    // test for GRAILS-9050
    void testMethodMissingForTwoFilters() {
        def mockDefinition = new MockFiltersDefinition()
        def testFilterConfig = new MethodMissingCountingFilterConfig(name: 'Test filter', initialised: true, filtersDefinition: mockDefinition)

        def mockDefinition2 = new MockFiltersDefinition2()
        def testFilterConfig2 = new MethodMissingCountingFilterConfig(name: 'Test filter 2', initialised: true, filtersDefinition: mockDefinition2)

        mockDefinition2.reset()
        assert testFilterConfig2.hello() == "Hello from definition 2"
        assert mockDefinition2.helloCalled

        mockDefinition.reset()
        assert testFilterConfig.hello() == "Hello from definition 1"
        assert mockDefinition.helloCalled
    }
}

class MethodMissingCountingFilterConfig extends FilterConfig {
    int methodMissingCounter=0

    def methodMissing(String methodName, args) {
        methodMissingCounter++
        super.methodMissing(methodName, args)
    }
}

class MockFiltersDefinition {
    def propOne = 1000
    def prop2 = 'Test property'
    def runCalled
    def generateNumberCalled
    def checkArgsCalled
    def sumCalled
    def expectedStringArg
    def expectedIntArg
    def expectedDoubleArg
    def returnValue
    def helloCalled

    MockFiltersDefinition() {
        reset()
    }

    void run() {
        runCalled = true
    }

    def generateNumber() {
        generateNumberCalled = true
        return returnValue
    }

    void checkArgs(String arg1, int arg2) {
        checkArgsCalled = true
        assert arg1 == expectedStringArg
        assert arg2 == expectedIntArg
    }

    void checkArgs(String arg1, double arg2) {
        checkArgsCalled = true
        assert arg1 == expectedStringArg
        assert arg2 == expectedDoubleArg
    }

    def sum(items) {
        sumCalled = true
        items.sum()
    }

    def hello() {
        helloCalled=true
        "Hello from definition 1"
    }

    void reset() {
        runCalled = false
        generateNumberCalled = false
        checkArgsCalled = false
        sumCalled = false
        expectedStringArg = null
        expectedIntArg = null
        expectedDoubleArg = null
        returnValue = null
        helloCalled = false
    }
}

class MockFiltersDefinition2 extends MockFiltersDefinition {

    def hello() {
        helloCalled=true
        "Hello from definition 2"
    }
}