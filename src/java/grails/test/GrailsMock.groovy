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

import groovy.mock.interceptor.Demand
import groovy.mock.interceptor.StrictExpectation
import groovy.mock.interceptor.LooseExpectation

/**
 * <p>Provides similar behaviour to MockFor and StubFor, but uses
 * ExpandoMetaClass to mock the methods. This means that it fits much
 * better with the rest of the Grails unit testing framework than the
 * classes it replaces.</p>
 * <p>Instances of this class support the exact same syntax as MockFor/
 * StubFor with the addition of explicit support for static methods.
 * For example:</p>
 * <pre>
 *    def mockControl = new GrailsMock(MyDomainClass)
 *    mockControl.demand.save() {-> return true}           // Instance method
 *    mockControl.demand.static.get() {id -> return null}  // Static method
 *    ...
 *    mockControl.verify()
 * </pre>
 * <p>You can even create a mock instance of the target class by calling
 * the {@link GrailsMock#createMock()} method.</p>
 * <p>Note that you have to be careful when using this class directly
 * because it uses ExpandoMetaClass to override methods. Any of the
 * demanded methods will stay on the class for the life of the VM unless
 * you either override them or save the previous meta-class and restore
 * it at the end of the test. This is why you should use the
 * {@link GrailsUnitTestCase#mockFor(Class)} method instead, since it
 * handles the meta-class management automatically.</p>
 */
class GrailsMock {
    Class mockedClass
    DemandProxy demand

    /**
     * Creates a new strict mock for the given class.
     * @param clazz The class to mock.
     */
    GrailsMock(Class clazz) {
        this(clazz, false)
    }

    /**
     * Creates a new mock for the given class.
     * @param clazz The class to mock.
     * @param loose If <code>true</code>, a loose-expecation mock is
     * created, otherwise the mock is strict.
     */
    GrailsMock(Class clazz, boolean loose) {
        this.mockedClass = clazz
        this.demand = new DemandProxy(clazz, loose)
    }

    /**
     * Returns a "demand" object that supports the "control.demand.myMethod() {}"
     * syntax.
     */
    DemandProxy getDemand() {
        return this.demand
    }

    /**
     * Creates a mock instance that can be passed as a collaborator to
     * classes under test.
     */
    def createMock() {
        // Create the instance of the required type.
        def mock = [:].asType(this.mockedClass)

        // If we're mocking a class rather than an interface, we don't
        // want the real methods invoked at all. So, we override the
        // "invokeMethod()" method so that if the method exists in the
        // ExpandoMetaClass we call that one, otherwise we forward it
        // to the expectation object which will throw an assertion
        // failure.
        mock.metaClass.invokeMethod = { String name, Object[] args ->
            // Find an expando method with the same signature as the
            // one being invoked.
			def paramTypes = []
			args.each {
				if(it) {
				    paramTypes << it.getClass()
				} else {
					paramTypes << null
				}
			}
            def method = delegate.metaClass.expandoMethods.find { MetaMethod m ->
                // First check the name
                m.name == name &&
                        // Then the number of method arguments
                        m.parameterTypes.size() == paramTypes.size() &&
                        // And finally the argument types
                        (0..<m.parameterTypes.size()).every { n ->
                            paramTypes[n] == null || m.parameterTypes[n].cachedClass.isAssignableFrom(paramTypes[n])
                        }
            }

            if (method) {
                // We found an expando method with the required signature,
                // so just call it.
                return method.doMethodInvoke(delegate, args)
            }
            else {
                // No expando method found so pass the invocation on
                // to the expectation object. This should throw an
                // assertion error.
                demand.expectation.match(name)
            }
        }
        return mock
    }

    /**
     * Checks that all the expected methods have been called. Throws an
     * assertion failure if any expected method call has not occurred.
     */
    def verify() {
        this.demand.expectation.verify()
    }
}

/**
 * A class that keeps track of demands and expectations for a particular
 * Grails mock.
 */
class DemandProxy {
    Class mockedClass
    Demand demand
    Object expectation
    boolean isStatic

    DemandProxy(Class mockedClass, boolean loose) {
        this.mockedClass = mockedClass
        this.demand = new Demand()
        this.isStatic = false

        if (loose) {
            this.expectation = new LooseExpectation(this.demand)
        }
        else {
            this.expectation = new StrictExpectation(this.demand)
        }
    }

    def invokeMethod(String methodName, Object args) {
        this.demand.invokeMethod(methodName, args)

        def c = new MockClosureProxy(args[-1], methodName, this.expectation)
        if (this.isStatic) {
            this.mockedClass.metaClass.static."${methodName}" = c
        }
        else {
            this.mockedClass.metaClass."${methodName}" = c
        }
    }

    def getStatic() {
        this.isStatic = true
        return this
    }
}
